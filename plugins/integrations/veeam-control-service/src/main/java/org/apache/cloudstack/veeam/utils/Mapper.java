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

package org.apache.cloudstack.veeam.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

public class Mapper {
    private final ObjectMapper json;
    private final XmlMapper xml;

    public Mapper() {
        this.json = new ObjectMapper();
        this.xml = new XmlMapper();

        configure(json);
        configure(xml);
    }

    private static void configure(final ObjectMapper mapper) {
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        // If you ever add enums etc:
        // mapper.enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING);
        // mapper.enable(DeserializationFeature.READ_ENUMS_USING_TO_STRING);
    }

    public String toJson(final Object value) throws JsonProcessingException {
        return json.writeValueAsString(value);
    }

    public String toXml(final Object value) throws JsonProcessingException {
        return xml.writeValueAsString(value);
    }

    public ObjectMapper jsonMapper() {
        return json;
    }

    public XmlMapper xmlMapper() {
        return xml;
    }
}
