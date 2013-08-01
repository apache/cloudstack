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

public class MessageDetector implements MessageSubscriber {

    private MessageBus _messageBus;
    private String[] _subjects;

    private volatile boolean _signalled = false;

    public MessageDetector() {
        _messageBus = null;
        _subjects = null;
    }

    public boolean waitAny(long timeoutInMiliseconds) {
        _signalled = false;
        synchronized (this) {
            try {
                wait(timeoutInMiliseconds);
            } catch (InterruptedException e) {
            }
        }
        return _signalled;
    }

    public void open(MessageBus messageBus, String[] subjects) {
        assert (messageBus != null);
        assert (subjects != null);

        _messageBus = messageBus;
        _subjects = subjects;

        if (subjects != null) {
            for (String subject : subjects) {
                messageBus.subscribe(subject, this);
            }
        }
    }

    public void close() {
        if (_subjects != null) {
            assert (_messageBus != null);

            for (String subject : _subjects) {
                _messageBus.unsubscribe(subject, this);
            }
        }
    }

    @Override
    public void onPublishMessage(String senderAddress, String subject, Object args) {
        synchronized (this) {
            _signalled = true;
            notifyAll();
        }
    }
}
