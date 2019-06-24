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

package org.apache.cloudstack.framework.messagebus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import org.apache.cloudstack.framework.serializer.MessageSerializer;

import com.cloud.utils.db.TransactionLegacy;
import com.cloud.utils.exception.CloudRuntimeException;

public class MessageBusBase implements MessageBus {

    private final Gate _gate;
    private final List<ActionRecord> _pendingActions;

    private final SubscriptionNode _subscriberRoot;
    private MessageSerializer _messageSerializer;

    private static final Logger s_logger = Logger.getLogger(MessageBusBase.class);

    public MessageBusBase() {
        _gate = new Gate();
        _pendingActions = new ArrayList<ActionRecord>();

        _subscriberRoot = new SubscriptionNode(null, "/", null);
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
    public void subscribe(String subject, MessageSubscriber subscriber) {
        assert (subject != null);
        assert (subscriber != null);
        if (_gate.enter()) {
            if (s_logger.isTraceEnabled()) {
                s_logger.trace("Enter gate in message bus subscribe");
            }
            try {
                SubscriptionNode current = locate(subject, null, true);
                assert (current != null);
                current.addSubscriber(subscriber);
            } finally {
                _gate.leave();
            }
        } else {
            synchronized (_pendingActions) {
                _pendingActions.add(new ActionRecord(ActionType.Subscribe, subject, subscriber));
            }
        }
    }

    @Override
    public void unsubscribe(String subject, MessageSubscriber subscriber) {
        if (_gate.enter()) {
            if (s_logger.isTraceEnabled()) {
                s_logger.trace("Enter gate in message bus unsubscribe");
            }
            try {
                if (subject != null) {
                    SubscriptionNode current = locate(subject, null, false);
                    if (current != null)
                        current.removeSubscriber(subscriber, false);
                } else {
                    _subscriberRoot.removeSubscriber(subscriber, true);
                }
            } finally {
                _gate.leave();
            }
        } else {
            synchronized (_pendingActions) {
                _pendingActions.add(new ActionRecord(ActionType.Unsubscribe, subject, subscriber));
            }
        }
    }

    @Override
    public void clearAll() {
        if (_gate.enter()) {
            if (s_logger.isTraceEnabled()) {
                s_logger.trace("Enter gate in message bus clearAll");
            }
            try {
                _subscriberRoot.clearAll();
                doPrune();
            } finally {
                _gate.leave();
            }
        } else {
            synchronized (_pendingActions) {
                _pendingActions.add(new ActionRecord(ActionType.ClearAll, null, null));
            }
        }
    }

    @Override
    public void prune() {
        if (_gate.enter()) {
            if (s_logger.isTraceEnabled()) {
                s_logger.trace("Enter gate in message bus prune");
            }
            try {
                doPrune();
            } finally {
                _gate.leave();
            }
        } else {
            synchronized (_pendingActions) {
                _pendingActions.add(new ActionRecord(ActionType.Prune, null, null));
            }
        }
    }

    private void doPrune() {
        List<SubscriptionNode> trimNodes = new ArrayList<SubscriptionNode>();
        _subscriberRoot.prune(trimNodes);

        while (trimNodes.size() > 0) {
            SubscriptionNode node = trimNodes.remove(0);
            SubscriptionNode parent = node.getParent();
            if (parent != null) {
                parent.removeChild(node.getNodeKey());
                if (parent.isTrimmable()) {
                    trimNodes.add(parent);
                }
            }
        }
    }

    @Override
    public void publish(String senderAddress, String subject, PublishScope scope, Object args) {
        // publish cannot be in DB transaction, which may hold DB lock too long, and we are guarding this here
        if (!noDbTxn()){
            String errMsg = "NO EVENT PUBLISH CAN BE WRAPPED WITHIN DB TRANSACTION!";
            s_logger.error(errMsg, new CloudRuntimeException(errMsg));
        }
        if (_gate.enter(true)) {
            if (s_logger.isTraceEnabled()) {
                s_logger.trace("Enter gate in message bus publish");
            }
            try {
                List<SubscriptionNode> chainFromTop = new ArrayList<SubscriptionNode>();
                SubscriptionNode current = locate(subject, chainFromTop, false);

                if (current != null)
                    current.notifySubscribers(senderAddress, subject, args);

                Collections.reverse(chainFromTop);
                for (SubscriptionNode node : chainFromTop)
                    node.notifySubscribers(senderAddress, subject, args);
            } finally {
                _gate.leave();
            }
        }
    }

    private void onGateOpen() {
        synchronized (_pendingActions) {
            ActionRecord record = null;
            while (_pendingActions.size() > 0) {
                record = _pendingActions.remove(0);
                switch (record.getType()) {
                    case Subscribe: {
                        SubscriptionNode current = locate(record.getSubject(), null, true);
                        assert (current != null);
                        current.addSubscriber(record.getSubscriber());
                    }
                        break;

                    case Unsubscribe:
                        if (record.getSubject() != null) {
                            SubscriptionNode current = locate(record.getSubject(), null, false);
                            if (current != null)
                                current.removeSubscriber(record.getSubscriber(), false);
                        } else {
                            _subscriberRoot.removeSubscriber(record.getSubscriber(), true);
                        }
                        break;

                    case ClearAll:
                        _subscriberRoot.clearAll();
                        break;

                    case Prune:
                        doPrune();
                        break;

                    default:
                        assert (false);
                        break;

                }
            }
        }
    }

    private SubscriptionNode locate(String subject, List<SubscriptionNode> chainFromTop, boolean createPath) {

        assert (subject != null);
        // "/" is special name for root node
        if (subject.equals("/"))
            return _subscriberRoot;

        String[] subjectPathTokens = subject.split("\\.");
        return locate(subjectPathTokens, _subscriberRoot, chainFromTop, createPath);
    }

    private static SubscriptionNode locate(String[] subjectPathTokens, SubscriptionNode current, List<SubscriptionNode> chainFromTop, boolean createPath) {

        assert (current != null);
        assert (subjectPathTokens != null);
        assert (subjectPathTokens.length > 0);

        if (chainFromTop != null)
            chainFromTop.add(current);

        SubscriptionNode next = current.getChild(subjectPathTokens[0]);
        if (next == null) {
            if (createPath) {
                next = new SubscriptionNode(current, subjectPathTokens[0], null);
                current.addChild(subjectPathTokens[0], next);
            } else {
                return null;
            }
        }

        if (subjectPathTokens.length > 1) {
            return locate(Arrays.copyOfRange(subjectPathTokens, 1, subjectPathTokens.length), next, chainFromTop, createPath);
        } else {
            return next;
        }
    }

    private boolean noDbTxn() {
        TransactionLegacy txn = TransactionLegacy.currentTxn();
        return !txn.dbTxnStarted();
    }

    //
    // Support inner classes
    //
    private static enum ActionType {
        Subscribe, Unsubscribe, ClearAll, Prune
    }

    private static class ActionRecord {
        private final ActionType _type;
        private final String _subject;
        private final MessageSubscriber _subscriber;

        public ActionRecord(ActionType type, String subject, MessageSubscriber subscriber) {
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

        public MessageSubscriber getSubscriber() {
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
            while (true) {
                synchronized (this) {
                    if (_reentranceCount == 0) {
                        assert (_gateOwner == null);

                        _reentranceCount++;
                        _gateOwner = Thread.currentThread();
                        return true;
                    } else {
                        if (wait) {
                            try {
                                wait();
                            } catch (InterruptedException e) {
                                s_logger.debug("[ignored] interupted while guarding re-entrance on message bus.");
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
            synchronized (this) {
                if (_reentranceCount > 0) {
                    try {
                        assert (_gateOwner == Thread.currentThread());

                        onGateOpen();
                    } finally {
                        if (s_logger.isTraceEnabled()) {
                            s_logger.trace("Open gate of message bus");
                        }
                        _reentranceCount--;
                        assert (_reentranceCount == 0);
                        _gateOwner = null;

                        notifyAll();
                    }
                }
            }
        }
    }

    private static class SubscriptionNode {
        private final String _nodeKey;
        private final List<MessageSubscriber> _subscribers;
        private final Map<String, SubscriptionNode> _children;
        private final SubscriptionNode _parent;

        public SubscriptionNode(SubscriptionNode parent, String nodeKey, MessageSubscriber subscriber) {
            assert (nodeKey != null);
            _parent = parent;
            _nodeKey = nodeKey;
            _subscribers = new ArrayList<MessageSubscriber>();

            if (subscriber != null)
                _subscribers.add(subscriber);

            _children = new HashMap<String, SubscriptionNode>();
        }

        public SubscriptionNode getParent() {
            return _parent;
        }

        public String getNodeKey() {
            return _nodeKey;
        }

        @SuppressWarnings("unused")
        public List<MessageSubscriber> getSubscriber() {
            return _subscribers;
        }

        public void addSubscriber(MessageSubscriber subscriber) {
            if (!_subscribers.contains(subscriber))
                _subscribers.add(subscriber);
        }

        public void removeSubscriber(MessageSubscriber subscriber, boolean recursively) {
            if (recursively) {
                for (Map.Entry<String, SubscriptionNode> entry : _children.entrySet()) {
                    entry.getValue().removeSubscriber(subscriber, true);
                }
            }
            _subscribers.remove(subscriber);
        }

        public SubscriptionNode getChild(String key) {
            return _children.get(key);
        }

        public void addChild(String key, SubscriptionNode childNode) {
            _children.put(key, childNode);
        }

        public void removeChild(String key) {
            _children.remove(key);
        }

        public void clearAll() {
            // depth-first
            for (Map.Entry<String, SubscriptionNode> entry : _children.entrySet()) {
                entry.getValue().clearAll();
            }
            _subscribers.clear();
        }

        public void prune(List<SubscriptionNode> trimNodes) {
            assert (trimNodes != null);

            for (Map.Entry<String, SubscriptionNode> entry : _children.entrySet()) {
                entry.getValue().prune(trimNodes);
            }

            if (isTrimmable())
                trimNodes.add(this);
        }

        public void notifySubscribers(String senderAddress, String subject, Object args) {
            for (MessageSubscriber subscriber : _subscribers) {
                subscriber.onPublishMessage(senderAddress, subject, args);
            }
        }

        public boolean isTrimmable() {
            return _children.size() == 0 && _subscribers.size() == 0;
        }
    }
}
