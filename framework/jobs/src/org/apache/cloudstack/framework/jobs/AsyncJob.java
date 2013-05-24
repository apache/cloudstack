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
package org.apache.cloudstack.framework.jobs;

import java.util.Date;

import org.apache.cloudstack.api.Identity;
import org.apache.cloudstack.api.InternalIdentity;


public interface AsyncJob extends Identity, InternalIdentity {

    public static interface Topics {
        public static final String JOB_HEARTBEAT = "job.heartbeat";
        public static final String JOB_STATE = "job.state";
    }
	
	public enum JournalType {
		SUCCESS, FAILURE
	};
	
    String getType();
    
    String getDispatcher();

    int getPendingSignals();
    public enum Type {
        None,
        VirtualMachine,
        DomainRouter,
        Volume,
        ConsoleProxy,
        Snapshot,
        Template,
        Iso,
        SystemVm,
        Host,
        StoragePool,
        IpAddress,
        PortableIpAddress,
        SecurityGroup,
        PhysicalNetwork,
        TrafficType,
        PhysicalNetworkServiceProvider,
        FirewallRule,
        Account,
        User,
        PrivateGateway,
        StaticRoute,
        Counter,
        Condition,
        AutoScalePolicy,
        AutoScaleVmProfile,
        AutoScaleVmGroup,
        GlobalLoadBalancerRule,
        LoadBalancerRule,
        AffinityGroup,
        InternalLbVm,
        DedicatedGuestVlanRange
    }
    
    long getUserId();

    long getAccountId();

    String getCmd();

    int getCmdVersion();

    String getCmdInfo();
    
    int getStatus();

    int getProcessStatus();

    int getResultCode();

    String getResult();

    Long getInitMsid();
    void setInitMsid(Long msid);

    Long getExecutingMsid();
    
    Long getCompleteMsid();
    void setCompleteMsid(Long msid);

    Date getCreated();

    Date getLastUpdated();

    Date getLastPolled();

    Date getRemoved();

    String getInstanceType();

    Long getInstanceId();

    SyncQueueItem getSyncSource();
    void setSyncSource(SyncQueueItem item);
}
