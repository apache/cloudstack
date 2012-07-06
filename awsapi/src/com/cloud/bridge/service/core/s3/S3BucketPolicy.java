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

import com.cloud.bridge.service.core.s3.S3PolicyAction.PolicyActions;

public class S3BucketPolicy {
	
	/**
	 * 'NORESULT' is returned when no applicable statement can be found to evaluate
	 * for the S3 access request.  If no evaluated statement results to true then the
	 * default deny result is returned (allow ACL definitions to override it).  
	 */
	public enum PolicyAccess { ALLOW, DEFAULT_DENY, DENY }

	private List<S3PolicyStatement> statementList = new ArrayList<S3PolicyStatement>();
	private String bucketName = null;
	private String id = null;

	public S3BucketPolicy() {
		
	}
	
	public S3PolicyStatement[] getStatements() {
		return statementList.toArray(new S3PolicyStatement[0]);
	}
	
	public void addStatement(S3PolicyStatement param) {
		statementList.add( param );
	}
	
	public String getBucketName() {
		return bucketName;
	}
	
	public void setBucketName(String param) {
		bucketName = param;
	}

	public String getId() {
		return id;
	}
	
	public void setId(String param) {
		id = param;
	}
	
	/**
	 * This function evaluates all applicable policy statements.  Following the "evaluation logic"
	 * as defined by Amazon the type of access derived from the policy is returned.
	 * 
	 * @param context - parameters from either the REST or SOAP request 
	 * @param objectToAccess - key to the S3 object in the bucket associated by this policy, should be
	 *                         null if access is just to the bucket.
	 * @param userAccount - the user performing the access request
	 * @return PolicyAccess type
	 * @throws Exception 
	 */
	public PolicyAccess eval(S3PolicyContext context, String userAccount) throws Exception 
	{
		PolicyAccess result = PolicyAccess.DEFAULT_DENY;
		
		Iterator<S3PolicyStatement> itr = statementList.iterator();
		while( itr.hasNext()) 
		{
			S3PolicyStatement oneStatement = itr.next();
			if (statementIsRelevant( oneStatement, context.getKeyName(), userAccount, context.getRequestedAction()))
			{
				// -> a missing condition block means the statement is true 
				S3PolicyConditionBlock block = oneStatement.getConditionBlock();				
			    if (null == block || block.isTrue( context, oneStatement.getSid())) 
				{
					result = oneStatement.getEffect();
					if (PolicyAccess.DENY == result) return result;
				}
			}
		}
		return result;
	}
	
	/**
	 * To support debugging we print out what the parsing process has resulted in.
	 */
	public String toString() {
		
		StringBuffer value = new StringBuffer();
		Iterator<S3PolicyStatement> itr = statementList.iterator();
		
		value.append( "Bucket Policy for: " + bucketName + " \n" );
		if (null != id) value.append( "Id: " + id + "\n" );
		
		while( itr.hasNext()) {
			S3PolicyStatement oneStatement = itr.next();
			value.append( oneStatement.toString());
			value.append( "\n" );
		}
		
		return value.toString();
	}
	
	/**
	 * Does the Policy Statement have anything to do with the requested access by the user?
	 * 
	 * @return true - statement is relevant, false it is not
	 */
	private boolean statementIsRelevant( S3PolicyStatement oneStatement, String objectToAccess, String userAccount, PolicyActions operationRequested ) 
	{
		String path = null;
		
		// [A] Is the userAccount one of the principals of the policy statement?
		S3PolicyPrincipal principals = oneStatement.getPrincipals();
		if (null == principals || !principals.contains( userAccount )) return false;
		//System.out.println( "Statement: " + oneStatement.getSid() + " principal matches");
		
		
		// [B] Is the operationRequested included in the policy statement?
		//  -> if the value in "NotAction:" matches that requested then the statement does not apply
		//  (i.e., "refers to all actions other" than defined).
		PolicyActions notActions = oneStatement.getNotAction();
	    //System.out.println( "Statement: NotAction:" + notActions + " op requested: " + operationRequested );

    	if ( PolicyActions.UnknownAction != notActions ) {
    		 if (notActions == operationRequested) return false;
    	}
    	else {
		     S3PolicyAction actions = oneStatement.getActions();
		     if (null == actions || !actions.contains( operationRequested )) return false;
		     //System.out.println( "Statement: " + oneStatement.getSid() + " action matches");
    	}
		
		// [C] Does the objectToAccess included in the resource of the policy statement?
		//  -> is it just the bucket being accessed?
		if ( null == objectToAccess ) 
			 path = bucketName;
		else path = new String( bucketName + "/" + objectToAccess );	 
		
	    if (!oneStatement.containsResource( path )) return false;
		
	    //System.out.println( "Statement: " + oneStatement.getSid() + " is relevant to access request");
		return true;
	}
}
