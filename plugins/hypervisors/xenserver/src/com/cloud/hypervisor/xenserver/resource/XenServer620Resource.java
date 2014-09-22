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
package com.cloud.hypervisor.xenserver.resource;
import java.util.Map;
import java.util.Set;

import javax.ejb.Local;

import org.apache.log4j.Logger;

import com.xensource.xenapi.Connection;
import com.xensource.xenapi.Host;
import com.xensource.xenapi.HostPatch;
import com.xensource.xenapi.PoolPatch;

import org.apache.cloudstack.hypervisor.xenserver.XenserverConfigs;

import com.cloud.agent.api.StartupRoutingCommand;
import com.cloud.resource.ServerResource;

@Local(value = ServerResource.class)
public class XenServer620Resource extends XenServer610Resource {
    private static final Logger s_logger = Logger.getLogger(XenServer620Resource.class);

    public XenServer620Resource() {
        super();
    }


    protected boolean hostHasHotFix(Connection conn, String hotFixUuid) {
        try {
            Host host = Host.getByUuid(conn, _host.uuid);
            Host.Record re = host.getRecord(conn);
            Set<HostPatch> patches = re.patches;
            PoolPatch poolPatch = PoolPatch.getByUuid(conn, hotFixUuid);
            for(HostPatch patch : patches) {
                PoolPatch pp = patch.getPoolPatch(conn);
                if (pp.equals(poolPatch) && patch.getApplied(conn)) {
                    return true;
                }
            }
         } catch (Exception e) {
            s_logger.debug("can't get patches information for hotFix: " + hotFixUuid);
        }
        return false;
    }

    @Override
    protected void fillHostInfo(Connection conn, StartupRoutingCommand cmd) {
        super.fillHostInfo(conn, cmd);
        Map<String, String> details = cmd.getHostDetails();
        Boolean hotFix62ESP1004 = hostHasHotFix(conn, XenserverConfigs.XSHotFix62ESP1004);
        if( hotFix62ESP1004 != null && hotFix62ESP1004 ) {
            details.put(XenserverConfigs.XS620HotFix , XenserverConfigs.XSHotFix62ESP1004);
        } else {
            Boolean hotFix62ESP1 = hostHasHotFix(conn, XenserverConfigs.XSHotFix62ESP1);
            if( hotFix62ESP1 != null && hotFix62ESP1 ) {
                details.put(XenserverConfigs.XS620HotFix , XenserverConfigs.XSHotFix62ESP1);
            }
        }
        cmd.setHostDetails(details);
    }
}
