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

package org.apache.cloudstack.framework.eventbus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cloudstack.framework.serializer.MessageSerializer;

public class EventBusBase implements EventBus {

	private Gate _gate;
	private List<ActionRecord> _pendingActions;
	
	private SubscriptionNode _subscriberRoot;
	private MessageSerializer _messageSerializer; 
	
	public EventBusBase() {
		_gate = new Gate();
		_pendingActions = new ArrayList<ActionRecord>();
		
		_subscriberRoot = new SubscriptionNode("/", null);
	}
	
	@Override
	public void setMessageSerializer(MessageSerializer messageSerializer) {
		_messageSerializer = messageSerializer;
	}
	
	@Override
	public MessageSerializer getMessageSerializer() {
		return _messageSerializer;
	}
	
	@Override
	public void subscribe(String subject, Subscriber subscriber) {
		assert(subject != null);
		assert(subscriber != null);
		if(_gate.enter()) {
			SubscriptionNode current = locate(subject, null, true);
			assert(current != null);
			current.addSubscriber(subscriber);
			_gate.leave();
		} else {
			synchronized(_pendingActions) {
				_pendingActions.add(new ActionRecord(ActionType.Subscribe, subject, subscriber));
			}
		}
	}

	@Override
	public void unsubscribe(String subject, Subscriber subscriber) {
		if(_gate.enter()) {
			SubscriptionNode current = locate(subject, null, false);
			if(current != null)
				current.removeSubscriber(subscriber);
			
			_gate.leave();
		} else {
			synchronized(_pendingActions) {
				_pendingActions.add(new ActionRecord(ActionType.Unsubscribe, subject, subscriber));
			}
		}
	}

	@Override
	public void publish(String senderAddress, String subject, PublishScope scope, 
		Object args) {
		
		if(_gate.enter(true)) {

			List<SubscriptionNode> chainFromTop = new ArrayList<SubscriptionNode>();
			SubscriptionNode current = locate(subject, chainFromTop, false);
			
			if(current != null)
				current.notifySubscribers(senderAddress, subject, args);
			
			Collections.reverse(chainFromTop);
			for(SubscriptionNode node : chainFromTop)
				node.notifySubscribers(senderAddress, subject, args);
			
			_gate.leave();
		}
	}
	
	private void onGateOpen() {
		synchronized(_pendingActions) {
			ActionRecord record = null;
			if(_pendingActions.size() > 0) {
				while((record = _pendingActions.remove(0)) != null) {
					switch(record.getType()) {
					case Subscribe :
						{
							SubscriptionNode current = locate(record.getSubject(), null, true);
							assert(current != null);
							current.addSubscriber(record.getSubscriber());
						}
						break;
						
					case Unsubscribe :
						{
							SubscriptionNode current = locate(record.getSubject(), null, false);
							if(current != null)
								current.removeSubscriber(record.getSubscriber());
						}
						break;
						
					default :
						assert(false);
						break;
					
					}
				}
			}
		}
	}
	
	
	private SubscriptionNode locate(String subject, List<SubscriptionNode> chainFromTop,
		boolean createPath) {
		
		assert(subject != null);
		
		String[] subjectPathTokens = subject.split("\\.");
		return locate(subjectPathTokens, _subscriberRoot, chainFromTop, createPath);
	}
	
	private static SubscriptionNode locate(String[] subjectPathTokens, 
		SubscriptionNode current, List<SubscriptionNode> chainFromTop, boolean createPath) {
		
		assert(current != null);
		assert(subjectPathTokens != null);
		assert(subjectPathTokens.length > 0);

		if(chainFromTop != null)
			chainFromTop.add(current);
		
		SubscriptionNode next = current.getChild(subjectPathTokens[0]);
		if(next == null) {
			if(createPath) {
				next = new SubscriptionNode(subjectPathTokens[0], null);
				current.addChild(subjectPathTokens[0], next);
			} else {
				return null;
			}
		}
		
		if(subjectPathTokens.length > 1) {
			return locate((String[])Arrays.copyOfRange(subjectPathTokens, 1, subjectPathTokens.length),
				next, chainFromTop, createPath);
		} else {
			return next;
		}
	}
	
	
	//
	// Support inner classes
	//
	private static enum ActionType {
		Subscribe,
		Unsubscribe
	}
	
	private static class ActionRecord {
		private ActionType _type;
		private String _subject;
		private Subscriber _subscriber;
		
		public ActionRecord(ActionType type, String subject, Subscriber subscriber) {
			_type = type;
			_subject = subject;
			_subscriber = subscriber;
		}
		
		public ActionType getType() { 
			return _type; 
		}
		
		public String getSubject() {
			return _subject;
		}
		
		public Subscriber getSubscriber() {
			return _subscriber;
		}
	}
	
	private class Gate {
		private int _reentranceCount;
		private Thread _gateOwner;
		
		public Gate() {
			_reentranceCount = 0;
			_gateOwner = null;
		}
		
		public boolean enter() {
			return enter(false);
		}
		
		public boolean enter(boolean wait) {
			while(true) {
				synchronized(this) {
					if(_reentranceCount == 0) {
						assert(_gateOwner == null);
						
						_reentranceCount++;
						_gateOwner = Thread.currentThread();
						return true;
					} else {
						if(wait) {
							try {
								wait();
							} catch (InterruptedException e) {
							}
						} else {
							break;
						}
					}
				}
			}
			
			return false;
		}
		
		public void leave() {
			synchronized(this) {
				if(_reentranceCount > 0) {
					assert(_gateOwner == Thread.currentThread());
					
					onGateOpen();
					_reentranceCount--;
					assert(_reentranceCount == 0);
					_gateOwner = null;
					
					notifyAll();
				}
			}
		}
	}
	
	private static class SubscriptionNode {
		@SuppressWarnings("unused")
		private String _nodeKey;
		private List<Subscriber> _subscribers;
		private Map<String, SubscriptionNode> _children;
		
		public SubscriptionNode(String nodeKey, Subscriber subscriber) {
			assert(nodeKey != null);
			_nodeKey = nodeKey;
			_subscribers = new ArrayList<Subscriber>();
			
			if(subscriber != null)
				_subscribers.add(subscriber);
			
			_children = new HashMap<String, SubscriptionNode>();
		}
		
		@SuppressWarnings("unused")
		public List<Subscriber> getSubscriber() {
			return _subscribers;
		}
		
		public void addSubscriber(Subscriber subscriber) {
			_subscribers.add(subscriber);
		}
		
		public void removeSubscriber(Subscriber subscriber) {
			_subscribers.remove(subscriber);
		}
		
		public SubscriptionNode getChild(String key) {
			return _children.get(key);
		}
		
		public void addChild(String key, SubscriptionNode childNode) {
			_children.put(key, childNode);
		}
		
		public void notifySubscribers(String senderAddress, String subject,  Object args) {
			for(Subscriber subscriber : _subscribers) {
				subscriber.onPublishEvent(senderAddress, subject, args);
			}
		}
	}
}
