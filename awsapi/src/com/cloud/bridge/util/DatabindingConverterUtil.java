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

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;

import org.apache.axis2.databinding.utils.ConverterUtil;

/**
 * Custom subclass of org.apache.axis2.databinding.utils.ConverterUtil, i.e. the data conversion utility for
 * databindings in the ADB framework of Axis2.  The purpose of a custom class is to provide specific value
 * format conversions for certain datatypes, especially dates, timestamps and calendars for which the default
 * is inappropriate.  For these simple cases, the return value is of type String.
 * Converter methods to go from 1. simple type -> String 2. simple type -> Object 3. String ->
 * simpletype 4. Object list -> array
 */


public class DatabindingConverterUtil extends ConverterUtil {

    // Custom behaviour for java.util.Date
	public static String convertToString(Date dateValue) 
	{
		
		return (new ISO8601SimpleDateTimeFormat()).format(dateValue);
	 }
    
	// Custom behaviour for java.util.Calendar
	public static String convertToString(Calendar calendarValue) 
	{
		
		return (new ISO8601SimpleDateTimeFormat()).format(calendarValue.getTime());
	 }

    // Custom behaviour for java.sql.Timestamp
	public static String convertToString(Timestamp timestampValue) 
	{
		
		return timestampValue.toString();
	 }
	
	// Otherwise String convertToString(Object any) is handled by invoker (which happens to be superclass).
	// No need to reference super explicitly because it is the invoker of static methods 
	// @see org.apache.axis2.databinding.utils.ConverterUtil
	
	
}
