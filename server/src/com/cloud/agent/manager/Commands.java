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
package com.cloud.agent.manager;

import java.util.ArrayList;
import java.util.List;

import com.cloud.agent.AgentManager.OnError;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.utils.exception.CloudRuntimeException;

public class Commands {
    OnError _handler;
    private ArrayList<String> _ids = new ArrayList<String>();
    private ArrayList<Command> _cmds = new ArrayList<Command>();
    private Answer[] _answers;
    
    public Commands(OnError handler) {
        _handler = handler;
    }
    
    public Commands(Command cmd) {
        this(OnError.Revert);
        addCommand(cmd);
    }
    
    public void addCommands(List<Command> cmds) {
        int i = 0;
        for (Command cmd : cmds) {
            addCommand(Integer.toString(i++), cmd);
        }
    }
    
    public int size() {
        return _cmds.size();
    }
    
    public void addCommand(String id, Command cmd) {
        _ids.add(id);
        _cmds.add(cmd);
    }
    
    public void addCommand(Command cmd) {
        addCommand(null, cmd);
    }
    
    public void addCommand(int index, Command cmd) {
    	_cmds.add(index, cmd);
    }
    
    public Answer getAnswer(String id) {
        int i = _ids.indexOf(id);
        return i == -1 ? null : _answers[i];
    }

    @SuppressWarnings("unchecked")
    public <T extends Answer> T getAnswer(Class<T> clazz) {
        assert(clazz != Answer.class) : "How do you expect to get a unique answer in this case?  huh?  How? How? How?....one more time....How?";
        for (Answer answer : _answers) {
            if (answer.getClass() == clazz) {
                return (T)answer;
            }
        }
        throw new CloudRuntimeException("Unable to get answer that is of " + clazz);
    }
    
    public <T extends Command> Answer getAnswerFor(Class<T> clazz) {
        assert (clazz != Command.class) : "You passed in a generic Command.  Seriously, you think you did that?";
        int i = 0;
        for (Command cmd : _cmds) {
            if (cmd.getClass() == clazz) {
                break;
            }
            i++;
        }
        
        assert i < _cmds.size() : "You sure you actually sent this command " + clazz;
        
        return _answers[i];
    }
    
    public Command[] toCommands() {
        return _cmds.toArray(new Command[_cmds.size()]);
    }
    
    public void setAnswers(Answer[] answers) {
        assert answers.length == _cmds.size() : "We didn't get back the same number of answers as commands sent";
        _answers = answers;
    }
    
    public OnError getErrorHandling() {
        return _handler;
    }
    
    public boolean stopOnError() {
        return _handler == OnError.Revert || _handler == OnError.Stop;
    }
    
    public boolean revertOnError() {
        return _handler == OnError.Revert;
    }
    
    public Answer[] getAnswers() {
        return _answers;
    }
    
    @SuppressWarnings("unchecked")
    public <T extends Command> T getCommand(Class<T> clazz) {
        for (Command cmd : _cmds) {
            if (cmd.getClass() == clazz) {
                return (T)cmd;
            }
        }
        return null;
    }
    
    /**
     * @return For Commands with handler OnError.Continue, one command succeeding is successful.  If not, all commands must succeed to be successful. 
     */
    public boolean isSuccessful() {
        if (_answers == null) {
            return false;
        }
        if (_handler == OnError.Continue) {
            return true;
        }
        for (Answer answer : _answers) {
            if (_handler == OnError.Continue && answer.getResult()) {
                return true;
            } else if (_handler != OnError.Continue && !answer.getResult()) {
                return false;
            }
        }
        
        return _handler != OnError.Continue;
    }
}
