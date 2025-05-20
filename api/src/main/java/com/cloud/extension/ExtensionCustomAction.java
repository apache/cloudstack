//Licensed to the Apache Software Foundation (ASF) under one
//or more contributor license agreements.  See the NOTICE file
//distributed with this work for additional information
//regarding copyright ownership.  The ASF licenses this file
//to you under the Apache License, Version 2.0 (the
//"License"); you may not use this file except in compliance
//with the License.  You may obtain a copy of the License at
//
//http://www.apache.org/licenses/LICENSE-2.0
//
//Unless required by applicable law or agreed to in writing,
//software distributed under the License is distributed on an
//"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
//KIND, either express or implied.  See the License for the
//specific language governing permissions and limitations
//under the License.

package com.cloud.extension;

import java.util.List;

import org.apache.cloudstack.api.Identity;
import org.apache.cloudstack.api.InternalIdentity;
import org.apache.cloudstack.utils.reflectiontostringbuilderutils.ReflectionToStringBuilderUtils;
import org.apache.commons.lang3.StringUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

public interface ExtensionCustomAction extends InternalIdentity, Identity {
    String getName();

    String getDescription();

    long getExtensionId();

    String getRolesList();

    boolean isEnabled();

    class Parameter {
        public enum Type {
            STRING,
            INTEGER,
            DECIMAL,
            BOOLEAN,
            UUID,
            DATE;

            public static Type fromString(String value) {
                if (StringUtils.isBlank(value)) {
                    return STRING;
                }
                try {
                    return Type.valueOf(value.trim().toUpperCase());
                } catch (IllegalArgumentException ex) {
                    return STRING;
                }
            }
        }
        private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

        final String name;
        final Type type;
        final boolean required;

        public Parameter (String name, Type type, boolean required) {
            this.name = name;
            this.type = type;
            this.required = required;
        }

        public String getName() {
            return name;
        }

        public Type getType() {
            return type;
        }

        public boolean isRequired() {
            return required;
        }

        @Override
        public String toString() {
            return String.format("Parameter %s", ReflectionToStringBuilderUtils.reflectOnlySelectedFields(this,
                    "name", "type", "required"));
        }

        public static String toJsonFromList(List<Parameter> parameters) {
            return gson.toJson(parameters);
        }

        public static List<Parameter> toListFromJson(String json) {
            java.lang.reflect.Type listType = new TypeToken<List<Parameter>>() {}.getType();
            return gson.fromJson(json, listType);
        }
    }
}
