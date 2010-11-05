package com.cloud.api;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;

import org.apache.log4j.Logger;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.annotations.SerializedName;

public class ResponseObjectTypeAdapter implements JsonSerializer<ResponseObject> {
    private static final Logger s_logger = Logger.getLogger(ResponseObjectTypeAdapter.class.getName());
    private static final GsonBuilder s_gBuilder;
    static {
        s_gBuilder = new GsonBuilder().excludeFieldsWithModifiers(Modifier.TRANSIENT);
        s_gBuilder.registerTypeAdapter(ResponseObject.class, new ResponseObjectTypeAdapter());
    }

    @Override
    public JsonElement serialize(ResponseObject responseObj, Type typeOfResponseObj, JsonSerializationContext ctx) {
        JsonObject obj = new JsonObject();

        // Get the declared fields from the response obj, create a new JSON Object, add props to it.
        // Once that object is done, create a new JSON Object with the response name and the JSON Obj as the name/value pair.  Return that as the serialized element.
        Field[] fields = responseObj.getClass().getDeclaredFields();
        for (Field field : fields) {
            if ((field.getModifiers() & Modifier.TRANSIENT) != 0) {
                continue;  // skip transient fields
            }

            SerializedName serializedName = field.getAnnotation(SerializedName.class);
            if (serializedName == null) {
                continue; // skip fields w/o serialized name
            }

            String propName = field.getName();                
            Method method = getGetMethod(responseObj, propName);
            if (method != null) {
                try {
                    Object fieldValue = method.invoke(responseObj);
                    if (fieldValue != null) {
                        if (fieldValue instanceof ResponseObject) {
                            ResponseObject subObj = (ResponseObject)fieldValue;
                            obj.add(serializedName.value(), serialize(subObj, subObj.getClass(), ctx));
                        } else {
                        	if (fieldValue instanceof Number) {
                        		obj.addProperty(serializedName.value(), (Number)fieldValue);
                        	} else if (fieldValue instanceof Character) {
                        		obj.addProperty(serializedName.value(), (Character)fieldValue);
                        	} else if (fieldValue instanceof Boolean) {
                        		obj.addProperty(serializedName.value(), (Boolean)fieldValue);
                        	} else {
                        		obj.addProperty(serializedName.value(), fieldValue.toString());
                        	}
                        }
                    }
                } catch (IllegalArgumentException e) {
                    s_logger.error("Illegal argument exception when calling ResponseObject " + responseObj.getClass().getName() + " get method for property: " + propName);
                } catch (IllegalAccessException e) {
                    s_logger.error("Illegal access exception when calling ResponseObject " + responseObj.getClass().getName() + " get method for property: " + propName);
                } catch (InvocationTargetException e) {
                    s_logger.error("Invocation target exception when calling ResponseObject " + responseObj.getClass().getName() + " get method for property: " + propName);
                }
            }
        }

        JsonObject response = new JsonObject();
        response.add(responseObj.getResponseName(), obj);
        return response;
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
                s_logger.trace("ResponseObject " + o.getClass().getName() + " does not have " + methodName + "() method for property: " + propName + ", will check is-prefixed method to see if it is boolean property");
            }
        }
        
        if( method != null)
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
        
        if(fieldName.length() >= prefix.length() && fieldName.substring(0, prefix.length()).equals(prefix)) {
            return fieldName;
        } else {
            sb.append(fieldName.substring(0, 1).toUpperCase());
            sb.append(fieldName.substring(1));
        }
        
        return sb.toString();
    }
}
