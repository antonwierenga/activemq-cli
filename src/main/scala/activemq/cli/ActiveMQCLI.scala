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

package activemq.cli

import activemq.cli.domain.Broker
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import java.io.File
import java.nio.file.Paths
import java.rmi.RMISecurityManager
import org.apache.activemq.broker.BrokerService
import org.springframework.shell.Bootstrap
import org.springframework.shell.core.annotation.CliCommand
import org.springframework.shell.core.CommandMarker
import org.springframework.stereotype.Component

@Component
class ActiveMQCLI extends CommandMarker {

  @CliCommand(value = Array("release-notes"), help = "Displays release notes")
  def releaseNotes: String = ActiveMQCLI.ReleaseNotes.keySet.toSeq.sorted.map(x ⇒ s"$x\n" + ActiveMQCLI.ReleaseNotes(x)
    .map(y ⇒ s"    - $y").mkString("\n")).mkString("\n")
}

object ActiveMQCLI extends App {

  lazy val ReleaseNotes = Map("v0.6.0" → List(
    "Added shell command 'pause-queue'",
    "Added shell command 'resume-queue'",
    "Updated shell command 'send-message': support for '~' in file option",
    "Updated shell command 'export-messages': support for '~' in file option",
    "Fixed java version check (openjdk)"
  ), "v0.5.0" → List(
    "Updated shell command 'send-message': new option --reply-to",
    "Fixed a bug that caused the <reply-to> header to be omitted when sending messages from file"
  ), "v0.4.0" → List(
    "Updated shell command 'connect': option --broker now uses tab completion",
    "Updated shell command 'list-messages': option --queue now mandatory",
    "Fixed a bug that prevented the embedded broker from being started",
    "Fixed a bug that caused an error when export-messages is run against a queue containing a message with empty text"
  ), "v0.3.0" → List(
    "Updated shell command 'list-queues': new filter options --pending, --enqueued, --dequeued and --consumers (replaces --no-consumer)",
    "Updated shell command 'purge-all-queues': new filter options --pending, --enqueued, --dequeued and --consumers (replaces --no-consumer)",
    "Updated shell command 'remove-all-queues': new filter options --pending, --enqueued, --dequeued and --consumers (replaces --no-consumer)",
    "Updated shell command 'list-topics': new filter options --enqueued and --dequeued",
    "Updated shell command 'remove-all-topics': new filter options --enqueued and --dequeued"
  ), "v0.2.0" → List(
    "Updated shell command 'connect-broker': SSL support (specify keyStore, keyStorePassword and trustStore in broker config)"
  ), "v0.1.0" → List(
    "Updated shell command 'remove-all-topics': new options --filter and --dry-run",
    "Updated shell command 'remove-all-queues': new options --filter, --no-consumers and --dry-run",
    "Updated shell command 'purge-all-queues': new options --filter, --no-consumers and --dry-run",
    "Updated shell command 'list-topics': display total number of topics",
    "Updated shell command 'list-queues': display total number of queues, new option --no-consumers",
    "Renamed shell command 'queues' to 'list-queues'",
    "Renamed shell command 'topics' to 'list-topics'",
    "Updated shell command 'send-message': --times option now supported when sending messages using the --file option",
    "Updated shell command 'export-messages': display full path of the export file",
    "Updated shell command 'export-messages': default export path is 'activemq-cli/output' when no path is given",
    "Updated shell command 'export-broker': display full path of the export file",
    "Updated shell command 'export-broker': default export path is 'activemq-cli/output' when no path is given",
    "Updated shell command 'connect-broker': validate amqurl (as specified in activemq-cli.config)",
    "Fixed a bug that caused an error when activemq-cli was running from a path that contains spaces (Windows)"
  ), "v0.0.0" → List(
    "New shell command 'add-queue'",
    "New shell command 'add-topic'",
    "New shell command 'connect'",
    "New shell command 'copy-messages '",
    "New shell command 'disconnect'",
    "New shell command 'export-broker'",
    "New shell command 'export-messages'",
    "New shell command 'info'",
    "New shell command 'list-messages'",
    "New shell command 'move-messages'",
    "New shell command 'purge-all-queues'",
    "New shell command 'purge-queue'",
    "New shell command 'queues'",
    "New shell command 'release-notes'",
    "New shell command 'remove-all-queues'",
    "New shell command 'remove-all-topics'",
    "New shell command 'remove-queue'",
    "New shell command 'remove-topic'",
    "New shell command 'send-message'",
    "New shell command 'start-embedded-broker'",
    "New shell command 'stop-embedded-broker'",
    "New shell command 'topics'"
  ))

  lazy val ApplicationPath: File = new File(s"${
    Paths.get(classOf[ActiveMQCLI].getProtectionDomain.getCodeSource.getLocation.toURI).toFile.getParentFile
      .getParentFile
  }")

  lazy val ApplicationOutputPath: File = new File(ApplicationPath, "output")

  if (!ApplicationOutputPath.exists()) ApplicationOutputPath.mkdir()

  System.setProperty("config.file", new File(ApplicationPath, "conf/activemq-cli.config").getPath)

  lazy val Config: Config = ConfigFactory.load

  var broker: Option[Broker] = None

  Bootstrap.main(args)

}
