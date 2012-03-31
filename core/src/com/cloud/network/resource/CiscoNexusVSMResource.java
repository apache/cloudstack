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

import com.cloud.utils.cisco.n1kv.vsm.CiscoNexusVSMService;

import org.apache.log4j.Logger;

class VSMError {
    static final int VSM_RESOURCE_EXISTS = 89901;
    static final int VSM_RESOURCE_NOT_EXISTS= 89902;
    static final int VSM_NO_SERIVCE = 89903;
    static final int VSM_OPERATION_NOT_PERMITTED = 89904;
}

public class CiscoNexusVSMResource implements ServerResource {

    // deployment configuration
    private String _name;
    //private String _zoneId;
    private String _ip;
    private String _username;
    private String _password;
    private Integer _numRetries;
    private static final Logger s_logger = Logger.getLogger(CiscoNexusVSMResource.class);
    protected Gson _gson;

    // interface to interact with Cisco Nexus VSM devices
    CiscoNexusVSMService _vsmService;

    Long _timeout = new Long(100000);
    //base_response apiCallResult;
    
    // We need to store the result of the XML-RPC command sent to
    // the VSM. For now it's a string. We should make this the appropriate XSD object.
    String xml_rpc_response;

    public CiscoNexusVSMResource() {
        _gson = GsonHelper.getGsonLogger();
    }


    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        try {
        	// What is this name?! Is it a name for the VSM device? What do we set this to??
        	// Can't understand why the "Manager" interface needs a String name parameter for
        	// configure().
        	
        	// Do we need this zone id???? We may need to add other info also, like a/c id etc.
        	/**
            _zoneId = (String) params.get("zoneId");
            if (_zoneId == null) {
                throw new ConfigurationException("Unable to find zone Id  in the configuration parameters");
            } **/

            _ip = (String) params.get(ApiConstants.IP_ADDRESS);
            if (_ip == null) {
                throw new ConfigurationException("Unable to find IP address in the configuration parameters");
            }

            _username = (String) params.get(ApiConstants.USERNAME);
            if (_username == null) {
                throw new ConfigurationException("Unable to find username in the configuration parameters");
            }

            _password = (String) params.get(ApiConstants.PASSWORD);
            if (_password == null) {
                throw new ConfigurationException("Unable to find password in the configuration parameters");
            }

            _numRetries = NumbersUtil.parseInt((String) params.get("numretries"), 2);
            
            // we may want to validate whether the username/password is right.. so we may want to
            // issue a login to the VSM. However note that the VSM has a max limit of 8 concurrent
            // sessions. We don't want a situation where we are opening concurrent sessions at all.

            return true;
        } catch (Exception e) {
            throw new ConfigurationException(e.getMessage());
        }
    }

    @Override
    public IAgentControl getAgentControl() {
        return null;
    }
    
    @Override
    public void setAgentControl(IAgentControl agentControl) {
        return;
    }
    
    
    @Override
    public void disconnected() {
        return;
    }
    
    @Override
    public Type getType() {
        return Host.Type.ExternalVirtualSwitchSupervisor;
    }
    
    @Override
    public PingCommand getCurrentStatus(long id) {
        return new PingCommand(Host.Type.ExternalVirtualSwitchSupervisor, id);
    }
    
    @Override
    public StartupCommand[] initialize() {
        StartupExternalLoadBalancerCommand cmd = new StartupExternalLoadBalancerCommand();
        cmd.setName(_name);
        //cmd.setDataCenter(_zoneId);
        cmd.setPod("");
        cmd.setPrivateIpAddress(_ip);
        cmd.setStorageIpAddress("");
        cmd.setVersion("");
        return new StartupCommand[]{cmd};
    }

    @Override
    public Answer executeRequest(Command cmd) {
        return executeRequest(cmd, _numRetries);
    }

    // We will need to change this executeRequest() function.
    
    private Answer executeRequest(Command cmd, int numRetries) {
             return Answer.createUnsupportedCommandAnswer(cmd);
     }
   
    @Override
    public String getName() {
        return _name;
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }    

}