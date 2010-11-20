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
 * Mimics the shell cat command
 */
public class CatCommand extends AbstractCommand
{

  public CliBuilder getCli()
  {
    def cli = new CliBuilder(usage: 'cat <path> ...')
    return cli
  }

  public void doExecute(IZKClient client, OptionAccessor options)
  {
    def paths = options.arguments()

    log.debug "CAT command"
    log.debug "paths=${paths}"

    // execute
    paths.each {path ->
      if (!client.exists(path))
      {
        log.warn ("Couldn't find path ${path}")
      }
      else
      {
        def res = client.getZKByteData(path)
        byte[] data = res.data
        if (data?.size() > 0)
        {
          data.each {b ->
            print new String(b)
          }
        }
      }
    }
  }

}