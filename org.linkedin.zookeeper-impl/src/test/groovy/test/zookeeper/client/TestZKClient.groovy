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

package test.zookeeper.client

import org.apache.zookeeper.CreateMode
import org.apache.zookeeper.KeeperException
import org.apache.zookeeper.WatchedEvent
import org.apache.zookeeper.Watcher
import org.apache.zookeeper.Watcher.Event.KeeperState
import org.apache.zookeeper.ZooDefs.Ids
import org.apache.zookeeper.ZooKeeper
import org.linkedin.groovy.util.net.SingletonURLStreamHandlerFactory
import org.linkedin.util.clock.Timespan
import org.linkedin.util.concurrent.ThreadControl
import org.linkedin.util.exceptions.InternalException
import org.linkedin.zookeeper.client.IZKClient
import org.linkedin.zookeeper.client.IZooKeeperFactory
import org.linkedin.zookeeper.client.LifecycleListener
import org.linkedin.zookeeper.client.ZKClient
import org.linkedin.zookeeper.client.ZooKeeperImpl
import org.linkedin.zookeeper.client.ZooKeeperURLHandler
import org.linkedin.zookeeper.server.StandaloneZooKeeperServer
import org.linkedin.groovy.util.io.fs.FileSystemImpl
import java.util.concurrent.TimeoutException

/**
 * Test for the zookeeper client
 *
 * @author ypujante@linkedin.com
 */
class TestZKClient extends GroovyTestCase
{
  FileSystemImpl fs = FileSystemImpl.createTempFileSystem()
  StandaloneZooKeeperServer zookeeperServer

  protected void setUp()
  {
    super.setUp();

    zkStart()
  }

  private void zkStart()
  {
    zookeeperServer = new StandaloneZooKeeperServer(tickTime: 2000,
                                                    clientPort: 2121,
                                                    dataDir: fs.root.file.canonicalPath)
    zookeeperServer.start()
  }

  protected void tearDown()
  {
    try
    {
      zkShutdown()
      fs.destroy()
    }
    finally
    {
      super.tearDown();
    }
  }

  private void zkShutdown()
  {
    zookeeperServer.shutdown()
    zookeeperServer.waitForShutdown(1000)
  }

  void testClient()
  {
    ZKClient client
    client = new ZKClient('localhost:2121', Timespan.parse('1m'), null)

    try
    {
      client.start()
      client.waitForStart(Timespan.parse('10s'))

      assertEquals('localhost:2121', client.getConnectString())

      // registers url factory
      def factory = new SingletonURLStreamHandlerFactory()
      factory.registerHandler('zookeeper') {
        return new ZooKeeperURLHandler(client)
      }
      URL.setURLStreamHandlerFactory(factory)

      assertEquals(ZooKeeper.States.CONNECTED, client.state)

      // there is always a zookeeper child when zookeeper starts...
      assertEquals(1, client.exists('/').numChildren)
      assertEquals('zookeeper', client.getChildren('/')[0])

      assertNull(client.exists('/a'))
      client.create('/a', 'test1', Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT)
      assertEquals(2, client.exists('/').numChildren)
      assertEquals('test1', client.getStringData('/a'))
      assertEquals('test1', new URL('zookeeper:/a').text)
      assertEquals(new HashSet(['zookeeper', 'a']), new HashSet(client.getChildren('/')))

      // cannot create a child with no parent
      shouldFail(KeeperException.NoNodeException) {
        client.create('/a/b/c/d/e/f', 'test2', Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT)
      }

      // now it should work
      client.createWithParents('/a/b/c/d/e/f', 'test2', Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT)
      assertEquals('test2', client.getStringData('/a/b/c/d/e/f'))
      assertEquals('test2', new URL('zookeeper:/a/b/c/d/e/f').text)

      // cannot delete a non empty directory
      shouldFail(KeeperException.NotEmptyException) {
        client.delete('/a/b')
      }

      // this one should work
      client.deleteWithChildren('/a/b')
      assertNull(client.exists('/a/b'))

      // we make sure that /a still exists
      assertEquals('test1', client.getStringData('/a'))

      IZKClient chrootedClient = client.chroot("/a")
      assertEquals(0, chrootedClient.exists('/').numChildren)

      chrootedClient.createWithParents('/z/k/w', 'test3', Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT)
      assertEquals('test3', chrootedClient.getStringData('/z/k/w'))
      assertEquals('test3', client.getStringData('/a/z/k/w'))

      // now we test createOrSet
      client.createOrSetWithParents('/a/b/c', 'test4', Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT)
      assertEquals('test4', client.getStringData('/a/b/c'))
      client.createOrSetWithParents('/a/b/c', 'test4.updated', Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT)
      assertEquals('test4.updated', client.getStringData('/a/b/c'))

      // at this state we have /a, /a/b, /a/b/c, /a/z, /a/z/k, /a/z/k/w
      assertEquals(['b', 'z'], client.getChildren('/a').sort())
      assertEquals(['b', 'b/c', 'z', 'z/k', 'z/k/w'], client.getAllChildren('/a').sort())

      // error cases
      assertEquals('host/port not supported yet',
                   shouldFail(MalformedURLException) { new URL('zookeeper://localhost:2121/a') })
      assertEquals('no query string is allowed',
                   shouldFail(MalformedURLException) { new URL('zookeeper:/a?p1=v1') })
    }
    finally
    {
      client.destroy()
    }
  }

  private ZooKeeper _testableZooKeeper
  private boolean _failCreation = false
  private int _failedCount = 0

  private testableZooKeeperFactory = { Watcher watcher ->
    if(_failCreation)
    {
      _failedCount++
      throw new InternalException('TestZKClient', 'failing creation')
    }

    _testableZooKeeper = new ZooKeeper('localhost:2121', (int) Timespan.parse('1m').durationInMilliseconds, watcher)
    new ZooKeeperImpl(_testableZooKeeper)
  }

  /**
   * We verify that we can recover properly.
   */
  public void testRecovery()
  {
    ZKClient client
    client = new ZKClient(testableZooKeeperFactory as IZooKeeperFactory)
    client.reconnectTimeout = Timespan.parse('500')

    ThreadControl th = new ThreadControl()

    client.registerListener([
        onConnected: {
          th.block('onConnected')
        },

        onDisconnected: {
          th.block('onDisconnected')
        }
    ] as LifecycleListener)

    try
    {
      assertEquals(ZKClient.State.NONE, client.ZKClientState)

      // start has not been called yet
      assertEquals("not connected", shouldFail(IllegalStateException) {
                   client.exists('/a')
                   })

      client.start()
      client.waitForStart(Timespan.parse('5s'))

      th.waitForBlock('onConnected')
      th.unblock('onConnected')

      assertEquals(ZKClient.State.CONNECTED, client.ZKClientState)

      assertNull(client.exists('/a'))

      assertEquals(ZKClient.State.CONNECTED, client.ZKClientState)

      client.create('/a', 'testa', Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT)
      assertEquals('testa', client.getStringData('/a'))

      client.create('/b', 'testb', Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL)
      assertEquals('testb', client.getStringData('/b'))

      // now we simulate a disconnect from zookeeper:
      zkShutdown()

      // depending on when we receive the event, we can get either exception...
      try
      {
        client.getStringData('/a')
        fail("should fail with an exception")
      }
      catch (KeeperException.ConnectionLossException e)
      {
        // ok
      }
      catch (IllegalStateException e)
      {
        assertEquals("not connected", e.message)
      }

      client.waitForState(ZKClient.State.RECONNECTING, Timespan.parse('5s'))

      th.waitForBlock('onDisconnected')
      th.unblock('onDisconnected')

      // once the client is disconnected then we should get the IllegalStateException only
      assertEquals("not connected", shouldFail(IllegalStateException) {
                   client.getStringData('/a')
                   })

      // we wait a little... to make sure that we excercise the recovery loop
      Thread.sleep(600)

      // we restart the server
      zkStart()

      // we wait for the client to 'reconnect'
      client.waitForStart(Timespan.parse('5s'))

      th.waitForBlock('onConnected')
      th.unblock('onConnected')

      assertEquals('testa', client.getStringData('/a'))
      // in this type of recovery, ephemeral nodes should not be lost!
      assertEquals('testb', client.getStringData('/b'))

      // we force an expired event
      _testableZooKeeper.close()
      client.process(new WatchedEvent(Watcher.Event.EventType.None, KeeperState.Expired, null))

      // this will trigger a disconnet and connect event
      th.waitForBlock('onDisconnected')
      th.unblock('onDisconnected')

      th.waitForBlock('onConnected')
      th.unblock('onConnected')

      // we wait for start again
      client.waitForStart(Timespan.parse('5s'))

      assertEquals('testa', client.getStringData('/a'))
      // when reconnecting from an expiration, ephemeral nodes are lost
      shouldFail(KeeperException.NoNodeException) {
        client.getStringData('/b')
      }

      // we generate a failure
      _failCreation = true

      _testableZooKeeper.close()
      client.process(new WatchedEvent(Watcher.Event.EventType.None, KeeperState.Expired, null))

      th.waitForBlock('onDisconnected')
      th.unblock('onDisconnected')

      // the expiration recovery mode should kick in and fail several time since reconnect timeout
      // is 500ms
      shouldFail(TimeoutException) {
        client.waitForStart(Timespan.parse('2s'))
      }

      _failCreation = false

      th.waitForBlock('onConnected')
      th.unblock('onConnected')

      assertTrue(_failedCount > 4)

      // we wait for start again
      client.waitForStart(Timespan.parse('5s'))

      assertEquals('testa', client.getStringData('/a'))
      // when reconnecting from an expiration, ephemeral nodes are lost
      shouldFail(KeeperException.NoNodeException) {
        client.getStringData('/b')
      }      
    }
    finally
    {
      client.destroy()
    }

    th.waitForBlock('onDisconnected')
    th.unblock('onDisconnected')
  }

  /**
   * We verify that the listener gets the onConnected event even if registered after the start
   * of the client.
   */
  public void testListener()
  {
    ZKClient client
    client = new ZKClient('localhost:2121', Timespan.parse('1m'), null)
    client.reconnectTimeout = Timespan.parse('500')

    ThreadControl th = new ThreadControl()

    try
    {
      client.start()
      client.waitForStart(Timespan.parse('5s'))

      client.registerListener([
          onConnected: {
            th.block('onConnected')
          },

          onDisconnected: {
            th.block('onDisconnected')
          }
      ] as LifecycleListener)

      th.waitForBlock('onConnected')
      th.unblock('onConnected')

    }
    finally
    {
      client.destroy()
    }

    th.waitForBlock('onDisconnected')
    th.unblock('onDisconnected')
  }
}
