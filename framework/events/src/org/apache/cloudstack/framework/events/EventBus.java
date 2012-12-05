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

package org.apache.cloudstack.framework.events;

import com.cloud.utils.component.Adapter;

/**
 * Interface to publish and subscribe to CloudStack events
 *
 */
public interface EventBus extends Adapter{

    /**
     * publish an event
     *
     * @param event event that needs to be published
     * @return true if the event has been successfully published on event bus
     */
    boolean publish(Event event);

    /**
     * subscribe to events of a category and a type
     *
     * @param topic defines category and type of the events being subscribed to
     * @param subscriber subscriber that intends to receive event notification
     * @return true if the subscriber has been successfully registered.
     */
    boolean subscribe(EventTopic topic, EventSubscriber subscriber);

    /**
     * unsubscribe to events of a category and a type
     *
     * @param topic defines category and type of the events to unsubscribe
     * @param subscriber subscriber that intends to unsubscribe from the event notification
     * @return true if the subscriber has been successfully unsubscribed.
     */
    boolean unsubscribe(EventTopic topic, EventSubscriber subscriber);

}
