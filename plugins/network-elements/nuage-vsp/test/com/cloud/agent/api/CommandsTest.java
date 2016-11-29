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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Map;

import net.nuage.vsp.acs.client.api.model.Protocol;
import net.nuage.vsp.acs.client.api.model.VspAclRule;
import net.nuage.vsp.acs.client.api.model.VspNetwork;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.Maps;
import com.google.common.testing.EqualsTester;
import com.google.gson.Gson;

import com.cloud.agent.api.element.ApplyAclRuleVspCommand;
import com.cloud.agent.api.element.ApplyStaticNatVspCommand;
import com.cloud.agent.api.element.ImplementVspCommand;
import com.cloud.agent.api.element.ShutDownVpcVspCommand;
import com.cloud.agent.api.guru.DeallocateVmVspCommand;
import com.cloud.agent.api.guru.ImplementNetworkVspCommand;
import com.cloud.agent.api.guru.ReserveVmInterfaceVspCommand;
import com.cloud.agent.api.guru.TrashNetworkVspCommand;
import com.cloud.agent.api.manager.EntityExistsCommand;
import com.cloud.agent.api.manager.SupportedApiVersionCommand;
import com.cloud.agent.api.sync.SyncDomainCommand;
import com.cloud.agent.api.sync.SyncNuageVspCmsIdCommand;
import com.cloud.serializer.GsonHelper;

import static org.hamcrest.core.Is.is;

public class CommandsTest {
    private static final Gson s_gson = GsonHelper.getGson();

    private EqualsTester tester = new EqualsTester();

    @Test
    public void testCommandEquals() throws IllegalAccessException, InvocationTargetException, InstantiationException {
        addCommandEqualityGroup(ApplyAclRuleVspCommand.class);
        addCommandEqualityGroup(ImplementVspCommand.class);
        addCommandEqualityGroup(ApplyStaticNatVspCommand.class);
        addCommandEqualityGroup(ShutDownVpcVspCommand.class);
        addCommandEqualityGroup(DeallocateVmVspCommand.class);
        addCommandEqualityGroup(ImplementNetworkVspCommand.class);
        addCommandEqualityGroup(ReserveVmInterfaceVspCommand.class);
        addCommandEqualityGroup(TrashNetworkVspCommand.class);
        addCommandEqualityGroup(SyncDomainCommand.class);
        addCommandEqualityGroup(SyncNuageVspCmsIdCommand.class);
        addCommandEqualityGroup(PingNuageVspCommand.class);

        SupportedApiVersionCommand supportedApiVersionCommandA = new SupportedApiVersionCommand("3.2");
        SupportedApiVersionCommand supportedApiVersionCommandB = new SupportedApiVersionCommand("3.2");

        EntityExistsCommand entityExistsCommandA = new EntityExistsCommand(Command.class, "uuid");
        EntityExistsCommand entityExistsCommandB = new EntityExistsCommand(Command.class, "uuid");

        tester
            .addEqualityGroup(supportedApiVersionCommandA, supportedApiVersionCommandB)
            .addEqualityGroup(entityExistsCommandA, entityExistsCommandB)
            .testEquals();
    }

    @Test
    public void testCommandGsonEquals() throws IllegalAccessException, InvocationTargetException, InstantiationException {
        addCommandGsonEqualityGroup(ApplyAclRuleVspCommand.class);
        addCommandGsonEqualityGroup(ImplementVspCommand.class);
        addCommandGsonEqualityGroup(ApplyStaticNatVspCommand.class);
        addCommandGsonEqualityGroup(ShutDownVpcVspCommand.class);
        addCommandGsonEqualityGroup(DeallocateVmVspCommand.class);
        addCommandGsonEqualityGroup(ImplementNetworkVspCommand.class);
        addCommandGsonEqualityGroup(ReserveVmInterfaceVspCommand.class);
        addCommandGsonEqualityGroup(TrashNetworkVspCommand.class);
        addCommandGsonEqualityGroup(new SupportedApiVersionCommand("3.2"));
        addCommandGsonEqualityGroup(SyncDomainCommand.class);
        addCommandGsonEqualityGroup(SyncNuageVspCmsIdCommand.class);
        addCommandGsonEqualityGroup(PingNuageVspCommand.class);
        addCommandGsonEqualityGroup(new EntityExistsCommand(Command.class, "uuid"));

        tester.testEquals();
    }

    @Test
    public void testApplyAclRuleVspCommandGsonEquals() throws IllegalAccessException, InvocationTargetException, InstantiationException {
        VspNetwork vspNetwork = new VspNetwork.Builder()
                .id(1)
                .uuid("uuid")
                .name("name")
                .cidr("192.168.1.0/24")
                .gateway("192.168.1.1")
                .build();

        VspAclRule aclRule = new VspAclRule.Builder()
                .action(VspAclRule.ACLAction.Allow)
                .uuid("uuid")
                .trafficType(VspAclRule.ACLTrafficType.Egress)
                .protocol(Protocol.TCP)
                .startPort(80)
                .endPort(80)
                .priority(1)
                .state(VspAclRule.ACLState.Active)
                .build();

        ApplyAclRuleVspCommand before = new ApplyAclRuleVspCommand(VspAclRule.ACLType.NetworkACL, vspNetwork, Arrays.asList(aclRule), false);
        ApplyAclRuleVspCommand after = serializeAndDeserialize(before);

        Assert.assertThat(after.getAclRules().get(0).getProtocol().hasPort(), is(Protocol.TCP.hasPort()));
    }

    private <T extends Command> T serializeAndDeserialize(T command) {
        Command[] forwardedCommands = s_gson.fromJson(s_gson.toJson(new Command[] { command }), Command[].class);
        return (T) forwardedCommands[0];
    }

    private <T extends Command> void addCommandGsonEqualityGroup(Class<T> clazz) throws IllegalAccessException, InvocationTargetException, InstantiationException{
        addCommandGsonEqualityGroup(fillObject(clazz));
    }

    private <T extends Command> void addCommandGsonEqualityGroup(Command command) throws IllegalAccessException, InvocationTargetException, InstantiationException{
        Command[] forwardedCommands = s_gson.fromJson(s_gson.toJson(new Command[] { command }), Command[].class);
        Assert.assertEquals(command, forwardedCommands[0]);
        tester.addEqualityGroup(command, forwardedCommands[0]);
    }

    private <T extends Command> void addCommandEqualityGroup(Class<T> clazz) throws IllegalAccessException, InvocationTargetException, InstantiationException {
        Command a = fillObject(clazz);
        Command b = fillObject(clazz);
        tester.addEqualityGroup(a, b);
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
