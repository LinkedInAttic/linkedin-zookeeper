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
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Stat;
import org.linkedin.util.io.PathUtils;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author ypujante@linkedin.com
 */
public abstract class AbstractZKClient extends AbstractZooKeeper implements IZKClient
{
  public static final String MODULE = AbstractZKClient.class.getName();
  public static final Logger log = org.slf4j.LoggerFactory.getLogger(MODULE);

  private final static String CHARSET = "UTF-8";

  /**
   * Constructor
   */
  public AbstractZKClient(String chroot)
  {
    super(chroot);
  }

  // ZooKeeper convenient calls
  @Override
  public Stat exists(String path) throws InterruptedException, KeeperException
  {
    return exists(path, false);
  }

  @Override
  public List<String> getChildren(String path) throws InterruptedException, KeeperException
  {
    return getChildren(path, false);
  }

  /**
   * @return both children and stat in one object
   */
  @Override
  public ZKChildren getZKChildren(String path, Watcher watcher)
    throws KeeperException, InterruptedException
  {
    Stat stat = new Stat();

    List<String> children = getChildren(path, watcher, stat);

    return new ZKChildren(children, stat);
  }

  /**
   * Returns all the children (recursively) of the provided path. Note that like {@link #getChildren(String)}
   * the result is relative to <code>path</code>.
   */
  @Override
  public List<String> getAllChildren(String path) throws InterruptedException, KeeperException
  {
    return findAllChildren(path);
  }

  @Override
  public void create(String path, String data, List<ACL> acl, CreateMode createMode)
    throws InterruptedException, KeeperException

  {
    create(path, toByteData(data), acl, createMode);
  }

  @Override
  public void createBytesNode(String path, byte[] data, List<ACL> acl, CreateMode createMode)
    throws InterruptedException, KeeperException

  {
    create(path, data, acl, createMode);
  }

  /**
   * Creates the parents if they don't exist
   */
  @Override
  public void createWithParents(String path, String data, List<ACL> acl, CreateMode createMode)
    throws InterruptedException, KeeperException

  {
    createParents(path, acl);
    create(path, data, acl, createMode);
  }

  /**
   * Creates the parents if they don't exist
   */
  @Override
  public void createBytesNodeWithParents(String path, byte[] data, List<ACL> acl, CreateMode createMode)
    throws InterruptedException, KeeperException

  {
    createParents(path, acl);
    createBytesNode(path, data, acl, createMode);
  }

  @Override
  public byte[] getData(String path) throws InterruptedException, KeeperException
  {
    return getData(path, false, null);
  }

  @Override
  public String getStringData(String path) throws InterruptedException, KeeperException
  {
    return toStringData(getData(path, false, null));
  }

  private String toStringData(byte[] data)
  {
    if(data == null)
      return null;
    else
    {
      try
      {
        return new String(data, CHARSET);
      }
      catch(UnsupportedEncodingException e)
      {
        throw new RuntimeException(e);
      }
    }
  }

  private byte[] toByteData(String data)
  {
    if(data == null)
      return null;
    else
    {
      try
      {
        return data.getBytes(CHARSET);
      }
      catch(UnsupportedEncodingException e)
      {
        throw new RuntimeException(e);
      }
    }
  }

  /**
   * Returns both the data as a string as well as the stat
   */
  @Override
  public ZKData<String> getZKStringData(String path) throws InterruptedException, KeeperException
  {
    return getZKStringData(path, null);
  }

  /**
   * Returns both the data as a string as well as the stat (and sets a watcher if not null)
   */
  @Override
  public ZKData<String> getZKStringData(String path, Watcher watcher)
    throws InterruptedException, KeeperException

  {
    ZKData<byte[]> zkData = getZKByteData(path, watcher);

    return new ZKData<String>(toStringData(zkData.getData()), zkData.getStat());
  }

  /**
   * Returns both the data as a byte[] as well as the stat
   */
  @Override
  public ZKData<byte[]> getZKByteData(String path) throws InterruptedException, KeeperException
  {
    return getZKByteData(path, null);
  }

  /**
   * Returns both the data as a byte[] as well as the stat (and sets a watcher if not null)
   */
  @Override
  public ZKData<byte[]> getZKByteData(String path, Watcher watcher)
    throws InterruptedException, KeeperException

  {
    Stat stat = new Stat();
    return new ZKData<byte[]>(getData(path, watcher, stat), stat);
  }

  @Override
  public Stat setData(String path, String data) throws InterruptedException, KeeperException
  {
    return setByteData(path, toByteData(data));
  }

  @Override
  public Stat setByteData(String path, byte[] data) throws InterruptedException, KeeperException
  {
    return setData(path, data, -1);
  }

  /**
   * Tries to create first and if the node exists, then does a setData.
   *
   * @return <code>null</code> if create worked, otherwise the result of setData
   */
  @Override
  public Stat createOrSetWithParents(String path, String data, List<ACL> acl, CreateMode createMode)
    throws InterruptedException, KeeperException

  {
    if(exists(path) != null)
      return setData(path, data);

    try
    {
      createWithParents(path, data, acl, createMode);
      return null;
    }
    catch(KeeperException.NodeExistsException e)
    {
      // this should not happen very often (race condition)
      return setData(path, data);
    }
  }

  @Override
  public void delete(String path) throws InterruptedException, KeeperException
  {
    delete(path, -1);
  }

  /**
   * delete all the children if they exist
   */
  @Override
  public void deleteWithChildren(String path) throws InterruptedException, KeeperException
  {
    List<String> allChildren = findAllChildren(path);

    for(String child : allChildren)
    {
      delete(PathUtils.addPaths(path, child));
    }

    delete(path);
  }

  /**
   * Implementation note: the method adjusts the path and use getZk() directly because in the
   * case where chroot is not null, the chroot path itself may not exist which is why we have to go
   * all the way to the root.
   */
  private void createParents(String path, List<ACL> acl)
    throws InterruptedException, KeeperException

  {
    path = PathUtils.getParentPath(adjustPath(path));
    path = PathUtils.removeTrailingSlash(path);

    List<String> paths = new ArrayList<String>();

    while(!path.equals("") && getZk().exists(path, false) == null)
    {
      paths.add(path);

      path = PathUtils.getParentPath(path);
      path = PathUtils.removeTrailingSlash(path);
    }

    Collections.reverse(paths);

    for(String p : paths)
    {
      try
      {
        getZk().create(p,
                       null,
                       acl,
                       CreateMode.PERSISTENT);
      }
      catch(KeeperException.NodeExistsException e)
      {
        // ok we continue...
        if(log.isDebugEnabled())
          log.debug("parent already exists " + p);
      }
    }
  }

  private List<String> findAllChildren(String path) throws InterruptedException, KeeperException
  {
    List<String> allChildren = new ArrayList<String>();

    findAllChildren(path, null, allChildren);

    return allChildren;
  }

  private void findAllChildren(String path, String parentPath, List<String> allChildren)
    throws InterruptedException, KeeperException

  {
    List<String> directChildren = getChildren(path);

    Collections.sort(directChildren);

    for(String child : directChildren)
    {
      String childPath = parentPath == null ? child : PathUtils.addPaths(parentPath, child);
      findAllChildren(PathUtils.addPaths(path, child), childPath, allChildren);
      allChildren.add(childPath);
    }
  }
}