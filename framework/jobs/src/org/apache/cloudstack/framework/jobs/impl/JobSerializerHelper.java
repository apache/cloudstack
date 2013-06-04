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
package org.apache.cloudstack.framework.jobs.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import com.cloud.utils.exception.CloudRuntimeException;

/**
 * Note: toPairList and appendPairList only support simple POJO objects currently
 */
public class JobSerializerHelper {
    private static final Logger s_logger = Logger.getLogger(JobSerializerHelper.class);
    public static String token = "/";

    private static Gson s_gson;
    static {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.setVersion(1.5);
        s_logger.debug("Job GSON Builder initialized.");
        s_gson = gsonBuilder.create();
    }

    public static String toSerializedString(Object result) {
        if(result != null) {
            Class<?> clz = result.getClass();
            return clz.getName() + token + s_gson.toJson(result);
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

                Object obj = s_gson.fromJson(content, clz);
                return obj;
            }
            return null;
        } catch(RuntimeException e) {
            throw new CloudRuntimeException("Unable to deserialize: " + result, e);
        }
    }
    
    public static String toObjectSerializedString(Serializable object) {
    	assert(object != null);
    	
    	ByteArrayOutputStream bs = new ByteArrayOutputStream();
    	try {
    		ObjectOutputStream os = new ObjectOutputStream(bs);
    		os.writeObject(object);
    		os.close();
    		bs.close();
    		
    		return Base64.encodeBase64URLSafeString(bs.toByteArray());
    	} catch(IOException e) {
            throw new CloudRuntimeException("Unable to serialize: " + object, e);
    	}
    }
    
    public static Object fromObjectSerializedString(String base64EncodedString) {
    	if(base64EncodedString == null)
    		return null;
    	
    	byte[] content = Base64.decodeBase64(base64EncodedString);
    	ByteArrayInputStream bs = new ByteArrayInputStream(content);
    	try {
    		ObjectInputStream is = new ObjectInputStream(bs);
    		Object obj = is.readObject();
    		is.close();
    		bs.close();
    		return obj;
    	} catch(IOException e) {
            throw new CloudRuntimeException("Unable to serialize: " + base64EncodedString, e);
        } catch (ClassNotFoundException e) {
            throw new CloudRuntimeException("Unable to serialize: " + base64EncodedString, e);
    	}
    }
}
