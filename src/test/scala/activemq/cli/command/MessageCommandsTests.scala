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
import activemq.cli.command.MessageCommandsTests._
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

class MessageCommandsTests {

  @Before
  def before = {
    assertTrue(shell.executeCommand("remove-all-queues --force").isSuccess)
    assertTrue(shell.executeCommand("remove-all-topics --force").isSuccess)
  }

  @Test
  def testExportMessageFileAlreadyExists = {
    val messageFile = File.createTempFile("MessageCommandsTests_testExportMessageFileAlreadyExists", ".xml")
    try assertEquals(warn(s"File '${messageFile.getAbsolutePath}' already exists"), shell.executeCommand(s"export-messages --queue testQueue --file ${messageFile.getAbsolutePath}").getResult)
    finally messageFile.delete
  }

  @Test
  def testSendAndExportInlineMessage = {
    assertEquals(info("Messages sent to queue 'testQueue': 1"), shell.executeCommand("send-message --queue testQueue --body testMessage").getResult)
    assertEquals(info("Messages listed: 1"), shell.executeCommand("list-messages --queue testQueue").getResult)

    val messageFilePath = createTempFilePath("MessageCommandsTests_testSendAndExportMessage")
    try {
      assertEquals(info(s"Messages exported to ${new File(messageFilePath).getCanonicalPath()}: 1"), shell.executeCommand(s"export-messages --queue testQueue --file $messageFilePath").getResult)
      val xml = XML.loadFile(messageFilePath)
      assertFalse((xml \ "jms-message" \ "header" \ "message-id").isEmpty)
      assertTrue((xml \ "jms-message" \ "header" \ "correlation-id").isEmpty)
      assertEquals("2", (xml \ "jms-message" \ "header" \ "delivery-mode") text)
      assertFalse((xml \ "jms-message" \ "header" \ "destination").isEmpty)
      assertEquals("0", (xml \ "jms-message" \ "header" \ "expiration") text)
      assertEquals("0", (xml \ "jms-message" \ "header" \ "priority") text)
      assertEquals("false", (xml \ "jms-message" \ "header" \ "redelivered") text)
      assertTrue((xml \ "jms-message" \ "header" \ "reply-to").isEmpty)
      assertFalse((xml \ "jms-message" \ "header" \ "timestamp").isEmpty)
      assertTrue((xml \ "jms-message" \ "header" \ "type").isEmpty)
      assertEquals("testMessage", (xml \ "jms-message" \ "body") text)
    } finally new File(messageFilePath).delete
  }

  @Test
  def testSendAndExportInlineMessageAllHeadersProvided = {
    assertEquals(info("Messages sent to queue 'testQueue': 1"), shell.executeCommand("send-message --queue testQueue --correlation-id testCorrelationId --delivery-mode NON_PERSISTENT --time-to-live 2000 --priority 1 --body testMessage").getResult)
    assertEquals(info("Messages listed: 1"), shell.executeCommand("list-messages --queue testQueue").getResult)

    val messageFilePath = createTempFilePath("MessageCommandsTests_testSendAndExportMessage")
    try {
      println("**********")
      println(new File(messageFilePath).getCanonicalPath())
      assertEquals(info(s"Messages exported to ${new File(messageFilePath).getCanonicalPath()}: 1"), shell.executeCommand(s"export-messages --queue testQueue --file $messageFilePath").getResult)
      val xml = XML.loadFile(messageFilePath)
      assertFalse((xml \ "jms-message" \ "header" \ "message-id").isEmpty)
      assertEquals("testCorrelationId", (xml \ "jms-message" \ "header" \ "correlation-id") text)
      assertEquals(s"${javax.jms.DeliveryMode.NON_PERSISTENT}", (xml \ "jms-message" \ "header" \ "delivery-mode") text)
      assertFalse((xml \ "jms-message" \ "header" \ "destination").isEmpty)
      assertEquals("1", (xml \ "jms-message" \ "header" \ "priority") text)
      assertEquals("false", (xml \ "jms-message" \ "header" \ "redelivered") text)
      assertTrue((xml \ "jms-message" \ "header" \ "reply-to").isEmpty)
      assertFalse((xml \ "jms-message" \ "header" \ "timestamp").isEmpty)
      assertTrue((xml \ "jms-message" \ "header" \ "type").isEmpty)
      assertEquals("testMessage", (xml \ "jms-message" \ "body") text)
    } finally new File(messageFilePath).delete
  }

  @Test
  def testSendInlineMessageTimes2 = {
    assertEquals(info("Messages sent to queue 'testQueue': 2"), shell.executeCommand("send-message --queue testQueue --body testMessage --times 2").getResult)
    assertEquals(info("Messages listed: 2"), shell.executeCommand("list-messages --queue testQueue").getResult)
  }

  @Test
  def testListMessagesNonExistingQueue = {
    assertEquals(warn("Queue 'testQueue' does not exist"), shell.executeCommand("list-messages --queue testQueue").getResult)
  }

  @Test
  def testSendInlineMessageToTopic = {
    assertEquals(info("Topic 'VirtualTopic.testTopic' added"), shell.executeCommand("add-topic --name VirtualTopic.testTopic").getResult)
    assertEquals(info("Queue 'Consumer.testQueue.VirtualTopic.testTopic' added"), shell.executeCommand("add-queue --name Consumer.testQueue.VirtualTopic.testTopic").getResult)
    assertEquals(info("Messages sent to topic 'VirtualTopic.testTopic': 1"), shell.executeCommand("send-message --topic VirtualTopic.testTopic --body testMessage").getResult)
    assertEquals(info("Messages listed: 1"), shell.executeCommand("list-messages --queue Consumer.testQueue.VirtualTopic.testTopic").getResult)
  }

  @Test
  def testAvailabilityIndicators: Unit = {
    assertTrue(shell.executeCommand("disconnect").isSuccess)
    try {
      List("move-messages", "copy-messages", "list-messages", "send-message", "export-messages").map(command ⇒ {
        assertCommandFailed(shell.executeCommand(command))
      })
    } finally {
      assertTrue(shell.executeCommand("connect --broker test").isSuccess)
    }
  }
}

object MessageCommandsTests {

  val shell = createShell

  @BeforeClass
  def beforeClass() = startAndConnectToEmbeddedBroker(shell)

  @AfterClass
  def afterClass() = stopEmbeddedBroker(shell)
}