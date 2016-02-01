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
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.junit.Test
import org.springframework.shell.Bootstrap
import org.springframework.shell.core.CommandResult
import org.springframework.shell.core.JLineShellComponent
import scala.xml.XML

class MessageCommandsTests extends CommandsTests {

  @Before
  def before = {
    assertTrue(shell.executeCommand("remove-all-queues --force").isSuccess)
    assertTrue(shell.executeCommand("remove-all-topics --force").isSuccess)
  }

  @Test
  def testSaveMessageFileAlreadyExists = {
    val messageFile = File.createTempFile("MessageCommandsTests_testSaveMessageFileAlreadyExists", ".xml")
    try assertEquals(warn(s"File '${messageFile.getAbsolutePath}' already exists"), shell.executeCommand(s"save-messages --queue testQueue --file ${messageFile.getAbsolutePath}").getResult)
    finally messageFile.delete
  }

  @Test
  def testSendAndSaveInlineMessage = {
    assertEquals(info("Messages sent to queue 'testQueue': 1"), shell.executeCommand("send-message --queue testQueue --body testMessage").getResult)
    assertEquals(info("Messages listed: 1"), shell.executeCommand("list-messages --queue testQueue").getResult)

    val messageFilePath = createTempFilePath("MessageCommandsTests_testSendAndSaveMessage")
    try {
      assertEquals(info(s"Messages saved: 1"), shell.executeCommand(s"save-messages --queue testQueue --file $messageFilePath").getResult)
      println("*******************")
      println(XML.loadFile(messageFilePath))
      assertTrue(true)
    } finally new File(messageFilePath).delete

    /*assertEquals(
      """|  Queue Name  Pending  Consumers  Enqueued  Dequeued
         |  ----------  -------  ---------  --------  --------
         |  testQueue   1        0          1         0
         |""".stripMargin,
      shell.executeCommand("queues").getResult
    )*/
  }

  @Test
  def testListMessagesNonExistingQueue = {
    assertEquals(warn("Queue 'testQueue' does not exist"), shell.executeCommand("list-messages --queue testQueue").getResult)
  }

  @Test
  def testAvailabilityIndicators: Unit = {
    assertTrue(shell.executeCommand("disconnect").isSuccess)
    try {
      List("move-messages", "copy-messages", "list-messages", "send-message", "save-messages").map(command ⇒ {
        assertCommandFailed(shell.executeCommand(command))
      })
    } finally {
      assertTrue(shell.executeCommand("connect --broker test").isSuccess)
    }
  }
}