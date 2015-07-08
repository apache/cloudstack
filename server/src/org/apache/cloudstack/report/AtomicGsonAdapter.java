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
package org.apache.cloudstack.report;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.google.common.util.concurrent.AtomicLongMap;
import java.util.Map;
import java.io.IOException;

public class AtomicGsonAdapter extends TypeAdapter<AtomicLongMap> {

    public AtomicLongMap<Object> read(JsonReader reader) throws IOException {
        reader.nextNull();
        return null;
    }

    public void write(JsonWriter writer, AtomicLongMap value) throws IOException {
        if (value == null) {
            writer.nullValue();
            return;
        }

        @SuppressWarnings("unchecked")
        Map <String, Long> map = value.asMap();

        writer.beginObject();
        for (Map.Entry<String, Long> entry : map.entrySet()) {
            writer.name(entry.getKey()).value(entry.getValue());
        }
        writer.endObject();
    }
}