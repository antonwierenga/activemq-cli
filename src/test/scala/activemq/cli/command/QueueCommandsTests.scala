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
import activemq.cli.command.QueueCommandsTests._
import activemq.cli.util.Console._
import activemq.cli.command.CommandsTests._

import java.io.File
import org.springframework.shell.Bootstrap

class QueueCommandsTests {

  @Before
  def before = {
    assertTrue(shell.executeCommand("remove-all-queues --force").isSuccess)
  }

  @Test
  def testQueuesEmpty = {
    assertEquals(warn(s"No queues found for broker '${ActiveMQCLI.broker.get.jmxurl}'"), shell.executeCommand("queues").getResult)
  }

  @Test
  def testAddQueue = {
    assertEquals(info("Queue 'testQueue' added"), shell.executeCommand("add-queue --name testQueue").getResult)
    assertEquals(
      """|  Queue Name  Pending  Consumers  Enqueued  Dequeued
         |  ----------  -------  ---------  --------  --------
         |  testQueue   0        0          0         0
         |""".stripMargin,
      shell.executeCommand("queues").getResult
    )
  }

  @Test
  def testRemoveQueue = {
    assertEquals(info("Queue 'testQueue' added"), shell.executeCommand("add-queue --name testQueue").getResult)
    assertEquals(info("Queue 'testQueue' removed"), shell.executeCommand("remove-queue --name testQueue --force").getResult)
    assertEquals(warn(s"No queues found for broker '${ActiveMQCLI.broker.get.jmxurl}'"), shell.executeCommand("queues").getResult)
  }

  @Test
  def testPurgeQueue = {
    assertEquals(info("Messages sent to queue 'testQueue': 1"), shell.executeCommand("send-message --queue testQueue --body testMessage").getResult)
    assertEquals(
      """|  Queue Name  Pending  Consumers  Enqueued  Dequeued
         |  ----------  -------  ---------  --------  --------
         |  testQueue   1        0          1         0
         |""".stripMargin,
      shell.executeCommand("queues").getResult
    )

    assertEquals(info("Queue 'testQueue' purged"), shell.executeCommand("purge-queue --name testQueue --force").getResult)
    assertEquals(
      """|  Queue Name  Pending  Consumers  Enqueued  Dequeued
         |  ----------  -------  ---------  --------  --------
         |  testQueue   0        0          1         1
         |""".stripMargin,
      shell.executeCommand("queues").getResult
    )
  }

  @Test
  def testPurgeNonExistingQueue = {
    assertEquals(warn("Queue 'testQueue' does not exist"), shell.executeCommand("purge-queue --name testQueue --force").getResult)
  }

  @Test
  def testRemoveAllQueues = {
    assertEquals(info("Queue 'testQueue1' added"), shell.executeCommand("add-queue --name testQueue1").getResult)
    assertEquals(info("Queue 'testQueue2' added"), shell.executeCommand("add-queue --name testQueue2").getResult)
    assertEquals(info("Queues removed: 2"), shell.executeCommand("remove-all-queues --force").getResult)
    assertEquals(warn(s"No queues found for broker '${ActiveMQCLI.broker.get.jmxurl}'"), shell.executeCommand("queues").getResult)
  }

  @Test
  def testPurgeAllQueues = {
    assertEquals(info("Messages sent to queue 'testQueue1': 1"), shell.executeCommand("send-message --queue testQueue1 --body testMessage1").getResult)
    assertEquals(info("Messages sent to queue 'testQueue2': 1"), shell.executeCommand("send-message --queue testQueue2 --body testMessage2").getResult)

    assertEquals(
      """|  Queue Name  Pending  Consumers  Enqueued  Dequeued
         |  ----------  -------  ---------  --------  --------
         |  testQueue2  1        0          1         0
         |  testQueue1  1        0          1         0
         |""".stripMargin,
      shell.executeCommand("queues").getResult
    )

    assertEquals(info("Queues purged: 2"), shell.executeCommand("purge-all-queues --force").getResult)
    assertEquals(
      """|  Queue Name  Pending  Consumers  Enqueued  Dequeued
         |  ----------  -------  ---------  --------  --------
         |  testQueue2  0        0          1         1
         |  testQueue1  0        0          1         1
         |""".stripMargin,
      shell.executeCommand("queues").getResult
    )
  }

  @Test
  def testRemoveNonExistingQueue = {
    assertEquals(warn("Queue 'testQueue' does not exist"), shell.executeCommand("remove-queue --name testQueue --force").getResult)
  }

  @Test
  def testAddExistingQueue = {
    assertEquals(info("Queue 'testQueue' added"), shell.executeCommand("add-queue --name testQueue").getResult)
    assertEquals(warn("Queue 'testQueue' already exists"), shell.executeCommand("add-queue --name testQueue").getResult)
  }

  @Test
  def testAvailabilityIndicators: Unit = {
    assertTrue(shell.executeCommand("disconnect").isSuccess)
    try {
      List("queues", "add-queue", "purge-queue", "purge-all-queues", "remove-queue", "remove-all-queues").map(command ⇒ {
        assertCommandFailed(shell.executeCommand(command))
      })
    } finally {
      assertTrue(shell.executeCommand("connect --broker test").isSuccess)
    }
  }
}

object QueueCommandsTests {

  val shell = createShell

  @BeforeClass
  def beforeClass() = startAndConnectToEmbeddedBroker(shell)

  @AfterClass
  def afterClass() = stopEmbeddedBroker(shell)
}
