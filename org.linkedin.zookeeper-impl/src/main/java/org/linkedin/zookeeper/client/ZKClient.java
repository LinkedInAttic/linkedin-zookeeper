/*
 * Copyright 2010-2010 LinkedIn, Inc
 * Portions Copyright 2013 Yan Pujante
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.linkedin.zookeeper.client;

import org.linkedin.util.lang.LangUtils;
import org.slf4j.Logger;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.linkedin.util.annotations.Initializer;
import org.linkedin.util.clock.Clock;
import org.linkedin.util.clock.SystemClock;
import org.linkedin.util.clock.Timespan;
import org.linkedin.util.concurrent.ConcurrentUtils;
import org.linkedin.util.exceptions.InternalException;
import org.linkedin.util.lifecycle.Destroyable;
import org.linkedin.util.lifecycle.Startable;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.TimeoutException;

/**
 * @author ypujante@linkedin.com
 */
public class ZKClient extends AbstractZKClient implements Startable, Destroyable
{
  public static final String MODULE = ZKClient.class.getName();
  public static final Logger log = org.slf4j.LoggerFactory.getLogger(MODULE);

  public static enum State
  {
    NONE,
    CONNECTING,
    CONNECTED,
    RECONNECTING
  }

  private IZooKeeper _zk;
  private Object _uniqueID;

  private volatile Clock _clock = SystemClock.instance();
  private volatile Timespan _reconnectTimeout = Timespan.parse("20s");

  // the content of the set is never changed (in other words it is an immutable set!)
  private Set<LifecycleListener> _listeners = null;

  private final Object _lock = new Object();

  private StateChangeDispatcher _stateChangeDispatcher = null;
  private ExpiredSessionRecovery _expiredSessionRecovery = null;

  private volatile State _state = State.NONE;

  private final IZooKeeperFactory _factory;

  /**
   * The purpose of this class is to ignore events if they are coming from a ZooKeeper instance
   * that is not being used anymore in order to not step on each others feet! It is unclear
   * whether it can happen after an Expired event and depends on how ZooKeeper is actually
   * implemented, thus I prefer to be more careful about it.
   */
  private class UniqueWatcher implements Watcher
  {
    private final Object _uniqueID;

    private UniqueWatcher(Object uniqueID)
    {
      _uniqueID = uniqueID;
    }

    @Override
    public void process(WatchedEvent event)
    {
      synchronized(_lock)
      {
        if(LangUtils.isEqual(_uniqueID, ZKClient.this._uniqueID))
          processWatchedEvent(event);
        else
          log.warn("Received an event on a different zk instance... (ignoring)");
      }
    }
  }

  private static class StateChangeDispatcher extends Thread
  {
    public final String MODULE = StateChangeDispatcher.class.getName();
    public final Logger log = org.slf4j.LoggerFactory.getLogger(MODULE);

    private static enum DispatchEventType
    {
      CONNECTED,
      DISCONNECTED
    }

    private static class DispatchEvent
    {
      private final Collection<LifecycleListener> _listeners;
      private final DispatchEventType _dispatchEventType;

      private DispatchEvent(Collection<LifecycleListener> listeners,
                            DispatchEventType dispatchEventType)
      {
        _listeners = listeners;
        _dispatchEventType = dispatchEventType;
      }

      private Collection<LifecycleListener> getListeners()
      {
        return _listeners;
      }

      private DispatchEventType getDispatchEventType()
      {
        return _dispatchEventType;
      }
    }

    private volatile boolean _running = true;

    private final Queue<DispatchEvent> _events = new LinkedList<DispatchEvent>();

    @Override
    public void run()
    {
      log.info("Starting StateChangeDispatcher");

      while(_running)
      {
        DispatchEvent dispatchEvent = null;

        synchronized(_events)
        {
          while(_running && _events.isEmpty())
          {
            try
            {
              _events.wait();
            }
            catch(InterruptedException e)
            {
              if(log.isDebugEnabled())
                log.debug("ignoring exception", e);
            }
          }

          if(_running)
            dispatchEvent = _events.poll();

        } // end of synchronized

        // we don't want to call the listener(s) in the synchronized section!
        if(dispatchEvent != null)
        {
          if(dispatchEvent.getListeners() != null)
          {
            for(LifecycleListener listener : dispatchEvent.getListeners())
            {
              try
              {
                switch(dispatchEvent.getDispatchEventType())
                {
                  case CONNECTED:
                    listener.onConnected();
                    break;

                  case DISCONNECTED:
                    listener.onDisconnected();
                    break;

                  default:
                    throw new RuntimeException("not reached");
                }
              }
              catch(Throwable e)
              {
                log.warn("Exception while executing listener [" + listener + "] (ignored)", e);
              }
            }
          }
        }
      }

      log.info("StateChangeDispatcher terminated.");
    }

    public void end()
    {
      synchronized(_events)
      {
        _running = false;
        _events.notifyAll();
      }
    }

    public void addEvent(Collection<LifecycleListener> listeners,
                         ZKClient.State oldState,
                         ZKClient.State newState)
    {
      synchronized(_events)
      {
        if(log.isDebugEnabled())
          log.debug("addEvent: " + oldState + " => " + newState);

        if(_running)
        {
          if(newState == ZKClient.State.CONNECTED)
          {
            _events.add(new DispatchEvent(listeners, DispatchEventType.CONNECTED));
            _events.notifyAll();
          }
          else
          {
            if(oldState == ZKClient.State.CONNECTED)
            {
              _events.add(new DispatchEvent(listeners, DispatchEventType.DISCONNECTED));
              _events.notifyAll();
            }
          }
        }
      }
    }
  }

  private class ExpiredSessionRecovery extends Thread
  {
    @Override
    public void run()
    {
      log.info("Entering recovery mode");
      synchronized(_lock)
      {
        try
        {
          int count = 0;
          while(_state == ZKClient.State.NONE)
          {
            try
            {
              count++;
              log.warn("Recovery mode: trying to reconnect to zookeeper [" + count + "]");
              ZKClient.this.start();
            }
            catch(Throwable e)
            {
              log.warn("Recovery mode: reconnect attempt failed [" + count + "]... waiting for " + _reconnectTimeout, e);
              try
              {
                _lock.wait(_reconnectTimeout.getDurationInMilliseconds());
              }
              catch(InterruptedException e1)
              {
                throw new RuntimeException("Recovery mode: wait interrupted... bailing out", e1);
              }
            }
          }
        }
        finally
        {
          _expiredSessionRecovery = null;
          log.info("Exiting recovery mode.");
        }
      }
    }
  }

  /**
   * Constructor
   */
  public ZKClient(String connectString, Timespan sessionTimeout, Watcher watcher)
  {
    this(new ZooKeeperFactory(connectString, sessionTimeout, watcher));
  }

  /**
   * Constructor
   */
  public ZKClient(IZooKeeperFactory factory)
  {
    this(factory, null);
  }

  /**
   * Constructor
   */
  public ZKClient(IZooKeeperFactory factory, String chroot)
  {
    super(chroot);
    _factory = factory;
  }

  public State getZKClientState()
  {
    return _state;
  }

  public IZooKeeperFactory getFactory()
  {
    return _factory;
  }

  public Clock getClock()
  {
    return _clock;
  }

  @Initializer
  public void setClock(Clock clock)
  {
    _clock = clock;
  }

  public Timespan getReconnectTimeout()
  {
    return _reconnectTimeout;
  }

  @Initializer
  public void setReconnectTimeout(Timespan reconnectTimeout)
  {
    _reconnectTimeout = reconnectTimeout;
  }

  public Set<LifecycleListener> getListeners()
  {
    return _listeners;
  }

  /**
   * When the listener is set, it will receive the {@link LifecycleListener#onConnected()} callback
   * right away if ZooKeeper is already connected. This guarantees that you will never miss the
   * connected event irrelevant of whether you set the listener before or after calling
   * {@link #start()}.
   */
  @Override
  public void registerListener(LifecycleListener listener)
  {
    if(listener == null)
      throw new NullPointerException("listener is null");

    synchronized(_lock)
    {
      if(_listeners == null || !_listeners.contains(listener))
      {
        Set<LifecycleListener> listeners = new HashSet<LifecycleListener>();
        if(_listeners != null)
          listeners.addAll(_listeners);
        listeners.add(listener);
        _listeners = Collections.unmodifiableSet(listeners);

        if(_stateChangeDispatcher == null)
        {
          _stateChangeDispatcher = new StateChangeDispatcher();
          _stateChangeDispatcher.setDaemon(true);
          _stateChangeDispatcher.start();
        }

        if(_state == State.CONNECTED)
        {
          // since the listener is new and the client is already connected, we need to send
          // the connected event to this listener!
          _stateChangeDispatcher.addEvent(Arrays.asList(listener),
                                          null,
                                          State.CONNECTED);
        }
      }
    }
  }

  /**
   * Removes a listener previously set with {@link #registerListener(LifecycleListener)}
   *
   * @param listener
   */
  @Override
  public void removeListener(LifecycleListener listener)
  {
    synchronized(_lock)
    {
      if(_listeners != null && _listeners.contains(listener))
      {
        Set<LifecycleListener> listeners = new HashSet<LifecycleListener>(_listeners);
        listeners.remove(listener);

        // no more listeners... stopping the dispatcher
        if(listeners.size() == 0)
        {
          listeners = null;
          
          if(_stateChangeDispatcher != null)
          {
            _stateChangeDispatcher.end();
            _stateChangeDispatcher = null;
          }
        }

        if(listeners == null)
          _listeners = null;
        else
          _listeners = Collections.unmodifiableSet(listeners);
      }

    }
  }

  /**
   * @return a new client with the path that has been chrooted... meaning all paths will be relative
   *         to the path provided.
   */
  @Override
  public IZKClient chroot(String path)
  {
    return new ChrootedZKClient(this, adjustPath(path));
  }

  @Override
  public void start()
  {
    synchronized(_lock)
    {
      if(_state != State.NONE)
        throw new IllegalStateException("already started");

      if(log.isDebugEnabled())
        log.debug("Starting ZKClient");

      changeState(State.CONNECTING);

      try
      {
        createZooKeeper();
      }
      catch(InternalException e)
      {
        if(log.isDebugEnabled())
          log.debug("Failed to start ZKClient", e);
        changeState(State.NONE);
        throw e;
      }
      catch(Throwable e)
      {
        if(log.isDebugEnabled())
          log.debug("Failed to start ZKClient", e);
        changeState(State.NONE);
        throw new InternalException(MODULE, e);
      }
    }
  }

  private void createZooKeeper()
  {
    _uniqueID = new Object();
    _zk = _factory.createZooKeeper(new UniqueWatcher(_uniqueID));
  }

  private void changeState(State newState)
  {
    synchronized(_lock)
    {
      if(_state != newState)
      {
        if(_stateChangeDispatcher != null)
          _stateChangeDispatcher.addEvent(_listeners, _state, newState);
        _state = newState;
        _lock.notifyAll();
      }
    }
  }

  /**
   * @return <code>true</code> if connected
   */
  @Override
  public boolean isConnected()
  {
    return _state == State.CONNECTED;
  }

  /**
   * Wait (no longer than timeout if provided) for the client to be started
   */
  public void waitForStart() throws InterruptedException
  {
    try
    {
      waitForStart(null);
    }
    catch(TimeoutException e)
    {
      // should not happen...
      throw new RuntimeException(e);
    }
  }

  /**
   * Wait (no longer than timeout if provided) for the client to be started
   */
  public void waitForStart(Timespan timeout) throws TimeoutException, InterruptedException
  {
    waitForState(State.CONNECTED, timeout);
  }

  /**
   * Wait (no longer than timeout if provided) for the client to be started
   */
  public void waitForState(State state, Timespan timeout)
    throws TimeoutException, InterruptedException
  {
    long endTime = timeout == null ? 0 : timeout.futureTimeMillis(_clock);

    synchronized(_lock)
    {
      while(_state != state)
      {
        ConcurrentUtils.awaitUntil(_clock, _lock, endTime);
      }
    }
  }

  @Override
  public void destroy()
  {
    synchronized(_lock)
    {
      if(_zk != null)
      {
        try
        {
          if(log.isDebugEnabled())
            log.debug("destroying ZKClient");

          changeState(State.NONE);
          _zk.close();
          _zk = null;
          if(_expiredSessionRecovery != null)
            _expiredSessionRecovery.interrupt();
        }
        catch(Throwable e)
        {
          if(log.isDebugEnabled())
            log.debug("ignored exception", e);
        }
      }
    }
  }

  private void processWatchedEvent(WatchedEvent event)
  {
    synchronized(_lock)
    {
      if(event.getState() != null)
      {
        if(log.isDebugEnabled())
          log.debug("event: " + event.getState());

        switch(event.getState())
        {
          case SyncConnected:
            changeState(State.CONNECTED);
            break;

          case Disconnected:
            if(_state != State.NONE)
            {
              changeState(State.RECONNECTING);
            }
            break;

          case Expired:
            // when expired, the zookeeper object is invalid and we need to recreate a new one
            _zk = null;
            changeState(State.NONE);
            log.warn("Expiration detected: trying to restart...");
            if(_expiredSessionRecovery == null)
            {
              _expiredSessionRecovery = new ExpiredSessionRecovery();
              _expiredSessionRecovery.setDaemon(true);
              _expiredSessionRecovery.start();
            }
            break;
          
          default:
            log.warn("unprocessed event state: " + event.getState());
        }
      }
    }
  }

  @Override
  protected IZooKeeper getZk() throws InternalException
  {
    synchronized(_lock)
    {
      if(!isConnected())
      {
        throw new IllegalStateException("not connected");
      }

      return _zk;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getConnectString() {
      return _factory.getConnectString();
  }

}
