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

import org.apache.log4j.Logger;

public class MessageDetector implements MessageSubscriber {
    private static final Logger s_logger = Logger.getLogger(MessageDetector.class);

    private MessageBus _messageBus;
    private String[] _subjects;

    public MessageDetector() {
        _messageBus = null;
        _subjects = null;
    }

    public void waitAny(long timeoutInMiliseconds) {
        if (timeoutInMiliseconds < 100) {
            s_logger.warn("waitAny is passed with a too short time-out interval. " + timeoutInMiliseconds + "ms");
            timeoutInMiliseconds = 100;
        }

        synchronized (this) {
            try {
                wait(timeoutInMiliseconds);
            } catch (InterruptedException e) {
                s_logger.debug("[ignored] interupted while waiting on any message.");
            }
        }
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
        if (subjectMatched(subject)) {
            synchronized (this) {
                notifyAll();
            }
        }
    }

    private boolean subjectMatched(String subject) {
        if (_subjects != null) {
            for (String sub : _subjects) {
                if (sub.equals(subject))
                    return true;
            }
        }
        return false;
    }
}
