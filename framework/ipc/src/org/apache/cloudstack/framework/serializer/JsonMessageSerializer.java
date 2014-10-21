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
package org.apache.cloudstack.framework.serializer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class JsonMessageSerializer implements MessageSerializer {

    // this will be injected from external to allow installation of
    // type adapters needed by upper layer applications
    private Gson _gson;

    private OnwireClassRegistry _clzRegistry;

    public JsonMessageSerializer() {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.setVersion(1.5);
        _gson = gsonBuilder.create();
    }

    public Gson getGson() {
        return _gson;
    }

    public void setGson(Gson gson) {
        _gson = gson;
    }

    public OnwireClassRegistry getOnwireClassRegistry() {
        return _clzRegistry;
    }

    public void setOnwireClassRegistry(OnwireClassRegistry clzRegistry) {
        _clzRegistry = clzRegistry;
    }

    @Override
    public <T> String serializeTo(Class<?> clz, T object) {
        assert (clz != null);
        assert (object != null);

        StringBuffer sbuf = new StringBuffer();

        OnwireName onwire = clz.getAnnotation(OnwireName.class);
        if (onwire == null)
            throw new RuntimeException("Class " + clz.getCanonicalName() + " is not declared to be onwire");

        sbuf.append(onwire.name()).append("|");
        sbuf.append(_gson.toJson(object));

        return sbuf.toString();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T serializeFrom(String message) {
        assert (message != null);
        int contentStartPos = message.indexOf('|');
        if (contentStartPos < 0)
            throw new RuntimeException("Invalid on-wire message format");

        String onwireName = message.substring(0, contentStartPos);
        Class<?> clz = _clzRegistry.getOnwireClass(onwireName);
        if (clz == null)
            throw new RuntimeException("Onwire class is not registered. name: " + onwireName);

        return (T)_gson.fromJson(message.substring(contentStartPos + 1), clz);
    }
}
