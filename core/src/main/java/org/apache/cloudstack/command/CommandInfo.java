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

import com.cloud.agent.api.Command;
import com.cloud.serializer.GsonHelper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.Date;

public class CommandInfo {
    public static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss.SSSZ";
    public static final Gson GSON = GsonHelper.setDefaultGsonConfig(new GsonBuilder().setDateFormat(DATE_FORMAT));

    long requestSeq;
    Command.State state;
    Date startTime;
    Date updateTime;
    String commandName;
    String command;
    int timeout;
    String answerName;
    String answer;

    public CommandInfo() {
    }

    public CommandInfo(long requestSeq, Command command, Command.State state) {
        this.requestSeq = requestSeq;
        this.state = state;
        this.startTime = this.updateTime = new Date();
        this.commandName = command.getClass().getName();
        this.command = GSON.toJson(command);
        this.timeout = command.getWait();
    }

    public long getRequestSeq() {
        return requestSeq;
    }

    public void setRequestSeq(long requestSeq) {
        this.requestSeq = requestSeq;
    }

    public Command.State getState() {
        return state;
    }

    public void setState(Command.State state) {
        this.state = state;
        this.updateTime = new Date();
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public String getCommandName() {
        return commandName;
    }

    public void setCommandName(String commandName) {
        this.commandName = commandName;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public String getAnswerName() {
        return answerName;
    }

    public void setAnswerName(String answerName) {
        this.answerName = answerName;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }
}
