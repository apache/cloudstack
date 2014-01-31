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
package com.cloud.api;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.ServerApiException;

public class ParameterHandler {

    private static final Logger s_logger = Logger.getLogger(ParameterHandler.class.getName());

    /**
     * Non-printable ASCII characters - numbers 0 to 31 and 127 decimal
     */
    public static final String CONTROL_CHARACTERS = "[\000-\011\013-\014\016-\037\177]";

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Map<String, Object> unpackParams(final Map params) {
        final Map<String, String> stringMap = new HashMap<String, String>();
        final Set keys = params.keySet();
        final Iterator keysIter = keys.iterator();
        while (keysIter.hasNext()) {
            final String key = (String)keysIter.next();
            final String[] value = (String[])params.get(key);
            // fail if parameter value contains ASCII control (non-printable) characters
            if (value[0] != null) {
                final Pattern pattern = Pattern.compile(CONTROL_CHARACTERS);
                final Matcher matcher = pattern.matcher(value[0]);
                if (matcher.find()) {
                    throw new ServerApiException(ApiErrorCode.PARAM_ERROR, "Received value " + value[0] + " for parameter " + key +
                        " is invalid, contains illegal ASCII non-printable characters");
                }
            }
            stringMap.put(key, value[0]);
        }

        final Map<String, Object> lowercaseParams = new HashMap<String, Object>();
        for (final Object keyObject : stringMap.keySet()) {
            final String key = (String) keyObject;
            final int arrayStartIndex = key.indexOf('[');
            final int arrayStartLastIndex = key.lastIndexOf('[');
            if (arrayStartIndex != arrayStartLastIndex) {
                throw new ServerApiException(ApiErrorCode.MALFORMED_PARAMETER_ERROR, "Unable to decode parameter " + key +
                    "; if specifying an object array, please use parameter[index].field=XXX, e.g. userGroupList[0].group=httpGroup");
            }

            if (arrayStartIndex > 0) {
                final int arrayEndIndex = key.indexOf(']');
                final int arrayEndLastIndex = key.lastIndexOf(']');
                if ((arrayEndIndex < arrayStartIndex) || (arrayEndIndex != arrayEndLastIndex)) {
                    // malformed parameter
                    throw new ServerApiException(ApiErrorCode.MALFORMED_PARAMETER_ERROR, "Unable to decode parameter " + key +
                        "; if specifying an object array, please use parameter[index].field=XXX, e.g. userGroupList[0].group=httpGroup");
                }

                // Now that we have an array object, check for a field name in the case of a complex object
                final int fieldIndex = key.indexOf('.');
                String fieldName = null;
                if (fieldIndex < arrayEndIndex) {
                    throw new ServerApiException(ApiErrorCode.MALFORMED_PARAMETER_ERROR, "Unable to decode parameter " + key +
                        "; if specifying an object array, please use parameter[index].field=XXX, e.g. userGroupList[0].group=httpGroup");
                } else {
                    fieldName = key.substring(fieldIndex + 1);
                }

                // parse the parameter name as the text before the first '[' character
                String paramName = key.substring(0, arrayStartIndex);
                paramName = paramName.toLowerCase();

                Map<Integer, Map> mapArray = null;
                Map<String, Object> mapValue = null;
                final String indexStr = key.substring(arrayStartIndex + 1, arrayEndIndex);
                int index = 0;
                boolean parsedIndex = false;
                try {
                    if (indexStr != null) {
                        index = Integer.parseInt(indexStr);
                        parsedIndex = true;
                    }
                } catch (final NumberFormatException nfe) {
                    s_logger.warn("Invalid parameter " + key + " received, unable to parse object array, returning an error.");
                }

                if (!parsedIndex) {
                    throw new ServerApiException(ApiErrorCode.MALFORMED_PARAMETER_ERROR, "Unable to decode parameter " + key +
                        "; if specifying an object array, please use parameter[index].field=XXX, e.g. userGroupList[0].group=httpGroup");
                }

                final Object value = lowercaseParams.get(paramName);
                if (value == null) {
                    // for now, assume object array with sub fields
                    mapArray = new HashMap<Integer, Map>();
                    mapValue = new HashMap<String, Object>();
                    mapArray.put(Integer.valueOf(index), mapValue);
                } else if (value instanceof Map) {
                    mapArray = (HashMap)value;
                    mapValue = mapArray.get(Integer.valueOf(index));
                    if (mapValue == null) {
                        mapValue = new HashMap<String, Object>();
                        mapArray.put(Integer.valueOf(index), mapValue);
                    }
                }

                // we are ready to store the value for a particular field into the map for this object
                mapValue.put(fieldName, stringMap.get(key));

                lowercaseParams.put(paramName, mapArray);
            } else {
                lowercaseParams.put(key.toLowerCase(), stringMap.get(key));
            }
        }
        return lowercaseParams;
    }

}
