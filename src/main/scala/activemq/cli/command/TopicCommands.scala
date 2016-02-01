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
import javax.management.MBeanServerConnection
import javax.management.MBeanServerInvocationHandler
import org.apache.activemq.broker.jmx.BrokerViewMBean
import org.apache.activemq.broker.jmx.TopicViewMBean
import org.springframework.shell.core.annotation.CliAvailabilityIndicator
import org.springframework.shell.core.annotation.CliCommand
import org.springframework.shell.core.annotation.CliOption
import org.springframework.stereotype.Component

@Component
class TopicCommands extends Commands {

  @CliAvailabilityIndicator(Array("add-topic", "remove-topic", "topics", "remove-all-topics"))
  def isBrokerAvailable: Boolean = {
    ActiveMQCLI.broker match {
      case Some(matched) ⇒
        true
      case _ ⇒
        false
    }
  }

  @CliCommand(value = Array("add-topic"), help = "Adds a topic")
  def addTopic(@CliOption(key = Array("name"), mandatory = true, help = "The Name of the Topic") name: String): String = {
    withBroker((brokerViewMBean: BrokerViewMBean, mBeanServerConnection: MBeanServerConnection) ⇒ {
      validateTopicNotExists(brokerViewMBean, name)
      brokerViewMBean.addTopic(name)
      info(s"Topic '$name' added")
    })
  }

  @CliCommand(value = Array("remove-topic"), help = "Removes a topic")
  def removeTopic(
    @CliOption(key = Array("name"), mandatory = true, help = "The Name of the Topic") name: String,
    @CliOption(key = Array("force"), specifiedDefaultValue = "yes", mandatory = false, help = "No prompt") force: String
  ): String = {
    withBroker((brokerViewMBean: BrokerViewMBean, mBeanServerConnection: MBeanServerConnection) ⇒ {
      validateTopicExists(brokerViewMBean, name)
      confirm(force)
      brokerViewMBean.removeTopic(name)
      info(s"Topic '$name' removed")
    })
  }

  @CliCommand(value = Array("remove-all-topics"), help = "Removes all topics")
  def removeAllTopics(@CliOption(key = Array("force"), specifiedDefaultValue = "yes", mandatory = false, help = "No prompt") force: String): String = {
    withBroker((brokerViewMBean: BrokerViewMBean, mBeanServerConnection: MBeanServerConnection) ⇒ {
      confirm(force)
      val topics = brokerViewMBean.getTopics
      topics.par.map(objectName ⇒
        brokerViewMBean.removeTopic(MBeanServerInvocationHandler.newProxyInstance(mBeanServerConnection, objectName, classOf[TopicViewMBean], true).getName))
      info(s"Topics removed: ${topics.size}")
    })
  }

  @CliCommand(value = Array("topics"), help = "Displays topics")
  def topics(@CliOption(key = Array("filter"), mandatory = false, help = "The query") filter: String): String = {
    val headers = List("Topic Name", "Enqueued", "Dequeued")
    withBroker((brokerViewMBean: BrokerViewMBean, mBeanServerConnection: MBeanServerConnection) ⇒ {
      renderTable(
        brokerViewMBean.getTopics.filter(objectName ⇒
          if (filter) {
            getDestinationKeyProperty(objectName).toLowerCase.contains(Option(filter).getOrElse("").toLowerCase)
          } else {
            true
          }).par.map({ objectName ⇒
          (MBeanServerInvocationHandler.newProxyInstance(mBeanServerConnection, objectName, classOf[TopicViewMBean], true))
        }).par.map(topicViewMBean ⇒ List(topicViewMBean.getName, topicViewMBean.getEnqueueCount, topicViewMBean.getDequeueCount))
          .seq.sortBy(ActiveMQCLI.Config.getOptionalString(s"command.topics.order.field") match {
            case Some("Enqueued") ⇒ (row: Seq[Any]) ⇒ { "%015d".format(row(headers.indexOf("Enqueued"))).asInstanceOf[String] }
            case Some("Dequeued") ⇒ (row: Seq[Any]) ⇒ { "%015d".format(row(headers.indexOf("Dequeued"))).asInstanceOf[String] }
            case _ ⇒ (row: Seq[Any]) ⇒ { row(0).asInstanceOf[String] }
          }),
        headers
      )
    })
  }
}