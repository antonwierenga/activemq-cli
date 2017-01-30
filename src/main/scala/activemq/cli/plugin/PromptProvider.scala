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

package activemq.cli.plugin

import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.shell.plugin.support.DefaultPromptProvider
import org.springframework.stereotype.Component
import activemq.cli.ActiveMQCLI
import activemq.cli.command.BrokerCommands
import activemq.cli.util.Console
import activemq.cli.util.Implicits._

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class PromptProvider extends DefaultPromptProvider {

  override def getPrompt: String = {
    ActiveMQCLI.broker match {
      case Some(matched) ⇒
        Console.AnsiCodes.get(ActiveMQCLI.Config.getOptionalString(s"broker.${matched.alias}.prompt-color").getOrElse("").toLowerCase) match {
          case Some(ansiCode) ⇒ Console.decorate(s"${matched.alias}>", ansiCode)
          case _              ⇒ s"${matched.alias}>"
        }
      case _ ⇒
        "activemq-cli>"
    }
  }
}
