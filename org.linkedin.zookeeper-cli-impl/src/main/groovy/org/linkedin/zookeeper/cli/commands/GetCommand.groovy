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
 * Get data and stats for a command
 */
public class GetCommand extends AbstractCommand
{

  public CliBuilder getCli()
  {
    def cli = new CliBuilder(usage: 'get <path> ...')
    return cli
  }

  public void doExecute(IZKClient client, OptionAccessor options)
  {
    def paths = options.arguments()

    log.debug "GET command"
    log.debug "paths=${paths}"

    // execute
    paths.each { path ->
      if (!client.exists(path))
      {
        println "${path} - NOT FOUND"
      }
      else
      {
        println "${path}:"
        def res = client.getByteDataAndStat(path)
        byte[] data = res.data
        def stat = res.stat
        println "cZxid = " + stat.czxid
        println "ctime = " + new Date(stat.ctime).toString()
        println "mZxid = " + stat.mzxid
        println "mtime = " + new Date(stat.mtime).toString()
        println "pZxid = " + stat.pzxid
        println "cversion = " + stat.cversion
        println "dataVersion = " + stat.version
        println "aclVersion = " + stat.aversion
        println "ephemeralOwner = " + stat.ephemeralOwner
        println "dataLength = " + stat.dataLength
        println "numChildren = " + stat.numChildren
        if (stat.dataLength > 0)
        {
          println new String(data , "UTF-8")
        }
        else
        {
          println "no data"
        }
      }
    }
  }

}