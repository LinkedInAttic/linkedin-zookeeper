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
import org.apache.zookeeper.AsyncCallback;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Stat;
import org.linkedin.util.io.PathUtils;

import java.util.List;


/**
 * Base abstract class which delegate all calls to another {@link IZooKeeper} implementation
 * and ajusts the path variable. Handle chroot as well.
 *
 * @author ypujante@linkedin.com
 */
public abstract class AbstractZooKeeper implements IZooKeeper
{
  public static final String MODULE = AbstractZooKeeper.class.getName();
  public static final Logger log = org.slf4j.LoggerFactory.getLogger(MODULE);

  private final String _chroot;

  /**
   * Constructor
   */
  public AbstractZooKeeper()
  {
    this(null);
  }

  /**
   * Constructor
   */
  public AbstractZooKeeper(String chroot)
  {
    _chroot = chroot;
  }

  public static String adjustPath(String path, String chroot)
  {
    if(chroot != null)
      path = PathUtils.addPaths(chroot, path);
    path = PathUtils.removeTrailingSlash(path);
    path = PathUtils.addLeadingSlash(path);
    return path;
  }

  protected String adjustPath(String path)
  {
    return adjustPath(path, _chroot);
  }

  @Override
  public void addAuthInfo(String scheme, byte[] auth)
  {
    getZk().addAuthInfo(scheme, auth);
  }

  protected abstract IZooKeeper getZk();

  @Override
  public void close() throws InterruptedException
  {
    getZk().close();
  }

  @Override
  public String create(String path, byte[] data, List<ACL> acl, CreateMode createMode)
    throws KeeperException, InterruptedException
  {
    return getZk().create(adjustPath(path), data, acl, createMode);
  }

  @Override
  public void create(String path,
                     byte[] data,
                     List<ACL> acl,
                     CreateMode createMode,
                     AsyncCallback.StringCallback cb, Object ctx)
  {
    getZk().create(adjustPath(path), data, acl, createMode, cb, ctx);
  }

  @Override
  public void delete(String path, int version)
    throws KeeperException, InterruptedException
  {
    getZk().delete(adjustPath(path), version);
  }

  @Override
  public void delete(String path, int version, AsyncCallback.VoidCallback cb, Object ctx)
  {
    getZk().delete(adjustPath(path), version, cb, ctx);
  }

  @Override
  public Stat exists(String path, boolean watch)
    throws KeeperException, InterruptedException
  {
    return getZk().exists(adjustPath(path), watch);
  }

  @Override
  public void exists(String path, boolean watch, AsyncCallback.StatCallback cb, Object ctx)
  {
    getZk().exists(adjustPath(path), watch, cb, ctx);
  }

  @Override
  public Stat exists(String path, Watcher watcher)
    throws KeeperException, InterruptedException
  {
    return getZk().exists(adjustPath(path), watcher);
  }

  @Override
  public void exists(String path, Watcher watcher, AsyncCallback.StatCallback cb, Object ctx)
  {
    getZk().exists(adjustPath(path), watcher, cb, ctx);
  }

  @Override
  public List<ACL> getACL(String path, Stat stat)
    throws KeeperException, InterruptedException
  {
    return getZk().getACL(adjustPath(path), stat);
  }

  @Override
  public void getACL(String path, Stat stat, AsyncCallback.ACLCallback cb, Object ctx)
  {
    getZk().getACL(adjustPath(path), stat, cb, ctx);
  }

  @Override
  public List<String> getChildren(String path, boolean watch)
    throws KeeperException, InterruptedException
  {
    return getZk().getChildren(adjustPath(path), watch);
  }

  @Override
  public void getChildren(String path,
                          boolean watch,
                          AsyncCallback.ChildrenCallback cb,
                          Object ctx)
  {
    getZk().getChildren(adjustPath(path), watch, cb, ctx);
  }

  @Override
  public List<String> getChildren(String path, Watcher watcher)
    throws KeeperException, InterruptedException
  {
    return getZk().getChildren(adjustPath(path), watcher);
  }

  @Override
  public void getChildren(String path,
                          Watcher watcher,
                          AsyncCallback.ChildrenCallback cb,
                          Object ctx)
  {
    getZk().getChildren(adjustPath(path), watcher, cb, ctx);
  }

  @Override
  public void getData(String path, boolean watch, AsyncCallback.DataCallback cb, Object ctx)
  {
    getZk().getData(adjustPath(path), watch, cb, ctx);
  }

  @Override
  public byte[] getData(String path, boolean watch, Stat stat)
    throws KeeperException, InterruptedException
  {
    return getZk().getData(adjustPath(path), watch, stat);
  }

  @Override
  public void getData(String path, Watcher watcher, AsyncCallback.DataCallback cb, Object ctx)
  {
    getZk().getData(adjustPath(path), watcher, cb, ctx);
  }

  @Override
  public byte[] getData(String path, Watcher watcher, Stat stat)
    throws KeeperException, InterruptedException
  {
    return getZk().getData(adjustPath(path), watcher, stat);
  }

  @Override
  public long getSessionId()
  {
    return getZk().getSessionId();
  }

  @Override
  public byte[] getSessionPasswd()
  {
    return getZk().getSessionPasswd();
  }

  @Override
  public ZooKeeper.States getState()
  {
    return getZk().getState();
  }

  @Override
  public void register(Watcher watcher)
  {
    getZk().register(watcher);
  }

  @Override
  public Stat setACL(String path, List<ACL> acl, int version)
    throws KeeperException, InterruptedException
  {
    return getZk().setACL(adjustPath(path), acl, version);
  }

  @Override
  public void setACL(String path,
                     List<ACL> acl,
                     int version,
                     AsyncCallback.StatCallback cb,
                     Object ctx)
  {
    getZk().setACL(adjustPath(path), acl, version, cb, ctx);
  }

  @Override
  public Stat setData(String path, byte[] data, int version)
    throws KeeperException, InterruptedException
  {
    return getZk().setData(adjustPath(path), data, version);
  }

  @Override
  public void setData(String path,
                      byte[] data,
                      int version,
                      AsyncCallback.StatCallback cb,
                      Object ctx)
  {
    getZk().setData(adjustPath(path), data, version, cb, ctx);
  }

  @Override
  public void sync(String path, AsyncCallback.VoidCallback cb, Object ctx)
  {
    getZk().sync(adjustPath(path), cb, ctx);
  }

  @Override
  public void getChildren(String path,
                          boolean watch,
                          AsyncCallback.Children2Callback cb,
                          Object ctx)
  {
    getZk().getChildren(adjustPath(path), watch, cb, ctx);
  }

  @Override
  public int getSessionTimeout()
  {
    return getZk().getSessionTimeout();
  }

  @Override
  public List<String> getChildren(String path, Watcher watcher, Stat stat)
    throws KeeperException, InterruptedException
  {
    return getZk().getChildren(adjustPath(path), watcher, stat);
  }

  @Override
  public List<String> getChildren(String path, boolean watch, Stat stat)
    throws KeeperException, InterruptedException
  {
    return getZk().getChildren(adjustPath(path), watch, stat);
  }

  @Override
  public void getChildren(String path,
                          Watcher watcher,
                          AsyncCallback.Children2Callback cb,
                          Object ctx)
  {
    getZk().getChildren(adjustPath(path), watcher, cb, ctx);
  }
}