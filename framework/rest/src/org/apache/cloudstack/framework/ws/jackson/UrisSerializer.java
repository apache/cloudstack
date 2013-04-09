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
package org.apache.cloudstack.framework.ws.jackson;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import javax.ws.rs.core.UriBuilder;

import org.apache.cxf.jaxrs.impl.tl.ThreadLocalUriInfo;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;


/**
 * Serializer for a list of ids.
 *
 */
public class UrisSerializer extends JsonSerializer<List<? extends Object>> {
    Url _annotation;

    public UrisSerializer(Url annotation) {
        _annotation = annotation;
    }

    protected UrisSerializer() {
    }

    @Override
    public void serialize(List<? extends Object> lst, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonProcessingException {
        Iterator<? extends Object> it = lst.iterator();
        jgen.writeStartObject();
        while (it.hasNext()) {
            Object id = it.next();
            jgen.writeStartObject();
            jgen.writeFieldName("id");
            jgen.writeObject(id);
            jgen.writeFieldName("uri");
            jgen.writeString(buildUri(_annotation.clazz(), _annotation.method(), id));
            jgen.writeEndObject();
        }
        jgen.writeEndObject();
    }

    protected String buildUri(Class<?> clazz, String method, Object id) {
        ThreadLocalUriInfo uriInfo = new ThreadLocalUriInfo();
        UriBuilder ub = uriInfo.getAbsolutePathBuilder().path(clazz, method);
        ub.build(id);
        return ub.toString();
    }
}
