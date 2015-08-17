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
package com.cloud.ha;

import java.util.List;

import javax.ejb.Local;
import javax.inject.Inject;

import org.apache.log4j.Logger;

import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.host.dao.HostDao;
import com.cloud.network.NetworkModel;
import com.cloud.network.Networks.TrafficType;
import com.cloud.vm.Nic;
import com.cloud.vm.VirtualMachine;

@Local(value = {Investigator.class})
public class ManagementIPSystemVMInvestigator extends AbstractInvestigatorImpl {
    private static final Logger s_logger = Logger.getLogger(ManagementIPSystemVMInvestigator.class);

    @Inject
    private final HostDao _hostDao = null;
    @Inject
    private final NetworkModel _networkMgr = null;

    @Override
    public boolean isVmAlive(VirtualMachine vm, Host host) throws UnknownVM {
        if (!vm.getType().isUsedBySystem()) {
            s_logger.debug("Not a System Vm, unable to determine state of " + vm + " returning null");
        }

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Testing if " + vm + " is alive");
        }

        if (vm.getHostId() == null) {
            s_logger.debug("There's no host id for " + vm);
            throw new UnknownVM();
        }

        HostVO vmHost = _hostDao.findById(vm.getHostId());
        if (vmHost == null) {
            s_logger.debug("Unable to retrieve the host by using id " + vm.getHostId());
            throw new UnknownVM();
        }

        List<? extends Nic> nics = _networkMgr.getNicsForTraffic(vm.getId(), TrafficType.Management);
        if (nics.size() == 0) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Unable to find a management nic, cannot ping this system VM, unable to determine state of " + vm + " returning null");
            }
            throw new UnknownVM();
        }

        for (Nic nic : nics) {
            if (nic.getIPv4Address() == null) {
                continue;
            }
            // get the data center IP address, find a host on the pod, use that host to ping the data center IP address
            List<Long> otherHosts = findHostByPod(vmHost.getPodId(), vm.getHostId());
            for (Long otherHost : otherHosts) {
                Status vmState = testIpAddress(otherHost, nic.getIPv4Address());
                assert vmState != null;
                // In case of Status.Unknown, next host will be tried
                if (vmState == Status.Up) {
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("successfully pinged vm's private IP (" + vm.getPrivateIpAddress() + "), returning that the VM is up");
                    }
                    return Boolean.TRUE;
                } else if (vmState == Status.Down) {
                    // We can't ping the VM directly...if we can ping the host, then report the VM down.
                    // If we can't ping the host, then we don't have enough information.
                    Status vmHostState = testIpAddress(otherHost, vmHost.getPrivateIpAddress());
                    assert vmHostState != null;
                    if (vmHostState == Status.Up) {
                        if (s_logger.isDebugEnabled()) {
                            s_logger.debug("successfully pinged vm's host IP (" + vmHost.getPrivateIpAddress() +
                                "), but could not ping VM, returning that the VM is down");
                        }
                        return Boolean.FALSE;
                    }
                }
            }
        }

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("unable to determine state of " + vm + " returning null");
        }
        throw new UnknownVM();
    }

    @Override
    public Status isAgentAlive(Host agent) {
        return null;
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
