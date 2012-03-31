// Copyright 2012 Citrix Systems, Inc. Licensed under the
// Apache License, Version 2.0 (the "License"); you may not use this
// file except in compliance with the License.  Citrix Systems, Inc.
// reserves all rights not expressly granted by the License.
// You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//

package com.cloud.utils.cisco.n1kv.vsm;

import com.cloud.utils.cisco.n1kv.vsm.VSMCommand;
import com.cloud.utils.cisco.n1kv.vsm.VSMResponse;

import org.apache.log4j.Logger;

// import all the packages we need here.
// We'll need the Nexus vsm xsd files for one..
// We'll need some netconf libraries probably..
// Definitely some XML parser libraries.


public class CiscoNexusVSMService extends Object {
	// This class contains static routines to interact with Cisco N1KV VSM devices.
	
	public static final Logger s_logger = Logger.getLogger(CiscoNexusVSMService.class.getName());
	
	public static int connectToVSM(String ipAddr, String userName, String password) {
		return 0;	// for now.. we'll return some kind of session id, which will probably be
					// a thread ID. Not sure how this will shape up but these are just placeholder
					// comments.		
	}
	
	public static void disconnectFromVSM(int sessionId) {
		return;	// Always return success for this one. Layers calling this will simply clean up
				// any state info regarding this session.
	}
	
	public static VSMResponse executeVSMCommand(VSMCommand cmd) {
		VSMResponse resp = new VSMResponse();
		return resp;	// for now we just return a dummy response. We define these new
						// VSMCommand and VSMResponse classes in this same package. The
						// input params to this function may change as we put in more
						// concrete functionality.
	}
	
}
		
	 
	 
	
