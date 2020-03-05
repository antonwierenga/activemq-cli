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

import activemq.cli.ActiveMQCLI
import activemq.cli.ActiveMQCLI.ApplicationOutputPath
import activemq.cli.util.Console._
import activemq.cli.util.Implicits._
import collection.JavaConversions._
import java.io.File
import java.io.FileWriter
import java.io.BufferedWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.UUID
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
import scala.concurrent.duration.Duration
import scala.xml.XML

@Component
class MessageCommands extends Commands {

  val JMSCorrelationID = ("JMSCorrelationID", "correlation-id")
  val JMSPriority = ("JMSPriority", "priority")
  val JMSDeliveryMode = ("JMSDeliveryMode", "delivery-mode")
  val JMSReplyTo = ("JMSReplyTo", "reply-to")
  val TimeToLive = ("timeToLive", "time-to-live")
  val JMSType = ("JMSType", "type")

  @CliAvailabilityIndicator(Array("move-messages", "copy-messages", "list-messages", "send-message", "export-messages"))
  def isBrokerAvailable: Boolean = ActiveMQCLI.broker.isDefined

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
  def sendMessage( //scalastyle:ignore
    @CliOption(key = Array("queue"), mandatory = false, help = "The name of the queue") queue: String,
    @CliOption(key = Array("topic"), mandatory = false, help = "The name of the topic") topic: String,
    @CliOption(key = Array("body"), mandatory = false, help = "The body of the message") body: String,
    @CliOption(key = Array("correlation-id"), mandatory = false, help = "The correlation id of the message") correlationId: String,
    @CliOption(key = Array("reply-to"), mandatory = false, help = "Name of the destination (topic or queue) the message replies should be sent to") replyTo: String,
    @CliOption(key = Array("delivery-mode"), mandatory = false, help = "The delivery mode of the message") deliveryMode: DeliveryMode,
    @CliOption(key = Array("time-to-live"), mandatory = false, help = "The time to live (in milliseconds) of the message") timeToLive: String,
    @CliOption(key = Array("priority"), mandatory = false, help = "The priority of the message") priority: String,
    @CliOption(key = Array("times"), mandatory = false, unspecifiedDefaultValue = "1", help = "The number of times the message is send") times: Int,
    @CliOption(key = Array("file"), mandatory = false, help = "The file containing messages to send") file: String
  ): String = {
    val start = System.currentTimeMillis
    val pFile = if (file) file.replaceFirst("^~", System.getProperty("user.home")) else file

    withBroker((brokerViewMBean: BrokerViewMBean, mBeanServerConnection: MBeanServerConnection) ⇒ {
      if (!file && !body) throw new IllegalArgumentException("Either --body or --file must be specified, but not both")
      if ((!queue && !topic) || (queue && topic)) throw new IllegalArgumentException("Either --queue or --topic must be specified, but not both")
      if (file) {
        if (!new File(pFile).exists) throw new IllegalArgumentException(s"File '$file' does not exist")
        try {
          if ((XML.loadFile(pFile) \ "jms-message").isEmpty) throw new IllegalArgumentException(s"No message found in '$file'")
        } catch {
          case spe: org.xml.sax.SAXParseException ⇒ throw new IllegalArgumentException(
            s"Error in $file line: ${spe.getLineNumber}, column: ${spe.getColumnNumber}, error: ${spe.getMessage}"
          )
        }
        if (replyTo || correlationId || Option(deliveryMode).isDefined || timeToLive || priority || timeToLive) {
          throw new IllegalArgumentException("When --file is specified only --queue or --topic is allowed")
        }
      }

      def sentToQueueOrTopic(headers: java.util.Map[String, String], body: String) = {
        if (queue) {
          MBeanServerInvocationHandler.newProxyInstance(mBeanServerConnection, getQueueObjectName(brokerViewMBean, queue), classOf[QueueViewMBean], true)
            .sendTextMessage(headers, body, ActiveMQCLI.broker.get.username, ActiveMQCLI.broker.get.password)
        } else {
          MBeanServerInvocationHandler.newProxyInstance(mBeanServerConnection, getTopicObjectName(brokerViewMBean, topic), classOf[TopicViewMBean], true)
            .sendTextMessage(headers, body, ActiveMQCLI.broker.get.username, ActiveMQCLI.broker.get.password)
        }
      }

      var totalSent = 0
      if (body) {
        val headers = new java.util.HashMap[String, String]()
        for (
          (key, value) ← Map(JMSReplyTo → replyTo, JMSCorrelationID → correlationId, JMSPriority → priority, TimeToLive → timeToLive,
            JMSDeliveryMode → Option(deliveryMode).getOrElse(DeliveryMode.PERSISTENT).getJMSDeliveryMode)
        ) {
          if (Option(value).isDefined) headers.put(key._1, value.toString)
        }
        for (i ← (1 to times)) yield {
          sentToQueueOrTopic(headers, body)
          totalSent += 1
        }
      } else { // file
        for (i ← (1 to times)) yield {
          (XML.loadFile(pFile) \ "jms-message").map(xmlMessage ⇒ {
            val headers = new java.util.HashMap[String, String]()
            Seq(JMSCorrelationID, JMSPriority, TimeToLive, JMSDeliveryMode, JMSReplyTo, JMSType).map(header ⇒ {
              if (!(xmlMessage \ "header" \ header._2).isEmpty) headers.put(header._1, (xmlMessage \ "header" \ header._2).text)
            })
            if (!headers.containsKey(JMSDeliveryMode._1)) headers.put(JMSDeliveryMode._1, DeliveryMode.PERSISTENT.getJMSDeliveryMode.toString)
            (xmlMessage \ "properties" \ "property").map(property ⇒ headers.put((property \ "name").text, (property \ "value").text))
            sentToQueueOrTopic(headers, (xmlMessage \ "body").text)
            totalSent += 1
          })
        }
      }
      val duration = System.currentTimeMillis - start
      formatDuration(duration)
      info(s"Messages sent to ${if (queue) s"queue '$queue'" else s"topic '$topic'"}: $totalSent${if (duration > 1000) s" (${formatDuration(duration)})" else ""}") //scalastyle:ignore
    })
  }

  @CliCommand(value = Array("export-messages"), help = "Exports messages to file")
  def exportMessages(
    @CliOption(key = Array("queue"), mandatory = true, help = "The name of the queue") queue: String,
    @CliOption(key = Array("selector"), mandatory = false, help = "the jms message selector") selector: String,
    @CliOption(key = Array("regex"), mandatory = false, help = "The regular expression the JMS text message must match") regex: String,
    @CliOption(key = Array("file"), mandatory = false, help = "The file that is used to save the messages in") file: String
  ): String = {
    val pFile = if (file) file.replaceFirst("^~", System.getProperty("user.home")) else file
    if (file && new File(pFile).exists()) {
      warn(s"File '$file' already exists")
    } else {
      val messageFile = Option(pFile).getOrElse(s"${queue}_${new SimpleDateFormat("ddMMyyyy_HHmmss").format(new Date())}.xml")
      val outputFile: File = Option(new File(messageFile).getParent) match {
        case Some(parent) ⇒ new File(messageFile)
        case _            ⇒ new File(ApplicationOutputPath, messageFile)
      }

      val bufferedWriter = new BufferedWriter(new FileWriter(outputFile))
      try {
        bufferedWriter.write("<jms-messages>\n")
        val result = withEveryMirrorQueueMessage(queue, Option(selector), Option(regex), s"Messages exported to ${outputFile.getCanonicalPath()}",
          (message: Message) ⇒ {
            bufferedWriter.write(s"${message.toXML(ActiveMQCLI.Config.getOptionalString("command.list-messages.timestamp-format"))}\n"
              .replaceAll("(?m)^", "  "))
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
    @CliOption(key = Array("queue"), mandatory = true, help = "The name of the queue") queue: String,
    @CliOption(key = Array("selector"), mandatory = false, help = "the JMS message selector") selector: String,
    @CliOption(key = Array("regex"), mandatory = false, help = "The regular expression the JMS text message must match") regex: String
  ): String = {
    withEveryMirrorQueueMessage(queue, Option(selector), Option(regex), "Messages listed", (message: Message) ⇒ {
      println(info(s"${message.toXML(ActiveMQCLI.Config.getOptionalString("command.list-messages.timestamp-format"))}\n")) //scalastyle:ignore
    })
  }
}
