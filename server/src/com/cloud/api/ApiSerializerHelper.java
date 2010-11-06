package com.cloud.api;

import org.apache.log4j.Logger;

import com.cloud.serializer.GsonHelper;
import com.google.gson.Gson;

public class ApiSerializerHelper {
	public static final Logger s_logger = Logger.getLogger(ApiSerializerHelper.class.getName());
    public static String token = "/";
    
    public static String toSerializedStringOld(Object result) {
		if(result != null) {
			Class<?> clz = result.getClass();
	    	Gson gson = ApiGsonHelper.getBuilder().create();

	    	if (result instanceof ResponseObject) {
	            return clz.getName() + token + ((ResponseObject)result).getResponseName() + token + gson.toJson(result); 
	    	} else {
	            return clz.getName() + token + gson.toJson(result); 
	    	}
		} 
		return null;
	}
    
	public static Object fromSerializedString(String result) {
		try {
			if(result != null && !result.isEmpty()) {
			    
			    String[] serializedParts = result.split(token);

			    if (serializedParts.length < 2) {
			        return null;
			    }
                String clzName = serializedParts[0];
                String nameField = null;
                String content = null;
                if (serializedParts.length == 2) {
                    content = serializedParts[1];
                } else {
                    nameField = serializedParts[1];
                    int index = result.indexOf(token + nameField + token);
                    content = result.substring(index + nameField.length() + 2);
                }

				Class<?> clz;
				try {
					clz = Class.forName(clzName);
				} catch (ClassNotFoundException e) {
					return null;
				}
				
		    	Gson gson = ApiGsonHelper.getBuilder().create();
		    	Object obj = gson.fromJson(content, clz);
		    	if (nameField != null) {
		    	    ((ResponseObject)obj).setResponseName(nameField);
		    	}
		    	return obj;
			}
			return null;
		} catch(RuntimeException e) {
			s_logger.error("Caught runtime exception when doing GSON deserialization on: " + result);
			throw e; 
		}
	}
}
