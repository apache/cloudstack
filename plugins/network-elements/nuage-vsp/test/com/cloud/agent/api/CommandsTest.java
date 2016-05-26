//
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
//

package com.cloud.agent.api;

import com.cloud.agent.api.element.ApplyAclRuleVspCommand;
import com.cloud.agent.api.element.ApplyStaticNatVspCommand;
import com.cloud.agent.api.element.ImplementVspCommand;
import com.cloud.agent.api.element.ShutDownVpcVspCommand;
import com.cloud.agent.api.guru.DeallocateVmVspCommand;
import com.cloud.agent.api.guru.ImplementNetworkVspCommand;
import com.cloud.agent.api.guru.ReserveVmInterfaceVspCommand;
import com.cloud.agent.api.guru.TrashNetworkVspCommand;
import com.cloud.agent.api.manager.SupportedApiVersionCommand;
import com.cloud.agent.api.sync.SyncDomainCommand;
import com.cloud.agent.api.sync.SyncNuageVspCmsIdCommand;
import com.google.common.collect.Maps;
import com.google.common.testing.EqualsTester;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

public class CommandsTest {

    @Test
    public void testCommandEquals() throws IllegalAccessException, InvocationTargetException, InstantiationException {
        ApplyAclRuleVspCommand applyAclRuleVspCommand = fillObject(ApplyAclRuleVspCommand.class);
        ApplyAclRuleVspCommand otherApplyAclRuleVspCommand = fillObject(ApplyAclRuleVspCommand.class);

        ApplyStaticNatVspCommand applyStaticNatVspCommand = fillObject(ApplyStaticNatVspCommand.class);
        ApplyStaticNatVspCommand otherApplyStaticNatVspCommand = fillObject(ApplyStaticNatVspCommand.class);

        ImplementVspCommand implementVspCommand = fillObject(ImplementVspCommand.class);
        ImplementVspCommand otherImplementVspCommand = fillObject(ImplementVspCommand.class);

        ShutDownVpcVspCommand shutDownVpcVspCommand = fillObject(ShutDownVpcVspCommand.class);
        ShutDownVpcVspCommand otherShutDownVpcVspCommand = fillObject(ShutDownVpcVspCommand.class);

        DeallocateVmVspCommand deallocateVmVspCommand = fillObject(DeallocateVmVspCommand.class);
        DeallocateVmVspCommand otherDeallocateVmVspCommand = fillObject(DeallocateVmVspCommand.class);

        ImplementNetworkVspCommand implementNetworkVspCommand = fillObject(ImplementNetworkVspCommand.class);
        ImplementNetworkVspCommand otherImplementNetworkVspCommand = fillObject(ImplementNetworkVspCommand.class);

        ReserveVmInterfaceVspCommand reserveVmInterfaceVspCommand = fillObject(ReserveVmInterfaceVspCommand.class);
        ReserveVmInterfaceVspCommand otherReserveVmInterfaceVspCommand = fillObject(ReserveVmInterfaceVspCommand.class);

        TrashNetworkVspCommand trashNetworkVspCommand = fillObject(TrashNetworkVspCommand.class);
        TrashNetworkVspCommand otherTrashNetworkVspCommand  = fillObject(TrashNetworkVspCommand.class);

        SupportedApiVersionCommand supportedApiVersionCommand = new SupportedApiVersionCommand("3.2");
        SupportedApiVersionCommand otherSupportedApiVersionCommand = new SupportedApiVersionCommand("3.2");

        SyncDomainCommand syncDomainCommand = fillObject(SyncDomainCommand.class);
        SyncDomainCommand otherSyncDomainCommand = fillObject(SyncDomainCommand.class);

        SyncNuageVspCmsIdCommand syncNuageVspCmsIdCommand = fillObject(SyncNuageVspCmsIdCommand.class);
        SyncNuageVspCmsIdCommand otherSyncNuageVspCmsIdCommand = fillObject(SyncNuageVspCmsIdCommand.class);

        PingNuageVspCommand pingNuageVspCommand = fillObject(PingNuageVspCommand.class);
        PingNuageVspCommand otherPingNuageVspCommand = fillObject(PingNuageVspCommand.class);

        new EqualsTester()
                .addEqualityGroup(applyAclRuleVspCommand, otherApplyAclRuleVspCommand)
                .addEqualityGroup(applyStaticNatVspCommand, otherApplyStaticNatVspCommand)
                .addEqualityGroup(implementVspCommand, otherImplementVspCommand)
                .addEqualityGroup(shutDownVpcVspCommand, otherShutDownVpcVspCommand)
                .addEqualityGroup(deallocateVmVspCommand, otherDeallocateVmVspCommand)
                .addEqualityGroup(implementNetworkVspCommand, otherImplementNetworkVspCommand)
                .addEqualityGroup(reserveVmInterfaceVspCommand, otherReserveVmInterfaceVspCommand)
                .addEqualityGroup(trashNetworkVspCommand, otherTrashNetworkVspCommand)
                .addEqualityGroup(supportedApiVersionCommand, otherSupportedApiVersionCommand)
                .addEqualityGroup(syncDomainCommand, otherSyncDomainCommand)
                .addEqualityGroup(syncNuageVspCmsIdCommand, otherSyncNuageVspCmsIdCommand)
                .addEqualityGroup(pingNuageVspCommand, otherPingNuageVspCommand)
                .testEquals();
    }

    private <T> T fillObject(Class<T> clazz) throws IllegalAccessException, InvocationTargetException, InstantiationException {
        Constructor constructor = clazz.getDeclaredConstructors()[0];
        Object[] constructorArgs = new Object[constructor.getParameterTypes().length];
        for (int i = 0; i < constructor.getParameterTypes().length; i++) {
            Class constructorArgType = constructor.getParameterTypes()[i];
            if (isNumericType(constructorArgType)) {
                constructorArgs[i] = constructorArgType.getName().length();
            } else if (String.class.isAssignableFrom(constructorArgType)) {
                constructorArgs[i] = constructorArgType.getName();
            } else if (Boolean.class.isAssignableFrom(constructorArgType) || boolean.class.isAssignableFrom(constructorArgType)) {
                constructorArgs[i] = constructorArgType.getName().length() % 2 == 0;
            } else if (Map.class.isAssignableFrom(constructorArgType)) {
                constructorArgs[i] = Maps.newHashMap();
            } else {
                constructorArgs[i] = null;
            }
        }
        return (T) constructor.newInstance(constructorArgs);
    }

    private boolean isNumericType(Class type) {
        return Number.class.isAssignableFrom(type) || int.class.isAssignableFrom(type) || long.class.isAssignableFrom(type);
    }
}
