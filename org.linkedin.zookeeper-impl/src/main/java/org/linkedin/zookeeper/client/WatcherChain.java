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

import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.WatchedEvent;

import java.util.Collection;
import java.util.Arrays;
import java.util.ArrayList;

/**
 * @author ypujante@linkedin.com
 */
public class WatcherChain implements Watcher
{
  private final Collection<Watcher> _watchers;

  /**
   * Constructor
   */
  public WatcherChain(Watcher... watchers)
  {
    this(Arrays.asList(watchers));
  }

  /**
   * Constructor
   */
  public WatcherChain(Collection<Watcher> watchers)
  {
    _watchers = watchers;
  }

  @Override
  public void process(WatchedEvent event)
  {
    for(Watcher watcher : _watchers)
    {
      watcher.process(event);
    }
  }

  public static Watcher createChain(Watcher... watchers)
  {
    if(watchers == null || watchers.length == 0)
      return null;

    ArrayList<Watcher> list = new ArrayList<Watcher>();

    for(Watcher watcher : watchers)
    {
      if(watcher != null)
        list.add(watcher);
    }

    if(list.size() == 1)
      return list.get(0);
    else
      return new WatcherChain(list);
  }
}
