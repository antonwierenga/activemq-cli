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
import activemq.cli.domain.Broker
import activemq.cli.util.Console._
import activemq.cli.util.Implicits._
import java.io.File
import javax.management.MBeanServerConnection
import org.apache.activemq.broker.jmx.BrokerViewMBean
import org.springframework.shell.core.annotation.CliAvailabilityIndicator
import org.springframework.shell.core.annotation.CliCommand
import org.springframework.shell.core.annotation.CliOption
import org.springframework.stereotype.Component
import scala.tools.jline.console.ConsoleReader

@Component
class BrokerCommands extends Commands {

  @CliAvailabilityIndicator(Array("info", "disconnect"))
  def isBrokerAvailable: Boolean = {
    ActiveMQCLI.broker match {
      case Some(matched) ⇒
        true
      case _ ⇒
        false
    }
  }

  @CliCommand(value = Array("info"), help = "Displays broker info")
  def brokerInfo(): String = {
    withBroker((brokerViewMBean: BrokerViewMBean, mBeanServerConnection: MBeanServerConnection) ⇒ {
      renderTable(List(List(brokerViewMBean.getBrokerId, brokerViewMBean.getBrokerName, brokerViewMBean.getBrokerVersion, s"${brokerViewMBean.getMemoryPercentUsage}%",
        s"${brokerViewMBean.getStorePercentUsage}%", brokerViewMBean.getUptime)), List("Broker ID", "Broker Name", "Broker Version", "Memory Limit used",
        "Store Limit used", "Uptime"))
    })
  }

  @CliCommand(value = Array("disconnect"), help = "Disconnect from the broker")
  def disconnect() = {
    ActiveMQCLI.broker = None
    info(s"Disconnected from broker")
  }

  @CliCommand(value = Array("connect"), help = "Connects to a broker")
  def connect(
    @CliOption(key = Array("broker"), mandatory = true, help = "The Broker Alias") pBroker: String,
    @CliOption(key = Array("username"), mandatory = false, help = "The JMX username") username: String,
    @CliOption(key = Array("password"), mandatory = false, help = "The JMX password") password: String
  ): String = {

    try {
      if (!ActiveMQCLI.Config.hasPath(s"broker.$pBroker")) throw new IllegalArgumentException(s"Alias '$pBroker' not found in ${new File(System.getProperty("ActiveMQCLI.Config.file")).getCanonicalPath}") //scalastyle:ignore
      if (!ActiveMQCLI.Config.hasPath(s"broker.$pBroker.jmxurl")) throw new IllegalArgumentException(s"jmxurl not found for Alias '$pBroker' in ${new File(System.getProperty("ActiveMQCLI.Config.file")).getCanonicalPath}") //scalastyle:ignore
      if (!ActiveMQCLI.Config.hasPath(s"broker.$pBroker.amqurl")) throw new IllegalArgumentException(s"amqurl not found for Alias '$pBroker' in ${new File(System.getProperty("ActiveMQCLI.Config.file")).getCanonicalPath}") //scalastyle:ignore

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

      ActiveMQCLI.broker = Option(new Broker(pBroker, ActiveMQCLI.Config.getString(s"broker.$pBroker.amqurl"), ActiveMQCLI.Config.getString(s"broker.$pBroker.jmxurl"), ActiveMQCLI.Config.getOptionalString(s"broker.$pBroker.jmxname"), username, password))

      withBroker((brokerViewMBean: BrokerViewMBean, mBeanServerConnection: MBeanServerConnection) ⇒ {
        info(s"Broker is set to '${ActiveMQCLI.broker.get.jmxurl}'")
      })
    } catch {
      case iae: IllegalArgumentException ⇒ {
        ActiveMQCLI.broker = None
        warn(iae.getMessage)
      }
      case e: Exception ⇒ {
        ActiveMQCLI.broker = None
        warn(s"Failed to connect to Broker: ${e.getMessage}")
      }
    }
  }
}