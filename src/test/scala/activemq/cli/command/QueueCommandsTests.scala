/*
 * Copyright 2017 Anton Wierenga
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
    assertEquals(warn(s"No queues found"), shell.executeCommand("list-queues").getResult)
  }

  @Test
  def testAddQueue = {
    assertEquals(info("Queue 'testQueue' added"), shell.executeCommand("add-queue --name testQueue").getResult)
    assertEquals(
      """|  Queue Name  Pending  Consumers  Enqueued  Dequeued
         |  ----------  -------  ---------  --------  --------
         |  testQueue   0        0          0         0
         |
         |Total queues: 1""".stripMargin,
      shell.executeCommand("list-queues").getResult
    )
  }

  @Test
  def testRemoveQueue = {
    assertEquals(info("Queue 'testQueue' added"), shell.executeCommand("add-queue --name testQueue").getResult)
    assertEquals(info("Queue 'testQueue' removed"), shell.executeCommand("remove-queue --name testQueue --force").getResult)
    assertEquals(warn(s"No queues found"), shell.executeCommand("list-queues").getResult)
  }

  @Test
  def testRemoveQueueWithAsteriskAndColon = {
    assertEquals(info("Queue 'test*:Queue' added"), shell.executeCommand("add-queue --name test*:Queue").getResult)
    assertEquals(info("Queue 'test*:Queue' removed"), shell.executeCommand("remove-queue --name test*:Queue --force").getResult)
    assertEquals(warn(s"No queues found"), shell.executeCommand("list-queues").getResult)
  }

  @Test
  def testPurgeQueue = {
    assertEquals(info("Messages sent to queue 'testQueue': 1"), shell.executeCommand("send-message --queue testQueue --body testMessage").getResult)
    assertEquals(
      """|  Queue Name  Pending  Consumers  Enqueued  Dequeued
         |  ----------  -------  ---------  --------  --------
         |  testQueue   1        0          1         0
         |
         |Total queues: 1""".stripMargin,
      shell.executeCommand("list-queues").getResult
    )

    assertEquals(info("Queue 'testQueue' purged"), shell.executeCommand("purge-queue --name testQueue --force").getResult)
    assertEquals(
      """|  Queue Name  Pending  Consumers  Enqueued  Dequeued
         |  ----------  -------  ---------  --------  --------
         |  testQueue   0        0          1         1
         |
         |Total queues: 1""".stripMargin,
      shell.executeCommand("list-queues").getResult
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
    assertEquals(
      """|Queue to be removed: 'testQueue1'
         |Queue to be removed: 'testQueue2'
         |Total queues to be removed: 2""".stripMargin,
      shell.executeCommand("remove-all-queues --dry-run").getResult
    )

    assertEquals(
      """|  Queue Name  Pending  Consumers  Enqueued  Dequeued
         |  ----------  -------  ---------  --------  --------
         |  testQueue2  0        0          0         0
         |  testQueue1  0        0          0         0
         |
         |Total queues: 2""".stripMargin,
      shell.executeCommand("list-queues").getResult
    )
    assertEquals(
      """|Queue removed: 'testQueue1'
         |Queue removed: 'testQueue2'
         |Total queues removed: 2""".stripMargin,
      shell.executeCommand("remove-all-queues --force").getResult
    )
    assertEquals(warn(s"No queues found"), shell.executeCommand("list-queues").getResult)
  }

  @Test
  def testPurgeAllQueuesDryRun = {
    assertEquals(info("Messages sent to queue 'testQueue1': 1"), shell.executeCommand("send-message --queue testQueue1 --body testMessage1").getResult)
    assertEquals(info("Messages sent to queue 'testQueue2': 1"), shell.executeCommand("send-message --queue testQueue2 --body testMessage2").getResult)

    assertEquals(
      """|  Queue Name  Pending  Consumers  Enqueued  Dequeued
         |  ----------  -------  ---------  --------  --------
         |  testQueue2  1        0          1         0
         |  testQueue1  1        0          1         0
         |
         |Total queues: 2""".stripMargin,
      shell.executeCommand("list-queues").getResult
    )

    assertEquals(
      """|Queue to be purged: 'testQueue1'
         |Queue to be purged: 'testQueue2'
         |Total queues to be purged: 2""".stripMargin,
      shell.executeCommand("purge-all-queues --force --dry-run").getResult
    )

    assertEquals(
      """|  Queue Name  Pending  Consumers  Enqueued  Dequeued
         |  ----------  -------  ---------  --------  --------
         |  testQueue2  1        0          1         0
         |  testQueue1  1        0          1         0
         |
         |Total queues: 2""".stripMargin,
      shell.executeCommand("list-queues").getResult
    )
  }

  @Test
  def testPurgeAllQueuesFilter = {
    assertEquals(info("Messages sent to queue 'testQueue1': 1"), shell.executeCommand("send-message --queue testQueue1 --body testMessage1").getResult)
    assertEquals(info("Messages sent to queue 'testQueue2': 1"), shell.executeCommand("send-message --queue testQueue2 --body testMessage2").getResult)

    assertEquals(
      """|  Queue Name  Pending  Consumers  Enqueued  Dequeued
         |  ----------  -------  ---------  --------  --------
         |  testQueue2  1        0          1         0
         |  testQueue1  1        0          1         0
         |
         |Total queues: 2""".stripMargin,
      shell.executeCommand("list-queues").getResult
    )

    assertEquals(
      """|Queue purged: 'testQueue2'
         |Total queues purged: 1""".stripMargin,
      shell.executeCommand("purge-all-queues --force --filter 2").getResult
    )

    assertEquals(
      """|  Queue Name  Pending  Consumers  Enqueued  Dequeued
         |  ----------  -------  ---------  --------  --------
         |  testQueue2  0        0          1         1
         |  testQueue1  1        0          1         0
         |
         |Total queues: 2""".stripMargin,
      shell.executeCommand("list-queues").getResult
    )
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
         |
         |Total queues: 2""".stripMargin,
      shell.executeCommand("list-queues").getResult
    )

    assertEquals(
      """|Queue purged: 'testQueue1'
         |Total queues purged: 1""".stripMargin,
      shell.executeCommand("purge-all-queues --force --filter testQueue1").getResult
    )

    assertEquals(
      """|Queue purged: 'testQueue1'
         |Queue purged: 'testQueue2'
         |Total queues purged: 2""".stripMargin,
      shell.executeCommand("purge-all-queues --force").getResult
    )

    assertEquals(
      """|  Queue Name  Pending  Consumers  Enqueued  Dequeued
         |  ----------  -------  ---------  --------  --------
         |  testQueue2  0        0          1         1
         |  testQueue1  0        0          1         1
         |
         |Total queues: 2""".stripMargin,
      shell.executeCommand("list-queues").getResult
    )
  }

  @Test
  def testListQueuesFilters = {
    assertEquals(info("Messages sent to queue 'testQueue': 1"), shell.executeCommand("send-message --queue testQueue --body testMessage1").getResult)

    List(
      "list-queues --pending =1",
      "list-queues --pending >0",
      "list-queues --pending <2",
      "list-queues --enqueued =1",
      "list-queues --enqueued >0",
      "list-queues --enqueued <2",
      "list-queues --dequeued =0",
      "list-queues --dequeued >-1",
      "list-queues --dequeued <1",
      "list-queues --consumers =0",
      "list-queues --consumers >-1",
      "list-queues --consumers <1",
      "list-queues --pending >0 --consumers <1",
      "list-queues --enqueued >0 --dequeued <1"

    ).map { command ⇒
        assertEquals("""|  Queue Name  Pending  Consumers  Enqueued  Dequeued
                        |  ----------  -------  ---------  --------  --------
                        |  testQueue   1        0          1         0
                        |
                        |Total queues: 1""".stripMargin, shell.executeCommand(command).getResult)
      }

    assertFalse(List(
      "list-queues --pending =0",
      "list-queues --pending >1",
      "list-queues --pending <1",
      "list-queues --enqueued =0",
      "list-queues --enqueued >1",
      "list-queues --enqueued <1",
      "list-queues --dequeued =1",
      "list-queues --dequeued >0",
      "list-queues --dequeued <0",
      "list-queues --consumers =1",
      "list-queues --consumers >0",
      "list-queues --consumers <0",
      "list-queues --pending >0 --consumers >0",
      "list-queues --enqueued >0 --dequeued >0"
    ).map { command ⇒
        assertEquals(warn("No queues found"), shell.executeCommand(command).getResult)
      }.isEmpty)
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
      List("list-queues", "add-queue", "purge-queue", "purge-all-queues", "remove-queue", "remove-all-queues").map(command ⇒ {
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
