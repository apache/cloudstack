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
    private static final long mem_32m = 33554432L;

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

    /**
     XCP provides four memory configuration fields through which
     administrators can control this behaviour:

     * static-min
     * dynamic-min
     * dynamic-max
     * static-max

     The fields static-{min,max} act as *hard* lower and upper
     bounds for a guest's memory. For a running guest:
     * it's not possible to assign the guest more memory than
     static-max without first shutting down the guest.
     * it's not possible to assign the guest less memory than
     static-min without first shutting down the guest.

     The fields dynamic-{min,max} act as *soft* lower and upper
     bounds for a guest's memory. It's possible to change these
     fields even when a guest is running.

     The dynamic range must lie wholly within the static range. To
     put it another way, XCP at all times ensures that:

     static-min <= dynamic-min <= dynamic-max <= static-max

     At all times, XCP will attempt to keep a guest's memory usage
     between dynamic-min and dynamic-max.

     If dynamic-min = dynamic-max, then XCP will attempt to keep
     a guest's memory allocation at a constant size.

     If dynamic-min < dynamic-max, then XCP will attempt to give
     the guest as much memory as possible, while keeping the guest
     within dynamic-min and dynamic-max.

     If there is enough memory on a given host to give all resident
     guests dynamic-max, then XCP will attempt do so.

     If there is not enough memory to give all guests dynamic-max,
     then XCP will ask each of the guests (on that host) to use
     an amount of memory that is the same *proportional* distance
     between dynamic-min and dynamic-max.

     XCP will refuse to start guests if starting those guests would
     cause the sum of all the dynamic-min values to exceed the total
     host memory (taking into account various memory overheads).

     cf: http://wiki.xen.org/wiki/XCP_FAQ_Dynamic_Memory_Control
     */
    @Override
    protected void setMemory(Connection conn, VM vm, long minMemsize, long maxMemsize) throws XmlRpcException, XenAPIException {
        vm.setMemoryStaticMin(conn, mem_32m);
        vm.setMemoryDynamicMin(conn, minMemsize);
        vm.setMemoryDynamicMax(conn, maxMemsize);
        vm.setMemoryStaticMax(conn, maxMemsize);
    }
}
