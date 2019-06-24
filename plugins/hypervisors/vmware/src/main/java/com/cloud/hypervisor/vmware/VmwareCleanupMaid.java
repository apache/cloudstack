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
package com.cloud.hypervisor.vmware;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.cloud.hypervisor.vmware.manager.VmwareManager;
import com.cloud.hypervisor.vmware.mo.ClusterMO;
import com.cloud.hypervisor.vmware.mo.DatacenterMO;
import com.cloud.hypervisor.vmware.mo.HostMO;
import com.cloud.hypervisor.vmware.mo.VirtualMachineMO;
import com.cloud.hypervisor.vmware.util.VmwareContext;

public class VmwareCleanupMaid {
    private static final Logger s_logger = Logger.getLogger(VmwareCleanupMaid.class);

    private static Map<String, List<VmwareCleanupMaid>> s_leftoverDummyVMs = new HashMap<String, List<VmwareCleanupMaid>>();

    private String _vCenterAddress;
    private String _dcMorValue;
    private String _hostMorValue;
    private String _vmName;
    private long _checkPoint;

    public VmwareCleanupMaid() {
    }

    public VmwareCleanupMaid(String hostGuid, String vmName) {
        String[] tokens = hostGuid.split("@");
        assert (tokens != null);
        assert (tokens.length == 2);
        _vCenterAddress = tokens[1];

        String[] hostTokens = tokens[0].split(":");
        assert (hostTokens != null);
        assert (hostTokens.length == 2);

        _hostMorValue = hostTokens[1];

        _vmName = vmName;
    }

    public VmwareCleanupMaid(String vCenterAddress, String dcMorValue, String vmName) {
        _vCenterAddress = vCenterAddress;
        _dcMorValue = dcMorValue;
        _vmName = vmName;
    }

//    @Override
//    public int cleanup(CheckPointManager checkPointMgr) {
//
//        // save a check-point in case we crash at current run so that we won't lose it
//        _checkPoint = checkPointMgr.pushCheckPoint(new VmwareCleanupMaid(_vCenterAddress, _dcMorValue, _vmName));
//        addLeftOverVM(this);
//        return 0;
//    }

    public String getCleanupProcedure() {
        return null;
    }

    public String getVCenterServer() {
        return _vCenterAddress;
    }

    public String getDatacenterMorValue() {
        return _dcMorValue;
    }

    public String getHostMorValue() {
        return _hostMorValue;
    }

    public String getVmName() {
        return _vmName;
    }

    public long getCheckPoint() {
        return _checkPoint;
    }

    public synchronized static void gcLeftOverVMs(VmwareContext context) {
        List<VmwareCleanupMaid> l = s_leftoverDummyVMs.get(context.getServerAddress());
        VmwareManager mgr = context.getStockObject(VmwareManager.CONTEXT_STOCK_NAME);
        assert (mgr != null);

        if (l != null && l.size() > 0) {
            for (VmwareCleanupMaid cleanupMaid : l) {
                try {
                    VirtualMachineMO vmMo = null;
                    if (cleanupMaid.getDatacenterMorValue() != null) {
                        DatacenterMO dcMo = new DatacenterMO(context, "Datacenter", cleanupMaid.getDatacenterMorValue());
                        vmMo = dcMo.findVm(cleanupMaid.getVmName());
                    } else {
                        assert (cleanupMaid.getHostMorValue() != null);
                        HostMO hostMo = new HostMO(context, "HostSystem", cleanupMaid.getHostMorValue());
                        ClusterMO clusterMo = new ClusterMO(context, hostMo.getHyperHostCluster());
                        vmMo = clusterMo.findVmOnHyperHost(cleanupMaid.getVmName());
                    }

                    if (vmMo != null) {
                        s_logger.info("Found left over dummy VM " + cleanupMaid.getVmName() + ", destroy it");
                        vmMo.destroy();
                    }
                } catch (Throwable e) {
                    s_logger.warn("Unable to destroy left over dummy VM " + cleanupMaid.getVmName());
                } finally {
// FIXME                    mgr.popCleanupCheckpoint(cleanupMaid.getCheckPoint());
                }
            }

            l.clear();
        }
    }
}
