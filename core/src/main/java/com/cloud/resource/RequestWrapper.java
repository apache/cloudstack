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

package com.cloud.resource;

import java.text.MessageFormat;
import java.util.Hashtable;
import java.util.Set;

import org.apache.log4j.Logger;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;

public abstract class RequestWrapper {
    static public class CommandNotSupported extends NullPointerException {
        public CommandNotSupported(String msg) {
            super(msg);
        }
        public CommandNotSupported(String msg, Throwable cause) {
            super(msg);
            initCause(cause);
        }
    }

    private static final Logger s_logger = Logger.getLogger(RequestWrapper.class);

    @SuppressWarnings("rawtypes")
    protected Hashtable<Class<? extends ServerResource>, Hashtable<Class<? extends Command>, CommandWrapper>> resources = new Hashtable<Class<? extends ServerResource>, Hashtable<Class<? extends Command>, CommandWrapper>>();

    /**
     * @param command to be executed.
     * @return an Answer for the executed command.
     */
    public abstract Answer execute(Command command, ServerResource serverResource);

    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected Hashtable<Class<? extends Command>, CommandWrapper> retrieveResource(final Command command, final Class<? extends ServerResource> resourceClass) {
        Class<? extends ServerResource> keepResourceClass = resourceClass;
        Hashtable<Class<? extends Command>, CommandWrapper> resource = resources.get(keepResourceClass);
        while (resource == null) {
            try {
                final Class<? extends ServerResource> keepResourceClass2 = (Class<? extends ServerResource>) keepResourceClass.getSuperclass();
                resource = resources.get(keepResourceClass2);

                keepResourceClass = keepResourceClass2;
            } catch (final ClassCastException e) {
                throw new CommandNotSupported("No key found for '" + command.getClass() + "' in the Map!");
            }
        }
        return resource;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected CommandWrapper<Command, Answer, ServerResource> retrieveCommands(final Class<? extends Command> commandClass,
            final Hashtable<Class<? extends Command>, CommandWrapper> resourceCommands) {

        Class<? extends Command> keepCommandClass = commandClass;
        CommandWrapper<Command, Answer, ServerResource> commandWrapper = resourceCommands.get(keepCommandClass);
        while (commandWrapper == null) {
            try {
                final Class<? extends Command> commandClass2 = (Class<? extends Command>) keepCommandClass.getSuperclass();

                if (commandClass2 == null) {
                    throw new CommandNotSupported("All the COMMAND hierarchy tree has been visited but no compliant key has been found for '" + commandClass + "'.");
                }

                commandWrapper = resourceCommands.get(commandClass2);

                keepCommandClass = commandClass2;
            } catch (final ClassCastException e) {
                throw new CommandNotSupported("No key found for '" + keepCommandClass.getClass() + "' in the Map!");
            } catch (final NullPointerException e) {
                // Will now traverse all the resource hierarchy. Returning null
                // is not a problem.
                // It is all being nicely checked and in case we do not have a
                // resource, an Unsupported answer will be thrown by the base
                // class.
                return null;
            }
        }
        return commandWrapper;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected CommandWrapper<Command, Answer, ServerResource> retryWhenAllFails(final Command command, final Class<? extends ServerResource> resourceClass,
            final Hashtable<Class<? extends Command>, CommandWrapper> resourceCommands) {

        Class<? extends ServerResource> keepResourceClass = resourceClass;
        CommandWrapper<Command, Answer, ServerResource> commandWrapper = resourceCommands.get(command.getClass());
        while (commandWrapper == null) {
            // Could not find the command in the given resource, will traverse
            // the family tree.
            try {
                final Class<? extends ServerResource> resourceClass2 = (Class<? extends ServerResource>) keepResourceClass.getSuperclass();

                if (resourceClass2 == null) {
                    throw new CommandNotSupported("All the SERVER-RESOURCE hierarchy tree has been visited but no compliant key has been found for '" + command.getClass() + "'.");
                }

                final Hashtable<Class<? extends Command>, CommandWrapper> resourceCommands2 = retrieveResource(command,
                        (Class<? extends ServerResource>) keepResourceClass.getSuperclass());
                keepResourceClass = resourceClass2;

                commandWrapper = retrieveCommands(command.getClass(), resourceCommands2);
            } catch (final ClassCastException | NullPointerException e) {
                throw new CommandNotSupported("No key found for '" + command.getClass() + "' in the Map!", e);
            }
        }
        return commandWrapper;
    }

    @SuppressWarnings("rawtypes")
    protected Hashtable<Class<? extends Command>, CommandWrapper> processAnnotations(final Set<Class<? extends CommandWrapper>> wrappers) {
        final String errorMessage = "Error when adding Xen command to map ==> '{0}'. CommandWrapper class is ==> '{1}'";

        final Hashtable<Class<? extends Command>, CommandWrapper> commands = new Hashtable<Class<? extends Command>, CommandWrapper>();

        for (final Class<? extends CommandWrapper> wrapper : wrappers) {
            final ResourceWrapper annotation = wrapper.getAnnotation(ResourceWrapper.class);
            if (annotation == null) {
                // Just in case people add classes without the annotation in the package and we don't see it.
                continue;
            }
            try {
                commands.put(annotation.handles(), wrapper.newInstance());
            } catch (final InstantiationException e) {
                s_logger.warn(MessageFormat.format(errorMessage, e.getLocalizedMessage(), wrapper.toString()));
            } catch (final IllegalAccessException e) {
                s_logger.warn(MessageFormat.format(errorMessage, e.getLocalizedMessage(), wrapper.toString()));
            }
        }

        return commands;
    }
}
