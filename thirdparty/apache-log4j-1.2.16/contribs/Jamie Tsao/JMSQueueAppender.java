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

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.ErrorHandler;
import org.apache.log4j.spi.ErrorCode;
import org.apache.log4j.helpers.LogLog;

import java.util.Hashtable;
import java.util.Properties;
import javax.jms.*;
import javax.naming.InitialContext;
import javax.naming.Context;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;

/**
 * A Simple JMS (P2P) Queue Appender. 
 *
 * @author Ceki G&uuml;lc&uuml;
 * @author Jamie Tsao
*/
public class JMSQueueAppender extends AppenderSkeleton {

    protected QueueConnection queueConnection;
    protected QueueSession queueSession;
    protected QueueSender queueSender;
    protected Queue queue;
    
    String initialContextFactory;
    String providerUrl;
    String queueBindingName;
    String queueConnectionFactoryBindingName;
    
    public 
	JMSQueueAppender() {
    }

  
    /**
     * The <b>InitialContextFactory</b> option takes a string value.
     * Its value, along with the <b>ProviderUrl</b> option will be used
     * to get the InitialContext.
     */
    public void setInitialContextFactory(String initialContextFactory) {
	this.initialContextFactory = initialContextFactory;
    }

    /**
     * Returns the value of the <b>InitialContextFactory</b> option.
     */
    public String getInitialContextFactory() {
	return initialContextFactory;
    }

    /**
     * The <b>ProviderUrl</b> option takes a string value.
     * Its value, along with the <b>InitialContextFactory</b> option will be used
     * to get the InitialContext.
     */
    public void setProviderUrl(String providerUrl) {
	this.providerUrl = providerUrl;
    }

    /**
     * Returns the value of the <b>ProviderUrl</b> option.
     */
    public String getProviderUrl() {
	return providerUrl;
    }

    /**
     * The <b>QueueConnectionFactoryBindingName</b> option takes a
     * string value. Its value will be used to lookup the appropriate
     * <code>QueueConnectionFactory</code> from the JNDI context.
     */
    public void setQueueConnectionFactoryBindingName(String queueConnectionFactoryBindingName) {
	this.queueConnectionFactoryBindingName = queueConnectionFactoryBindingName;
    }
  
    /**
     * Returns the value of the <b>QueueConnectionFactoryBindingName</b> option.
     */
    public String getQueueConnectionFactoryBindingName() {
	return queueConnectionFactoryBindingName;
    }
    
    /**
     * The <b>QueueBindingName</b> option takes a
     * string value. Its value will be used to lookup the appropriate
     * destination <code>Queue</code> from the JNDI context.
     */
    public void setQueueBindingName(String queueBindingName) {
	this.queueBindingName = queueBindingName;
    }
  
    /**
       Returns the value of the <b>QueueBindingName</b> option.
    */
    public String getQueueBindingName() {
	return queueBindingName;
    }
    

    /**
     * Overriding this method to activate the options for this class
     * i.e. Looking up the Connection factory ...
     */
    public void activateOptions() {
	
	QueueConnectionFactory queueConnectionFactory;
	
	try {

	    Context ctx = getInitialContext();      
	    queueConnectionFactory = (QueueConnectionFactory) ctx.lookup(queueConnectionFactoryBindingName);
	    queueConnection = queueConnectionFactory.createQueueConnection();
    
	    queueSession = queueConnection.createQueueSession(false,
							      Session.AUTO_ACKNOWLEDGE);
      
	    Queue queue = (Queue) ctx.lookup(queueBindingName);
	    queueSender = queueSession.createSender(queue);
	    
	    queueConnection.start();

	    ctx.close();      

	} catch(Exception e) {
	    errorHandler.error("Error while activating options for appender named ["+name+
			       "].", e, ErrorCode.GENERIC_FAILURE);
	}
    }
 
    protected InitialContext getInitialContext() throws NamingException {
	try {
	    Hashtable ht = new Hashtable();
	    
	    //Populate property hashtable with data to retrieve the context.
	    ht.put(Context.INITIAL_CONTEXT_FACTORY, initialContextFactory);
	    ht.put(Context.PROVIDER_URL, providerUrl);
	    
	    return (new InitialContext(ht));
	    
	} catch (NamingException ne) {
	    LogLog.error("Could not get initial context with ["+initialContextFactory + "] and [" + providerUrl + "]."); 
	    throw ne;
	}
    }

  
    protected boolean checkEntryConditions() {
	
	String fail = null;
	
	if(this.queueConnection == null) {
	    fail = "No QueueConnection";
	} else if(this.queueSession == null) {
	    fail = "No QueueSession";
	} else if(this.queueSender == null) {
	    fail = "No QueueSender";
	} 
	
	if(fail != null) {
	    errorHandler.error(fail +" for JMSQueueAppender named ["+name+"].");      
	    return false;
	} else {
	    return true;
	}
    }

  /**
   * Close this JMSQueueAppender. Closing releases all resources used by the
   * appender. A closed appender cannot be re-opened. 
   */
    public synchronized // avoid concurrent append and close operations
	void close() {

	if(this.closed) 
	    return;
	
	LogLog.debug("Closing appender ["+name+"].");
	this.closed = true;    
	
	try {
	    if(queueSession != null) 
		queueSession.close();	
	    if(queueConnection != null) 
		queueConnection.close();
	} catch(Exception e) {
	    LogLog.error("Error while closing JMSQueueAppender ["+name+"].", e);	
	}   

	// Help garbage collection
	queueSender = null;
	queueSession = null;
	queueConnection = null;
    }
    
    /**
     * This method called by {@link AppenderSkeleton#doAppend} method to
     * do most of the real appending work.  The LoggingEvent will be
     * be wrapped in an ObjectMessage to be put on the JMS queue.
     */
    public void append(LoggingEvent event) {

	if(!checkEntryConditions()) {
	    return;
	}
	
	try {

	    ObjectMessage msg = queueSession.createObjectMessage();
	    msg.setObject(event);
	    queueSender.send(msg);

	} catch(Exception e) {
	    errorHandler.error("Could not send message in JMSQueueAppender ["+name+"].", e, 
			       ErrorCode.GENERIC_FAILURE);
	}
    }
    
    public boolean requiresLayout() {
	return false;
    }  
}