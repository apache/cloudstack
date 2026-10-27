// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

package org.apache.cloudstack.mom.webhook;

import java.util.Date;
import java.util.List;

import org.apache.cloudstack.api.Identity;
import org.apache.cloudstack.api.InternalIdentity;

public interface WebhookFilter extends Identity, InternalIdentity {

    enum Type {
        EventType
    }

    enum Mode {
        Include, Exclude
    }

    enum MatchType {
        Exact, Prefix, Suffix, Contains
    }

    long getId();
    long getWebhookId();
    Type getType();
    Mode getMode();
    MatchType getMatchType();
    String getValue();
    Date getCreated();

    static boolean overlaps(WebhookFilter.MatchType oldMatchType, String oldValue, WebhookFilter.MatchType newMatchType, String newValue) {
        switch (oldMatchType) {
            case Exact:
                switch (newMatchType) {
                    case Exact:
                        return oldValue.equals(newValue);
                }
                break;

            case Prefix:
                switch (newMatchType) {
                    case Exact:
                    case Prefix:
                        return newValue.startsWith(oldValue);
                }
                break;

            case Suffix:
                switch (newMatchType) {
                    case Exact:
                    case Suffix:
                        return newValue.endsWith(oldValue);
                }
                break;

            case Contains:
                switch (newMatchType) {
                    case Exact:
                    case Prefix:
                    case Suffix:
                    case Contains:
                        return newValue.contains(oldValue);
                }
                break;

            default:
                break;
        }
        return false;
    }

    default WebhookFilter getConflicting(List<? extends WebhookFilter> existing) {
        for (WebhookFilter f : existing) {
            if (f.getType() != this.getType()) {
                continue;
            }

            // 1. Duplicate entry (same mode, match type, and value)
            if (f.getMode() == this.getMode()
                    && f.getMatchType() == this.getMatchType()
                    && f.getValue().equalsIgnoreCase(this.getValue())) {
                return f;
            }

            // 2. Opposite mode (INCLUDE vs EXCLUDE) — check for overlap
            if (f.getMode() != this.getMode()) {
                String oldVal = f.getValue().toUpperCase();
                String newVal = this.getValue().toUpperCase();

                if (overlaps(f.getMatchType(), oldVal, this.getMatchType(), newVal)) {
                    return f;
                }
            }
        }
        return null;
    }
}
