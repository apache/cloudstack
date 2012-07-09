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
package com.cloud.bridge.util;

public class Converter {
	public static boolean toBool(String value, boolean defaultValue) {
		if(value != null && !value.isEmpty()) {
			return Boolean.parseBoolean(value);
		}
		return defaultValue;
	}

	public static short toShort(String value, short defaultValue) {
		if(value != null && !value.isEmpty()) {
			return Short.parseShort(value);
		}
		
		return defaultValue;
	}
	
	public static int toInt(String value, int defaultValue) {
		if(value != null && !value.isEmpty()) {
			return Integer.parseInt(value);
		}
		
		return defaultValue;
	}
	
	public static long toLong(String value, long defaultValue) {
		if(value != null && !value.isEmpty()) {
			return Long.parseLong(value);
		}
		
		return defaultValue;
	}
	
	public static float toFloat(String value, float defaultValue) {
		if(value != null && !value.isEmpty()) {
			return Float.parseFloat(value);
		}
		
		return defaultValue;
	}
	
	public static double toDouble(String value, double defaultValue) {
		if(value != null && !value.isEmpty()) {
			return Double.parseDouble(value);
		}
		
		return defaultValue;
	}
}
