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

/**
 * @author ypujante@linkedin.com
 */
public class ChrootedZKClient extends AbstractZKClient implements IZKClient
{
  private final IZKClient _zkClient;

  /**
   * Constructor
   */
  public ChrootedZKClient(IZKClient zkClient, String chroot)
  {
    super(chroot);
    _zkClient = zkClient;
  }

  @Override
  protected IZooKeeper getZk()
  {
    return _zkClient;
  }

  /**
   * @return a new client with the path that has been chrooted... meaning all paths will be relative
   *         to the path provided.
   */
  @Override
  public IZKClient chroot(String path)
  {
    return new ChrootedZKClient(_zkClient, adjustPath(path));
  }

  /**
   * @return <code>true</code> if connected
   */
  @Override
  public boolean isConnected()
  {
    return _zkClient.isConnected();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void registerListener(LifecycleListener listener)
  {
    _zkClient.registerListener(listener);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeListener(LifecycleListener listener)
  {
    _zkClient.removeListener(listener);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getConnectString() {
      return _zkClient.getConnectString();
  }
}
