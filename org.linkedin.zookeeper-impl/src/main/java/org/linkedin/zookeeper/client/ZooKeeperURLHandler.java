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

import org.apache.zookeeper.KeeperException;
import org.linkedin.util.exceptions.InternalException;
import org.linkedin.util.text.TextUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

/**
 * @author ypujante@linkedin.com
 */
public class ZooKeeperURLHandler extends URLStreamHandler
{
  /**
   * Handle <code>zookeeper:/a/b/c</code> style urls
   *
   * @author ypujante@linkedin.com
   */
  public static class ZooKeeperURLConnection extends URLConnection
  {
    private final IZKClient _zk;

    ZooKeeperURLConnection(URL url, IZKClient zk)
    {
      super(url);
      _zk = zk;
    }

    @Override
    public void connect()
    {
      // nothing to do here because the client is already connected... need to revisit when
      // we support host/port
    }

    @Override
    public InputStream getInputStream() throws IOException
    {
      try
      {
        return new ByteArrayInputStream(_zk.getData(url.getPath()));
      }
      catch(InternalException e)
      {
        throw new IOException(e);
      }
      catch(InterruptedException e)
      {
        throw new IOException(e);
      }
      catch(KeeperException e)
      {
        throw new IOException(e);
      }
    }
  }

  private final IZKClient _defaultClient;

  ZooKeeperURLHandler(IZKClient defaultClient)
  {
    _defaultClient = defaultClient;
  }

  @Override
  protected URLConnection openConnection(URL url)
  {
    return new ZooKeeperURLConnection(url, _defaultClient);
  }

  @Override
  protected void parseURL(URL u, String spec, int start, int limit)
  {
    super.parseURL(u, spec, start, limit);

    if(!TextUtils.isEmptyString(u.getHost()) || u.getPort() != -1)
      throw new UnsupportedOperationException("host/port not supported yet");

    if(!TextUtils.isEmptyString(u.getQuery()))
      throw new IllegalArgumentException("no query string is allowed");
  }
}
