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

public class MessageBusEndpoint {
    private MessageBus _eventBus;
    private String _sender;
    private PublishScope _scope;

    public MessageBusEndpoint(MessageBus eventBus, String sender, PublishScope scope) {
        _eventBus = eventBus;
        _sender = sender;
        _scope = scope;
    }

    public MessageBusEndpoint setEventBus(MessageBus eventBus) {
        _eventBus = eventBus;
        return this;
    }

    public MessageBusEndpoint setScope(PublishScope scope) {
        _scope = scope;
        return this;
    }

    public PublishScope getScope() {
        return _scope;
    }

    public MessageBusEndpoint setSender(String sender) {
        _sender = sender;
        return this;
    }

    public String getSender() {
        return _sender;
    }

    public void Publish(String subject, Object args) {
        assert (_eventBus != null);
        _eventBus.publish(_sender, subject, _scope, args);
    }
}
