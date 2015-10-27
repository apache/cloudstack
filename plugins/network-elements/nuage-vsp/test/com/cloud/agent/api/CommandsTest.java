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
import com.cloud.agent.api.sync.SyncVspCommand;
import com.google.common.collect.Maps;
import com.google.common.testing.EqualsTester;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

public class CommandsTest {

    @Test
    public void testCommandEquals() throws IllegalAccessException, InvocationTargetException, InstantiationException {
        ApplyAclRuleVspCommand applyAclRuleVspCommand = fillBuilderObject(new ApplyAclRuleVspCommand.Builder()).build();
        ApplyAclRuleVspCommand otherApplyAclRuleVspCommand = fillBuilderObject(new ApplyAclRuleVspCommand.Builder()).build();

        ApplyStaticNatVspCommand applyStaticNatVspCommand = fillBuilderObject(new ApplyStaticNatVspCommand.Builder()).build();
        ApplyStaticNatVspCommand otherApplyStaticNatVspCommand = fillBuilderObject(new ApplyStaticNatVspCommand.Builder()).build();

        ImplementVspCommand implementVspCommand = fillBuilderObject(new ImplementVspCommand.Builder()).build();
        ImplementVspCommand otherImplementVspCommand = fillBuilderObject(new ImplementVspCommand.Builder()).build();

        ShutDownVpcVspCommand shutDownVpcVspCommand = fillBuilderObject(new ShutDownVpcVspCommand.Builder()).build();
        ShutDownVpcVspCommand otherShutDownVpcVspCommand = fillBuilderObject(new ShutDownVpcVspCommand.Builder()).build();

        DeallocateVmVspCommand deallocateVmVspCommand = fillBuilderObject(new DeallocateVmVspCommand.Builder()).build();
        DeallocateVmVspCommand otherDeallocateVmVspCommand = fillBuilderObject(new DeallocateVmVspCommand.Builder()).build();

        ImplementNetworkVspCommand implementNetworkVspCommand = fillBuilderObject(new ImplementNetworkVspCommand.Builder()).build();
        ImplementNetworkVspCommand otherImplementNetworkVspCommand = fillBuilderObject(new ImplementNetworkVspCommand.Builder()).build();

        ReserveVmInterfaceVspCommand reserveVmInterfaceVspCommand = fillBuilderObject(new ReserveVmInterfaceVspCommand.Builder()).build();
        ReserveVmInterfaceVspCommand otherReserveVmInterfaceVspCommand = fillBuilderObject(new ReserveVmInterfaceVspCommand.Builder()).build();

        TrashNetworkVspCommand trashNetworkVspCommand = fillBuilderObject(new TrashNetworkVspCommand.Builder()).build();
        TrashNetworkVspCommand otherTrashNetworkVspCommand  = fillBuilderObject(new TrashNetworkVspCommand.Builder()).build();

        SupportedApiVersionCommand supportedApiVersionCommand = new SupportedApiVersionCommand("3.2");
        SupportedApiVersionCommand otherSupportedApiVersionCommand = new SupportedApiVersionCommand("3.2");

        SyncDomainCommand syncDomainCommand = fillObject(SyncDomainCommand.class);
        SyncDomainCommand otherSyncDomainCommand = fillObject(SyncDomainCommand.class);

        SyncNuageVspCmsIdCommand syncNuageVspCmsIdCommand = fillObject(SyncNuageVspCmsIdCommand.class);
        SyncNuageVspCmsIdCommand otherSyncNuageVspCmsIdCommand = fillObject(SyncNuageVspCmsIdCommand.class);

        SyncVspCommand syncVspCommand = fillObject(SyncVspCommand.class);
        SyncVspCommand otherSyncVspCommand = fillObject(SyncVspCommand.class);

        PingNuageVspCommand pingNuageVspCommand = fillObject(PingNuageVspCommand.class);
        PingNuageVspCommand otherPingNuageVspCommand = fillObject(PingNuageVspCommand.class);

        VspResourceCommand vspResourceCommand = fillObject(VspResourceCommand.class);
        VspResourceCommand otherVspResourceCommand = fillObject(VspResourceCommand.class);

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
                .addEqualityGroup(syncVspCommand, otherSyncVspCommand)
                .addEqualityGroup(pingNuageVspCommand, otherPingNuageVspCommand)
                .addEqualityGroup(vspResourceCommand, otherVspResourceCommand)
                .testEquals();
    }

    private <T extends CmdBuilder> T fillBuilderObject(T obj) throws IllegalAccessException, InvocationTargetException {
        Class clazz = obj.getClass();
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.getParameterTypes().length == 1) {
                Class paramType = method.getParameterTypes()[0];
                if (isNumericType(paramType)) {
                    if (Long.class.isAssignableFrom(paramType)) {
                        method.invoke(obj, Long.valueOf(method.getName().length()));
                    } else {
                        method.invoke(obj, method.getName().length());
                    }
                } else if (String.class.isAssignableFrom(paramType)) {
                    method.invoke(obj, method.getName());
                } else if (Boolean.class.isAssignableFrom(paramType) || boolean.class.isAssignableFrom(paramType)) {
                    method.invoke(obj, method.getName().length() % 2 == 0);
                }
            }
        }
        return obj;
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
