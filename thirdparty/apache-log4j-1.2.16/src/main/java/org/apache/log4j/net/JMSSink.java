/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.log4j.net;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.xml.DOMConfigurator;

import javax.jms.JMSException;
import javax.jms.ObjectMessage;
import javax.jms.Session;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicSession;
import javax.jms.TopicSubscriber;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * A simple application that consumes logging events sent by a {@link
 * JMSAppender}.
 *
 *
 * @author Ceki G&uuml;lc&uuml; 
 * */
public class JMSSink implements javax.jms.MessageListener {

  static Logger logger = Logger.getLogger(JMSSink.class);

  static public void main(String[] args) throws Exception {
    if(args.length != 5) {
      usage("Wrong number of arguments.");
    }
    
    String tcfBindingName = args[0];
    String topicBindingName = args[1];
    String username = args[2];
    String password = args[3];
    
    
    String configFile = args[4];

    if(configFile.endsWith(".xml")) {
      DOMConfigurator.configure(configFile);
    } else {
      PropertyConfigurator.configure(configFile);
    }
    
    new JMSSink(tcfBindingName, topicBindingName, username, password);

    BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
    // Loop until the word "exit" is typed
    System.out.println("Type \"exit\" to quit JMSSink.");
    while(true){
      String s = stdin.readLine( );
      if (s.equalsIgnoreCase("exit")) {
	System.out.println("Exiting. Kill the application if it does not exit "
			   + "due to daemon threads.");
	return; 
      }
    } 
  }

  public JMSSink( String tcfBindingName, String topicBindingName, String username,
		  String password) {
    
    try {
      Context ctx = new InitialContext();
      TopicConnectionFactory topicConnectionFactory;
      topicConnectionFactory = (TopicConnectionFactory) lookup(ctx,
                                                               tcfBindingName);

      TopicConnection topicConnection =
	                        topicConnectionFactory.createTopicConnection(username,
									     password);
      topicConnection.start();

      TopicSession topicSession = topicConnection.createTopicSession(false,
                                                       Session.AUTO_ACKNOWLEDGE);

      Topic topic = (Topic)ctx.lookup(topicBindingName);

      TopicSubscriber topicSubscriber = topicSession.createSubscriber(topic);
    
      topicSubscriber.setMessageListener(this);

    } catch(JMSException e) {
      logger.error("Could not read JMS message.", e);
    } catch(NamingException e) {
      logger.error("Could not read JMS message.", e);
    } catch(RuntimeException e) {
      logger.error("Could not read JMS message.", e);
    }
  }

  public void onMessage(javax.jms.Message message) {
    LoggingEvent event;
    Logger remoteLogger;

    try {
      if(message instanceof  ObjectMessage) {
	ObjectMessage objectMessage = (ObjectMessage) message;
	event = (LoggingEvent) objectMessage.getObject();
	remoteLogger = Logger.getLogger(event.getLoggerName());
	remoteLogger.callAppenders(event);
      } else {
	logger.warn("Received message is of type "+message.getJMSType()
		    +", was expecting ObjectMessage.");
      }      
    } catch(JMSException jmse) {
      logger.error("Exception thrown while processing incoming message.", 
		   jmse);
    }
  }


  protected static Object lookup(Context ctx, String name) throws NamingException {
    try {
      return ctx.lookup(name);
    } catch(NameNotFoundException e) {
      logger.error("Could not find name ["+name+"].");
      throw e;
    }
  }

  static void usage(String msg) {
    System.err.println(msg);
    System.err.println("Usage: java " + JMSSink.class.getName()
            + " TopicConnectionFactoryBindingName TopicBindingName username password configFile");
    System.exit(1);
  }
}
