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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import com.cloud.bridge.service.exception.PermissionDeniedException;

public class S3PolicyNumericCondition extends S3PolicyCondition {
    protected final static Logger logger = Logger.getLogger(S3PolicyNumericCondition.class);

	private Map<ConditionKeys,Float[]> keys = new HashMap<ConditionKeys,Float[]>();

	public S3PolicyNumericCondition() {
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
	 * @return String[]
	 */
	public Float[] getKeyValues(ConditionKeys key) {
		return keys.get(key);
	}
	
	/** 
	 * Convert the key's values into the type depending on the what
	 * the condition expects.
	 * @throws ParseException 
	 */
	public void setKey(ConditionKeys key, String[] values) throws ParseException {	
		Float[] numbers = new Float[ values.length ];
		
	    for( int i=0; i < values.length; i++ ) numbers[i] = new Float( values[i]);
	    keys.put(key, numbers);
	}
	
	public boolean isTrue(S3PolicyContext context, String SID) 
	{		
		Float toCompareWith;
		String temp = null;
		
		// -> improperly defined condition evaluates to false
		Set<ConditionKeys> keySet = getAllKeys();
		if (null == keySet) return false;
		Iterator<ConditionKeys> itr = keySet.iterator();
		if (!itr.hasNext()) return false;
		
		while( itr.hasNext()) 
		{
			ConditionKeys keyName = itr.next();
			Float[] valueList = getKeyValues( keyName );
			boolean keyResult = false;
			
			// -> not having the proper parameters to evaluate an expression results in false
        	if (null == (temp = context.getEvalParam(keyName))) return false;
        	try {
        	    toCompareWith = new Float( temp );
        	}
        	catch( NumberFormatException e ) {
        		return false;
        	}
			
			// -> stop when we hit the first true key value (i.e., key values are 'OR'ed together)
            for( int i=0; i < valueList.length && !keyResult; i++ )
            {            		
            	int difference = valueList[i].compareTo( toCompareWith );
            	
            	switch( condition ) {
        		case NumericEquals:   
		        	 if (0 == difference) keyResult = true;
       			     break;
       		    case NumericNotEquals:  
		        	 if (0 != difference) keyResult = true;
       			     break;
       		    case NumericLessThan:   
		        	 if (0 > difference) keyResult = true;
       			     break;
       		    case NumericLessThanEquals:
		        	 if (0 > difference || 0 == difference) keyResult = true;
       			     break;
       		    case NumericGreaterThan:	
		        	 if (0 < difference) keyResult = true;
       			     break;
       		    case NumericGreaterThanEquals:
		        	 if (0 < difference || 0 == difference) keyResult = true;
       			     break;
		        default: 
			         return false;
            	}
    			logger.info( "S3PolicyNumericCondition eval - SID: " + SID + ", " + condition + ", key: " + keyName + ", valuePassedIn: " + toCompareWith + ", valueInRule: " + valueList[i] + ", result: " + keyResult );
            }
            
            // -> if all key values are, false then that key is false and then the entire condition is then false
            if (!keyResult) return false;
		}
		
		return true;

	}
	
	public void verify() throws PermissionDeniedException
	{
		if (0 == keys.size())
   	       throw new PermissionDeniedException( "S3 Bucket Policy Numeric Condition needs at least one key-value pairs" );
	}

	public String toString() 
	{	
		StringBuffer value = new StringBuffer();
		Set<ConditionKeys> keySet = getAllKeys();
		if (null == keySet) return "";
		Iterator<ConditionKeys> itr = keySet.iterator();
		
		value.append( condition + " (a numeric condition):\n" );
		while( itr.hasNext()) {
			ConditionKeys keyName = itr.next();
			value.append( keyName );
			value.append( ": \n" );
			Float[] valueList = getKeyValues( keyName );
			for( int i=0; i < valueList.length; i++ ) {
				if (0 < i) value.append( "\n" );
				value.append( valueList[i].toString());
			}
			value.append( "\n\n" );
		}
		
		return value.toString();
	}
}
