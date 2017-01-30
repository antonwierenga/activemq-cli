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
import activemq.cli.domain.Broker
import activemq.cli.util.Console._
import activemq.cli.util.Implicits._
import java.io.File
import javax.jms.Session
import javax.management.MBeanServerConnection
import javax.management.MBeanServerInvocationHandler
import org.apache.activemq.broker.jmx.BrokerViewMBean
import org.apache.activemq.broker.jmx.QueueViewMBean
import org.springframework.shell.core.annotation.CliAvailabilityIndicator
import org.springframework.shell.core.annotation.CliCommand
import org.springframework.shell.core.annotation.CliOption
import org.springframework.stereotype.Component
import scala.tools.jline.console.ConsoleReader
import java.io.BufferedWriter
import java.util.Date
import java.io.FileWriter
import java.text.SimpleDateFormat
import javax.jms.Message

@Component
class BrokerCommands extends Commands {

  @CliAvailabilityIndicator(Array("info", "disconnect", "export-broker"))
  def isBrokerAvailable: Boolean = ActiveMQCLI.broker.isDefined

  @CliCommand(value = Array("info"), help = "Displays broker info")
  def brokerInfo(): String = {
    withBroker((brokerViewMBean: BrokerViewMBean, mBeanServerConnection: MBeanServerConnection) ⇒ {
      val queues = brokerViewMBean.getQueues
      val messageTotal = queues.par.map({ objectName ⇒
        (MBeanServerInvocationHandler.newProxyInstance(mBeanServerConnection, objectName, classOf[QueueViewMBean], true))
      }).par.map(queueViewMBean ⇒ queueViewMBean.getQueueSize).sum

      renderTable(List(List(brokerViewMBean.getBrokerId, brokerViewMBean.getBrokerName, brokerViewMBean.getBrokerVersion,
        s"${brokerViewMBean.getMemoryPercentUsage}%", s"${brokerViewMBean.getStorePercentUsage}%", brokerViewMBean.getUptime, queues.size,
        brokerViewMBean.getTopics.size, messageTotal, brokerViewMBean.getTotalConsumerCount)), List("Broker ID", "Broker Name", "Broker Version",
        "Memory Limit used", "Store Limit used", "Uptime", "Queues", "Topics", "Messages", "Consumers"))
    })
  }

  @CliCommand(value = Array("export-broker"), help = "Exports topics, queues and messages")
  def exportBroker(
    @CliOption(key = Array("file"), mandatory = false, help = "The file that will used to for the export") file: String
  ): String = {
    val backupFile = Option(file).getOrElse(s"backup_${new SimpleDateFormat("ddMMyyyy_HHmmss").format(new Date())}.xml")
    if (new File(backupFile).exists()) {
      warn(s"File '${new File(backupFile).getCanonicalPath()}' already exists")
    } else {
      val outputFile: File = Option(new File(backupFile).getParent) match {
        case Some(parent) ⇒ new File(backupFile)
        case _            ⇒ new File(ApplicationOutputPath, backupFile)
      }

      val bufferedWriter = new BufferedWriter(new FileWriter(outputFile))
      try {
        bufferedWriter.write("<broker>\n")
        val result = withBroker((brokerViewMBean: BrokerViewMBean, mBeanServerConnection: MBeanServerConnection) ⇒ {
          brokerViewMBean.getTopics.sortWith(getDestinationKeyProperty(_) < getDestinationKeyProperty(_)).map(objectName ⇒ bufferedWriter.write(
            s"""  <topic name="${getDestinationKeyProperty(objectName)}"/>\n"""
          ))
          brokerViewMBean.getQueues.sortWith(getDestinationKeyProperty(_) < getDestinationKeyProperty(_)).map(objectName ⇒ {
            var totalMessages = 0
            withEveryMirrorQueueMessage(getDestinationKeyProperty(objectName), None, None, "", (message: Message) ⇒ {
              totalMessages += 1
              if (totalMessages == 1) bufferedWriter.write(s"""  <queue name="${getDestinationKeyProperty(objectName)}">\n""")
              bufferedWriter.write(s"${message.toXML(ActiveMQCLI.Config.getOptionalString("command.export-broker.timestamp-format"))}\n"
                .replaceAll("(?m)^", "    "))
            })

            if (totalMessages == 0) {
              bufferedWriter.write(s"""  <queue name="${getDestinationKeyProperty(objectName)}"/>\n""")
            } else {
              bufferedWriter.write(s"  </queue>\n")
            }
          })
          s"Broker exported to ${outputFile.getCanonicalPath()}"
        })
        bufferedWriter.write("</broker>\n")
        result
      } finally {
        bufferedWriter.close
      }
    }
  }

  @CliCommand(value = Array("disconnect"), help = "Disconnect from the broker")
  def disconnect(): String = {
    ActiveMQCLI.broker = None
    info(s"Disconnected from broker")
  }

  @CliCommand(value = Array("connect"), help = "Connects to a broker")
  def connect(
    @CliOption(key = Array("broker"), mandatory = true, help = "The Broker Alias") pBroker: String
  ): String = {

    try {
      if (!ActiveMQCLI.Config.hasPath(s"broker.$pBroker")) throw new IllegalArgumentException(s"Broker '$pBroker' not found in ${new File(System.getProperty("config.file")).getCanonicalPath}") //scalastyle:ignore
      if (!ActiveMQCLI.Config.hasPath(s"broker.$pBroker.jmxurl")) throw new IllegalArgumentException(s"jmxurl not found for broker '$pBroker' in ${new File(System.getProperty("config.file")).getCanonicalPath}") //scalastyle:ignore
      if (!ActiveMQCLI.Config.hasPath(s"broker.$pBroker.amqurl")) throw new IllegalArgumentException(s"amqurl not found for broker '$pBroker' in ${new File(System.getProperty("config.file")).getCanonicalPath}") //scalastyle:ignore

      val username = if (ActiveMQCLI.Config.hasPath(s"broker.$pBroker.username")) {
        ActiveMQCLI.Config.getString(s"broker.$pBroker.username")
      } else {
        new ConsoleReader().readLine(prompt("Enter username: "))
      }

      val password = if (ActiveMQCLI.Config.hasPath(s"broker.$pBroker.password")) {
        ActiveMQCLI.Config.getString(s"broker.$pBroker.password")
      } else {
        new ConsoleReader().readLine(prompt("Enter password: "), new Character('*'))
      }

      ActiveMQCLI.broker = Option(new Broker(pBroker, ActiveMQCLI.Config.getString(s"broker.$pBroker.amqurl"),
        ActiveMQCLI.Config.getString(s"broker.$pBroker.jmxurl"), ActiveMQCLI.Config.getOptionalString(s"broker.$pBroker.jmxname"), username, password))

      setSslProperties(pBroker)
      withSession((session: Session) ⇒ {})
      withBroker((brokerViewMBean: BrokerViewMBean, mBeanServerConnection: MBeanServerConnection) ⇒ {
        info(s"Connected to broker '${ActiveMQCLI.broker.get.alias}'")
      })
    } catch {
      case iae: IllegalArgumentException ⇒ {
        ActiveMQCLI.broker = None
        warn(iae.getMessage)
      }
      case e: Exception ⇒ {
        ActiveMQCLI.broker = None
        warn(s"Failed to connect to broker: ${e.getMessage}")
      }
    }
  }

  def setSslProperties(pBroker: String): Unit = {
    if (ActiveMQCLI.Config.hasPath(s"broker.$pBroker.keyStore")) {
      System.setProperty("javax.net.ssl.keyStore", ActiveMQCLI.Config.getString(s"broker.$pBroker.keyStore"))
    }
    if (ActiveMQCLI.Config.hasPath(s"broker.$pBroker.keyStorePassword")) {
      System.setProperty("javax.net.ssl.keyStorePassword", ActiveMQCLI.Config.getString(s"broker.$pBroker.keyStorePassword"))
    }
    if (ActiveMQCLI.Config.hasPath(s"broker.$pBroker.trustStore")) {
      System.setProperty("javax.net.ssl.trustStore", ActiveMQCLI.Config.getString(s"broker.$pBroker.trustStore"))
    }
  }
}
