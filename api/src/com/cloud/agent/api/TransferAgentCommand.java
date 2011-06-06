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

import com.cloud.host.Status.Event;

public class TransferAgentCommand extends Command {
    protected long agentId;
    protected long futureOwner;
    protected long currentOwner;
    Event event;
    
    protected TransferAgentCommand() {
    }
    
    public TransferAgentCommand(long agentId, long currentOwner, long futureOwner, Event event) {
        this.agentId = agentId;
        this.currentOwner = currentOwner;
        this.futureOwner = futureOwner;
        this.event = event;
    }

    public long getAgentId() {
        return agentId;
    }

    public long getFutureOwner() {
        return futureOwner;
    }

    public Event getEvent() {
        return event;
    }

    public long getCurrentOwner() {
        return currentOwner;
    }

    @Override
    public boolean executeInSequence() {
        return false;
    }
}
