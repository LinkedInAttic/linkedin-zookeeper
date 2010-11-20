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

/**
 * Deletes a node (could be recursive)
 */
public class DeleteCommand extends AbstractCommand
{

  public CliBuilder getCli()
  {
    def cli = new CliBuilder(usage: 'rm [-r] <path> ...')
    cli.r(longOpt: 'recurse', 'recursively delete nodes')
    return cli

  }

  public void doExecute(IZKClient client, OptionAccessor options)
  {
    boolean recurse = options.r
    // valid paths start with a /
    def paths = options.arguments()

    log.debug "RM command"
    log.debug "recurse=${recurse}, paths=${paths}"

    // execute
    paths.each {path ->
      if (!client.exists(path))
      {
        println "${path} - NOT FOUND"
      }
      else
      {
        def children = client.getChildren(path)
        log.debug "children: ${children}"
        if (recurse || !children)
        {
          delete(client, path)
          println "${path} - DELETED"
        }
        else
        {
          println "${path} - CANNOT DELETE (not empty)"
        }
      }
    }
  }

  /**
   * delete a node and its children
   */
  private void delete(IZKClient client, String path)
  {
    client.getChildren(path).each { delete(client, "${path}/${it}") }
    client.delete(path)
  }

}