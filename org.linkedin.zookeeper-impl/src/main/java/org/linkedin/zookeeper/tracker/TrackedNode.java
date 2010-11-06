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

import org.apache.zookeeper.data.Stat;
import org.linkedin.util.annotations.Initializer;

/**
 * A node being tracked
 *
 * @author ypujante@linkedin.com
 */
public class TrackedNode<T>
{
  private String _path;
  private T _data;
  private Stat _stat;
  private int _depth;

  public TrackedNode()
  {
    // use initializers...
  }

  public TrackedNode(String path, T data, Stat stat, int depth)
  {
    _data = data;
    _depth = depth;
    _path = path;
    _stat = stat;
  }

  public T getData()
  {
    return _data;
  }

  @Initializer(required = true)
  public void setData(T data)
  {
    _data = data;
  }

  public int getDepth()
  {
    return _depth;
  }

  @Initializer(required = true)
  public void setDepth(int depth)
  {
    _depth = depth;
  }

  public String getPath()
  {
    return _path;
  }

  @Initializer(required = true)
  public void setPath(String path)
  {
    _path = path;
  }

  public Stat getStat()
  {
    return _stat;
  }

  @Initializer(required = true)
  public void setStat(Stat stat)
  {
    _stat = stat;
  }

  public long getCreationTime()
  {
    return _stat.getCtime();
  }

  public long getModifiedTime()
  {
    return _stat.getMtime();
  }

  public long getZkTxId()
  {
    return Math.max(_stat.getMzxid(), _stat.getPzxid());
  }

  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    sb.append("{path:'").append(getPath()).append("',");
    sb.append("zktxid:").append(getZkTxId()).append(",");
    sb.append("stat:'").append(getStat().toString().trim()).append("',");
    sb.append("data:'").append(getData()).append("'}");
    return sb.toString();
  }

  @Override
  public boolean equals(Object o)
  {
    if(this == o) return true;
    if(o == null || getClass() != o.getClass()) return false;

    TrackedNode that = (TrackedNode) o;

    if(_depth != that._depth) return false;
    if(_data != null ? !_data.equals(that._data) : that._data != null) return false;
    if(_path != null ? !_path.equals(that._path) : that._path != null) return false;
    if(_stat != null ? !_stat.equals(that._stat) : that._stat != null) return false;

    return true;
  }

  @Override
  public int hashCode()
  {
    int result = _path != null ? _path.hashCode() : 0;
    result = 31 * result + (_stat != null ? _stat.hashCode() : 0);
    result = 31 * result + (_data != null ? _data.hashCode() : 0);
    result = 31 * result + _depth;
    return result;
  }
}
