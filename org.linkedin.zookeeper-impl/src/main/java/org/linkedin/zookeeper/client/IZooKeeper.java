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

import org.apache.zookeeper.AsyncCallback;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Stat;

import java.util.List;

/**
 * Provides an interface to ZooKeeper (which is a class!). PLease see <code>ZooKeeper</code> javadoc
 * for details
 *
 * @author ypujante@linkedin.com
 */
public interface IZooKeeper
{
  long getSessionId();

  byte[] getSessionPasswd();

  int getSessionTimeout();

  void addAuthInfo(String scheme, byte auth[]);

  void register(Watcher watcher);

  void close() throws InterruptedException;

  String create(String path, byte data[], List<ACL> acl,
                CreateMode createMode)
    throws KeeperException, InterruptedException;

  void create(String path, byte data[], List<ACL> acl,
              CreateMode createMode, AsyncCallback.StringCallback cb, Object ctx);

  void delete(String path, int version)
    throws InterruptedException, KeeperException;

  void delete(String path, int version, AsyncCallback.VoidCallback cb,
              Object ctx);

  Stat exists(String path, Watcher watcher)
    throws KeeperException, InterruptedException;

  Stat exists(String path, boolean watch) throws KeeperException,
                                                 InterruptedException;

  void exists(String path, Watcher watcher,
              AsyncCallback.StatCallback cb, Object ctx);

  void exists(String path, boolean watch, AsyncCallback.StatCallback cb, Object ctx);

  byte[] getData(String path, Watcher watcher, Stat stat)
    throws KeeperException, InterruptedException;

  byte[] getData(String path, boolean watch, Stat stat)
    throws KeeperException, InterruptedException;

  void getData(String path, Watcher watcher,
               AsyncCallback.DataCallback cb, Object ctx);

  void getData(String path, boolean watch, AsyncCallback.DataCallback cb, Object ctx);

  Stat setData(String path, byte data[], int version)
    throws KeeperException, InterruptedException;

  void setData(String path, byte data[], int version,
               AsyncCallback.StatCallback cb, Object ctx);

  List<ACL> getACL(String path, Stat stat)
    throws KeeperException, InterruptedException;

  void getACL(String path, Stat stat, AsyncCallback.ACLCallback cb,
              Object ctx);

  Stat setACL(String path, List<ACL> acl, int version)
    throws KeeperException, InterruptedException;


  void setACL(String path, List<ACL> acl, int version,
              AsyncCallback.StatCallback cb, Object ctx);


  List<String> getChildren(String path, Watcher watcher)
    throws KeeperException, InterruptedException;

  List<String> getChildren(String path, boolean watch)
    throws KeeperException, InterruptedException;

  void getChildren(String path, Watcher watcher,
                   AsyncCallback.ChildrenCallback cb, Object ctx);

  void getChildren(String path, boolean watch, AsyncCallback.ChildrenCallback cb,
                   Object ctx);

  List<String> getChildren(String path, Watcher watcher,
                           Stat stat)
    throws KeeperException, InterruptedException;

  List<String> getChildren(String path, boolean watch, Stat stat)
    throws KeeperException, InterruptedException;

  void getChildren(String path, Watcher watcher,
                   AsyncCallback.Children2Callback cb, Object ctx);

  void getChildren(String path, boolean watch, AsyncCallback.Children2Callback cb,
                   Object ctx);

  void sync(String path, AsyncCallback.VoidCallback cb, Object ctx);

  ZooKeeper.States getState();
}