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
import org.apache.activemq.broker.jmx.QueueViewMBean
import org.springframework.shell.core.annotation.CliAvailabilityIndicator
import org.springframework.shell.core.annotation.CliCommand
import org.springframework.shell.core.annotation.CliOption
import org.springframework.stereotype.Component

@Component
class QueueCommands extends Commands {

  @CliAvailabilityIndicator(Array("add-queue", "purge-queue", "purge-all-queues", "remove-queue", "remove-all-queues", "queues"))
  def isBrokerAvailable: Boolean = {
    ActiveMQCLI.broker match {
      case Some(matched) ⇒
        true
      case _ ⇒
        false
    }
  }

  @CliCommand(value = Array("add-queue"), help = "Adds a queue")
  def addQueue(@CliOption(key = Array("name"), mandatory = true, help = "The name of the queue") name: String): String = {
    withBroker((brokerViewMBean: BrokerViewMBean, mBeanServerConnection: MBeanServerConnection) ⇒ {
      validateQueueNotExists(brokerViewMBean, name)
      brokerViewMBean.addQueue(name)
      info(s"Queue '$name' added")
    })
  }

  @CliCommand(value = Array("purge-queue"), help = "Purges a queue")
  def purgeQueue(
    @CliOption(key = Array("name"), mandatory = true, help = "The name of the queue") name: String,
    @CliOption(key = Array("force"), specifiedDefaultValue = "yes", mandatory = false, help = "No prompt") force: String
  ): String = {
    withBroker((brokerViewMBean: BrokerViewMBean, mBeanServerConnection: MBeanServerConnection) ⇒ {
      confirm(force)
      MBeanServerInvocationHandler.newProxyInstance(mBeanServerConnection, validateQueueExists(brokerViewMBean, name), classOf[QueueViewMBean], true).purge()
      info(s"Queue '$name' purged")
    })
  }

  @CliCommand(value = Array("purge-all-queues"), help = "Purges all queues")
  def purgeAllQueues(@CliOption(key = Array("force"), specifiedDefaultValue = "yes", mandatory = false, help = "No prompt") force: String): String = {
    withBroker((brokerViewMBean: BrokerViewMBean, mBeanServerConnection: MBeanServerConnection) ⇒ {
      confirm(force)
      val queues = brokerViewMBean.getQueues
      queues.par.map(objectName ⇒
        MBeanServerInvocationHandler.newProxyInstance(mBeanServerConnection, objectName, classOf[QueueViewMBean], true).purge())
      info(s"Queues purged: ${queues.size}")
    })
  }

  @CliCommand(value = Array("remove-queue"), help = "Removes a queue")
  def removeQueue(
    @CliOption(key = Array("name"), mandatory = true, help = "The name of the queue") name: String,
    @CliOption(key = Array("force"), specifiedDefaultValue = "yes", mandatory = false, help = "No prompt") force: String
  ): String = {
    withBroker((brokerViewMBean: BrokerViewMBean, mBeanServerConnection: MBeanServerConnection) ⇒ {
      validateQueueExists(brokerViewMBean, name)
      confirm(force)
      brokerViewMBean.removeQueue(name)
      info(s"Queue '$name' removed")
    })
  }

  @CliCommand(value = Array("remove-all-queues"), help = "Removes all queues")
  def removeAllQueues(@CliOption(key = Array("force"), specifiedDefaultValue = "yes", mandatory = false, help = "No prompt") force: String): String = {
    withBroker((brokerViewMBean: BrokerViewMBean, mBeanServerConnection: MBeanServerConnection) ⇒ {
      confirm(force)
      val queues = brokerViewMBean.getQueues
      queues.par.map(objectName ⇒
        brokerViewMBean.removeQueue(MBeanServerInvocationHandler.newProxyInstance(mBeanServerConnection, objectName, classOf[QueueViewMBean], true).getName))
      info(s"Queues removed: ${queues.size}")
    })
  }

  @CliCommand(value = Array("queues"), help = "Displays queues")
  def queues(@CliOption(key = Array("filter"), mandatory = false, help = "The query") filter: String): String = {
    val headers = List("Queue Name", "Pending", "Consumers", "Enqueued", "Dequeued")
    withBroker((brokerViewMBean: BrokerViewMBean, mBeanServerConnection: MBeanServerConnection) ⇒ {
      renderTable(
        brokerViewMBean.getQueues.filter(objectName ⇒
          if (filter) {
            getDestinationKeyProperty(objectName).toLowerCase.contains(Option(filter).getOrElse("").toLowerCase)
          } else {
            true
          }).par.map({ objectName ⇒
          (MBeanServerInvocationHandler.newProxyInstance(mBeanServerConnection, objectName, classOf[QueueViewMBean], true))
        }).par.map(queueViewMBean ⇒ List(queueViewMBean.getName, queueViewMBean.getQueueSize, queueViewMBean.getConsumerCount,
          queueViewMBean.getEnqueueCount, queueViewMBean.getDequeueCount))
          .seq.sortBy(ActiveMQCLI.Config.getOptionalString(s"command.queues.order.field") match {
            case Some("Pending") ⇒ (row: Seq[Any]) ⇒ { "%015d".format(row(headers.indexOf("Pending"))).asInstanceOf[String] }
            case Some("Consumers") ⇒ (row: Seq[Any]) ⇒ { "%015d".format(row(headers.indexOf("Consumers"))).asInstanceOf[String] }
            case Some("Enqueued") ⇒ (row: Seq[Any]) ⇒ { "%015d".format(row(headers.indexOf("Enqueued"))).asInstanceOf[String] }
            case Some("Dequeued") ⇒ (row: Seq[Any]) ⇒ { "%015d".format(row(headers.indexOf("Dequeued"))).asInstanceOf[String] }
            case _ ⇒ (row: Seq[Any]) ⇒ { row(headers.indexOf("Queue Name" + 1)).asInstanceOf[String] }
          })(ActiveMQCLI.Config.getOptionalString(s"command.queues.order.direction") match {
            case Some("reverse") ⇒ Ordering[String].reverse
            case _               ⇒ Ordering[String]
          }),
        headers
      )
    })
  }
}