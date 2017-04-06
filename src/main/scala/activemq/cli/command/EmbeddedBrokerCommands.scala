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
import activemq.cli.ActiveMQCLI.ApplicationPath
import activemq.cli.command.util.PrintStackTraceExecutionProcessor
import activemq.cli.util.Console._
import java.io.File
import javax.annotation.PreDestroy
import org.apache.activemq.broker.BrokerService
import org.springframework.shell.core.annotation.CliAvailabilityIndicator
import org.springframework.shell.core.annotation.CliCommand
import org.springframework.shell.core.CommandMarker
import org.springframework.stereotype.Component

@Component
class EmbeddedBrokerCommands extends Commands {

  var embeddedBroker: Option[BrokerService] = None

  @CliAvailabilityIndicator(Array("start-embedded-broker"))
  def isStartEmbeddedBrokerAvailable: Boolean = embeddedBroker.isEmpty || !embeddedBroker.get.isStarted
  @CliAvailabilityIndicator(Array("stop-embedded-broker"))
  def isStopEmbeddedBrokerAvailable: Boolean = {
    embeddedBroker match {
      case Some(matched) if matched.isStarted ⇒
        true
      case _ ⇒
        false
    }
  }

  @CliCommand(value = Array("start-embedded-broker"), help = "Starts the embedded broker")
  def startEmbeddedBroker: String = {
    embeddedBroker match {
      case Some(matched) if matched.isStarted ⇒
        warn("Embedded broker already started")
      case _ ⇒
        try {
          embeddedBroker = Some(new BrokerService)
          if (ActiveMQCLI.Config.hasPath(s"embedded-broker.datadir")) {
            embeddedBroker.get.setDataDirectory(ActiveMQCLI.Config.getString("embedded-broker.datadir"))
          } else {
            embeddedBroker.get.setDataDirectory(new File(ApplicationPath, "data/activemq-data").getPath)
          }
          embeddedBroker.get.addConnector(ActiveMQCLI.Config.getString("embedded-broker.connector"))
          embeddedBroker.get.getManagementContext.setConnectorPort(ActiveMQCLI.Config.getInt("embedded-broker.jmxport"));
          embeddedBroker.get.start
          info("Embedded broker started")
        } catch {
          case e: Exception ⇒ {
            embeddedBroker = None
            warn(s"Failed to start embedded broker: ${e.getMessage}")
          }
        }
    }
  }

  @CliCommand(value = Array("stop-embedded-broker"), help = "Stops the embedded broker")
  def stopEmbeddedBroker: String = {
    embeddedBroker match {
      case Some(matched) if matched.isStarted ⇒
        embeddedBroker.get.stop
        info("Embedded broker stopped")
      case _ ⇒
        warn("Embedded broker is not running")
    }
  }

  @PreDestroy
  def preDestroy: Unit = {
    embeddedBroker match {
      case Some(matched) if matched.isStarted ⇒
        printInfo("Stopping embedded broker")
        stopEmbeddedBroker
        printInfo("Embedded broker stopped")
    }
  }
}
