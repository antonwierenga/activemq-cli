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

package activemq.cli

import activemq.cli.domain.Broker
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import java.io.File
import java.rmi.RMISecurityManager
import org.apache.activemq.broker.BrokerService
import org.springframework.shell.Bootstrap
import org.springframework.shell.core.annotation.CliCommand
import org.springframework.shell.core.CommandMarker
import org.springframework.stereotype.Component

@Component
class ActiveMQCLI extends CommandMarker {

  @CliCommand(value = Array("release-notes"), help = "Displays release notes")
  def releaseNotes: String = ActiveMQCLI.ReleaseNotes.keySet.map(x ⇒ s"$x\n" + ActiveMQCLI.ReleaseNotes(x).map(y ⇒ s"    - $y").mkString("\n")).mkString("\n")

}

object ActiveMQCLI extends App {

  lazy val ReleaseNotes = Map("0.0.1" → List(""))
  lazy val ApplicationPath: String = s"${new File(classOf[ActiveMQCLI].getProtectionDomain.getCodeSource.getLocation.getFile).getParent}/.."

  System.setProperty("config.file", s"$ApplicationPath/conf/activemq-cli.config")
  lazy val Config: Config = ConfigFactory.load

  var broker: Option[Broker] = _

  Bootstrap.main(args)

}
