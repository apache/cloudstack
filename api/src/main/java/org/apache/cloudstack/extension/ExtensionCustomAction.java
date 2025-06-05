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

package org.apache.cloudstack.extension;

import java.security.InvalidParameterException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Identity;
import org.apache.cloudstack.api.InternalIdentity;
import org.apache.cloudstack.utils.reflectiontostringbuilderutils.ReflectionToStringBuilderUtils;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.utils.DateUtil;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

public interface ExtensionCustomAction extends InternalIdentity, Identity {
    enum ResourceType {
        VirtualMachine(com.cloud.vm.VirtualMachine.class);

        private final Class<?> clazz;

        ResourceType(Class<?> clazz) {
            this.clazz = clazz;
        }

        public Class<?> getAssociatedClass() {
            return this.clazz;
        }

        public static ResourceType fromString(String value) {
            if (value == null || value.trim().isEmpty()) {
                return null;
            }
            for (ResourceType type : ResourceType.values()) {
                if (type.name().equalsIgnoreCase(value.trim())) {
                    return type;
                }
            }
            throw new IllegalArgumentException(String.format("Unknown resource type - %s", value));
        }
    }

    String getName();

    String getDescription();

    long getExtensionId();

    ResourceType getResourceType();

    Integer getRoles();

    String getSuccessMessage();

    String getErrorMessage();

    boolean isEnabled();

    Date getCreated();


    class Parameter {
        private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

        private final String name;
        private final BaseCmd.CommandType type;
        private final boolean required;

        public Parameter(String name, BaseCmd.CommandType type, boolean required) {
            this.name = name;
            this.type = type;
            this.required = required;
        }

        public static Parameter fromMap(Map<String, String> map) throws InvalidParameterException {
            final String name = map.get("name");
            final String typeStr = map.get("type");
            final String required = map.get("required");
            if (StringUtils.isBlank(name)) {
                throw new InvalidParameterValueException("Invalid parameter specified with empty name");
            }
            if (StringUtils.isBlank(typeStr)) {
                throw new InvalidParameterException(String.format("Not type specified for parameter: %s", name));
            }
            BaseCmd.CommandType parsedType = EnumUtils.getEnumIgnoreCase(BaseCmd.CommandType.class, typeStr);
            if (parsedType == null) {
                throw new IllegalArgumentException(String.format("Invalid type: %s specified for parameter: %s",
                        typeStr, name));
            }
            return new Parameter(name, parsedType, Boolean.parseBoolean(required));
        }

        public String getName() {
            return name;
        }

        public BaseCmd.CommandType getType() {
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

        public Object validatedValue(String value) {
            if (StringUtils.isBlank(value)) {
                throw new InvalidParameterException("Empty value for parameter '" + name + "': " + value);
            }
            try {
                switch (type) {
                    case BOOLEAN:
                        return Arrays.asList("true", "false").contains(value);
                    case DATE:
                        return DateUtil.parseTZDateString(value);
                    case FLOAT:
                        return Float.valueOf(value);
                    case INTEGER:
                        return Integer.valueOf(value);
                    case SHORT:
                        return Short.valueOf(value);
                    case LONG:
                        return Long.valueOf(value);
                    case UUID:
                        return java.util.UUID.fromString(value);
                    default:
                        return value;
                }
            } catch (Exception e) {
                throw new InvalidParameterException("Invalid value for parameter '" + name + "': " + value);
            }
        }

        public static Map<String, Object> validateParameterValues(List<Parameter> parameterDefinitions,
                   Map<String, String> suppliedValues) throws InvalidParameterException {
            for (Parameter param : parameterDefinitions) {
                String value = suppliedValues.get(param.getName());
                if (param.isRequired()) {
                    if (value == null || value.trim().isEmpty()) {
                        throw new InvalidParameterException("Missing or empty required parameter: " + param.getName());
                    }
                }
            }
            Map<String, Object> validatedParams = new HashMap<>();
            for (Map.Entry<String, String> entry : suppliedValues.entrySet()) {
                String name = entry.getKey();
                String value = entry.getValue();
                Parameter param = parameterDefinitions.stream()
                        .filter(p -> p.getName().equals(name))
                        .findFirst()
                        .orElse(null);
                if (param != null) {
                    validatedParams.put(name, param.validatedValue(value));
                } else  {
                    validatedParams.put(name, value);
                }
            }
            return validatedParams;
        }
    }
}
