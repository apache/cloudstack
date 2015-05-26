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

package com.cloud.hypervisor.xenserver.resource.wrapper.xenbase;

import java.util.Hashtable;
import java.util.Set;

import org.reflections.Reflections;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.hypervisor.xenserver.resource.CitrixResourceBase;
import com.cloud.hypervisor.xenserver.resource.XcpServerResource;
import com.cloud.hypervisor.xenserver.resource.XenServer56FP1Resource;
import com.cloud.hypervisor.xenserver.resource.XenServer56Resource;
import com.cloud.hypervisor.xenserver.resource.XenServer610Resource;
import com.cloud.hypervisor.xenserver.resource.XenServer620SP1Resource;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.RequestWrapper;
import com.cloud.resource.ServerResource;

public class CitrixRequestWrapper extends RequestWrapper {

    private static CitrixRequestWrapper instance;

    static {
        instance = new CitrixRequestWrapper();
    }

    Reflections baseWrappers = new Reflections("com.cloud.hypervisor.xenserver.resource.wrapper.xenbase");
    @SuppressWarnings("rawtypes")
    Set<Class<? extends CommandWrapper>> baseSet = baseWrappers.getSubTypesOf(CommandWrapper.class);

    Reflections xenServer56Wrappers = new Reflections("com.cloud.hypervisor.xenserver.resource.wrapper.xen56");
    @SuppressWarnings("rawtypes")
    Set<Class<? extends CommandWrapper>> xenServer56Set = xenServer56Wrappers.getSubTypesOf(CommandWrapper.class);

    Reflections xenServer56P1Wrappers = new Reflections("com.cloud.hypervisor.xenserver.resource.wrapper.xen56p1");
    @SuppressWarnings("rawtypes")
    Set<Class<? extends CommandWrapper>> xenServer56P1Set = xenServer56P1Wrappers.getSubTypesOf(CommandWrapper.class);

    Reflections xenServer610Wrappers = new Reflections("com.cloud.hypervisor.xenserver.resource.wrapper.xen610");
    @SuppressWarnings("rawtypes")
    Set<Class<? extends CommandWrapper>> xenServer610Set = xenServer610Wrappers.getSubTypesOf(CommandWrapper.class);

    Reflections xenServer620SP1Wrappers = new Reflections("com.cloud.hypervisor.xenserver.resource.wrapper.xen620sp1");
    @SuppressWarnings("rawtypes")
    Set<Class<? extends CommandWrapper>> xenServer620SP1Set = xenServer620SP1Wrappers.getSubTypesOf(CommandWrapper.class);

    Reflections xcpWrappers = new Reflections("com.cloud.hypervisor.xenserver.resource.wrapper.xcp");
    @SuppressWarnings("rawtypes")
    Set<Class<? extends CommandWrapper>> xcpSet = xcpWrappers.getSubTypesOf(CommandWrapper.class);

    private CitrixRequestWrapper() {
        init();
    }

    @SuppressWarnings("rawtypes")
    private void init() {

        final Hashtable<Class<? extends Command>, CommandWrapper> citrixCommands = processAnnotations(baseSet);
        final Hashtable<Class<? extends Command>, CommandWrapper> xenServer56Commands = processAnnotations(xenServer56Set);
        final Hashtable<Class<? extends Command>, CommandWrapper> xenServer56P1Commands = processAnnotations(xenServer56P1Set);
        final Hashtable<Class<? extends Command>, CommandWrapper> xenServer610Commands = processAnnotations(xenServer610Set);
        final Hashtable<Class<? extends Command>, CommandWrapper> xenServer620SP1Commands = processAnnotations(xenServer620SP1Set);
        final Hashtable<Class<? extends Command>, CommandWrapper> xcpServerResourceCommand = processAnnotations(xcpSet);

        // CitrixResourceBase commands
        resources.put(CitrixResourceBase.class, citrixCommands);

        // XenServer56Resource commands
        resources.put(XenServer56Resource.class, xenServer56Commands);

        // XenServer56FP1Resource commands
        resources.put(XenServer56FP1Resource.class, xenServer56P1Commands);

        // XenServer620SP1Resource commands
        resources.put(XenServer620SP1Resource.class, xenServer620SP1Commands);

        // XenServer610Resource commands
        resources.put(XenServer610Resource.class, xenServer610Commands);

        // XcpServerResource commands
        resources.put(XcpServerResource.class, xcpServerResourceCommand);
    }

    public static CitrixRequestWrapper getInstance() {
        return instance;
    }

    @SuppressWarnings({"rawtypes" })
    @Override
    public Answer execute(final Command command, final ServerResource serverResource) {
        final Class<? extends ServerResource> resourceClass = serverResource.getClass();

        final Hashtable<Class<? extends Command>, CommandWrapper> resourceCommands = retrieveResource(command, resourceClass);

        CommandWrapper<Command, Answer, ServerResource> commandWrapper = retrieveCommands(command.getClass(), resourceCommands);

        while (commandWrapper == null) {
            //Could not find the command in the given resource, will traverse the family tree.
            commandWrapper = retryWhenAllFails(command, resourceClass, resourceCommands);
        }

        return commandWrapper.execute(command, serverResource);
    }
}
