/*
 * Copyright (C) 2011 Citrix Systems, Inc.  All rights reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.cloud.bridge.util;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.hibernate.Query;

/**
 * @author Kelven Yang
 */
public class QueryHelper {
	public static void bindParameters(Query query, Object[] params) {
		int pos = 0;
		if(params != null && params.length > 0) {
			for(Object param : params) {
				if(param instanceof Byte)
					query.setByte(pos++, ((Byte)param).byteValue());
				else if(param instanceof Short)
					query.setShort(pos++, ((Short)param).shortValue());
				else if(param instanceof Integer) 
					query.setInteger(pos++, ((Integer)param).intValue());
				else if(param instanceof Long)
					query.setLong(pos++, ((Long)param).longValue());
				else if(param instanceof Float)
					query.setFloat(pos++, ((Float)param).floatValue());
				else if(param instanceof Double)
					query.setDouble(pos++, ((Double)param).doubleValue());
				else if(param instanceof Boolean)	
					query.setBoolean(pos++, ((Boolean)param).booleanValue());
				else if(param instanceof Character)
					query.setCharacter(pos++, ((Character)param).charValue());
				else if(param instanceof Date)  
					query.setDate(pos++, (Date)param);
				else if(param instanceof Calendar)  
					query.setCalendar(pos++, (Calendar)param);
				else if(param instanceof CalendarDateParam)  
					query.setCalendarDate(pos++, ((CalendarDateParam)param).dateValue());
				else if(param instanceof TimestampParam)  
					query.setTimestamp(pos++, ((TimestampParam)param).timestampValue());
				else if(param instanceof TimeParam)  
					query.setTime(pos++, ((TimeParam)param).timeValue());
				else if(param instanceof String)  
					query.setString(pos++, (String)param);
				else if(param instanceof TextParam)  
					query.setText(pos++, ((TextParam)param).textValue());
				else if(param instanceof byte[])  
					query.setBinary(pos++, (byte[])param);
				else if(param instanceof BigDecimal)
					query.setBigDecimal(pos++, (BigDecimal)param);
				else if(param instanceof BigInteger)
					query.setBigInteger(pos++, (BigInteger)param);
				else if(param instanceof Locale)
					query.setLocale(pos++, (Locale)param);
				else if(param instanceof EntityParam) 
					query.setEntity(pos++, ((EntityParam)param).entityValue());
				else if(param instanceof Serializable)
					query.setSerializable(pos++, (Serializable)param);
				else 
					query.setEntity(pos++, param);
			}
		}
	}
	
	public static <T> List<T> executeQuery(Query query) {
		return (List<T>)query.list();
	}
}
