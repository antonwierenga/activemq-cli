/*
 * Copyright 2015 Anton Wierenga
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package activemq.cli.command

import activemq.cli.ActiveMQCLI
import activemq.cli.command.CommandsTests._
import activemq.cli.util.Console._
import java.io.File
import org.junit.After
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.springframework.shell.Bootstrap
import org.springframework.shell.core.CommandResult
import org.springframework.shell.core.JLineShellComponent

class CommandsTests {

}

object CommandsTests {

  System.setProperty("config.file", "./src/test/resources/activemq-cli.config")
  val shell = new Bootstrap().getJLineShellComponent()

  @BeforeClass
  def beforeClass() = {
    assertTrue(shell.executeCommand("start-embedded-broker").isSuccess)
    assertTrue(shell.executeCommand("connect --broker test").isSuccess)
  }

  @AfterClass
  def afterClass() = {
    assertTrue(shell.executeCommand("stop-embedded-broker").isSuccess)
  }

  def createTempFilePath(prefix: String): String = {
    val file = File.createTempFile(prefix, ".xml")
    file.delete
    file.getAbsolutePath
  }

  def assertCommandFailed(commandResult: CommandResult) {
    assertFalse(commandResult.isSuccess)
    assertNull(commandResult.getResult)
    assertNull(commandResult.getException)
  }
}

