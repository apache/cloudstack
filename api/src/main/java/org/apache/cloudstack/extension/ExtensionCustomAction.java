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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.Identity;
import org.apache.cloudstack.api.InternalIdentity;
import org.apache.cloudstack.utils.reflectiontostringbuilderutils.ReflectionToStringBuilderUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.utils.DateUtil;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
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
    }

    String getName();

    String getDescription();

    long getExtensionId();

    ResourceType getResourceType();

    Integer getAllowedRoleTypes();

    String getSuccessMessage();

    String getErrorMessage();

    int getTimeout();

    boolean isEnabled();

    Date getCreated();


    class Parameter {

        public enum Type {
            STRING(true),
            NUMBER(true),
            BOOLEAN(false),
            DATE(false);

            private final boolean supportsOptions;

            Type(boolean supportsOptions) {
                this.supportsOptions = supportsOptions;
            }

            public boolean canSupportsOptions() {
                return supportsOptions;
            }
        }

        public enum ValidationFormat {
            // Universal default format
            NONE(null),

            // String formats
            UUID(Type.STRING),
            EMAIL(Type.STRING),
            PASSWORD(Type.STRING),
            URL(Type.STRING),

            // Number formats
            DECIMAL(Type.NUMBER);

            private final Type baseType;

            ValidationFormat(Type baseType) {
                this.baseType = baseType;
            }

            public Type getBaseType() {
                return baseType;
            }
        }

        private static final Gson gson = new GsonBuilder()
                .registerTypeAdapter(Parameter.class, new ParameterDeserializer())
                .setPrettyPrinting()
                .create();

        private final String name;
        private final Type type;
        private final ValidationFormat validationformat;
        private final List<Object> valueoptions;
        private final boolean required;

        public Parameter(String name, Type type, ValidationFormat validationformat, List<Object> valueoptions, boolean required) {
            this.name = name;
            this.type = type;
            this.validationformat = validationformat;
            this.valueoptions = valueoptions;
            this.required = required;
        }

        /**
         * Parses a CSV string into a list of validated options.
         */
        private static List<Object> parseValueOptions(String name, String csv, Type parsedType, ValidationFormat parsedFormat) {
            if (StringUtils.isBlank(csv)) {
                return null;
            }
            List<String> values = Arrays.stream(csv.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
            switch (parsedType) {
                case STRING:
                    if (parsedFormat != null && parsedFormat != ValidationFormat.NONE) {
                        for (String value : values) {
                            if (!isValidStringValue(value, parsedFormat)) {
                                throw new InvalidParameterValueException(String.format("Invalid value options with validation format: %s for parameter: %s", parsedFormat.name(), name));
                            }
                        }
                    }
                    return new ArrayList<>(values);
                case NUMBER:
                    try {
                        return values.stream()
                                .map(v -> parseNumber(v, parsedFormat))
                                .collect(Collectors.toList());
                    } catch (NumberFormatException ignored) {
                        throw new InvalidParameterValueException(String.format("Invalid value options with validation format: %s for parameter: %s", parsedFormat.name(), name));
                    }
                default:
                    throw new InvalidParameterValueException(String.format("Options not supported for type: %s for parameter: %s", parsedType, name));
            }
        }

        private static Object parseNumber(String value, ValidationFormat parsedFormat) {
            if (parsedFormat == ValidationFormat.DECIMAL) {
                return Float.parseFloat(value);
            }
            return Integer.parseInt(value);
        }

        private static boolean isValidStringValue(String value, ValidationFormat validationFormat ) {
            switch (validationFormat) {
                case NONE:
                    return true;
                case UUID:
                    try {
                        UUID.fromString(value);
                        return true;
                    } catch (Exception ignored) {
                        return false;
                    }
                case EMAIL:
                    return value.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
                case PASSWORD:
                    return !value.trim().isEmpty();
                case URL:
                    try {
                        new java.net.URL(value);
                        return true;
                    } catch (Exception ignored) {
                        return false;
                    }
                default:
                    return false;
            }
        }

        public static Parameter fromMap(Map<String, String> map) throws InvalidParameterValueException {
            final String name = map.get(ApiConstants.NAME);
            final String typeStr = map.get(ApiConstants.TYPE);
            final String validationFormatStr = map.get(ApiConstants.VALIDATION_FORMAT);
            final String required = map.get(ApiConstants.REQUIRED);
            final String valueOptionsStr = map.get(ApiConstants.VALUE_OPTIONS);
            if (StringUtils.isBlank(name)) {
                throw new InvalidParameterValueException("Invalid parameter specified with empty name");
            }
            if (StringUtils.isBlank(typeStr)) {
                throw new InvalidParameterValueException(String.format("No type specified for parameter: %s", name));
            }
            Type parsedType = EnumUtils.getEnumIgnoreCase(Type.class, typeStr);
            if (parsedType == null) {
                throw new InvalidParameterValueException(String.format("Invalid type: %s specified for parameter: %s",
                        typeStr, name));
            }
            ValidationFormat parsedFormat = StringUtils.isBlank(validationFormatStr) ?
                    ValidationFormat.NONE : EnumUtils.getEnumIgnoreCase(ValidationFormat.class, validationFormatStr);
            if (parsedFormat == null || (!ValidationFormat.NONE.equals(parsedFormat) &&
                    parsedFormat.getBaseType() != parsedType)) {
                throw new InvalidParameterValueException(
                        String.format("Invalid validation format: %s specified for type: %s",
                                parsedFormat, parsedType.name()));
            }
            List<Object> valueOptions = parseValueOptions(name, valueOptionsStr, parsedType, parsedFormat);
            return new Parameter(name, parsedType, parsedFormat, valueOptions, Boolean.parseBoolean(required));
        }

        public String getName() {
            return name;
        }

        public Type getType() {
            return type;
        }

        public ValidationFormat getValidationFormat() {
            return validationformat;
        }

        public List<Object> getValueOptions() {
            return valueoptions;
        }

        public boolean isRequired() {
            return required;
        }

        @Override
        public String toString() {
            return String.format("Parameter %s", ReflectionToStringBuilderUtils.reflectOnlySelectedFields(this,
                    ApiConstants.NAME, ApiConstants.TYPE, ApiConstants.REQUIRED));
        }

        public static String toJsonFromList(List<Parameter> parameters) {
            return gson.toJson(parameters);
        }

        public static List<Parameter> toListFromJson(String json) {
            java.lang.reflect.Type listType = new TypeToken<List<Parameter>>() {}.getType();
            return gson.fromJson(json, listType);
        }

        private void validateValueInOptions(Object value) {
            if (CollectionUtils.isNotEmpty(valueoptions) && !valueoptions.contains(value)) {
                throw new InvalidParameterValueException("Invalid value for parameter '" + name + "': " + value +
                        ". Valid options are: " + valueoptions.stream().map(Object::toString).collect(Collectors.joining(", ")));
            }
        }

        public Object validatedValue(String value) {
            if (StringUtils.isBlank(value)) {
                throw new InvalidParameterValueException("Empty value for parameter '" + name + "': " + value);
            }
            try {
                switch (type) {
                    case BOOLEAN:
                        if (!Arrays.asList("true", "false").contains(value)) {
                            throw new IllegalArgumentException();
                        }
                        return Boolean.parseBoolean(value);
                    case DATE:
                        return DateUtil.parseTZDateString(value);
                    case NUMBER:
                        Object obj = parseNumber(value, validationformat);
                        validateValueInOptions(obj);
                        return obj;
                    default:
                        if (!isValidStringValue(value, validationformat)) {
                            throw new IllegalArgumentException();
                        }
                        validateValueInOptions(value);
                        return value;
                }
            } catch (Exception e) {
                throw new InvalidParameterValueException("Invalid value for parameter '" + name + "': " + value);
            }
        }

        public static Map<String, Object> validateParameterValues(List<Parameter> parameterDefinitions,
                   Map<String, String> suppliedValues) throws InvalidParameterValueException {
            if (suppliedValues == null) {
                suppliedValues = new HashMap<>();
            }
            for (Parameter param : parameterDefinitions) {
                String value = suppliedValues.get(param.getName());
                if (param.isRequired()) {
                    if (value == null || value.trim().isEmpty()) {
                        throw new InvalidParameterValueException("Missing or empty required parameter: " + param.getName());
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

        static class ParameterDeserializer implements JsonDeserializer<Parameter> {

            @Override
            public Parameter deserialize(JsonElement json, java.lang.reflect.Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
                JsonObject obj = json.getAsJsonObject();
                String name = obj.get(ApiConstants.NAME).getAsString();
                String typeStr = obj.get(ApiConstants.TYPE).getAsString();
                String validationFormatStr = obj.has(ApiConstants.VALIDATION_FORMAT) ? obj.get(ApiConstants.VALIDATION_FORMAT).getAsString() : null;
                boolean required = obj.has(ApiConstants.REQUIRED) && obj.get(ApiConstants.REQUIRED).getAsBoolean();

                Parameter.Type typeEnum = Parameter.Type.valueOf(typeStr);
                Parameter.ValidationFormat validationFormatEnum = (validationFormatStr != null)
                        ? Parameter.ValidationFormat.valueOf(validationFormatStr)
                        : Parameter.ValidationFormat.NONE;

                List<Object> valueOptions = null;
                if (obj.has(ApiConstants.VALUE_OPTIONS) && obj.get(ApiConstants.VALUE_OPTIONS).isJsonArray()) {
                    JsonArray valueOptionsArray = obj.getAsJsonArray(ApiConstants.VALUE_OPTIONS);
                    valueOptions = new ArrayList<>();
                    for (JsonElement el : valueOptionsArray) {
                        switch (typeEnum) {
                            case STRING:
                                valueOptions.add(el.getAsString());
                                break;
                            case NUMBER:
                                if (validationFormatEnum == Parameter.ValidationFormat.DECIMAL) {
                                    valueOptions.add(el.getAsFloat());
                                } else {
                                    valueOptions.add(el.getAsInt());
                                }
                                break;
                            default:
                                throw new JsonParseException("Unsupported type for value options");
                        }
                    }
                }

                return new Parameter(name, typeEnum, validationFormatEnum, valueOptions, required);
            }
        }
    }
}
