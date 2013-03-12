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

import org.apache.zookeeper.ZooDefs.Ids
import org.apache.zookeeper.CreateMode
import junit.framework.AssertionFailedError
import org.linkedin.groovy.util.io.fs.FileSystemImpl
import org.linkedin.zookeeper.server.StandaloneZooKeeperServer
import org.linkedin.zookeeper.client.ZKClient
import org.linkedin.util.clock.Timespan
import org.linkedin.zookeeper.tracker.ZooKeeperTreeTracker
import org.linkedin.zookeeper.tracker.NodeEventsListener
import org.linkedin.zookeeper.tracker.NodeEventType
import org.linkedin.zookeeper.tracker.NodeEvent
import java.util.concurrent.TimeoutException
import org.linkedin.zookeeper.tracker.TrackedNode
import org.linkedin.zookeeper.client.IZKClient
import org.linkedin.util.io.PathUtils
import org.linkedin.zookeeper.tracker.ZKStringDataReader
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * @author ypujante@linkedin.com */
class TestZooKeeperTreeTracker extends GroovyTestCase
{
  public static final String MODULE = TestZooKeeperTreeTracker.class.getName();
  public static final Logger log = LoggerFactory.getLogger(MODULE);

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

  public void testClient()
  {
    ZKClient client
    client = new ZKClient('127.0.0.1:2121', Timespan.parse('1m'), null)

    try
    {
      client.start()
      client.waitForStart(Timespan.parse('10s'))

      createNode(client, '/root', 'rootContent')

      def newEvents = []
      def listener = { events ->
        events.each { newEvents << it }
      }

      def tracker = new ZooKeeperTreeTracker(client, new ZKStringDataReader(), '/root', 2)

      try
      {
        // starting to track
        tracker.track(listener as NodeEventsListener)
        checkTracker(tracker, newEvents)
        checkEvents([[type: NodeEventType.ADDED, path: '/root', data: 'rootContent']], newEvents)

        // adding a node at depth 1
        createNode(client, '/root/depth1_node1', 'depth1_node1')
        checkTracker(tracker, newEvents)
        checkEvents([[type: NodeEventType.ADDED, path: '/root/depth1_node1', data: 'depth1_node1']], newEvents)

        // adding a node at depth 2
        createNode(client, '/root/depth1_node1/depth2_node1', 'depth2_node1')
        checkTracker(tracker, newEvents)
        checkEvents([[type: NodeEventType.ADDED, path: '/root/depth1_node1/depth2_node1', data: 'depth2_node1']], newEvents)

        // adding a node at depth 3 (no events because not tracked)
        createNode(client, '/root/depth1_node1/depth2_node1/depth3_node1', 'depth3_node1')
        checkTracker(tracker, newEvents)
        checkEvents([], newEvents)

        // updating node at depth2
        client.setData('root/depth1_node1/depth2_node1', 'depth2_node1_new')
        checkTracker(tracker, newEvents)
        checkEvents([[type: NodeEventType.UPDATED, path: '/root/depth1_node1/depth2_node1', data: 'depth2_node1_new']], newEvents)

        // updating node at depth2 with same value (no events)
        client.setData('root/depth1_node1/depth2_node1', 'depth2_node1_new')
        checkTracker(tracker, newEvents)
        checkEvents([], newEvents)

        // updating node at depth1
        client.setData('root/depth1_node1', 'depth1_node1_new')
        checkTracker(tracker, newEvents)
        checkEvents([[type: NodeEventType.UPDATED, path: '/root/depth1_node1', data: 'depth1_node1_new']], newEvents)

        // updating node at depth2 again
        client.setData('root/depth1_node1/depth2_node1', 'depth2_node1')
        checkTracker(tracker, newEvents)
        checkEvents([[type: NodeEventType.UPDATED, path: '/root/depth1_node1/depth2_node1', data: 'depth2_node1']], newEvents)

        // adding a node at depth 2
        createNode(client, '/root/depth1_node1/depth2_node2', 'depth2_node2')
        checkTracker(tracker, newEvents)
        checkEvents([[type: NodeEventType.ADDED, path: '/root/depth1_node1/depth2_node2', data: 'depth2_node2']], newEvents)

        // deleting node at depth2 and adding another one
        client.delete('root/depth1_node1/depth2_node2')
        createNode(client, '/root/depth1_node1/depth2_node3', 'depth2_node3')
        checkTracker(tracker, newEvents)
        checkEvents([[type: NodeEventType.DELETED, path: '/root/depth1_node1/depth2_node2', data: 'depth2_node2'],
                    [type: NodeEventType.ADDED, path: '/root/depth1_node1/depth2_node3', data: 'depth2_node3']], newEvents)

        // delete all nodes
        client.deleteWithChildren('root/depth1_node1')
        checkTracker(tracker, newEvents)
        checkEvents([[type: NodeEventType.DELETED, path: '/root/depth1_node1', data: 'depth1_node1_new'],
                    [type: NodeEventType.DELETED, path: '/root/depth1_node1/depth2_node1', data: 'depth2_node1'],
                    [type: NodeEventType.DELETED, path: '/root/depth1_node1/depth2_node3', data: 'depth2_node3']], newEvents)
      }
      finally
      {
        tracker.destroy()
      }
    }
    finally
    {
      client.destroy()
    }
  }

  /**
   * This test tries to reproduce the problem described in GLU-190: create/add/update children
   * in a very fast fashion and make sure we do not loose events
   */
  public void testRaceCondition()
  {
    ZKClient client
    client = new ZKClient('127.0.0.1:2121', Timespan.parse('1m'), null)

    try
    {
      client.start()
      client.waitForStart(Timespan.parse('10s'))

      def idx = 1

      while(idx < 2)
      {
        def root = "/root${idx++}".toString()

        log.debug "executing for ${root}"

        createNode(client, root, 'rootContent')

        def newEvents = []
        def listener = { events ->
          events.each { newEvents << it }
        }

        def tracker = new ZooKeeperTreeTracker(client, new ZKStringDataReader(), root, 2)

        try
        {
          // starting to track
          tracker.track(listener as NodeEventsListener)
          checkTracker(tracker, newEvents)
          checkEvents([[type: NodeEventType.ADDED, path: root, data: 'rootContent']], newEvents)

          def hostsCount = 10

          def threads = []

          def rnd = new Random()

          (1..hostsCount).each { host ->
            def thread = Thread.start {
              createNode(client, "${root}/h${host}", "host h${host} content")
              Thread.sleep(rnd.nextInt(100))

              def iterationsCount = rnd.nextInt(100) + 100

              def childrenCount = rnd.nextInt(4) + 1

              (1..iterationsCount).each { iter ->
                (0..childrenCount).each { child ->
                  def path = "${root}/h${host}/c${child}"
                  if(client.exists(path))
                  {
                    if(rnd.nextBoolean())
                      client.setData(path, "U: h${host}/${child} -> ${iter}")
                    else
                      client.delete(path)
                  }
                  else
                    createNode(client, path, "C: h${host}/${child} -> ${iter}")
                }
                Thread.sleep(rnd.nextInt(10))
              }


            }
            threads << thread
          }

          threads.each { it.join() }
          checkTracker(tracker, newEvents)
          client.deleteWithChildren(root)
        }
        finally
        {
          tracker.destroy()
        }
      }
    }
    finally
    {
      client.destroy()
    }
  }

  private void checkEvents(def expectedEvents, def receivedEvents)
  {
    try
    {
      receivedEvents = receivedEvents.sort { it.node.path }
      assertEquals(expectedEvents.size(), receivedEvents.size())
      receivedEvents.eachWithIndex { NodeEvent event, idx ->
        def expectedEvent = expectedEvents[idx]
        assertEquals(expectedEvent.type, event.eventType)
        assertEquals(expectedEvent.path, event.path)
        assertEquals(expectedEvent.data, event.node.data)
      }
    }
    catch(AssertionFailedError e)
    {
      def error = new AssertionFailedError("${expectedEvents} != ${receivedEvents}")
      error.initCause(e)
      throw error
    }

    receivedEvents.clear()
  }

  private void checkTracker(ZooKeeperTreeTracker tracker, newEvents)
  {
    def expectedTree = readTree(tracker.ZKCient, tracker.root, tracker.depth, 0, new TreeMap())

    def expectedLastZkTxId = expectedTree.values().zkTxId.max()

    try
    {
      def lastZkTxId = tracker.waitForZkTxId(expectedLastZkTxId, '10s')
      assertEquals(expectedLastZkTxId, lastZkTxId)
    }
    catch(TimeoutException e)
    {
      println "######## Issue with waitForZkTxId for ${expectedLastZkTxId}"
      println "### Events:"
      println newEvents.join('\n')

      println "### Expected tree:"
      expectedTree.each { k,v ->
        println "${k} | ${v}"
      }
      println "### Computed tree:"
      tracker.tree.each {
        k,v ->
        println "${k} | ${v}"
      }
      throw e
    }


    def tree = new TreeMap(tracker.tree)
    assertEquals(expectedTree.size(), tree.size())
    expectedTree.values().each { TrackedNode expectedTrackedNode ->
      TrackedNode trackedNode = tree[expectedTrackedNode.path]
      assertNotNull(expectedTrackedNode.path, trackedNode)
      assertEquals(expectedTrackedNode.depth, trackedNode.depth)
      assertEquals(expectedTrackedNode.data, trackedNode.data)
    }
  }

  private def readTree(IZKClient client, String path, int maxDepth, int depth, def tree)
  {
    def res = client.getZKStringData(path)
    tree[path] = new TrackedNode(path: path, data: res.data, stat: res.stat, depth: depth)

    if(depth < maxDepth)
    {
      client.getChildren(path)?.each { String child ->
        def childPath = PathUtils.addPaths(path, child)
        readTree(client, childPath, maxDepth, depth + 1, tree)
      }
    }
    else
    {
      // adjusting the pzxid for leaves as they are not tracked by the tracker
      tree[path].stat.pzxid = tree[path].stat.mzxid
    }

    return tree
  }



  private void createNode(ZKClient client, String path, String content)
  {
    client.createWithParents(path, content, Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT)
  }
}
