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
import org.apache.zookeeper.KeeperException
import org.apache.zookeeper.data.Stat
import org.linkedin.util.io.PathUtils

/**
 * Mimics the shell ls command
 */
public class DuCommand extends AbstractCommand
{

  public CliBuilder getCli()
  {
    def cli = new CliBuilder(usage: 'du [-r] <path> ...')
    cli.r(longOpt: 'recurse', 'recursively computes the du')
    return cli
  }

  public void doExecute(IZKClient client, OptionAccessor options)
  {
    boolean recurse = options.r
    def paths = options.arguments()

    if(log.isDebugEnabled())
    {
      log.debug "du command"
      log.debug "recurse=${recurse}, paths=${paths}"
    }


    Stat stat = new Stat()

    paths.each { String path ->
      long du = 0
      try
      {
        client.getACL(path, stat)
        du += stat.dataLength

        if(recurse)
        {
          def children = client.getAllChildren(path)
          children.each { String child ->
            def fullpath = PathUtils.addPaths(path, child)
            try
            {
              client.getACL(fullpath, stat)
              du += stat.dataLength
            }
            catch(KeeperException.NoNodeException e)
            {
              if(log.isDebugEnabled())
                log.debug("Node ${fullpath} has disapeared")
            }
          }
        }

        printf("%-7d ${path}\n", du)
      }
      catch(KeeperException.NoNodeException e)
      {
        if(log.isDebugEnabled())
          log.debug("Node ${path} has disapeared")
      }
    }
  }
}