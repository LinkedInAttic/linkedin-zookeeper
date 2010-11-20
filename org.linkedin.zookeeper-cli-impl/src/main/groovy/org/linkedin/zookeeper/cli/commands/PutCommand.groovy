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

import org.apache.zookeeper.ZooDefs.Ids
import org.apache.zookeeper.CreateMode
import org.linkedin.zookeeper.client.IZKClient

/**
 * Put a string into a node
 */
public class PutCommand extends AbstractCommand
{

  public CliBuilder getCli()
  {
    def cli = new CliBuilder(usage: 'put [-f] <string> <path> ...')
    cli.f(longOpt: 'force', 'force put')
    return cli
  }

  public void doExecute(IZKClient client, OptionAccessor options)
  {
    def data = options.arguments()[0]
    def paths = options.arguments()[1..-1]
    boolean force = options.f

    log.debug "PUT command"
    log.debug "force=${force}, data=${data}, paths=${paths}, options=${options.arguments()}"

    // execute
    paths.each { path ->
      if (!client.exists(path) || force)
      {
        if (client.exists(path))
        {
          client.setByteData(path, data.bytes)
          println "${path} - OVERWRITING"
        }
        else
        {
          client.createBytesNodeWithParents(path, data.bytes, Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT)
        }
      }
      else
      {
        println "${path} - SKIPPED (already exists)"
      }
    }
  }

}