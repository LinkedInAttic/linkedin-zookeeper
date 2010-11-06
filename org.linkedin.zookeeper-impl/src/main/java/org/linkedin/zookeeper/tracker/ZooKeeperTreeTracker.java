/*
 * Copyright 2010-2010 LinkedIn, Inc
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

package org.linkedin.zookeeper.tracker;

import org.slf4j.Logger;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.linkedin.util.annotations.Initializable;
import org.linkedin.util.clock.Clock;
import org.linkedin.util.clock.ClockUtils;
import org.linkedin.util.clock.SystemClock;
import org.linkedin.util.concurrent.ConcurrentUtils;
import org.linkedin.util.io.PathUtils;
import org.linkedin.util.lang.LangUtils;
import org.linkedin.util.lifecycle.Destroyable;
import org.linkedin.zookeeper.client.IZKClient;
import org.linkedin.zookeeper.client.ZKChildren;
import org.linkedin.zookeeper.client.ZKData;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeoutException;


/**
 * The purpose of this class is to essentially keep a replica of ZooKeeper data in memory and
 * be able to be notified when it changes.
 *
 * @author ypujante@linkedin.com */
public class ZooKeeperTreeTracker<T> implements Destroyable
{
  public static final String MODULE = ZooKeeperTreeTracker.class.getName();
  public static final Logger log = org.slf4j.LoggerFactory.getLogger(MODULE);

  @Initializable
  public Clock clock = SystemClock.INSTANCE;

  private final IZKClient _zk;
  private final ZKDataReader<T> _zkDataReader;
  private final String _root;
  private final int _depth;

  private final Set<ErrorListener> _errorListeners = new LinkedHashSet<ErrorListener>();
  private final Set<NodeEventsListener<T>> _eventsListeners = new LinkedHashSet<NodeEventsListener<T>>();

  private volatile Map<String, TrackedNode<T>> _tree = new LinkedHashMap<String, TrackedNode<T>>();
  private volatile boolean _destroyed = false;
  private long _lastZkTxId;
  private final Object _lock = new Object();
  private final int _rootDepth;
  private final Watcher _treeWacher = new TreeWatcher();

  private class TreeWatcher implements Watcher
  {
    @Override
    public void process(WatchedEvent event)
    {
      if(log.isDebugEnabled())
        log.debug(logString(event.getPath(),
                            "treeWatcher: type=" + event.getType()) + ", state=" + event.getState());

      Collection<NodeEvent<T>> events = new ArrayList<NodeEvent<T>>();

      try
      {
        synchronized(_lock)
        {
          if(!handleEvent(event))
            return;

          switch(event.getType())
          {
            case NodeDeleted:
              _tree = handleNodeDeleted(event.getPath(), events);
              break;

            case NodeCreated:
              throw new RuntimeException("getting node created event ? when ?");

            case NodeChildrenChanged:
              _tree = handleNodeChildrenChanged(event.getPath(), events);
              break;

            case NodeDataChanged:
              _tree = handleNodeDataChanged(event.getPath(), events);
              break;

            case None:
              // nothing to do
              break;
          }
        }

        raiseEvents(events);
      }
      catch (Throwable th)
      {
        log.warn(logString(event.getPath(), "Error in treeWatcher (ignored)"), th);
        raiseError(event, th);
      }
    }
  }


  public ZooKeeperTreeTracker(IZKClient zk, ZKDataReader<T> zkDataReader, String root)
  {
    this(zk, zkDataReader, root, Integer.MAX_VALUE);
  }

  public ZooKeeperTreeTracker(IZKClient zk, ZKDataReader<T> zkDataReader, String root, int depth)
  {
    _zk = zk;
    _zkDataReader = zkDataReader;
    _root = root;
    _rootDepth = computeAbsoluteDepth(_root);
    _depth = depth;
  }

  public static int computeAbsoluteDepth(String path)
  {
    if(path == null)
      return 0;

    int depth = 0;

    for(int i = 0; i < path.length(); i++)
    {
      char c = path.charAt(i);
      if(c == '/')
        depth++;
    }

    return depth;
  }

  public String getRoot()
  {
    return _root;
  }

  public IZKClient getZKCient()
  {
    return _zk;
  }

  public int getDepth()
  {
    return _depth;
  }

  public long getLastZkTxId()
  {
    synchronized(_lock)
    {
      return _lastZkTxId;
    }
  }

  /**
   * Waits no longer than the timeout provided (or forever if <code>null</code>) for this tracker
   * to have seen at least the provided zookeeper transaction id
   * @return the last known zkTxId (guaranteed to be &gt;= to zkTxId)
   */
  public long waitForZkTxId(long zkTxId, Object timeout)
    throws TimeoutException, InterruptedException
  {
    long endTime = ClockUtils.toEndTime(clock, timeout);

    synchronized(_lock)
    {
      while(_lastZkTxId < zkTxId)
      {
        ConcurrentUtils.awaitUntil(clock, _lock, endTime);
      }

      return _lastZkTxId;
    }
  }

  /**
   * It is not possible to remove a watcher... so we just set the tracker in destroyed mode which
   * will simply ignore all subsequent event.
   */
  @Override
  public void destroy()
  {
    synchronized(_lock)
    {
      _destroyed = true;
    }
  }

  /**
   * @return the tree
   */
  public Map<String, TrackedNode<T>> getTree()
  {
    // note that there is no need for synchronization because:
    // 1. the attribute is volatile
    // 2. the map is never modified (a new one is created / replaced when needed)
    return _tree;
  }

  public void track(NodeEventsListener<T> eventsListener)
    throws InterruptedException, KeeperException
  {
    registerListener(eventsListener);
    track();
  }

  public void track() throws InterruptedException, KeeperException
  {
    Collection<NodeEvent<T>> events = new ArrayList<NodeEvent<T>>();

    synchronized(_lock)
    {
      _tree = trackNode(_root, new LinkedHashMap<String, TrackedNode<T>>(), events, 0);
    }

    raiseEvents(events);
  }

  public void registerListener(NodeEventsListener<T> eventsListener)
  {
    synchronized(_lock)
    {
      _eventsListeners.add(eventsListener);
    }
  }

  public void registerErrorListener(ErrorListener errorListener)
  {
    synchronized(_lock)
    {
      _errorListeners.add(errorListener);
    }
  }

  /**
   * Must be called from a synchronized section
   */
  private Map<String, TrackedNode<T>> trackNode(String path,
                                                Map<String, TrackedNode<T>> tree,
                                                Collection<NodeEvent<T>> events,
                                                int depth)
    throws InterruptedException, KeeperException
  {
    if(depth > _depth)
    {
      if(log.isDebugEnabled())
        log.debug(logString(path, "max depth reached ${depth}"));

      return tree;
    }

    TrackedNode<T> oldTrackedNode = tree.get(path);

    try
    {
      ZKData<T> res = _zkDataReader.readData(_zk, path, _treeWacher);

      TrackedNode<T> newTrackedNode =
        new TrackedNode<T>(path, res.getData(), res.getStat(), depth);
      if(oldTrackedNode != null)
      {
        if(!_zkDataReader.isEqual(oldTrackedNode.getData(), newTrackedNode.getData()))
        {
          events.add(new NodeEvent<T>(NodeEventType.UPDATED,
                                      newTrackedNode));
        }
      }
      else
      {
        events.add(new NodeEvent<T>(NodeEventType.ADDED,
                                    newTrackedNode));
      }

      tree.put(path, newTrackedNode);

      if(depth < _depth)
      {
        ZKChildren children = _zk.getZKChildren(path, _treeWacher);
        // the stat may change between the 2 calls
        if(!newTrackedNode.getStat().equals(children.getStat()))
          newTrackedNode.setStat(children.getStat());
        Collections.sort(children.getChildren());
        for(String child : children.getChildren())
        {
          String childPath = PathUtils.addPaths(path, child);
          if(!tree.containsKey(childPath))
            trackNode(childPath, tree, events, depth + 1);
        }
      }

      _lastZkTxId = Math.max(_lastZkTxId, newTrackedNode.getZkTxId());
      _lock.notifyAll();
      if(log.isDebugEnabled())
        log.debug(logString(path,
                            "start tracking " + (depth < _depth ? "": "leaf ") +
                            "node zkTxId=" + newTrackedNode.getZkTxId()));

    }
    catch (KeeperException.NoNodeException e)
    {
      // this is a race condition which could happen between the moment the event is received
      // and this call
      if(log.isDebugEnabled())
        log.debug(logString(path, "no such node"));
      // it means that the node has disappeared

      tree.remove(path);

      if(oldTrackedNode != null)
      {
        events.add(new NodeEvent<T>(NodeEventType.DELETED, 
                                    oldTrackedNode));
      }
    }

    return tree;
  }

  private Map<String, TrackedNode<T>> handleNodeDeleted(String path,
                                                        Collection<NodeEvent<T>> events)
    throws InterruptedException, KeeperException
  {
    Map<String, TrackedNode<T>> tree = _tree;
    if(_tree.containsKey(path))
    {
      tree = new LinkedHashMap<String, TrackedNode<T>>(_tree);
      TrackedNode<T> trackedNode = tree.remove(path);
      events.add(new NodeEvent<T>(NodeEventType.DELETED,
                                  trackedNode));
      if(log.isDebugEnabled())
        log.debug(logString(path, "stop tracking node"));

      // after a delete event, we try to track the node again as a delete/add event could happen
      // and be undetected otherwise!
      trackNode(path, tree, events, trackedNode.getDepth());
    }

    return tree;
  }

  private Map<String, TrackedNode<T>> handleNodeDataChanged(String path,
                                                            Collection<NodeEvent<T>> events)
    throws InterruptedException, KeeperException
  {
    return trackNode(path,
                     new LinkedHashMap<String, TrackedNode<T>>(_tree),
                     events,
                     computeDepth(path));
  }

  private Map<String, TrackedNode<T>> handleNodeChildrenChanged(String path,
                                                                Collection<NodeEvent<T>> events)
    throws InterruptedException, KeeperException
  {
    return trackNode(path,
                     new LinkedHashMap<String, TrackedNode<T>>(_tree),
                     events,
                     computeDepth(path));
  }

  private int computeDepth(String path)
  {
    return computeAbsoluteDepth(path) - _rootDepth;
  }

  private void raiseEvents(Collection<NodeEvent<T>> events)
  {
    if(!events.isEmpty())
    {
      Set<NodeEventsListener<T>> listeners;
      synchronized(_lock)
      {
        listeners = new LinkedHashSet<NodeEventsListener<T>>(_eventsListeners);
      }

      for(NodeEventsListener<T> listener : listeners)
      {
        listener.onEvents(events);
      }
    }
  }

  private boolean handleEvent(WatchedEvent event)
  {
    if(_destroyed)
    {
      return false;
    }

    switch(event.getState())
    {
      case SyncConnected:
        return true;

      case Disconnected:
        return false;

      case Expired:
        return false;

      default:
        return false;
    }
  }

  private String logString(String path, String msg)
  {
    StringBuilder sb = new StringBuilder();
    sb.append("[").append(path).append("] ");
    sb.append("[").append(LangUtils.shortIdentityString(this)).append("] ");
    sb.append("[").append(Thread.currentThread()).append("] ");
    sb.append(msg);
    return sb.toString();
  }

  private void raiseError(WatchedEvent event, Throwable th)
  {
    Set<ErrorListener> listeners;
    synchronized(_lock)
    {
      listeners = new LinkedHashSet<ErrorListener>(_errorListeners);
    }

    if(!listeners.isEmpty())
    {
      for(ErrorListener listener : listeners)
      {
        try
        {
          if(log.isDebugEnabled())
            log.debug(logString(event.getPath(), "Raising error to " +
                                                 LangUtils.identityString(listener)),
                      th);

          listener.onError(event, th);
        }
        catch(Throwable th2)
        {
          log.warn(logString(event.getPath(), "Error in watcher while executing listener " +
                                              LangUtils.identityString(listener) +
                                              " (ignored)"),
                   th2);
        }
      }
    }
  }
}
