/*
 * Copyright 2016 Anton Wierenga
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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull

import org.junit.Test
import org.junit.BeforeClass
import org.junit.AfterClass
import org.junit.Before
import org.junit.After
import org.springframework.shell.Bootstrap
import org.springframework.shell.core.CommandResult
import org.springframework.shell.core.JLineShellComponent
import activemq.cli.ActiveMQCLI
import activemq.cli.util.Console._
import activemq.cli.command.TopicCommandsTests._
import activemq.cli.command.CommandsTests._

import java.io.File
import org.springframework.shell.Bootstrap

class TopicCommandsTests {

  @Before
  def before = {
    assertTrue(shell.executeCommand("remove-all-topics --force").isSuccess)
  }

  @Test
  def testTopicsEmpty = {
    assertEquals(warn(s"No topics found for broker '${ActiveMQCLI.broker.get.jmxurl}'"), shell.executeCommand("topics --filter testTopic").getResult)
  }

  @Test
  def testAddTopic = {
    assertEquals(info("Topic 'testTopic' added"), shell.executeCommand("add-topic --name testTopic").getResult)
    assertEquals(
      """|  Topic Name  Enqueued  Dequeued
         |  ----------  --------  --------
         |  testTopic   0         0
         |""".stripMargin,
      shell.executeCommand("topics --filter testTopic").getResult
    )
  }

  @Test
  def testRemoveTopic = {
    assertEquals(info("Topic 'testTopic' added"), shell.executeCommand("add-topic --name testTopic").getResult)
    assertEquals(info("Topic 'testTopic' removed"), shell.executeCommand("remove-topic --name testTopic --force").getResult)
    assertEquals(warn(s"No topics found for broker '${ActiveMQCLI.broker.get.jmxurl}'"), shell.executeCommand("topics --filter testTopic").getResult)
  }

  @Test
  def testRemoveAllTopics = {
    assertEquals(info("Topic 'testTopic1' added"), shell.executeCommand("add-topic --name testTopic1").getResult)
    assertEquals(info("Topic 'testTopic2' added"), shell.executeCommand("add-topic --name testTopic2").getResult)
    assertEquals(info("Topics removed: 3"), shell.executeCommand("remove-all-topics --force").getResult)
    assertEquals(warn(s"No topics found for broker '${ActiveMQCLI.broker.get.jmxurl}'"), shell.executeCommand("topics --filter testTopic").getResult)
  }

  @Test
  def testRemoveNonExistingTopic = {
    assertEquals(warn("Topic 'testTopic' does not exist"), shell.executeCommand("remove-topic --name testTopic --force").getResult)
  }

  @Test
  def testAddExistingTopic = {
    assertEquals(info("Topic 'testTopic' added"), shell.executeCommand("add-topic --name testTopic").getResult)
    assertEquals(warn("Topic 'testTopic' already exists"), shell.executeCommand("add-topic --name testTopic").getResult)
  }

  @Test
  def testAvailabilityIndicators: Unit = {
    assertTrue(shell.executeCommand("disconnect").isSuccess)
    try {
      List("topics", "add-topic", "purge-topic", "purge-all-topics", "remove-topic", "remove-all-topics").map(command ⇒ {
        assertCommandFailed(shell.executeCommand(command))
      })
    } finally {
      assertTrue(shell.executeCommand("connect --broker test").isSuccess)
    }
  }

}

object TopicCommandsTests {

  val shell = createShell

  @BeforeClass
  def beforeClass() = {
    startAndConnectToEmbeddedBroker(shell)
  }

  @AfterClass
  def afterClass() = stopEmbeddedBroker(shell)
}
