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
package org.apache.cloudstack.command;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonSyntaxException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;

public class ReconcileCommandUtils {

    protected static final Logger LOGGER = LogManager.getLogger(ReconcileCommandUtils.class.getName());

    public static void createLogFileForCommand(final String logPath, final Command cmd) {
        updateLogFileForCommand(logPath, cmd, Command.State.CREATED);
    }

    public static void updateLogFileForCommand(final String logPath, final Command cmd, final Command.State state) {
        if (cmd.isReconcile()) {
            String logFileName = getLogFileNameForCommand(logPath, cmd);
            LOGGER.debug(String.format("Updating log file %s with %s state", logFileName, state));
            File logFile = new File(logFileName);
            CommandInfo commandInfo = null;
            if (logFile.exists()) {
                commandInfo = readLogFileForCommand(logFileName);
                logFile.delete();
            }
            if (commandInfo == null) {
                commandInfo = new CommandInfo(cmd.getRequestSequence(), cmd, state);
            } else {
                commandInfo.setState(state);
            }
            try {
                BufferedWriter writer = new BufferedWriter(new FileWriter(logFile));
                writer.write(CommandInfo.GSON.toJson(commandInfo));
                writer.close();
            } catch (IOException e) {
                LOGGER.error(String.format("Failed to write log file %s", logFile));
            }
        }
    }

    public static void updateLogFileForCommand(final String logFullPath, final Command.State state) {
        File logFile = new File(logFullPath);
        LOGGER.debug(String.format("Updating log file %s with %s state", logFile.getName(), state));
        if (!logFile.exists()) {
            return;
        }
        CommandInfo commandInfo = readLogFileForCommand(logFullPath);
        if (commandInfo != null) {
            commandInfo.setState(state);
        }
        logFile.delete();
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(logFile));
            writer.write(CommandInfo.GSON.toJson(commandInfo));
            writer.close();
        } catch (IOException e) {
            LOGGER.error(String.format("Failed to write log file %s", logFile));
        }
    }

    public static void deleteLogFileForCommand(final String logPath, final Command cmd) {
        if (cmd.isReconcile()) {
            File logFile = new File(getLogFileNameForCommand(logPath, cmd));
            LOGGER.debug(String.format("Removing log file %s", logFile.getName()));
            if (logFile.exists()) {
                logFile.delete();
            }
        }
    }

    public static void deleteLogFile(final String logFullPath) {
        File logFile = new File(logFullPath);
        LOGGER.debug(String.format("Removing log file %s ", logFile.getName()));
        if (logFile.exists()) {
            logFile.delete();
        }
    }

    public static String getLogFileNameForCommand(final String logPath, final Command cmd) {
        return String.format("%s/%s-%s.json", logPath, cmd.getRequestSequence(), cmd);
    }

    public static CommandInfo readLogFileForCommand(final String logFullPath) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            SimpleDateFormat df = new SimpleDateFormat(CommandInfo.DATE_FORMAT);
            objectMapper.setDateFormat(df);
            return objectMapper.readValue(new File(logFullPath), CommandInfo.class);
        } catch (IOException e) {
            LOGGER.error(String.format("Failed to read log file %s: %s", logFullPath, e.getMessage()));
            return null;
        }
    }

    public static Command parseCommandInfo(final CommandInfo commandInfo) {
        if (commandInfo.getCommandName() == null || commandInfo.getCommand() == null) {
            return null;
        }
        return parseCommandInfo(commandInfo.getCommandName(), commandInfo.getCommand());
    }

    public static Command parseCommandInfo(final String commandName, final String commandInfo) {
        Object parsedObject = null;
        try {
            Class<?> commandClazz = Class.forName(commandName);
            parsedObject = CommandInfo.GSON.fromJson(commandInfo, commandClazz);
        } catch (ClassNotFoundException | JsonSyntaxException e) {
            LOGGER.error(String.format("Failed to parse command from CommandInfo %s due to %s", commandInfo, e.getMessage()));
        }
        if (parsedObject != null) {
            return (Command) parsedObject;
        }
        return null;
    }

    public static Answer parseAnswerFromCommandInfo(final CommandInfo commandInfo) {
        if (commandInfo.getAnswerName() == null || commandInfo.getAnswer() == null) {
            return null;
        }
        return parseAnswerFromAnswerInfo(commandInfo.getAnswerName(), commandInfo.getAnswer());
    }

    public static Answer parseAnswerFromAnswerInfo(final String answerName, final String answerInfo) {
        Object parsedObject = null;
        try {
            Class<?> commandClazz = Class.forName(answerName);
            parsedObject = CommandInfo.GSON.fromJson(answerInfo, commandClazz);
        } catch (ClassNotFoundException | JsonSyntaxException e) {
            LOGGER.error(String.format("Failed to parse answer from answerInfo %s due to %s", answerInfo, e.getMessage()));
        }
        if (parsedObject != null) {
            return (Answer) parsedObject;
        }
        return null;
    }

    public static void updateLogFileWithAnswerForCommand(final String logPath, final Command cmd, final Answer answer) {
        if (cmd.isReconcile()) {
            String logFileName = getLogFileNameForCommand(logPath, cmd);
            LOGGER.debug(String.format("Updating log file %s with answer %s", logFileName, answer));
            File logFile = new File(logFileName);
            if (!logFile.exists()) {
                return;
            }
            CommandInfo commandInfo = readLogFileForCommand(logFile.getAbsolutePath());
            if (commandInfo == null) {
                return;
            }
            if (Command.State.STARTED.equals(commandInfo.getState())) {
                if (answer.getResult()) {
                    commandInfo.setState(Command.State.COMPLETED);
                } else {
                    commandInfo.setState(Command.State.FAILED);
                }
            }
            commandInfo.setAnswerName(answer.toString());
            commandInfo.setAnswer(CommandInfo.GSON.toJson(answer));
            try {
                BufferedWriter writer = new BufferedWriter(new FileWriter(logFile));
                writer.write(CommandInfo.GSON.toJson(commandInfo));
                writer.close();
            } catch (IOException e) {
                LOGGER.error(String.format("Failed to write log file %s", logFile));
            }
        }
    }
}
