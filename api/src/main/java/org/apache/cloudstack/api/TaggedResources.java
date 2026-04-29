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
package org.apache.cloudstack.api;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import javax.annotation.Nullable;

import org.apache.commons.collections.MapUtils;

import com.cloud.exception.InvalidParameterValueException;

public abstract class TaggedResources {
    @Nullable
    public static Map<String, String> parseKeyValueMap(Map map, boolean allowNullValues) {
        Map<String, String> result = null;
        if (MapUtils.isNotEmpty(map)) {
            Map<Integer, Map<String, String>> typedMap = map;
            result = typedMap.values()
                        .stream()
                        .collect(toMap(
                                t -> t.get("key"),
                                t -> getValue(t, allowNullValues)
                        ));
        }
        return result;
    }

    @Nullable
    public static Map<String, List<String>> groupBy(Map map, String keyField, String valueField) {
        Map<String, List<String>> result = null;
        if (MapUtils.isNotEmpty(map)) {
            final Function<Map<String, String>, String> key = entry -> entry.get(keyField);
            final Function<Map<String, String>, String> value = entry -> entry.get(valueField);
            Map<Integer, Map<String, String>> typedMap = (Map<Integer, Map<String, String>>) map;
            result = typedMap.values()
                             .stream()
                             .collect(groupingBy(key, mapping(value, toList())));
        }

        return result;
    }

    private static String getValue(Map<String, String> tagEntry, boolean allowNullValues) {
        String value = tagEntry.get("value");
        if (value == null && !allowNullValues) {
            throw new InvalidParameterValueException("No value is passed in for key " + tagEntry.get("key"));
        }
        return value;
    }
}
