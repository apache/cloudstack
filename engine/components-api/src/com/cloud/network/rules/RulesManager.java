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
package com.cloud.network.rules;

import java.util.List;

import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.IpAddress;
import com.cloud.user.Account;
import com.cloud.uservm.UserVm;
import com.cloud.vm.Nic;
import com.cloud.vm.VirtualMachine;

/**
 * Rules Manager manages the network rules created for different networks.
 */
public interface RulesManager extends RulesService {

    boolean applyPortForwardingRulesForNetwork(long networkId, boolean continueOnError, Account caller);

    boolean applyStaticNatRulesForNetwork(long networkId, boolean continueOnError, Account caller);

    void checkRuleAndUserVm(FirewallRule rule, UserVm userVm, Account caller);

    boolean revokeAllPFAndStaticNatRulesForIp(long ipId, long userId, Account caller) throws ResourceUnavailableException;

    boolean revokeAllPFStaticNatRulesForNetwork(long networkId, long userId, Account caller) throws ResourceUnavailableException;

    boolean revokePortForwardingRulesForVm(long vmId);

    FirewallRule[] reservePorts(IpAddress ip, String protocol, FirewallRule.Purpose purpose, boolean openFirewall, Account caller, int... ports)
        throws NetworkRuleConflictException;

    boolean applyStaticNatsForNetwork(long networkId, boolean continueOnError, Account caller);

    void getSystemIpAndEnableStaticNatForVm(VirtualMachine vm, boolean getNewIp) throws InsufficientAddressCapacityException;

    boolean disableStaticNat(long ipAddressId, Account caller, long callerUserId, boolean releaseIpIfElastic) throws ResourceUnavailableException;

    /**
     * @param networkId
     * @param continueOnError
     * @param caller
     * @param forRevoke
     * @return
     */
    boolean applyStaticNatForNetwork(long networkId, boolean continueOnError, Account caller, boolean forRevoke);

    List<FirewallRuleVO> listAssociatedRulesForGuestNic(Nic nic);

}
