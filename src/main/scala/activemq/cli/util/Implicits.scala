/*
 * Copyright 2016 Anton Wierenga
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

package activemq.cli.util

import com.typesafe.config.Config
import java.util.Date
import java.util.Locale
import java.text.SimpleDateFormat
import javax.jms.Message
import javax.jms.TextMessage
import scala.collection.JavaConversions._

object Implicits {

  implicit class RichConfig(val underlying: Config) extends AnyVal {
    def getOptionalString(path: String): Option[String] = if (underlying.hasPath(path)) {
      Some(underlying.getString(path))
    } else {
      None
    }
  }

  /** Replicate Groovy Truth */
  implicit def stringToBoolean(s: String): Boolean = {
    !Option(s).getOrElse("").isEmpty
  }

  /** Replicate Groovy Truth */
  implicit def optionStringToBoolean(o: Option[String]): Boolean = {
    !o.getOrElse("").isEmpty
  }

  implicit class MessageImprovements(val message: Message) {

    val prettyPrinter = new scala.xml.PrettyPrinter(80, 2) //scalastyle:ignore

    def toXML(timestampFormat: Option[String] = None): String = {

      val addOptional = (condition: Boolean, xml: scala.xml.Elem) ⇒ if (condition) xml else scala.xml.NodeSeq.Empty

      prettyPrinter.format(<jms-message>
                             <header>
                               <message-id>{ message.getJMSMessageID }</message-id>
                               { addOptional(Option(message.getJMSCorrelationID).isDefined, <correlation-id>{ message.getJMSCorrelationID }</correlation-id>) }
                               <delivery-mode>{ message.getJMSDeliveryMode }</delivery-mode>
                               <destination>{ message.getJMSDestination }</destination>
                               <expiration>{ message.getJMSExpiration }</expiration>
                               <priority>{ message.getJMSPriority }</priority>
                               <redelivered>{ message.getJMSRedelivered }</redelivered>
                               { addOptional(Option(message.getJMSReplyTo).isDefined, <reply-to>{ message.getJMSReplyTo }</reply-to>) }
                               <timestamp>{
                                 timestampFormat match {
                                   case Some(matched)⇒ new SimpleDateFormat(matched).format(new Date(message.getJMSTimestamp))
                                   case _⇒ message.getJMSTimestamp
                                 }
                               }</timestamp>
                               { addOptional(Option(message.getJMSType).isDefined, <type>{ message.getJMSType }</type>) }
                             </header>
                             {
                               addOptional(message.getPropertyNames.hasMoreElements, <properties> {
                                 message.getPropertyNames.map(name ⇒
                                   <property><name>{ name }</name><value>{ message.getStringProperty(name.toString) }</value></property>)
                               } </properties>)
                             }
                             {
                               message match {
                                 case textMessage: TextMessage ⇒ addOptional(
                                   textMessage.getText,
                                   <body>{ scala.xml.PCData(textMessage.getText.replaceAll("]]>", "]]]]><![CDATA[>")) }</body>
                                 )
                                 case _⇒ scala.xml.NodeSeq.Empty
                               }
                             }
                           </jms-message>)
    }

    def textMatches(regex: String): Boolean = {
      if (regex) {
        message match {
          case textMessage: TextMessage ⇒ (regex.r findFirstIn textMessage.getText)
          case _                        ⇒ false
        }
      } else {
        true
      }
    }
  }
}
