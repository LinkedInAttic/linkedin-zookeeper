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

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Stat;

import java.util.List;

/**
 * @author ypujante@linkedin.com
 */
public interface IZKClient extends IZooKeeper
{
  /**
   * Registers a listener for lifecycle management. Due to the nature of ZooKeeper, you should always
   * register a listener and act appropriately on the 2 events that you can receive. Note that it is
   * guaranteed that the listener will be called even if the connection with ZooKeeper has been
   * established a while ago...
   *
   * @param listener the listener
   */
  void registerListener(LifecycleListener listener);

  /**
   * Removes a listener previously set with {@link #registerListener(LifecycleListener)}
   * @param listener
   */
  void removeListener(LifecycleListener listener);

  /**
   * @return a new client with the path that has been chrooted... meaning all paths will be relative
   * to the path provided.
   */
  IZKClient chroot(String path);

  /**
   * @return <code>true</code> if connected
   */
  boolean isConnected();
  
  // ZooKeeper convenient calls
  Stat exists(String path) throws InterruptedException, KeeperException;

  List<String> getChildren(String path) throws InterruptedException, KeeperException;

  /**
   * @return both children and stat in one object
   */
  ZKChildren getZKChildren(String path, Watcher watcher)
    throws KeeperException, InterruptedException;

  /**
   * Returns all the children (recursively) of the provided path. Note that like {@link #getChildren(String)}
   * the result is relative to <code>path</code>.
   */
  List<String> getAllChildren(String path) throws InterruptedException, KeeperException;

  void create(String path, String data, List<ACL> acl, CreateMode createMode)
    throws InterruptedException, KeeperException
    ;

  void createBytesNode(String path, byte[] data, List<ACL> acl, CreateMode createMode)
    throws InterruptedException, KeeperException
    ;

  /**
   * Creates the parents if they don't exist
   */
  void createWithParents(String path, String data, List<ACL> acl, CreateMode createMode)
    throws InterruptedException, KeeperException
    ;

  /**
   * Creates the parents if they don't exist
   */
  void createBytesNodeWithParents(String path, byte[] data, List<ACL> acl, CreateMode createMode)
    throws InterruptedException, KeeperException
    ;

  byte[] getData(String path) throws InterruptedException, KeeperException;

  String getStringData(String path) throws InterruptedException, KeeperException;

  /**
   * Returns both the data as a string as well as the stat
   */
  ZKData<String> getZKStringData(String path) throws InterruptedException, KeeperException;

  /**
   * Returns both the data as a string as well as the stat (and sets a watcher if not null)
   */
  ZKData<String> getZKStringData(String path, Watcher watcher)
    throws InterruptedException, KeeperException
    ;

  /**
   * Returns both the data as a byte[] as well as the stat
   */
  ZKData<byte[]> getZKByteData(String path) throws InterruptedException, KeeperException;

  /**
   * Returns both the data as a byte[] as well as the stat (and sets a watcher if not null)
   */
  ZKData<byte[]> getZKByteData(String path, Watcher watcher)
    throws InterruptedException, KeeperException
    ;

  Stat setData(String path, String data) throws InterruptedException, KeeperException;

  Stat setByteData(String path, byte[] data) throws InterruptedException, KeeperException;

  /**
   * Tries to create first and if the node exists, then does a setData.
   *
   * @return <code>null</code> if create worked, otherwise the result of setData
   */
  Stat createOrSetWithParents(String path, String data, List<ACL> acl, CreateMode createMode)
    throws InterruptedException, KeeperException
    ;

  void delete(String path) throws InterruptedException, KeeperException;

  /**
   * delete all the children if they exist
   */
  void deleteWithChildren(String path) throws InterruptedException, KeeperException;
}