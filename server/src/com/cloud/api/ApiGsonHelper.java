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

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.google.gson.GsonBuilder;

import org.apache.cloudstack.api.ResponseObject;
import org.apache.cloudstack.utils.GsonUtils;

import com.cloud.serializer.Param;
import com.cloud.utils.DateUtil;
import com.cloud.utils.Pair;

public class ApiGsonHelper {
    private static final Logger s_logger = Logger.getLogger(ApiGsonHelper.class);
    private static final GsonBuilder s_gBuilder;
    static {
        s_gBuilder = new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
        s_gBuilder.setVersion(1.3);
        s_gBuilder.registerTypeAdapter(ResponseObject.class, new ResponseObjectTypeAdapter());
        s_gBuilder.registerTypeAdapter(Map.class, new GsonUtils.StringMapTypeAdapter());
    }

    public static GsonBuilder getBuilder() {
        return s_gBuilder;
    }

    public static List<Pair<String, Object>> toPairList(Object o, String name) {
        List<Pair<String, Object>> l = new ArrayList<Pair<String, Object>>();
        return appendPairList(l, o, name);
    }

    public static List<Pair<String, Object>> appendPairList(List<Pair<String, Object>> l, Object o, String name) {
        if (o != null) {
            Class<?> clz = o.getClass();

            if (clz.isPrimitive() || clz.getSuperclass() == Number.class || clz == String.class || clz == Date.class) {
                l.add(new Pair<String, Object>(name, o.toString()));
                return l;
            }

            for (Field f : clz.getDeclaredFields()) {
                if ((f.getModifiers() & Modifier.STATIC) != 0) {
                    continue;
                }

                Param param = f.getAnnotation(Param.class);
                if (param == null) {
                    continue;
                }

                String propName = f.getName();
                if (!param.propName().isEmpty()) {
                    propName = param.propName();
                }

                String paramName = param.name();
                if (paramName.isEmpty()) {
                    paramName = propName;
                }

                Method method = getGetMethod(o, propName);
                if (method != null) {
                    try {
                        Object fieldValue = method.invoke(o);
                        if (fieldValue != null) {
                            if (f.getType() == Date.class) {
                                l.add(new Pair<String, Object>(paramName, DateUtil.getOutputString((Date)fieldValue)));
                            } else {
                                l.add(new Pair<String, Object>(paramName, fieldValue.toString()));
                            }
                        }
                        //else
                        //  l.add(new Pair<String, Object>(paramName, ""));
                    } catch (IllegalArgumentException e) {
                        s_logger.error("Illegal argument exception when calling POJO " + o.getClass().getName() + " get method for property: " + propName);

                    } catch (IllegalAccessException e) {
                        s_logger.error("Illegal access exception when calling POJO " + o.getClass().getName() + " get method for property: " + propName);
                    } catch (InvocationTargetException e) {
                        s_logger.error("Invocation target exception when calling POJO " + o.getClass().getName() + " get method for property: " + propName);
                    }
                }
            }
        }
        return l;
    }

    private static Method getGetMethod(Object o, String propName) {
        Method method = null;
        String methodName = getGetMethodName("get", propName);
        try {
            method = o.getClass().getMethod(methodName);
        } catch (SecurityException e1) {
            s_logger.error("Security exception in getting POJO " + o.getClass().getName() + " get method for property: " + propName);
        } catch (NoSuchMethodException e1) {
            if (s_logger.isTraceEnabled()) {
                s_logger.trace("POJO " + o.getClass().getName() + " does not have " + methodName + "() method for property: " + propName
                        + ", will check is-prefixed method to see if it is boolean property");
            }
        }

        if (method != null) {
            return method;
        }

        methodName = getGetMethodName("is", propName);
        try {
            method = o.getClass().getMethod(methodName);
        } catch (SecurityException e1) {
            s_logger.error("Security exception in getting POJO " + o.getClass().getName() + " get method for property: " + propName);
        } catch (NoSuchMethodException e1) {
            s_logger.warn("POJO " + o.getClass().getName() + " does not have " + methodName + "() method for property: " + propName);
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
}
