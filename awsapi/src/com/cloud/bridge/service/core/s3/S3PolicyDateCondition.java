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
package com.cloud.bridge.service.core.s3;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.DatatypeConverter;

import org.apache.log4j.Logger;

import com.cloud.bridge.service.exception.PermissionDeniedException;
import com.cloud.bridge.util.DateHelper;

public class S3PolicyDateCondition extends S3PolicyCondition {
    protected final static Logger logger = Logger.getLogger(S3PolicyDateCondition.class);

	private Map<ConditionKeys,Calendar[]> keys = new HashMap<ConditionKeys,Calendar[]>();

	public S3PolicyDateCondition() {
	}
	
	/**
	 * Return a set holding all the condition keys kept in this object.
	 * @return Set<String>
	 */
	public Set<ConditionKeys> getAllKeys() {
		return keys.keySet();
	}
	
	/**
	 * After calling getAllKeys(), pass in each key from that result to get
	 * the key's associated list of values.
	 * @param key
	 * @return Calendar[]
	 */
	public Calendar[] getKeyValues(ConditionKeys key) {
		return keys.get(key);
	}
	
	/** 
	 * Convert the key's values into the type depending on the what the condition expects.
	 * @throws ParseException 
	 */
	public void setKey(ConditionKeys key, String[] values) throws ParseException {
		Calendar[] dates = new Calendar[ values.length ];
		
		// -> aws:EpochTime - Number of seconds since epoch is supported here and can also be used in a numeric condition
		if ( ConditionKeys.EpochTime == key ) 
		{
		     for( int i=0; i < values.length; i++ ) {
		    	long epochTime = Long.parseLong( values[i] );
 		        dates[i] = DateHelper.toCalendar( new Date( epochTime ));
		     }
		}
		else
		{    for( int i=0; i < values.length; i++ )
		        dates[i] = DateHelper.toCalendar( DateHelper.parseISO8601DateString( values[i] ));
		}
		
	    keys.put(key, dates);
	}
	
	/**
	 * Evaluation logic is as follows:
	 * 1) An 'AND' operation is used over all defined keys
	 * 2) An 'OR'  operation is used over all key values
	 * 
	 * Each condition has one or more keys, and each keys have one or more values to test.
	 */
	public boolean isTrue(S3PolicyContext context, String SID) 
	{	
		// -> improperly defined condition evaluates to false
		Set<ConditionKeys> keySet = getAllKeys();
		if (null == keySet) return false;
		Iterator<ConditionKeys> itr = keySet.iterator();
		if (!itr.hasNext()) return false;
		
		// -> time to compare with is taken when the condition is evaluated
		Calendar tod = Calendar.getInstance();
		
		while( itr.hasNext()) 
		{
			ConditionKeys keyName = itr.next();
			Calendar[] valueList = getKeyValues( keyName );
			boolean keyResult = false;			
			
			// -> stop when we hit the first true key value (i.e., key values are 'OR'ed together)
            for( int i=0; i < valueList.length && !keyResult; i++ )
            {
            	int difference = tod.compareTo( valueList[i] );
            	
            	switch( condition ) {
		        case DateEquals:   
		        	 if (0 == difference) keyResult = true;
			         break;
		        case DateNotEquals:  
		        	 if (0 != difference) keyResult = true;
			         break;
		        case DateLessThan:     
		        	 if (0 > difference) keyResult = true;
			         break;
		        case DateLessThanEquals:    
		        	 if (0 > difference || 0 == difference) keyResult = true;
			         break;
		        case DateGreaterThan:      
		        	 if (0 < difference) keyResult = true;
			         break;
		        case DateGreaterThanEquals:  
		        	 if (0 < difference || 0 == difference) keyResult = true;
		          	 break;
		        default: 
			         return false;
            	}
    			logger.info( "S3PolicyDateCondition eval - SID: " + SID + ", " + condition + ", key: " + keyName + ", valuePassedIn: " + DatatypeConverter.printDateTime(tod) + ", valueInRule: " + DatatypeConverter.printDateTime(valueList[i]) + ", result: " + keyResult );
            }
            
            // -> if all key values are, false then that key is false and then the entire condition is then false
            if (!keyResult) return false;
		}
		
		return true;
	}

	public void verify() throws PermissionDeniedException
	{
		if (0 == keys.size())
   	       throw new PermissionDeniedException( "S3 Bucket Policy Date Condition needs at least one key-value pairs" );
	}

	public String toString() 
	{	
		StringBuffer value = new StringBuffer();
		Set<ConditionKeys> keySet = getAllKeys();
		if (null == keySet) return "";
		Iterator<ConditionKeys> itr = keySet.iterator();
		
		value.append( condition + " (a date condition): \n" );
		while( itr.hasNext()) {
			ConditionKeys keyName = itr.next();
			value.append( keyName );
			value.append( ": \n" );
			Calendar[] valueList = getKeyValues( keyName );
			for( int i=0; i < valueList.length; i++ ) {
				if (0 < i) value.append( "\n" );
				value.append( DatatypeConverter.printDateTime( valueList[i] ));
			}
			value.append( "\n\n" );
		}
		
		return value.toString();
	}
}
