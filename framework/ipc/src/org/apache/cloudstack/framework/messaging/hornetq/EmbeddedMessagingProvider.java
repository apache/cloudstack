/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cloudstack.framework.messaging.hornetq;

import org.apache.cloudstack.framework.messaging.Message;
import org.apache.cloudstack.framework.messaging.MessagingDeliveryListener;
import org.apache.cloudstack.framework.messaging.MessagingDeliveryStrategy;
import org.apache.cloudstack.framework.messaging.MessagingProvider;
import org.apache.cloudstack.framework.messaging.MessagingSubscriber;

public class EmbeddedMessagingProvider implements MessagingProvider {

	public void createChanel(String topic,
			MessagingDeliveryStrategy deliveryStrategy) {
		// TODO Auto-generated method stub
		
	}

	public void pulishMessage(String topic, Message message) {
		// TODO Auto-generated method stub
		
	}

	public void publishCertifiedMessage(String topic, Message message,
			int retryIntervalMillis, int timeoutMillis,
			MessagingDeliveryListener deliveryListener) {
		// TODO Auto-generated method stub
		
	}

	public void subscribe(String topicChannel, String messageTitle,
			MessagingSubscriber subscriber) {
		// TODO Auto-generated method stub
		
	}

	public void unsubscribe(String topicChannel, String messageTitle,
			MessagingSubscriber subscriber) {
		// TODO Auto-generated method stub
		
	}
}
