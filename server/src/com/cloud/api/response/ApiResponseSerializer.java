/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */

package com.cloud.api.response;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import com.cloud.api.ApiConstants;
import com.cloud.api.ApiDBUtils;
import com.cloud.api.ApiResponseGsonHelper;
import com.cloud.api.ApiServer;
import com.cloud.api.BaseCmd;
import com.cloud.utils.IdentityProxy;
import com.cloud.api.ResponseObject;
import com.cloud.utils.encoding.URLEncoder;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.uuididentity.dao.IdentityDao;
import com.cloud.uuididentity.dao.IdentityDaoImpl;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

public class ApiResponseSerializer {
    private static final Logger s_logger = Logger.getLogger(ApiResponseSerializer.class.getName());

    public static String toSerializedString(ResponseObject result, String responseType) {
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

            sb.append("{ \"" + result.getResponseName() + "\" : ");
            if (result instanceof ListResponse) {
                List<? extends ResponseObject> responses = ((ListResponse) result).getResponses();
                if ((responses != null) && !responses.isEmpty()) {

                    Integer count = ((ListResponse) result).getCount();
                    String jsonStr = gson.toJson(responses.get(0));                    
                    jsonStr = unescape(jsonStr);

                    if (count != null && count != 0) {
                        sb.append("{ \"" + ApiConstants.COUNT + "\":" + ((ListResponse) result).getCount() + " ,\"" + responses.get(0).getObjectName() + "\" : [  " + jsonStr);
                    }
                    for (int i = 1; i < count; i++) {
                        jsonStr = gson.toJson(responses.get(i));
                        jsonStr = unescape(jsonStr);
                        sb.append(", " + jsonStr);
                    }
                    sb.append(" ] }");
                } else {
                    sb.append("{ }");
                }
            } else if (result instanceof SuccessResponse) {
                sb.append("{ \"success\" : \"" + ((SuccessResponse) result).getSuccess() + "\"} ");
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
                        sb.append(" { \"" + result.getObjectName() + "\" : " + jsonStr + " } ");
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
        sb.append("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>");
        sb.append("<" + result.getResponseName() + " cloud-stack-version=\"" + ApiDBUtils.getVersion() + "\">");

        if (result instanceof ListResponse) {
            Integer count = ((ListResponse) result).getCount();

            if (count != null && count != 0) {
                sb.append("<" + ApiConstants.COUNT + ">" + ((ListResponse) result).getCount() + "</" + ApiConstants.COUNT + ">");
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

        sb.append("</" + result.getResponseName() + ">");
        return sb.toString();
    }

    private static void serializeResponseObjXML(StringBuilder sb, ResponseObject obj) {
        if (!(obj instanceof SuccessResponse) && !(obj instanceof ExceptionResponse)) {
            sb.append("<" + obj.getObjectName() + ">");
        }
        serializeResponseObjFieldsXML(sb, obj);
        if (!(obj instanceof SuccessResponse) && !(obj instanceof ExceptionResponse)) {
            sb.append("</" + obj.getObjectName() + ">");
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
                } else if (fieldValue instanceof List<?>) {
                    List<?> subResponseList = (List<Object>) fieldValue;                    
                    boolean usedUuidList = false;
                    for (Object value : subResponseList) {
                        if (value instanceof ResponseObject) {
                            ResponseObject subObj = (ResponseObject) value;
                            if (serializedName != null) {
                                subObj.setObjectName(serializedName.value());
                            }
                            serializeResponseObjXML(sb, subObj);
                        } else if (value instanceof IdentityProxy) {
                        	// Only exception reponses carry a list of IdentityProxy objects.
                        	IdentityProxy idProxy = (IdentityProxy)value;                        	
                        	String id = (idProxy.getValue() != null ? String.valueOf(idProxy.getValue()) : "");
                        	if(!id.isEmpty()) {
                        		IdentityDao identityDao = new IdentityDaoImpl();
                        		id = identityDao.getIdentityUuid(idProxy.getTableName(), id);
                        	}                        	
                        	if(id != null && !id.isEmpty()) {
                        		// If this is the first IdentityProxy field encountered, put in a uuidList tag.
                        		if (!usedUuidList) {
                        			sb.append("<" + serializedName.value() + ">");
                        			usedUuidList = true;
                        		}
                        		sb.append("<" + "uuid" + ">" + id + "</" + "uuid" + ">");                        		
                        	}
                        	// Append the new idFieldName property also.
                        	String idFieldName = idProxy.getidFieldName();
                        	if (idFieldName != null) {
                        		sb.append("<" + "uuidProperty" + ">" + idFieldName + "</" + "uuidProperty" + ">");                        		
                        	}
                        }                        
                    }
                    if (usedUuidList) {
                    	// close the uuidList.
                    	sb.append("</" + serializedName.value() + ">");
                    }
                } else if (fieldValue instanceof Date) {
                    sb.append("<" + serializedName.value() + ">" + BaseCmd.getDateString((Date) fieldValue) + "</" + serializedName.value() + ">");                
                } else if (fieldValue instanceof IdentityProxy) {                	
                	IdentityProxy idProxy = (IdentityProxy)fieldValue;
                	String id = (idProxy.getValue() != null ? String.valueOf(idProxy.getValue()) : "");
                	if(!id.isEmpty()) {
                		IdentityDao identityDao = new IdentityDaoImpl();
                		if(idProxy.getTableName() != null) {
                		    id = identityDao.getIdentityUuid(idProxy.getTableName(), id);
                		} else {
                		    s_logger.warn("IdentityProxy sanity check issue, invalid IdentityProxy table name found in class: " + obj.getClass().getName());
                		}
                	}
                	if(id != null && !id.isEmpty())
                		sb.append("<" + serializedName.value() + ">" + id + "</" + serializedName.value() + ">");
                } else {
                    String resultString = escapeSpecialXmlChars(fieldValue.toString());
                    if (!(obj instanceof ExceptionResponse)) {
                        resultString = encodeParam(resultString);
                    }
                    
                    sb.append("<" + serializedName.value() + ">" + resultString + "</" + serializedName.value() + ">");
                }
            }
        }
    }

    private static Method getGetMethod(Object o, String propName) {
        Method method = null;
        String methodName = getGetMethodName("get", propName);
        try {
            method = o.getClass().getMethod(methodName);
        } catch (SecurityException e1) {
            s_logger.error("Security exception in getting ResponseObject " + o.getClass().getName() + " get method for property: " + propName);
        } catch (NoSuchMethodException e1) {
            if (s_logger.isTraceEnabled()) {
                s_logger.trace("ResponseObject " + o.getClass().getName() + " does not have " + methodName + "() method for property: " + propName
                        + ", will check is-prefixed method to see if it is boolean property");
            }
        }

        if (method != null)
            return method;

        methodName = getGetMethodName("is", propName);
        try {
            method = o.getClass().getMethod(methodName);
        } catch (SecurityException e1) {
            s_logger.error("Security exception in getting ResponseObject " + o.getClass().getName() + " get method for property: " + propName);
        } catch (NoSuchMethodException e1) {
            s_logger.warn("ResponseObject " + o.getClass().getName() + " does not have " + methodName + "() method for property: " + propName);
        }
        return method;
    }

    private static String getGetMethodName(String prefix, String fieldName) {
        StringBuffer sb = new StringBuffer(prefix);

        if (fieldName.length() >= prefix.length() && fieldName.substring(0, prefix.length()).equals(prefix)) {
            return fieldName;
        } else {
            sb.append(fieldName.substring(0, 1).toUpperCase());
            sb.append(fieldName.substring(1));
        }

        return sb.toString();
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
