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

import java.util.HashMap;
import java.util.Map;

public class S3ConditionFactory {

	public enum PolicyConditions {
		UnknownCondition,
		StringEquals, StringNotEquals, StringEqualsIgnoreCase, StringNotEqualsIgnoreCase,
		StringLike,	StringNotLike,
		NumericEquals, NumericNotEquals, NumericLessThan, NumericLessThanEquals,
		NumericGreaterThan,	NumericGreaterThanEquals, 
		DateEquals, DateNotEquals, DateLessThan, DateLessThanEquals,
		DateGreaterThan, DateGreaterThanEquals, 
		Bool,
		IpAddress, NotIpAddres,
		ArnEquals, ArnNotEquals, ArnLike, ArnNotLike 
	}

	// -> map a string name into a policy condition constant
	private Map<String,PolicyConditions> conditionNames = new HashMap<String,PolicyConditions>();
	
	public S3ConditionFactory() {
		conditionNames.put("StringEquals",              PolicyConditions.StringEquals);
		conditionNames.put("streq",                     PolicyConditions.StringEquals);
		conditionNames.put("StringNotEquals",           PolicyConditions.StringNotEquals);
		conditionNames.put("strneq",                    PolicyConditions.StringNotEquals);
		conditionNames.put("StringEqualsIgnoreCase",    PolicyConditions.StringEqualsIgnoreCase);
		conditionNames.put("streqi",                    PolicyConditions.StringEqualsIgnoreCase);		
		conditionNames.put("StringNotEqualsIgnoreCase", PolicyConditions.StringNotEqualsIgnoreCase);
		conditionNames.put("strneqi",                   PolicyConditions.StringNotEqualsIgnoreCase);	
		conditionNames.put("StringLike",                PolicyConditions.StringLike);
		conditionNames.put("strl",                      PolicyConditions.StringLike);		
		conditionNames.put("StringNotLike",             PolicyConditions.StringNotLike);
		conditionNames.put("strnl",                     PolicyConditions.StringNotLike);

		conditionNames.put("NumericEquals",             PolicyConditions.NumericEquals);
		conditionNames.put("numeq",                     PolicyConditions.NumericEquals);
		conditionNames.put("NumericNotEquals",          PolicyConditions.NumericNotEquals);
		conditionNames.put("numneq",                    PolicyConditions.NumericNotEquals);
		conditionNames.put("NumericLessThan",           PolicyConditions.NumericLessThan);
		conditionNames.put("numlt",                     PolicyConditions.NumericLessThan);
		conditionNames.put("NumericLessThanEquals",     PolicyConditions.NumericLessThanEquals);
		conditionNames.put("numlteq",                   PolicyConditions.NumericLessThanEquals);
		conditionNames.put("NumericGreaterThan",        PolicyConditions.NumericGreaterThan);
		conditionNames.put("numgt",                     PolicyConditions.NumericGreaterThan);
		conditionNames.put("NumericGreaterThanEquals",  PolicyConditions.NumericGreaterThanEquals);
		conditionNames.put("numgteq",                   PolicyConditions.NumericGreaterThanEquals);
		
		conditionNames.put("DateEquals",                PolicyConditions.DateEquals);
		conditionNames.put("dateeq",                    PolicyConditions.DateEquals);
		conditionNames.put("DateNotEquals",             PolicyConditions.DateNotEquals);
		conditionNames.put("dateneq",                   PolicyConditions.DateNotEquals);
		conditionNames.put("DateLessThan",              PolicyConditions.DateLessThan);
		conditionNames.put("datelt",                    PolicyConditions.DateLessThan);
		conditionNames.put("DateLessThanEquals",        PolicyConditions.DateLessThanEquals);
		conditionNames.put("datelteq",                  PolicyConditions.DateLessThanEquals);
		conditionNames.put("DateGreaterThan",           PolicyConditions.DateGreaterThan);
		conditionNames.put("dategt",                    PolicyConditions.DateGreaterThan);
		conditionNames.put("DateGreaterThanEquals",     PolicyConditions.DateGreaterThanEquals);
		conditionNames.put("dategteq",                  PolicyConditions.DateGreaterThanEquals);

		conditionNames.put("Bool",                      PolicyConditions.Bool);
		conditionNames.put("IpAddress",                 PolicyConditions.IpAddress);
		conditionNames.put("NotIpAddres",               PolicyConditions.NotIpAddres);

		conditionNames.put("ArnEquals",                 PolicyConditions.ArnEquals);
		conditionNames.put("arneq",                     PolicyConditions.ArnEquals);
		conditionNames.put("ArnNotEquals",              PolicyConditions.ArnNotEquals);
		conditionNames.put("arnneq",                    PolicyConditions.ArnNotEquals);
		conditionNames.put("ArnLike",                   PolicyConditions.ArnLike);
		conditionNames.put("arnl",                      PolicyConditions.ArnLike);
		conditionNames.put("ArnNotLike",                PolicyConditions.ArnNotLike);
		conditionNames.put("arnnl",                     PolicyConditions.ArnNotLike);
	}
	
	public S3PolicyCondition createCondition(String param) 
	{	
		PolicyConditions type = toPolicyConditions( param );
		S3PolicyCondition result = null;
		
		switch( type ) {
		case DateEquals:      		    case DateNotEquals:
		case DateLessThan:   		    case DateLessThanEquals:
		case DateGreaterThan:		    case DateGreaterThanEquals: 
			 result = new S3PolicyDateCondition();
			 result.setCondition( type );
             break;
			 
		case StringEquals:   		    case StringNotEquals:
		case StringEqualsIgnoreCase: 	case StringNotEqualsIgnoreCase:
		case StringLike:         		case StringNotLike:
			 result = new S3PolicyStringCondition();
			 result.setCondition( type );
			 break;

		case NumericEquals:             case NumericNotEquals:  
		case NumericLessThan:           case NumericLessThanEquals:
		case NumericGreaterThan:	    case NumericGreaterThanEquals:
			 result = new S3PolicyNumericCondition();
			 result.setCondition( type );
			 break;
			 
		case Bool:
			 result = new S3PolicyBoolCondition();
			 result.setCondition( type );
			 break;
			 
		case IpAddress: 
		case NotIpAddres:
			 result = new S3PolicyIPAddressCondition();
			 result.setCondition( type );
			 break;
			 
		case ArnEquals:  case ArnNotEquals:   case ArnLike:  case ArnNotLike:
			 result = new S3PolicyArnCondition();
			 result.setCondition( type );
			 break;

		case UnknownCondition:
	    default:
	    	 return null;
		}
		return result;
	}
		
	private PolicyConditions toPolicyConditions(String operation) {
		
		Object value = conditionNames.get( operation );

		if ( null == value ) 
			 return PolicyConditions.UnknownCondition;
		else return (PolicyConditions)value;
	}
}
