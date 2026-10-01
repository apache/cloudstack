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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.cloudstack.framework.serializer.MessageSerializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.cloud.utils.db.TransactionLegacy;
import com.cloud.utils.exception.CloudRuntimeException;

/**
 * MessageBus implementation based on a hierarchical topic tree.
 *
 * Locking model:
 *   Multiple publishers can run concurrently - publish() takes a read lock,
 *   which only blocks when someone is modifying the subscriber tree (subscribe,
 *   unsubscribe, clearAll, prune). This means a pod restart with hundreds of
 *   hosts reconnecting and firing events simultaneously will not serialize
 *   through a single bottleneck.
 *
 * Subscriber callbacks are intentionally called OUTSIDE the read lock.
 *   Holding a lock during external callbacks is a classic source of production
 *   outages: a slow subscriber (DB call, GC pause, hypervisor roundtrip) would
 *   block ALL other publishers and eventually starve write lock holders
 *   (subscribe/unsubscribe). Instead we snapshot the subscriber list under
 *   the lock and release it before calling anyone.
 */
public class MessageBusBase implements MessageBus {

    // Fair mode: a queued writer (subscribe/unsubscribe/prune/clearAll) is served ahead of
    // newly arriving readers, so a steady stream of publish() read locks cannot starve writers.
    private final ReadWriteLock _lock = new ReentrantReadWriteLock(true);

    private final SubscriptionNode _subscriberRoot;
    private MessageSerializer _messageSerializer;

    protected Logger logger = LogManager.getLogger(getClass());

    public MessageBusBase() {
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
        _lock.writeLock().lock();
        try {
            logger.trace("Acquired write lock in message bus subscribe");
            SubscriptionNode current = locate(subject, null, true);
            assert (current != null);
            current.addSubscriber(subscriber);
        } finally {
            _lock.writeLock().unlock();
        }
    }

    @Override
    public void unsubscribe(String subject, MessageSubscriber subscriber) {
        _lock.writeLock().lock();
        try {
            logger.trace("Acquired write lock in message bus unsubscribe");
            if (subject != null) {
                SubscriptionNode current = locate(subject, null, false);
                if (current != null)
                    current.removeSubscriber(subscriber, false);
            } else {
                _subscriberRoot.removeSubscriber(subscriber, true);
            }
        } finally {
            _lock.writeLock().unlock();
        }
    }

    @Override
    public void clearAll() {
        _lock.writeLock().lock();
        try {
            logger.trace("Acquired write lock in message bus clearAll");
            _subscriberRoot.clearAll();
            doPrune();
        } finally {
            _lock.writeLock().unlock();
        }
    }

    @Override
    public void prune() {
        _lock.writeLock().lock();
        try {
            logger.trace("Acquired write lock in message bus prune");
            doPrune();
        } finally {
            _lock.writeLock().unlock();
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
        if (!noDbTxn()) {
            String errMsg = "NO EVENT PUBLISH CAN BE WRAPPED WITHIN DB TRANSACTION!";
            logger.error(errMsg, new CloudRuntimeException(errMsg));
        }
        // Collect subscribers under read lock (fast - just tree traversal and list copy),
        // then release the lock before calling any callbacks.
        // LinkedHashSet deduplicates: a subscriber registered on both "Host" and "Host.123"
        // gets exactly one callback when "Host.123" is published.
        Set<MessageSubscriber> toNotify = new LinkedHashSet<>();
        _lock.readLock().lock();
        try {
            logger.trace("Acquired read lock in message bus publish");
            List<SubscriptionNode> chainFromTop = new ArrayList<>();
            SubscriptionNode current = locate(subject, chainFromTop, false);

            if (current != null)
                current.collectSubscribers(toNotify);

            Collections.reverse(chainFromTop);
            for (SubscriptionNode node : chainFromTop)
                node.collectSubscribers(toNotify);
        } finally {
            _lock.readLock().unlock();
        }

        for (MessageSubscriber subscriber : toNotify) {
            try {
                subscriber.onPublishMessage(senderAddress, subject, args);
            } catch (Throwable t) {
                logger.error("Subscriber threw an exception during publish of subject: " + subject + " scope: " + scope + " args: " + args, t);
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

        public void collectSubscribers(Collection<MessageSubscriber> target) {
            target.addAll(_subscribers);
        }

        public boolean isTrimmable() {
            return _children.size() == 0 && _subscribers.size() == 0;
        }
    }
}
