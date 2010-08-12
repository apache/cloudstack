/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */
package com.cloud.exception;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.utils.SerialVersionUID;

public class UnableToExecuteException extends Exception {
    private static final long serialVersionUID = SerialVersionUID.UnableToExecuteException;
    
    Command _cmd;
    Answer _answer;
    
    public UnableToExecuteException(Command cmd, String msg) {
        this(cmd, msg, null);
    }
    
    public UnableToExecuteException(Command cmd, String msg, Throwable cause) {
        this(cmd, null, msg, cause);
    }
    
    public UnableToExecuteException(Command cmd, Answer answer, String msg, Throwable cause) {
        super(msg, cause);
        _cmd = cmd;
        _answer = answer;
    }
    
    public Command getCommand() {
        return _cmd;
    }
    
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Msg: ").append(getMessage()).append("; ");
        builder.append("Cmd: ").append(_cmd.toString()).append("; ");
        builder.append("Ans: ").append(_answer != null ? _answer.toString() : "").append("; ");
        return builder.toString();
    }
}
