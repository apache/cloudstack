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
import java.util.Map;

/**
 * Publish and Subscribe provider interface
 *
 */
public interface EventBus extends Adapter{
    /**
     * Publish an event
     * 
     * @param category category of the event being published (e.g. action, usage, alert etc)
     * @param type type of the event (e.g. vm stop, volume delete etc)
     * @param description description of the event
     * @return true if the event has been successfully published.
     */
    boolean publish(String category, String type, Map<String, String> description);

    /**
     * Subscribe to events of a category and a type
     *  
     * @param category category of the event being subscribed (e.g. action, usage, alert etc)
     * @param type type of the event (e.g. vm stop, volume delete etc)
     * @param subscriber class that is intends to receive subscribed event
     * @return true if the subscribe has been successfully registered.
     */
    boolean subscribe(String category, String type, EventSubscriber subscriber);
}