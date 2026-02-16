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

package org.apache.cloudstack.veeam.api.dto;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonCreator;

public class NamedList<T> {
    private final String name;
    private final List<T> items;

    private NamedList(String name, List<T> items) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("name must be non-empty");
        }
        this.name = name;
        this.items = items == null ? Collections.emptyList() : items;
    }

    public static <T> NamedList<T> of(String name, List<T> items) {
        return new NamedList<>(name, items);
    }

    @JsonAnyGetter
    public Map<String, List<T>> asMap() {
        return Collections.singletonMap(name, items);
    }

    @JsonCreator
    public static <T> NamedList<T> fromMap(Map<String, List<T>> map) {
        if (map == null || map.size() != 1) {
            throw new IllegalArgumentException("Expected single-property object for NamedList");
        }
        Entry<String, List<T>> e = map.entrySet().iterator().next();
        return new NamedList<>(e.getKey(), e.getValue());
    }
}
