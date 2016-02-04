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
import activemq.cli.util.Console._
import activemq.cli.util.Implicits._
import collection.JavaConversions._
import java.io.File
import java.io.FileWriter
import java.io.BufferedWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.UUID
import javax.jms.DeliveryMode
import javax.jms.Message
import javax.jms.Session
import javax.management.MBeanServerConnection
import javax.management.MBeanServerInvocationHandler
import org.apache.activemq.broker.jmx.BrokerViewMBean
import org.apache.activemq.broker.jmx.TopicViewMBean
import org.apache.activemq.broker.jmx.QueueViewMBean
import org.springframework.shell.core.annotation.CliAvailabilityIndicator
import org.springframework.shell.core.annotation.CliCommand
import org.springframework.shell.core.annotation.CliOption
import org.springframework.stereotype.Component

@Component
class MessageCommands extends Commands {

  @CliAvailabilityIndicator(Array("move-messages", "copy-messages", "list-messages", "send-message", "save-messages"))
  def isBrokerAvailable: Boolean = {
    ActiveMQCLI.broker match {
      case Some(matched) ⇒
        true
      case _ ⇒
        false
    }
  }

  @CliCommand(value = Array("move-messages"), help = "Moves messages between queues")
  def moveMessages(
    @CliOption(key = Array("from"), mandatory = true, help = "The source queue") from: String,
    @CliOption(key = Array("to"), mandatory = true, help = "The target queue") to: String,
    @CliOption(key = Array("selector"), mandatory = false, help = "The message selector") selector: String
  ): String = {
    withBroker((brokerViewMBean: BrokerViewMBean, mBeanServerConnection: MBeanServerConnection) ⇒ {
      s"""Moved ${
        MBeanServerInvocationHandler.newProxyInstance(mBeanServerConnection, validateQueueExists(brokerViewMBean, from), classOf[QueueViewMBean], true)
          .moveMatchingMessagesTo(selector, to)
      } messages from '$from' to '$to'"""
    })
  }

  @CliCommand(value = Array("copy-messages"), help = "Copies messages between queues")
  def copyMessages(
    @CliOption(key = Array("from"), mandatory = true, help = "The source queue") from: String,
    @CliOption(key = Array("to"), mandatory = true, help = "The target queue") to: String,
    @CliOption(key = Array("selector"), mandatory = false, help = "The message selector") selector: String
  ): String = {
    withBroker((brokerViewMBean: BrokerViewMBean, mBeanServerConnection: MBeanServerConnection) ⇒ {
      s"""Copied ${
        MBeanServerInvocationHandler.newProxyInstance(mBeanServerConnection, validateQueueExists(brokerViewMBean, from), classOf[QueueViewMBean], true)
          .copyMatchingMessagesTo(selector, to)
      } messages from '$from' to '$to'"""
    })
  }

  @CliCommand(value = Array("send-message"), help = "Sends a message to a queue or topic")
  def sendMessage(
    @CliOption(key = Array("queue"), mandatory = false, help = "The name of the queue") queue: String,
    @CliOption(key = Array("topic"), mandatory = false, help = "The name of the topic") topic: String,
    @CliOption(key = Array("body"), mandatory = true, help = "The body of the message") body: String,
    @CliOption(key = Array("correlation-id"), mandatory = false, help = "The correlation id of the message") correlationId: String,
    @CliOption(key = Array("delivery-mode"), mandatory = false, help = "The delivery mode of the message") deliveryMode: String,
    @CliOption(key = Array("time-to-live"), mandatory = false, help = "The time to live (in milliseconds) of the message") timeToLive: String,
    @CliOption(key = Array("priority"), mandatory = false, help = "The priority of the message") priority: String
  ): String = {
    withBroker((brokerViewMBean: BrokerViewMBean, mBeanServerConnection: MBeanServerConnection) ⇒ {
      if ((!queue && !topic) || (queue && topic)) throw new IllegalArgumentException("Either --queue or --topic must be specified, but not both)")
      if (queue) {
        brokerViewMBean.addQueue(queue)
        val headers = new java.util.HashMap[String, Any]()
        if (Option(correlationId).isDefined) headers.put("JMSCorrelationID", correlationId)
        if (Option(deliveryMode).isDefined) headers.put("JMSDeliveryMode", deliveryMode) else headers.put("JMSDeliveryMode", DeliveryMode.PERSISTENT)
        if (Option(priority).isDefined) headers.put("JMSPriority", priority)
        if (Option(timeToLive).isDefined) headers.put("timeToLive", timeToLive)

        MBeanServerInvocationHandler.newProxyInstance(mBeanServerConnection, getQueueObjectName(brokerViewMBean, queue), classOf[QueueViewMBean], true)
          .sendTextMessage(headers, body)
      } else {
        brokerViewMBean.addTopic(topic)
        MBeanServerInvocationHandler.newProxyInstance(mBeanServerConnection, getTopicObjectName(brokerViewMBean, topic), classOf[TopicViewMBean], true)
          .sendTextMessage(body)
      }
      info(s"Messages sent to ${if (queue) s"queue '$queue'" else s"topic '$topic'"}: 1")
    })
  }

  @CliCommand(value = Array("save-messages"), help = "Saves messages to file")
  def saveMessages(
    @CliOption(key = Array("queue"), mandatory = false, help = "The name of the queue") queue: String,
    @CliOption(key = Array("selector"), mandatory = false, help = "the jms message selector") selector: String,
    @CliOption(key = Array("regex"), mandatory = false, help = "The regular expression the JMS text message must match") regex: String,
    @CliOption(key = Array("file"), mandatory = false, help = "The file that will used to save the messages in") file: String
  ): String = {
    if (file && new File(file).exists()) {
      warn(s"File '$file' already exists")
    } else {
      val bufferedWriter = new BufferedWriter(new FileWriter(new File(Option(file).getOrElse(s"$queue.txt"))))
      try {
        bufferedWriter.write("<jms-messages>\n")
        val result = withEveryMirrorQueueMessage(queue, selector, regex, "Messages saved", (message: Message) ⇒ {
          bufferedWriter.write(s"${message.toXML}\n")
        })
        bufferedWriter.write("</jms-messages>\n")
        result
      } finally {
        bufferedWriter.close
      }
    }
  }

  @CliCommand(value = Array("list-messages"), help = "Displays messages")
  def listMessages(
    @CliOption(key = Array("queue"), mandatory = false, help = "The name of the queue") queue: String,
    @CliOption(key = Array("selector"), mandatory = false, help = "the JMS message selector") selector: String,
    @CliOption(key = Array("regex"), mandatory = false, help = "The regular expression the JMS text message must match") regex: String
  ): String = {
    withEveryMirrorQueueMessage(queue, selector, regex, "Messages listed", (message: Message) ⇒ {
      println(info(s"${message.toXML}\n"))
    })
  }

  def withEveryMirrorQueueMessage(queue: String, selector: String, regex: String, message: String, callback: (Message) ⇒ Unit): String = {
    withBroker((brokerViewMBean: BrokerViewMBean, mBeanServerConnection: MBeanServerConnection) ⇒ {
      var callbacks = 0
      val mirrorQueue = s"activemq-cli.$queue.mirror.${new SimpleDateFormat("ddMMyyyy_HHmmss").format(new Date())}.${UUID.randomUUID().toString()}"
      val messagesCopied = MBeanServerInvocationHandler.newProxyInstance(mBeanServerConnection, validateQueueExists(brokerViewMBean, queue), classOf[QueueViewMBean], true)
        .copyMatchingMessagesTo(selector, mirrorQueue)
      withSession((session: Session) ⇒ {
        val messageConsumer = session.createConsumer(session.createQueue(mirrorQueue))
        var received = 0
        for (received ← 1 to messagesCopied) {
          val message = messageConsumer.receive()
          if (message.textMatches(regex)) {
            callback(message)
            callbacks += 1
          }
        }
      })
      brokerViewMBean.removeQueue(mirrorQueue)
      info(s"$message: $callbacks")
    })
  }
}