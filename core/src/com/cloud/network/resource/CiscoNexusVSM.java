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

package com.cloud.network.resource;

import java.util.Map;
import javax.naming.ConfigurationException;
import com.cloud.agent.IAgentControl;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.PingCommand;
import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.api.StartupExternalLoadBalancerCommand;
import com.cloud.api.ApiConstants;
import com.cloud.host.Host;
import com.cloud.host.Host.Type;
import com.cloud.resource.ServerResource;
import com.cloud.serializer.GsonHelper;
import com.cloud.utils.NumbersUtil;
import com.google.gson.Gson;

import com.cloud.utils.ssh.*;
import com.cloud.utils.cisco.n1kv.vsm.CiscoNexusVSMService;
import com.cloud.utils.exception.CloudRuntimeException;

import org.apache.log4j.Logger;

class VSMError {
    static final int VSM_RESOURCE_EXISTS = 89901;
    static final int VSM_RESOURCE_NOT_EXISTS= 89902;
    static final int VSM_NO_SERIVCE = 89903;
    static final int VSM_OPERATION_NOT_PERMITTED = 89904;
}

public class CiscoNexusVSM {

    // deployment configuration
	private String vsmIpaddr;
	private String vsmUsername;
	private String vsmPassword;
	private String vsmSubsystem;
	com.trilead.ssh2.Connection sshConnection;
	private String _helloClientCmd = "<?xml version=\"1.0\"?><nc:hello xmlns:nc=\"urn:ietf:params:xml:ns:netconf:base:1.0\"><nc:capabilities><nc:capability>urn:ietf:params:xml:ns:netconf:base:1.0</nc:capability></nc:capabilities></nc:hello>]]>]]>";
	private String _clientCmd1 = "<?xml version=\"1.0\"?><nc:rpc message-id=\"1\" xmlns:nc=\"urn:ietf:params:xml:ns:netconf:base:1.0\"xmlns=\"http://www.cisco.com/nxos:1.0:xml\"><nc:get><nc:filter type=\"subtree\"><show><xml><server><status/></server></xml></show></nc:filter></nc:get></nc:rpc>]]>]]>";
	
    private static final Logger s_logger = Logger.getLogger(CiscoNexusVSM.class);

    // interface to interact with Cisco Nexus VSM devices
    CiscoNexusVSMService _vsmService;

    Long _timeout = new Long(100000);
    //base_response apiCallResult;
    
    // We need to store the result of the XML-RPC command sent to
    // the VSM. For now it's a string. We should make this the appropriate XSD object.
    String xml_rpc_response;

    public void setVsmIpaddr(String ipaddr) {
    	this.vsmIpaddr = ipaddr;
    }
    
    public void setVsmUsername(String username) {
    	this.vsmUsername = username; 
    }
    
    public void setVsmPassword(String password) {
    	this.vsmPassword = password;
    }
    
    public void setVsmSubsystem(String subsystem) {
    	this.vsmSubsystem = subsystem; 
    }
    
    public CiscoNexusVSM() {
    	this.vsmSubsystem = "xmlagent";
    }
    
    public String getVsmIpaddr() {
    	return vsmIpaddr;
    }
    
    public String getvsmUsername() {
    	return vsmUsername;
    }
    
    public String getvsmPassword() {
    	return vsmPassword;
    }
    
    public String getvsmSubsystem() {
    	return vsmSubsystem;
    }
    
    public CiscoNexusVSM(String username, String ipaddr, String password) {
    	
    }
    
    public boolean connectToVSM() {
    	sshConnection = SSHCmdHelper.acquireAuthorizedConnection(this.vsmIpaddr, this.vsmUsername, this.vsmPassword);
		if (sshConnection == null) {
			return false;
		}
		return true;
    }

}