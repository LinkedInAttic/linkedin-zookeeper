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


package org.linkedin.zookeeper.server;

import org.slf4j.Logger;
import org.apache.zookeeper.server.NIOServerCnxn;
import org.apache.zookeeper.server.NIOServerCnxnFactory;
import org.apache.zookeeper.server.ServerConfig;
import org.apache.zookeeper.server.ZooKeeperServer;
import org.apache.zookeeper.server.persistence.FileTxnSnapLog;
import org.apache.zookeeper.server.quorum.QuorumPeerConfig;
import org.linkedin.util.annotations.Initializable;
import org.linkedin.util.annotations.Initializer;
import org.linkedin.util.clock.Clock;
import org.linkedin.util.clock.ClockUtils;
import org.linkedin.util.clock.SystemClock;
import org.linkedin.util.concurrent.ConcurrentUtils;
import org.linkedin.util.exceptions.InternalException;
import org.linkedin.util.lifecycle.Shutdownable;
import org.linkedin.util.lifecycle.Startable;
import java.net.InetSocketAddress;

import java.io.File;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.TimeoutException;

/**
 * Encapsulates creation of a standalone zookeeper server (most likely
 * going to be used for testing only)
 *
 * @author ypujante@linkedin.com
 */
public class StandaloneZooKeeperServer implements Startable, Shutdownable
{
  public static final String MODULE = StandaloneZooKeeperServer.class.getName();
  public static final Logger log = org.slf4j.LoggerFactory.getLogger(MODULE);

  @Initializable
  public Clock clock = SystemClock.INSTANCE;

  private String _tickTime;
  private int _clientPort;
  private String _dataDir;

  private Thread _mainThread;
  
  private volatile boolean _shutdown = false;
  private NIOServerCnxnFactory _cnxnFactory;
  private ZooKeeperServer _zkServer;

  public StandaloneZooKeeperServer(String tickTime, String dataDir, int clientPort)
  {
    _dataDir = dataDir;
    _clientPort = clientPort;
    _tickTime = tickTime;
  }

  StandaloneZooKeeperServer()
  {
    _tickTime = "2000";
    _clientPort = 2120;
  }

  public int getClientPort()
  {
    return _clientPort;
  }

  @Initializer
  public void setClientPort(int clientPort)
  {
    _clientPort = clientPort;
  }

  public String getDataDir()
  {
    return _dataDir;
  }

  @Initializer(required = true)
  public void setDataDir(String dataDir)
  {
    _dataDir = dataDir;
  }

  public String getTickTime()
  {
    return _tickTime;
  }

  @Initializer
  public void setTickTime(String tickTime)
  {
    _tickTime = tickTime;
  }

  @Override
  public synchronized void start()
  {
    try
    {
      QuorumPeerConfig qpc = new QuorumPeerConfig();
      Properties p = new Properties();
      p.setProperty("tickTime", _tickTime);
      p.setProperty("clientPort", String.valueOf(_clientPort));
      p.setProperty("dataDir", _dataDir);
      qpc.parseProperties(p);

      ServerConfig serverConfig = new ServerConfig();
      serverConfig.readFrom(qpc);

      runFromConfig(serverConfig);

      _mainThread = new Thread(new Runnable()
      {
        @Override
        public void run()
        {
          try
          {
            _cnxnFactory.join();
          }
          catch(InterruptedException e)
          {
            throw new InternalException(e);
          }
        }
      });
      _mainThread.start();
    }
    catch(Exception e)
    {
      throw new InternalException(e);
    }
  }

  /**
   * This method is coming from <code>ZooKeeperServerMain</code> but tweaked to not block and to
   * have access to <code>_zkServer</code> and <code>_cnxnFactory</code>.
   */
  private void runFromConfig(ServerConfig config) throws IOException
  {
    log.info("Starting server");
    try
    {
      _zkServer = new ZooKeeperServer();

      FileTxnSnapLog ftxn = new FileTxnSnapLog(new File(config.getDataLogDir()),
                                               new File(config.getDataDir()));
      _zkServer.setTxnLogFactory(ftxn);
      _zkServer.setTickTime(config.getTickTime());
      _zkServer.setMinSessionTimeout(config.getMinSessionTimeout());
      _zkServer.setMaxSessionTimeout(config.getMaxSessionTimeout());
      _cnxnFactory = new NIOServerCnxnFactory();
      _cnxnFactory.configure(config.getClientPortAddress(), config.getMaxClientCnxns());
                                              
      _cnxnFactory.startup(_zkServer);
    }
    catch(InterruptedException e)
    {
      // warn, but generally this is ok
      log.warn("Server interrupted", e);
    }
  }

  @Override
  public synchronized void shutdown()
  {
    if(_mainThread == null)
      throw new IllegalStateException("not started");

    if(_shutdown)
      return;

    _cnxnFactory.shutdown();

    _shutdown = true;
  }

  @Override
  public void waitForShutdown() throws InterruptedException, IllegalStateException
  {
    try
    {
      waitForShutdown(-1);
    }
    catch(TimeoutException e)
    {
      throw new RuntimeException(e);
    }
  }

  /**
   * Waits for shutdown to be completed. After calling shutdown, there may still be some pending work
   * that needs to be accomplised. This method will block until it is done but no longer than the
   * timeout.
   *
   * @param timeout how long to wait maximum for the shutdown (see {@link ClockUtils#toTimespan(Object)})
   * @throws InterruptedException  if interrupted while waiting
   * @throws IllegalStateException if shutdown has not been called
   * @throws TimeoutException      if shutdown still not complete after timeout
   */
  @Override
  public void waitForShutdown(Object timeout)
    throws InterruptedException, IllegalStateException, TimeoutException
  {
    if(!_shutdown)
      throw new IllegalStateException("call shutdown first");
    ConcurrentUtils.joinFor(clock, _mainThread, timeout);

  }
}
