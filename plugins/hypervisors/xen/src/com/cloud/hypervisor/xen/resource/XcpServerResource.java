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
package com.cloud.hypervisor.xen.resource;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.NetworkUsageAnswer;
import com.cloud.agent.api.NetworkUsageCommand;
import com.cloud.resource.ServerResource;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.Script;
import com.xensource.xenapi.Connection;
import com.xensource.xenapi.Types.XenAPIException;
import com.xensource.xenapi.VM;
import org.apache.log4j.Logger;
import org.apache.xmlrpc.XmlRpcException;

import javax.ejb.Local;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Local(value=ServerResource.class)
public class XcpServerResource extends CitrixResourceBase {
    private final static Logger s_logger = Logger.getLogger(XcpServerResource.class);
    private String version;

    public XcpServerResource() {
        super();
    }

    @Override
    public Answer executeRequest(Command cmd) {
        if (cmd instanceof NetworkUsageCommand) {
            return execute((NetworkUsageCommand) cmd);
        } else {
            return super.executeRequest(cmd);
        }
    }

    @Override
    protected List<File> getPatchFiles() {
        List<File> files = new ArrayList<File>();
        String patch = "scripts/vm/hypervisor/xenserver/xcpserver/patch";
        String patchfilePath = Script.findScript("", patch);
        if (patchfilePath == null) {
            throw new CloudRuntimeException("Unable to find patch file " + patch);
        }
        File file = new File(patchfilePath);
        files.add(file);
        return files;
    }

    @Override
    protected String getGuestOsType(String stdType, boolean bootFromCD) {
        return CitrixHelper.getXcpGuestOsType(stdType);
    }

    @Override
    protected void setMemory(Connection conn, VM vm, long minMemsize, long maxMemsize) throws XmlRpcException, XenAPIException {

        vm.setMemoryStaticMin(conn, 33554432L);
        //vm.setMemoryDynamicMin(conn, 33554432L);
        //vm.setMemoryDynamicMax(conn, 33554432L);
        vm.setMemoryStaticMax(conn, 33554432L);

        //vm.setMemoryStaticMax(conn, maxMemsize );
        vm.setMemoryDynamicMax(conn, maxMemsize );
        vm.setMemoryDynamicMin(conn, minMemsize );
        //vm.setMemoryStaticMin(conn,  maxMemsize );
    }
    
    protected NetworkUsageAnswer execute(NetworkUsageCommand cmd) {
        try {
            Connection conn = getConnection();
            if(cmd.getOption()!=null && cmd.getOption().equals("create") ){
                String result = networkUsage(conn, cmd.getPrivateIP(), "create", null);
                NetworkUsageAnswer answer = new NetworkUsageAnswer(cmd, result, 0L, 0L);
                return answer;
            }
            long[] stats = getNetworkStats(conn, cmd.getPrivateIP());
            NetworkUsageAnswer answer = new NetworkUsageAnswer(cmd, "", stats[0], stats[1]);
            return answer;
        } catch (Exception ex) {
            s_logger.warn("Failed to get network usage stats due to ", ex);
            return new NetworkUsageAnswer(cmd, ex);
        }
    }
}
