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
import org.apache.zookeeper.CreateMode
import org.apache.zookeeper.ZooDefs.Ids
import org.apache.zookeeper.KeeperException.NodeExistsException
import org.apache.zookeeper.KeeperException.NoNodeException

/**
 * creates a folder
 */
public class MkDirCommand extends AbstractCommand
{

  public CliBuilder getCli()
  {
    def cli = new CliBuilder(usage: 'mkdir <path1> [<path>]*')
    cli.p(longOpt: 'parent', 'create parents if necessary')
    return cli
  }

  public void doExecute(IZKClient client, OptionAccessor options)
  {
    def paths = options.arguments()
    def createParents = options.p

    log.debug "mkdir paths=${paths}, createParents=${createParents}"

    def res = 0

    // execute
    paths.each { path ->
      if(createParents)
      {
        try
        {
          client.createWithParents(path, '', Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT)
        }
        catch (NodeExistsException e)
        {
          if(log.isDebugEnabled())
            log.debug("ignoring exception with -p option...", e)
        }
      }
      else
      {
        try
        {
          client.create(path, '', Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT)
        }
        catch (NodeExistsException e)
        {
          throw new CommandException("mkdir: ${path}: Node exists", e)
        }
        catch (NoNodeException e)
        {
          throw new CommandException("mkdir: ${path}: No such node", e)
        }
      }
    }
  }

}