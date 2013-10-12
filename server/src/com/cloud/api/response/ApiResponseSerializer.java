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
package com.cloud.api.response;

import com.cloud.api.ApiDBUtils;
import com.cloud.api.ApiResponseGsonHelper;
import com.cloud.api.ApiServer;
import com.cloud.utils.encoding.URLEncoder;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.exception.ExceptionProxyObject;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.ResponseObject;
import org.apache.cloudstack.api.response.*;
import org.apache.log4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ApiResponseSerializer {
    private static final Logger s_logger = Logger.getLogger(ApiResponseSerializer.class.getName());

    public static String toSerializedString(ResponseObject result, String responseType) {
        s_logger.trace("===Serializing Response===");
        if (BaseCmd.RESPONSE_TYPE_JSON.equalsIgnoreCase(responseType)) {
            return toJSONSerializedString(result);
        } else {
            return toXMLSerializedString(result);
        }
    }

    private static final Pattern s_unicodeEscapePattern = Pattern.compile("\\\\u([0-9A-Fa-f]{4})");

    public static String unescape(String escaped) {
        String str = escaped;
        Matcher matcher = s_unicodeEscapePattern.matcher(str);
        while (matcher.find()) {
            str = str.replaceAll("\\" + matcher.group(0), Character.toString((char) Integer.parseInt(matcher.group(1), 16)));
        }
        return str;
    }

    public static String toJSONSerializedString(ResponseObject result) {
        if (result != null) {
            Gson gson = ApiResponseGsonHelper.getBuilder().excludeFieldsWithModifiers(Modifier.TRANSIENT).create();

            StringBuilder sb = new StringBuilder();

            sb.append("{ \"").append(result.getResponseName()).append("\" : ");
            if (result instanceof ListResponse) {
                List<? extends ResponseObject> responses = ((ListResponse) result).getResponses();
                Integer count = ((ListResponse) result).getCount();
                boolean nonZeroCount = (count != null && count.longValue() != 0);
                if (nonZeroCount) {
                    sb.append("{ \"").append(ApiConstants.COUNT).append("\":").append(count);
                }

                if ((responses != null) && !responses.isEmpty()) {
                    String jsonStr = gson.toJson(responses.get(0));
                    jsonStr = unescape(jsonStr);

                    if (nonZeroCount) {
                        sb.append(" ,\"").append(responses.get(0).getObjectName()).append("\" : [  ").append(jsonStr);
                    }

                    for (int i = 1; i < ((ListResponse) result).getResponses().size(); i++) {
                        jsonStr = gson.toJson(responses.get(i));
                        jsonStr = unescape(jsonStr);
                        sb.append(", ").append(jsonStr);
                    }
                    sb.append(" ] }");
                } else  {
                    if (!nonZeroCount){
                        sb.append("{");
                    }

                    sb.append(" }");
                }
            } else if (result instanceof SuccessResponse) {
                sb.append("{ \"success\" : \"").append(((SuccessResponse) result).getSuccess()).append("\"} ");
            } else if (result instanceof ExceptionResponse) {
            	String jsonErrorText = gson.toJson((ExceptionResponse) result);
            	jsonErrorText = unescape(jsonErrorText);
            	sb.append(jsonErrorText);
            } else {
                String jsonStr = gson.toJson(result);
                if ((jsonStr != null) && !"".equals(jsonStr)) {
                    jsonStr = unescape(jsonStr);
                    if (result instanceof AsyncJobResponse || result instanceof CreateCmdResponse) {
                        sb.append(jsonStr);
                    } else {
                        sb.append(" { \"").append(result.getObjectName()).append("\" : ").append(jsonStr).append(" } ");
                    }
                } else {
                    sb.append("{ }");
                }
            }
            sb.append(" }");
            return sb.toString();
        }
        return null;
    }

    private static String toXMLSerializedString(ResponseObject result) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        sb.append("<").append(result.getResponseName()).append(" cloud-stack-version=\"").append(ApiDBUtils.getVersion()).append("\">");

        if (result instanceof ListResponse) {
            Integer count = ((ListResponse) result).getCount();

            if (count != null && count != 0) {
                sb.append("<").append(ApiConstants.COUNT).append(">").append(((ListResponse) result).getCount()).
                append("</").append(ApiConstants.COUNT).append(">");
            }
            List<? extends ResponseObject> responses = ((ListResponse) result).getResponses();
            if ((responses != null) && !responses.isEmpty()) {
                for (ResponseObject obj : responses) {
                    serializeResponseObjXML(sb, obj);
                }
            }
        } else {
            if (result instanceof CreateCmdResponse || result instanceof AsyncJobResponse) {
                serializeResponseObjFieldsXML(sb, result);
            } else {
                serializeResponseObjXML(sb, result);
            }
        }

        sb.append("</").append(result.getResponseName()).append(">");
        return sb.toString();
    }

    private static void serializeResponseObjXML(StringBuilder sb, ResponseObject obj) {
        if (!(obj instanceof SuccessResponse) && !(obj instanceof ExceptionResponse)) {
            sb.append("<").append(obj.getObjectName()).append(">");
        }
        serializeResponseObjFieldsXML(sb, obj);
        if (!(obj instanceof SuccessResponse) && !(obj instanceof ExceptionResponse)) {
            sb.append("</").append(obj.getObjectName()).append(">");
        }
    }

    public static Field[] getFlattenFields(Class<?> clz) {
        List<Field> fields = new ArrayList<Field>();
        fields.addAll(Arrays.asList(clz.getDeclaredFields()));
        if (clz.getSuperclass() != null) {
            fields.addAll(Arrays.asList(getFlattenFields(clz.getSuperclass())));
        }
        return fields.toArray(new Field[] {});
    }

    private static void serializeResponseObjFieldsXML(StringBuilder sb, ResponseObject obj) {
        boolean isAsync = false;
        if (obj instanceof AsyncJobResponse)
            isAsync = true;

        //Field[] fields = obj.getClass().getDeclaredFields();
        Field[] fields = getFlattenFields(obj.getClass());
        for (Field field : fields) {
            if ((field.getModifiers() & Modifier.TRANSIENT) != 0) {
                continue; // skip transient fields
            }

            SerializedName serializedName = field.getAnnotation(SerializedName.class);
            if (serializedName == null) {
                continue; // skip fields w/o serialized name
            }

            field.setAccessible(true);
            Object fieldValue = null;
            try {
                fieldValue = field.get(obj);
            } catch (IllegalArgumentException e) {
                throw new CloudRuntimeException("how illegal is it?", e);
            } catch (IllegalAccessException e) {
                throw new CloudRuntimeException("come on...we set accessible already", e);
            }
            if (fieldValue != null) {
                if (fieldValue instanceof ResponseObject) {
                    ResponseObject subObj = (ResponseObject) fieldValue;
                    if (isAsync) {
                        sb.append("<jobresult>");
                    }
                    serializeResponseObjXML(sb, subObj);
                    if (isAsync) {
                        sb.append("</jobresult>");
                    }
                } else if (fieldValue instanceof Collection<?>) {
                    Collection<?> subResponseList = (Collection<?>) fieldValue;
                    boolean usedUuidList = false;
                    for (Object value : subResponseList) {
                        if (value instanceof ResponseObject) {
                            ResponseObject subObj = (ResponseObject) value;
                            if (serializedName != null) {
                                subObj.setObjectName(serializedName.value());
                            }
                            serializeResponseObjXML(sb, subObj);
                        } else if (value instanceof ExceptionProxyObject) {
                            // Only exception reponses carry a list of
                            // ExceptionProxyObject objects.
                            ExceptionProxyObject idProxy = (ExceptionProxyObject) value;
                            // If this is the first IdentityProxy field
                            // encountered, put in a uuidList tag.
                            if (!usedUuidList) {
                                sb.append("<" + serializedName.value() + ">");
                                usedUuidList = true;
                            }
                            sb.append("<" + "uuid" + ">" + idProxy.getUuid() + "</" + "uuid" + ">");
                            // Append the new descriptive property also.
                            String idFieldName = idProxy.getDescription();
                            if (idFieldName != null) {
                                sb.append("<" + "uuidProperty" + ">" + idFieldName + "</" + "uuidProperty" + ">");
                            }
                        }
                    }
                    if (usedUuidList) {
                        // close the uuidList.
                        sb.append("</").append(serializedName.value()).append(">");
                    }
                } else if (fieldValue instanceof Date) {
                    sb.append("<").append(serializedName.value()).append(">").append(BaseCmd.getDateString((Date) fieldValue)).
                    append("</").append(serializedName.value()).append(">");
                } else {
                    String resultString = escapeSpecialXmlChars(fieldValue.toString());
                    if (!(obj instanceof ExceptionResponse)) {
                        resultString = encodeParam(resultString);
                    }

                    sb.append("<").append(serializedName.value()).append(">").append(resultString).append("</").append(serializedName.value()).append(">");
                }
            }
        }
    }

    private static String escapeSpecialXmlChars(String originalString) {
        char[] origChars = originalString.toCharArray();
        StringBuilder resultString = new StringBuilder();

        for (char singleChar : origChars) {
            if (singleChar == '"') {
                resultString.append("&quot;");
            } else if (singleChar == '\'') {
                resultString.append("&apos;");
            } else if (singleChar == '<') {
                resultString.append("&lt;");
            } else if (singleChar == '>') {
                resultString.append("&gt;");
            } else if (singleChar == '&') {
                resultString.append("&amp;");
            } else {
                resultString.append(singleChar);
            }
        }

        return resultString.toString();
    }

    private static String encodeParam(String value) {
        if (!ApiServer.encodeApiResponse) {
            return value;
        }
        try {
            return new URLEncoder().encode(value).replaceAll("\\+", "%20");
        } catch (Exception e) {
            s_logger.warn("Unable to encode: " + value, e);
        }
        return value;
    }

}
