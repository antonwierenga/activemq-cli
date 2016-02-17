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
import activemq.cli.command.util.PrintStackTraceExecutionProcessor
import activemq.cli.util.Console._
import activemq.cli.util.Implicits._
import collection.JavaConversions._
import javax.jms.Session
import javax.management.MBeanServerConnection
import javax.management.MBeanServerInvocationHandler
import javax.management.ObjectName
import javax.management.remote.JMXConnector.CREDENTIALS
import javax.management.remote.JMXConnectorFactory
import javax.management.remote.JMXServiceURL
import org.apache.activemq.ActiveMQConnectionFactory
import org.apache.activemq.broker.jmx.BrokerViewMBean
import org.springframework.shell.support.table.Table
import org.springframework.shell.support.table.TableHeader
import scala.tools.jline.console.ConsoleReader

abstract class Commands extends PrintStackTraceExecutionProcessor {

  val numberFormatter = java.text.NumberFormat.getIntegerInstance

  def format(number: Long): String = {
    numberFormatter.format(number)
  }

  def formatDuration(duration: Long): String = {
    val formatTimeUnit = (l: Long, timeUnit: String) ⇒ if (l == 0) None else if (l == 1) s"$l $timeUnit" else s"$l ${timeUnit}s"
    List(
      formatTimeUnit((duration / (1000 * 60 * 60)) % 24, "hour"),
      formatTimeUnit((duration / (1000 * 60)) % 60, "minute"),
      formatTimeUnit((duration / 1000) % 60, "second")
    ).filter(_ != None).mkString(" ")
  }

  def confirm(force: String): Unit = {
    force match {
      case "yes" ⇒ // skip confirmation
      case _ ⇒
        if (!List("Y", "y").contains(new ConsoleReader().readLine(prompt("Are you sure? (Y/N): ")))) {
          throw new IllegalArgumentException("Command aborted")
        }
    }
  }

  def renderTable(rows: Seq[Seq[Any]], headers: Seq[String]): String = {
    val table = new Table()
    headers.zipWithIndex.map {
      case (header, i) ⇒
        table.addHeader(i + 1, new TableHeader(header))
    }
    rows.map(row ⇒ row.map(_.toString)).map(row ⇒ table.addRow(row: _*))
    table.calculateColumnWidths
    table.toString
  }

  def getDestinationKeyProperty(objectName: ObjectName): String = {
    // Fuse ESB Enterprise 7.1.0 / ActiveMQ 5.9.0 use different ObjectNames
    Option(objectName.getKeyProperty("destinationName")).getOrElse(objectName.getKeyProperty("Destination"))
  }

  def validateTopicExists(brokerViewMBean: BrokerViewMBean, topic: String): ObjectName = {
    brokerViewMBean.getTopics.filter(objectName ⇒
      getDestinationKeyProperty(objectName).contains(topic)).headOption.getOrElse(
      throw new IllegalArgumentException(s"Topic '$topic' does not exist")
    )
  }

  def validateTopicNotExists(brokerViewMBean: BrokerViewMBean, topic: String): Unit = {
    if (!brokerViewMBean.getTopics.filter(objectName ⇒
      getDestinationKeyProperty(objectName).equals(topic)).isEmpty) {
      throw new IllegalArgumentException(s"Topic '$topic' already exists")
    }
  }

  def validateQueueExists(brokerViewMBean: BrokerViewMBean, queue: String): ObjectName = {
    brokerViewMBean.getQueues.filter(objectName ⇒
      getDestinationKeyProperty(objectName).equals(queue)).headOption.getOrElse(
      throw new IllegalArgumentException(s"Queue '$queue' does not exist")
    )
  }

  def validateQueueNotExists(brokerViewMBean: BrokerViewMBean, queue: String): Unit = {
    if (!brokerViewMBean.getQueues.filter(objectName ⇒
      getDestinationKeyProperty(objectName).equals(queue)).isEmpty) {
      throw new IllegalArgumentException(s"Queue '$queue' already exists")
    }
  }

  /** Gets the ObjectName for the given Queue. If the Queue does not exist, it is created first. */
  def getQueueObjectName(brokerViewMBean: BrokerViewMBean, queue: String): ObjectName = {
    brokerViewMBean.getQueues.filter(objectName ⇒
      getDestinationKeyProperty(objectName).equals(queue)).headOption.getOrElse({
      brokerViewMBean.addQueue(queue)
      validateQueueExists(brokerViewMBean, queue)
    })
  }

  /** Gets the ObjectName for the given Topic. If the Topic does not exist, it is created first. */
  def getTopicObjectName(brokerViewMBean: BrokerViewMBean, topic: String): ObjectName = {
    brokerViewMBean.getTopics.filter(objectName ⇒
      getDestinationKeyProperty(objectName).equals(topic)).headOption.getOrElse({
      brokerViewMBean.addTopic(topic)
      validateQueueExists(brokerViewMBean, topic)
    })
  }

  def withSession(callback: (Session) ⇒ Unit): Unit = {
    val connection = new ActiveMQConnectionFactory(ActiveMQCLI.broker.get.username, ActiveMQCLI.broker.get.password,
      ActiveMQCLI.broker.get.amqurl).createConnection
    connection.start
    val session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)
    callback(session)
    session.close
    connection.close
  }

  def withBroker(callback: (BrokerViewMBean, MBeanServerConnection) ⇒ String): String = {
    ActiveMQCLI.broker match {
      case Some(matched) ⇒
        val jmxConnector = JMXConnectorFactory.connect(
          new JMXServiceURL(matched.jmxurl),
          mapAsJavaMap(Map(CREDENTIALS → Array(matched.username, matched.password)))
        )
        try {
          jmxConnector.connect
          // Fuse ESB Enterprise 7.1.0 / ActiveMQ 5.9.0 use different ObjectNames
          val brokerViewMBeans = List(Map("type" → "type", "brokerName" → "brokerName"), Map("type" → "Type", "brokerName" → "BrokerName"))
            .par.map(properties ⇒
              jmxConnector.getMBeanServerConnection.queryNames(
                new ObjectName(s"org.apache.activemq:${properties.get("type").get}=Broker,${properties.get("brokerName").get}=${matched.jmxName.getOrElse("*")}"), //scalastyle:ignore
                null //scalastyle:ignore
              )).flatten
            .par.map(objectName ⇒ MBeanServerInvocationHandler.newProxyInstance(
              jmxConnector.getMBeanServerConnection, objectName, classOf[BrokerViewMBean], true
            ))
            .filter(!_.isSlave)

          brokerViewMBeans.headOption match {
            case Some(brokerViewMBean) ⇒ callback(brokerViewMBean, jmxConnector.getMBeanServerConnection())
            case _                     ⇒ "Broker not found"
          }
        } catch {
          case illegalArgumentException: IllegalArgumentException ⇒ warn(illegalArgumentException.getMessage)
        } finally {
          jmxConnector.close
        }
      case _ ⇒
        "No Broker set"
    }
  }
}