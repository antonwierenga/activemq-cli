# activemq-cli
Command-line interface utility (Windows/macOS/Linux) to interact with a JMX enabled Apache ActiveMQ message broker.

[![alt text](/activemq_demo.gif)](https://www.youtube.com/watch?v=e_D6qGl-ZC8 "YouTube demo video")
Here is a [three minute video](https://www.youtube.com/watch?v=e_D6qGl-ZC8) that shows how to install, configure and use ActiveMQ CLI. 

## Installation
Download activemq-cli in the [release section](https://github.com/antonwierenga/activemq-cli/releases) of this repository.

Unzip the activemq-cli-x.x.x.zip file and configure the broker you want to connect to in `activemq-cli-x.x.x/conf/activemq-cli.config`:

```scala
broker {
  local {
    amqurl = "tcp://localhost:61616"
    jmxurl = "service:jmx:rmi:///jndi/rmi://localhost:1099/jmxrmi"
    username = ""
    password = ""
    prompt-color = "light-blue" 
  }

  // add additional brokers here
  dev {
    amqurl = "tcp://development-server:61616"
    jmxurl = "service:jmx:rmi:///jndi/rmi://development-server:1099/jmxrmi"
    username = "admin"
    password = "admin"		
  }	
  prod {
    amqurl = "tcp://production-server:61616"
    jmxurl = "service:jmx:rmi:///jndi/rmi://production-server:1099/jmxrmi"
  }		
}
```

## Usage
To enter the activemq-cli shell run `activemq-cli-x.x.x/bin/activemq-cli` or `activemq-cli-x.x.x/bin/activemq-cli.bat` if you are using Windows.

activemq-cli provides tab completion to speed up typing commands, to see which commands are available and what parameters are supported.

*In addition to executing commands in the shell, activemq-cli also supports executing a file containing commands:*
`activemq-cli --cmdfile my_commands.txt`

To connect to a broker that is configured in `activemq-cli-x.x.x/conf/activemq-cli.config`: `connect --broker dev`

Below is a list of commands that activemq-cli supports.

### add-queue
Adds a queue.

##### Parameters:
  - name

Example:`add-queue --name foo`

### add-topic
Adds a topic.

##### Parameters:
  - name

Example:`add-topic --name foo`

### connect
Connects activemq-cli to a broker.

##### Parameters:
  - broker (broker must be defined in `activemq-cli-x.x.x/conf/activemq-cli.config`)

Example:`connect --broker local`

### copy-messages
Copies messages from a queue to another queue.

##### Parameters:
  - from
  - to 
  - selector (copy messages that match the (JMS) selector)

Example:`copy-messages --from foo --to bar`

### disconnect
Disconnects activemq-cli from the broker.

Example:`disconnect`

### export-broker
Exports queues, topics and messages to file.

##### Parameters:
  - file

Example:`export-broker --file broker.xml`

*For this command activemq-cli creates temporary mirror queues to ensure all messages are exported.*

### export-messages
Exports messages to file.

##### Parameters:
  - file
  - queue
  - selector (export messages that match the (JMS) selector)
  - regex (export messages whose body match the regex)
 
Example:`export-messages --queue foo`

*For this command activemq-cli creates a temporary mirror queue to ensure all messages are exported.*

### info
Displays information (e.g. version, uptime, total number of queues/topics/messages) about the broker 

Example:`info`

### list-messages
Lists messages.

##### Parameters:
  - queue
  - selector (lists messages that match the (JMS) selector)
  - regex (lists messages whose body match the regex)
 
Example 1:`list-messages --queue foo`

Example 2:`list-messages --queue foo --selector "JMSCorrelationID = '12345'"`

Example 3:`list-messages --queue foo --regex bar`

*For this command activemq-cli creates a temporary mirror queue to ensure all messages are listed.*

### list-queues
Lists queues.

##### Parameters:
  - filter (list queues with the specified filter in the name)
  - no-consumers (list queues with no consumers)
Example 1:`queues --filter foo`

Example 2:`queues --no-consumers`

### list-topics
Lists topics.

##### Parameters:
  - filter (list topics with the specified filter in the name)

Example:`topics --filter foo`

### move-messages
Moves messages from a queue to another queue.

##### Parameters:
  - from
  - to 
  - selector (move messages that match the (JMS) selector)

### purge-all-queues
Purges all queues.

##### Parameters:
  - force (no prompt for confirmation)
  - dry-run (use this to test what is going to be purged, no queues are actually purged)
  - filter (queues with the specified filter in the name)
  - no-consumers (queues with no consumers)
  
Example 1:`purge-all-queues`

Example 2:`purge-all-queues --filter foo --no-consumers --dry-run`

### purge-queue
Purges a queues.

##### Parameters:
  - name 
  - force (no prompt for confirmation)

Example:`purge-queue --name foo`

### release-notes
Displays the release notes.

Example:`release-notes`

### remove-all-queues
Removes all queues.

##### Parameters:
  - force (no prompt for confirmation)
  - dry-run (use this to test what is going to be removed, no queues are actually removed)
  - filter (queues with the specified filter in the name)
  - no-consumers (queues with no consumers)

Example 1:`remove-all-queues`

Example 2:`remove-all-queues --filter foo --no-consumers --dry-run`

### remove-all-topics
Removes all topics.

##### Parameters:
  - force (no prompt for confirmation)
  - dry-run (use this to test what is going to be removed, no queues are actually removed)
  - filter (queues with the specified filter in the name)

Example 1:`remove-all-topics`

Example 2:`remove-all-topics --filter foo --dry-run`

### remove-queue
Removes a queue.

##### Parameters:
  - name 
  - force (no prompt for confirmation)

Example:`remove-queue --name foo`

### remove-topic
Removes a topic.

##### Parameters:
  - name 
  - force (no prompt for confirmation)

Example:`remove-topic --name foo`

### send-message
Sends a message or file of messages to a queue or topic.

##### Parameters:
  - body     
  - queue 
  - topic
  - priority (not applicable if -file is specified)                    
  - correlation-id (not applicable if -file is specified) 
  - delivery-mode (not applicable if -file is specified) 
  - time-to-live (not applicable if -file is specified) 
  - times (number of times the message is send)
  - file

Example file:
```xml
<jms-messages>
  <jms-message>
    <body>Message 1</body>
  </jms-message>
  <jms-message>
    <body>Message 2</body>
  </jms-message>  
  <jms-message>
    <header>
      <priority>0</priority>
      <correlation-id>12345</correlation-id>
      <delivery-mode>2</delivery-mode>
      <time-to-live>1000</time-to-live>
    </header>
    <properties>
      <property>
        <name>my_custom_property</name>
        <value>1</value>
      </property>        
    </properties>      
    <body><![CDATA[<?xml version="1.0"?>
<catalog>
   <book id="1">
      <author>Basil, Matthew</author>
      <title>XML Developer's Guide</title>
      <genre>Computer</genre>
      <price>44.95</price>
      <publish_date>2002-10-01</publish_date>
      <description>An in-depth look at creating applications with XML.</description>
   </book>
</catalog>]]></body>
  </jms-message>  
</jms-messages>
```

Example 1:`send-message --body foo --queue bar`

Example 2:`send-message --file foo.xml --topic bar`

### start-embedded-broker
Starts the embedded broker.

The embedded broker is configured in `activemq-cli-x.x.x/conf/activemq-cli.config`:
```scala
embedded-broker {
	connector = "tcp://localhost:61616"
	jmxport = 1099	
}
```
Example:`start-embedded-broker`

### stop-embedded-broker
Stops the embedded broker.

Example:`stop-embedded-broker`
