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
package com.cloud.network.resource;

import java.util.Hashtable;
import java.util.Set;

import org.reflections.Reflections;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.RequestWrapper;
import com.cloud.resource.ServerResource;

public class NiciraNvpRequestWrapper extends RequestWrapper {

    private static NiciraNvpRequestWrapper instance;

    static {
        instance = new NiciraNvpRequestWrapper();
    }

    Reflections baseWrappers = new Reflections("com.cloud.network.resource.wrapper");
    @SuppressWarnings("rawtypes")
    Set<Class<? extends CommandWrapper>> baseSet = baseWrappers.getSubTypesOf(CommandWrapper.class);

    private NiciraNvpRequestWrapper() {
        init();
    }

    @SuppressWarnings("rawtypes")
    private void init() {
        // NiciraNvpResource commands
        final Hashtable<Class<? extends Command>, CommandWrapper> niciraCommands = processAnnotations(baseSet);

        resources.put(NiciraNvpResource.class, niciraCommands);
    }

    public static NiciraNvpRequestWrapper getInstance() {
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