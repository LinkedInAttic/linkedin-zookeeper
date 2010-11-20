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
 * Encapsulate some boiler plate code for commands
 */
public abstract class AbstractCommand implements Command
{
  public static final String MODULE = AbstractCommand.class.getName();
  public static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MODULE);

  public int execute(IZKClient client, Object args)
  {
    CliBuilder cli = getCli()

    OptionAccessor options = cli.parse(args)

    if(!options)
    {
      cli.usage()
      return
    }

    try
    {
      doExecute(client, options)
      return 0
    }
    catch (Throwable th)
    {
      if(log.isDebugEnabled())
        log.debug("command exception", th)
      System.err.println(th.message)
      return 1
    }
  }

  /**
   * Gets the cli for the command
   */
  public abstract CliBuilder getCli()

  /**
   * Executes the actual command
   */
  public abstract void doExecute(IZKClient client, OptionAccessor options)
}