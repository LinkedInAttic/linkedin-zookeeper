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

package org.linkedin.zookeeper.client;

import org.slf4j.Logger;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.linkedin.util.clock.Timespan;
import org.linkedin.util.exceptions.InternalException;

import java.io.IOException;

/**
 * @author ypujante@linkedin.com
 */
public class ZooKeeperFactory implements IZooKeeperFactory
{
  public static final String MODULE = ZooKeeperFactory.class.getName();
  public static final Logger log = org.slf4j.LoggerFactory.getLogger(MODULE);

  private final String _connectString;
  private final Timespan _sessionTimeout;
  private final Watcher _watcher;

  /**
   * Constructor
   */
  public ZooKeeperFactory(String connectString, Timespan sessionTimeout, Watcher watcher)
  {
    _connectString = connectString;
    _sessionTimeout = sessionTimeout;
    _watcher = watcher;
  }

  /**
   * Constructor
   */
  public ZooKeeperFactory(String connectString, Timespan sessionTimeout)
  {
    this(connectString, sessionTimeout, null);
  }

  @Override
  public IZooKeeper createZooKeeper(Watcher watcher)
  {
    try
    {
      return new ZooKeeperImpl(new ZooKeeper(_connectString,
                                             (int) _sessionTimeout.getDurationInMilliseconds(),
                                             WatcherChain.createChain(_watcher, watcher)));
    }
    catch(IOException e)
    {
      throw new InternalException(MODULE, e);
    }
  }

  public String getConnectString()
  {
    return _connectString;
  }

  public Timespan getSessionTimeout()
  {
    return _sessionTimeout;
  }

  public Watcher getWatcher()
  {
    return _watcher;
  }
}
