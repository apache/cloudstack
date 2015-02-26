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
package com.cloud.network.security;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.management.StandardMBean;

import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.network.security.LocalSecurityGroupWorkQueue.LocalSecurityGroupWork;
import com.cloud.network.security.SecurityGroupWork.Step;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine.Type;

public class SecurityManagerMBeanImpl extends StandardMBean implements SecurityGroupManagerMBean, RuleUpdateLog {
    SecurityGroupManagerImpl2 _sgMgr;
    boolean _monitoringEnabled = false;
    //keep track of last scheduled, last update sent and last seqno sent per vm. Make it available over JMX
    Map<Long, Date> _scheduleTimestamps = new ConcurrentHashMap<Long, Date>(4000, 100, 64);
    Map<Long, Date> _updateTimestamps = new ConcurrentHashMap<Long, Date>(4000, 100, 64);

    protected SecurityManagerMBeanImpl(SecurityGroupManagerImpl2 securityGroupManager) {
        super(SecurityGroupManagerMBean.class, false);
        this._sgMgr = securityGroupManager;
    }

    @Override
    public int getQueueSize() {
        return this._sgMgr.getQueueSize();
    }

    @Override
    public void logUpdateDetails(Long vmId, Long seqno) {
        if (_monitoringEnabled) {
            _updateTimestamps.put(vmId, new Date());
        }

    }

    @Override
    public void logScheduledDetails(Set<Long> vmIds) {
        if (_monitoringEnabled) {
            for (Long vmId : vmIds) {
                _scheduleTimestamps.put(vmId, new Date());
            }
        }
    }

    @Override
    public void enableUpdateMonitor(boolean enable) {
        _monitoringEnabled = enable;
        if (!enable) {
            _updateTimestamps.clear();
            _scheduleTimestamps.clear();
        }
    }

    @Override
    public Map<Long, Date> getScheduledTimestamps() {
        return _scheduleTimestamps;
    }

    @Override
    public Map<Long, Date> getLastUpdateSentTimestamps() {
        return _updateTimestamps;
    }

    @Override
    public List<Long> getVmsInQueue() {
        return _sgMgr.getWorkQueue().getVmsInQueue();
    }

    @Override
    public void disableSchedulerForVm(Long vmId) {
        _sgMgr.disableSchedulerForVm(vmId, true);

    }

    @Override
    public void enableSchedulerForVm(Long vmId) {
        _sgMgr.disableSchedulerForVm(vmId, false);

    }

    @Override
    public Long[] getDisabledVmsForScheduler() {
        return _sgMgr.getDisabledVmsForScheduler();
    }

    @Override
    public void enableSchedulerForAllVms() {
        _sgMgr.enableAllVmsForScheduler();

    }

    @Override
    public void scheduleRulesetUpdateForVm(Long vmId) {
        List<Long> affectedVms = new ArrayList<Long>(1);
        affectedVms.add(vmId);
        _sgMgr.scheduleRulesetUpdateToHosts(affectedVms, true, null);
    }

    @Override
    public void tryRulesetUpdateForVmBypassSchedulerVeryDangerous(Long vmId, Long seqno) {
        LocalSecurityGroupWork work = new LocalSecurityGroupWorkQueue.LocalSecurityGroupWork(vmId, seqno, Step.Scheduled);
        _sgMgr.sendRulesetUpdates(work);
    }

    @Override
    public void simulateVmStart(Long vmId) {
        //all we need is the vmId
        VMInstanceVO vm = new VMInstanceVO(vmId, 5, "foo", "foo", Type.User, null, HypervisorType.Any, 8, 1, 1, 1, false, false, null);
        _sgMgr.handleVmStarted(vm);
    }

    @Override
    public void disableSchedulerEntirelyVeryDangerous(boolean disable) {
        _sgMgr.disableScheduler(disable);
    }

    @Override
    public boolean isSchedulerDisabledEntirely() {
        return _sgMgr.isSchedulerDisabled();
    }

    @Override
    public void clearSchedulerQueueVeryDangerous() {
        _sgMgr.clearWorkQueue();
    }
}
