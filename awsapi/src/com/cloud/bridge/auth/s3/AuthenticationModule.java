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
package com.cloud.bridge.auth.s3;

import org.apache.axis2.modules.Module;

import org.apache.log4j.Logger;
import org.apache.axis2.AxisFault;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.description.AxisDescription;
import org.apache.axis2.description.AxisModule;
import org.apache.neethi.Assertion;
import org.apache.neethi.Policy;

public class AuthenticationModule implements Module {
     protected final static Logger logger = Logger.getLogger(AuthenticationModule.class);

	 public void init(ConfigurationContext configContext, AxisModule module) throws AxisFault
	 {
         logger.debug( "init entry S3 AuthenticationModule" );
 	 }
	 
	 public String[] getPolicyNamespaces() 
	 {
     	 return null;	
	 }
	 
	 public void engageNotify(AxisDescription axisDescription) throws AxisFault
	 {
		 //logger.info( "engageNotify entry S3AuthenticationModule" );
	 }

	 public boolean canSupportAssertion(Assertion assertion)
	 {
		 return true;
	 }
	 
	 public void applyPolicy(Policy policy, AxisDescription axisDescription) throws AxisFault
	 {
	 }

     public void shutdown(ConfigurationContext configurationContext) throws AxisFault
     {
		 //logger.info( "shutdown entry S3AuthenticationModule" );
     }
}
