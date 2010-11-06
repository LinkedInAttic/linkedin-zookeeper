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

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.Watcher;
import org.linkedin.zookeeper.client.IZKClient;
import org.linkedin.zookeeper.client.ZKData;

import java.util.Arrays;

/**
 * @author ypujante@linkedin.com
 *
 */
public class ZKByteArrayDataReader implements ZKDataReader<byte[]>
{
  @Override
  public ZKData<byte[]> readData(IZKClient zkClient, String path, Watcher watcher)
    throws InterruptedException, KeeperException
  {
    return zkClient.getZKByteData(path, watcher);
  }

  /**
   * Compare 2 data equality
   *
   * @return <code>true</code> if equal (in the {@link Object#equals(Object)} definition)
   */
  @Override
  public boolean isEqual(byte[] data1, byte[] data2)
  {
    return Arrays.equals(data1, data2);
  }
}
