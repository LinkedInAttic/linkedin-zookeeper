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

import org.linkedin.util.annotations.Initializer;

import java.io.File;

/**
 * Event generated when something changes
 *
 * @author ypujante@linkedin.com
 */
public class NodeEvent<T>
{
  private NodeEventType _eventType;
  private TrackedNode<T> _node;

  public NodeEvent()
  {
    // use initializers
  }
  
  public NodeEvent(NodeEventType eventType, TrackedNode<T> node)
  {
    _eventType = eventType;
    _node = node;
  }

  public NodeEventType getEventType()
  {
    return _eventType;
  }

  @Initializer(required = true)
  public void setEventType(NodeEventType eventType)
  {
    _eventType = eventType;
  }

  public TrackedNode<T> getNode()
  {
    return _node;
  }

  @Initializer(required = true)
  public void setNode(TrackedNode<T> node)
  {
    _node = node;
  }

  public T getData()
  {
    return _node.getData();
  }
  
  public String getPath()
  {
    return _node.getPath();
  }

  public String getParentName()
  {
    return new File(getPath()).getParentFile().getName();
  }

  public String getName()
  {
    return new File(getPath()).getName();
  }

  public int getDepth()
  {
    return _node.getDepth();
  }

  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    sb.append("NodeEvent ").append(getNode()).append(": ");
    sb.append(getEventType());
    return sb.toString();
  }
}
