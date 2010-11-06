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

package org.linkedin.zookeeper.cli.commands

import org.linkedin.zookeeper.client.IZKClient
import org.apache.zookeeper.data.Stat
import org.apache.zookeeper.KeeperException
import org.linkedin.util.io.PathUtils

/**
 * Mimics the shell ls command
 */
public class LsCommand extends AbstractCommand
{

  public CliBuilder getCli()
  {
    def cli = new CliBuilder(usage: 'ls [-l] [-r] <path> ...')
    cli.l(longOpt: 'list', 'display extra information for the elements')
    cli.r(longOpt: 'recurse', 'recursively display nodes')
    return cli
  }

  public void doExecute(IZKClient client, OptionAccessor options)
  {
    boolean list = options.l
    boolean recurse = options.r
    def paths = options.arguments()

    if(log.isDebugEnabled())
    {
      log.debug "LS command"
      log.debug "list=${list}, recurse=${recurse}, paths=${paths}"
    }

    
    // execute
    boolean summary = false
    if (paths.size() > 1)
    {
      summary = true
    }
    paths.each {String path ->
      if (!client.exists(path))
      {
        println "${path} - No such node"
      }
      else
      {
        if (summary) println "${path}:"
        def children = recurse ? client.getAllChildren(path) : client.getChildren(path)
        if(list)
        {
          Stat stat = new Stat()
          children.each { String child ->
            def fullpath = PathUtils.addPaths(path, child)
            try
            {
              def acl = client.getACL(fullpath, stat)
              printf("%-7d ${child}\n", stat.dataLength)
            }
            catch(KeeperException.NoNodeException e)
            {
              if(log.isDebugEnabled())
                log.debug("Node ${fullpath} has disapeared")
            }
          }
        }
        else
        {
          children.each {print "${it} "}
          println ""
        }
      }
    }
  }
}