//
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
//
package com.cloud.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.util.Iterator;
import java.util.Map.Entry;

import static com.cloud.utils.NumbersUtil.toHumanReadableSize;


public class HumanReadableJson {

    private boolean changeValue;
    private StringBuilder output = new StringBuilder();
    private boolean firstElement = true;

    private final String[] elementsToMatch = {
            "bytesSent","bytesReceived","BytesWrite","BytesRead","bytesReadRate","bytesWriteRate","iopsReadRate",
            "iopsWriteRate","ioRead","ioWrite","bytesWrite","bytesRead","networkkbsread","networkkbswrite",
            "diskkbsread","diskkbswrite","minRam","maxRam","volumeSize", "size","newSize","memorykbs",
            "memoryintfreekbs","memorytargetkbs","diskioread","diskiowrite","totalSize","capacityBytes",
            "availableBytes","maxDownloadSizeInBytes","templateSize","templatePhySicalSize"
    };

    public static String getHumanReadableBytesJson(String json){
        HumanReadableJson humanReadableJson = new HumanReadableJson();
        humanReadableJson.addElement(json);
        return humanReadableJson.output.toString();
    }

    private void addElement(String content) {
        JsonParser parser = new JsonParser();
        JsonElement jsonElement = parser.parse(content);
        if (jsonElement.isJsonArray()) {
            output.append("[");
            addArray(jsonElement.toString());
            output.append("]");
            firstElement = false;
        }
        if (jsonElement.isJsonObject()) {
            output.append("{");
            firstElement = true;
            addObject(jsonElement.getAsJsonObject().toString());
            output.append("}");
            firstElement = false;
        }
        if (jsonElement.isJsonPrimitive()) {
            if (changeValue) {
                output.append("\"" + toHumanReadableSize(jsonElement.getAsLong()) + "\"");
            } else {
                output.append("\"" + jsonElement.getAsString() + "\"");
            }
            firstElement = false;
        }
    }

    private void addObject(String content) {
        JsonParser parser = new JsonParser();
        JsonElement el1 = parser.parse(content);
        el1.getAsJsonObject().entrySet();
        Iterator<Entry<String, JsonElement>> it = el1.getAsJsonObject().entrySet().iterator();
        while(it.hasNext()) {
            Entry<String, JsonElement> value = it.next();
            String key = value.getKey();
            if (!firstElement){
                output.append(",");
            }
            output.append("\"" + key + "\":");
            for (int i = 0; i < elementsToMatch.length; i++){
                if (key.equals(elementsToMatch[i])) {
                    changeValue = true;
                    break;
                }
            }
            addElement(value.getValue().toString());
            changeValue = false;
        }
    }

    private void addArray(String content) {
        JsonParser parser = new JsonParser();
        JsonArray ar1 = parser.parse(content).getAsJsonArray();
        for (int count = 0; count < ar1.size(); count++) {
            if (count > 0) {
                output.append(",");
            }
            addElement(ar1.get(count).toString());
        }
    }
}
