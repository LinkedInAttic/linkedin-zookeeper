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

package org.linkedin.zookeeper.cli

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.linkedin.groovy.util.cli.CliUtils
import org.linkedin.util.clock.Timespan
import org.linkedin.zookeeper.cli.commands.CatCommand
import org.linkedin.zookeeper.cli.commands.Command
import org.linkedin.zookeeper.cli.commands.DeleteCommand
import org.linkedin.zookeeper.cli.commands.DuCommand
import org.linkedin.zookeeper.cli.commands.GetCommand
import org.linkedin.zookeeper.cli.commands.LsCommand
import org.linkedin.zookeeper.cli.commands.MkDirCommand
import org.linkedin.zookeeper.cli.commands.PutCommand
import org.linkedin.zookeeper.cli.commands.UploadCommand
import org.linkedin.zookeeper.client.IZKClient
import org.linkedin.zookeeper.client.ZKClient

/**
 * Zookeeper client app
 */
public class ClientMain
{
  public static final String MODULE = ClientMain.class.getName();
  public static final Logger log = LoggerFactory.getLogger(MODULE);

  private def config
  private Command command
  private int returnCode = 0
  private def commandArgs = []
  private IZKClient client

  private final static Map<String, Command> ACTIONS =
    [
        'get': new GetCommand(),
        'cat': new CatCommand(),
        'put': new PutCommand(),
        'upload': new UploadCommand(),
        'ls': new LsCommand(),
        'rm': new DeleteCommand(),
        'du': new DuCommand(),
        'mkdir': new MkDirCommand()
    ]

  def start()
  {
    def connectString = config.zkConnectionString ?: "127.0.0.1:2181"
    def connectTimeout = config.zkConnectionTimeout ?: "100"

    client = new ZKClient(connectString, Timespan.parse(connectTimeout), null)

    client.start()
    log.debug "Talking to zookeeper on ${connectString}"
    client.waitForStart(Timespan.parse('10s'))

    returnCode = command.execute(client, commandArgs)
  }

  def destroy()
  {
    client?.destroy()
  }

  static int mainNoExit(Object args)
  {
    def cli = new CliBuilder(usage: './bin/zk.sh [-h] [-s <zkConnectionString>] [-t <zkConnectionTimeout>] command')
    cli.h(longOpt: 'help', 'display help')
    cli.s(longOpt: 'zkConnectionString', 'the zookeeper connection string (host:port)', args: 1, required: false)
    cli.t(longOpt: 'zkConnectionTimeout', 'the zookeeper connection timeout (e.g. 10s)', args: 1, required: false)

    // this is splitting the main cmdline args from the command and its args
    boolean isMainArg = true
    def mainArgs = []
    def cmdArgs = []
    args.each {
      if (ACTIONS.keySet().contains(it))
      {
        isMainArg = false
      }
      if (isMainArg)
      {
        mainArgs << it
      }
      else
      {
        cmdArgs << it
      }
    }

    log.debug "Main args are ${mainArgs}"
    log.debug "CMD args are ${cmdArgs}"

    def options = CliUtils.parseCliOptions(cli: cli, args: mainArgs)

    if(!options || options.options.h || cmdArgs.size() == 0)
    {
      println "Allowed actions are ${ACTIONS.keySet()}!"
      cli.usage()
      return 0
    }

    Command cmd = ACTIONS.get(cmdArgs[0])

    cmdArgs = cmdArgs - cmdArgs[0]  // remove the name of the command from the list of args
    def clientMain = new ClientMain(config: options.config, command: cmd, commandArgs: cmdArgs)
    try
    {
      clientMain.start()
    }
    catch(ScriptException e)
    {
      log.error e.scriptStackTrace
      throw e
    }
    finally
    {
      clientMain.destroy()
    }

    return clientMain.returnCode
  }

  static void main(String[] args)
  {
    System.exit(mainNoExit(args))
  }
}