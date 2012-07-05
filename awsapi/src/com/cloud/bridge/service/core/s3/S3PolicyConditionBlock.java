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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.cloud.bridge.service.exception.PermissionDeniedException;

public class S3PolicyConditionBlock {
	
	private List<S3PolicyCondition> conditionList = new ArrayList<S3PolicyCondition>();
	public int nesting;
	
	public S3PolicyConditionBlock() {
	}

	public S3PolicyCondition[] getBlock() {
		return conditionList.toArray(new S3PolicyCondition[0]);
	}
	
	public void addCondition(S3PolicyCondition param) {
		conditionList.add( param );
	}
	
	/**
	 * Condition blocks are evaluated where as an 'AND' of the result of
	 * each separate condition in the block.   Thus, a single false value makes
	 * the entire block evaluate to false.  If no conditions are present and the
	 * condition is relevant to the request, then the default condition is considered
	 * to be true.
	 * @throws Exception 
	 */
	public boolean isTrue(S3PolicyContext context, String SID ) throws Exception 
	{	
		Iterator<S3PolicyCondition> itr = conditionList.iterator();
		while( itr.hasNext()) {
			S3PolicyCondition oneCondition = itr.next();
			if (!oneCondition.isTrue( context, SID )) return false;
		}
		
		// -> if no conditions exist in the block it defaults to true
		return true;
	}
	
	public void verify() throws PermissionDeniedException
	{
		if (0 == conditionList.size())
   	       throw new PermissionDeniedException( "S3 Bucket Policy Condition Block needs at least one condition" );
	}

	public String toString() 
	{	
		StringBuffer value = new StringBuffer();
		Iterator<S3PolicyCondition> itr = conditionList.iterator();
		
		value.append( "Condition: \n" );
		while( itr.hasNext()) {
			S3PolicyCondition oneCondition = itr.next();
			value.append( oneCondition.toString());
		}
		
		return value.toString();
	}
}
