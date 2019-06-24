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
package com.cloud.agent.manager;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.Command.OnError;
import com.cloud.utils.exception.CloudRuntimeException;

public class Commands implements Iterable<Command> {
    OnError _handler;
    private final ArrayList<String> _ids = new ArrayList<String>();
    private final ArrayList<Command> _cmds = new ArrayList<Command>();
    private Answer[] _answers;

    public Commands(OnError handler) {
        _handler = handler;
    }

    public Commands(Command cmd) {
        this(OnError.Stop);
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
        assert (clazz != Answer.class) : "How do you expect to get a unique answer in this case?  huh?  How? How? How?....one more time....How?";
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
        _answers = answers;
    }

    public OnError getErrorHandling() {
        return _handler;
    }

    public boolean stopOnError() {
        return _handler == OnError.Stop;
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

    @Override
    public Iterator<Command> iterator() {
        return _cmds.iterator();
    }
}
