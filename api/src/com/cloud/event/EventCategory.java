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

package com.cloud.event;

import java.util.ArrayList;
import java.util.List;

public class EventCategory {
    private static List<EventCategory> eventCategories = new ArrayList<EventCategory>();
    private String eventCategoryName;

    public EventCategory(String categoryName) {
        this.eventCategoryName = categoryName;
        eventCategories.add(this);
    }

    public String getName() {
        return eventCategoryName;
    }

    public static List<EventCategory> listAllEventCategories() {
        return eventCategories;
    }

    public static EventCategory getEventCategory(String categoryName) {
        for (EventCategory category : eventCategories) {
            if (category.getName().equalsIgnoreCase(categoryName)) {
                return category;
            }
        }
        return null;
    }

    public static final EventCategory ACTION_EVENT = new EventCategory("ActionEvent");
    public static final EventCategory ALERT_EVENT = new EventCategory("AlertEvent");
    public static final EventCategory USAGE_EVENT = new EventCategory("UsageEvent");
    public static final EventCategory RESOURCE_STATE_CHANGE_EVENT = new EventCategory("ResourceStateEvent");
    public static final EventCategory ASYNC_JOB_CHANGE_EVENT = new EventCategory("AsyncJobEvent");
}
