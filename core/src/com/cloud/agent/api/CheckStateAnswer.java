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
package com.cloud.agent.api;

import com.cloud.vm.State;

/**
 *  This returns an answer on the state of the VM.  If the state, is Error
 *  or Unknown, the details should give more details on what is wrong.
 */
public class CheckStateAnswer extends Answer {
    State state;

    public CheckStateAnswer() {}

    public CheckStateAnswer(CheckStateCommand cmd, State state) {
        this(cmd, state, null);
    }
    
    public CheckStateAnswer(CheckStateCommand cmd, String details) {
        super(cmd, false, details);
        this.state = null;
    }
    
    public CheckStateAnswer(CheckStateCommand cmd, State state, String details) {
        super(cmd, true, details);
        this.state = state;
    }
    
    public State getState() {
        return state;
    }
}
