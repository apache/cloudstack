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

package com.cloud.network.utils;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.resource.ServerResource;

public class CommandRetryUtility {

    private static final Logger s_logger = Logger.getLogger(CommandRetryUtility.class);

    private static final int ZERO = 0;
    private static CommandRetryUtility instance;

    static {
        instance = new CommandRetryUtility();
    }

    private final ConcurrentHashMap<Command, Integer> commandsToRetry;
    private ServerResource serverResource;

    private CommandRetryUtility() {
        commandsToRetry = new ConcurrentHashMap<Command, Integer>();
    }

    public static CommandRetryUtility getInstance() {
        return instance;
    }

    public void setServerResource(final ServerResource serverResource) {
        this.serverResource = serverResource;
    }

    public boolean addRetry(final Command command, final int retries) {
        if (commandsToRetry.containsKey(command)) {
            // A retry already exists for this command, do not add it again.
            // Once all retries are executed, the command will be removed from the map.
            return false;
        }
        commandsToRetry.put(command, retries);
        return true;
    }

    public Answer retry(final Command command, final Class<? extends Answer> answerClass, final Exception error) {
        if (commandsToRetry.containsKey(command)) {
            Integer numRetries = commandsToRetry.get(command);

            if (numRetries > ZERO) {
                commandsToRetry.put(command, --numRetries);

                s_logger.warn("Retrying " + command.getClass().getSimpleName() + ". Number of retries remaining: " + numRetries);

                return serverResource.executeRequest(command);
            } else {
                commandsToRetry.remove(command);
            }
        }
        try {
            final Constructor<? extends Answer> answerConstructor = answerClass.getConstructor(Command.class, Exception.class);
            return answerConstructor.newInstance(command, error);
        } catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            return Answer.createUnsupportedCommandAnswer(command);
        }
    }
}
