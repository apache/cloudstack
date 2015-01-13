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
package org.apache.cloudstack.storage.command;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import org.apache.cloudstack.engine.subsystem.api.storage.DataObject;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPoint;

import java.lang.reflect.Type;

public class TemplateOrVolumePostUploadCommandTypeAdapter implements JsonSerializer<TemplateOrVolumePostUploadCommand> {

    @Override public JsonElement serialize(TemplateOrVolumePostUploadCommand src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject obj = new JsonObject();

        EndPoint endPoint = src.getEndPoint();
        JsonObject endpointJSON = new JsonObject();
        endpointJSON.addProperty("id", endPoint.getId());
        endpointJSON.addProperty("hostAddress", endPoint.getHostAddr());
        endpointJSON.addProperty("publicAddr", endPoint.getPublicAddr());
        obj.add(endPoint.getClass().getName(), endpointJSON);

        DataObject dataObject = src.getDataObject();
        JsonObject dataobjectJSON = new JsonObject();
        dataobjectJSON.addProperty("id", dataObject.getId());
        dataobjectJSON.addProperty("size", dataObject.getSize());
        dataobjectJSON.addProperty("uuid", dataObject.getUuid());
        dataobjectJSON.addProperty("type", String.valueOf(dataObject.getType()));
        obj.add(dataObject.getClass().getName(), dataobjectJSON);

        return obj;
    }
}
