package com.cloud.api;

import com.google.gson.GsonBuilder;

public class ApiGsonHelper {
	private static final GsonBuilder s_gBuilder;
	static {
        s_gBuilder = new GsonBuilder();
        s_gBuilder.setVersion(1.3);
        s_gBuilder.registerTypeAdapter(ResponseObject.class, new ResponseObjectTypeAdapter());
	}
	
	public static GsonBuilder getBuilder() {
		return s_gBuilder;
	}
}

