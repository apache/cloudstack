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
package com.cloud.bridge.service;

import java.io.IOException;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;

import org.apache.ws.security.WSPasswordCallback;

public class PWCBHandler implements CallbackHandler {

	@SuppressWarnings("deprecation")
	public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
	   for (int i = 0; i < callbacks.length; i++) {
	      WSPasswordCallback pwcb = (WSPasswordCallback)callbacks[i];
	      String id = pwcb.getIdentifer();
	      if ( "client".equals(id)) {
	           pwcb.setPassword("apache");
	      } 
	      else if("service".equals(id)) {
	           pwcb.setPassword("apache");
	      }
	   }
	}
}

