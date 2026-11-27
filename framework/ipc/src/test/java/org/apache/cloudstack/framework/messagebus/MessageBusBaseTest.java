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
import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.cloud.utils.db.TransactionLegacy;

public class MessageBusBaseTest {

    private MessageBusBase bus;
    private TransactionLegacy txn;

    @Before
    public void setUp() {
        bus = new MessageBusBase();
        // publish() calls noDbTxn() which needs a thread-local transaction to exist.
        // open() only registers a lazy transaction; it does not open a DB connection.
        txn = TransactionLegacy.open("MessageBusBaseTest");
    }

    @After
    public void tearDown() {
        if (txn != null) {
            txn.close();
        }
    }

    /**
     * A subscriber registered on both an ancestor ("Host") and a descendant ("Host.123")
     * must receive exactly one callback when the descendant topic is published, not one per
     * matching node. This pins the LinkedHashSet dedup behavior in publish().
     */
    @Test
    public void testSubscriberOnAncestorAndDescendantNotifiedOnce() {
        CountingSubscriber subscriber = new CountingSubscriber();
        bus.subscribe("Host", subscriber);
        bus.subscribe("Host.123", subscriber);

        bus.publish(null, "Host.123", PublishScope.LOCAL, null);

        Assert.assertEquals(1, subscriber.count);
    }

    /**
     * Distinct subscribers on ancestor and descendant are both notified, and the more specific
     * (descendant) subscriber is notified before the ancestor one.
     */
    @Test
    public void testDescendantNotifiedBeforeAncestor() {
        final List<String> order = new ArrayList<>();
        MessageSubscriber ancestor = (sender, subject, args) -> order.add("ancestor");
        MessageSubscriber descendant = (sender, subject, args) -> order.add("descendant");
        bus.subscribe("Host", ancestor);
        bus.subscribe("Host.123", descendant);

        bus.publish(null, "Host.123", PublishScope.LOCAL, null);

        Assert.assertEquals(List.of("descendant", "ancestor"), order);
    }

    /**
     * A subscriber on a sibling topic ("Host.456") must not be notified for "Host.123".
     */
    @Test
    public void testSiblingSubscriberNotNotified() {
        CountingSubscriber sibling = new CountingSubscriber();
        bus.subscribe("Host.456", sibling);

        bus.publish(null, "Host.123", PublishScope.LOCAL, null);

        Assert.assertEquals(0, sibling.count);
    }

    private static class CountingSubscriber implements MessageSubscriber {
        int count = 0;

        @Override
        public void onPublishMessage(String senderAddress, String subject, Object args) {
            count++;
        }
    }
}
