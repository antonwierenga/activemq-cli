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

package activemq.cli.plugin

import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.shell.plugin.support.DefaultBannerProvider
import org.springframework.stereotype.Component
import activemq.cli.ActiveMQCLI

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class BannerProvider extends DefaultBannerProvider {

  override def getBanner: String = {
    """   ___      __  _          __  _______     _______   ____
       |  / _ |____/ /_(_)  _____ /  |/  / __ \   / ___/ /  /  _/
       | / __ / __/ __/ / |/ / -_) /|_/ / /_/ /  / /__/ /___/ /
       |/_/ |_\__/\__/_/|___/\__/_/  /_/\___\_\  \___/____/___/
       |                                                        """.stripMargin
  }

  override def getVersion: String = ActiveMQCLI.ReleaseNotes.keysIterator.next

  override def getWelcomeMessage: String = s"Welcome to ActiveMQ CLI $getVersion"

  override def getProviderName: String = "ActiveMQ CLI"
}
