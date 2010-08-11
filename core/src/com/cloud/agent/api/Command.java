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


/**
 * Command is a command that is sent between the management agent and management
 * server. Parameter and Command are loosely connected. The protocol layer does
 * not care what parameter is carried with which command. That tie in is made at
 * a higher level than here.
 * 
 * Parameter names can only be 4 characters long and is checked with an assert.
 * The value of the parameter is basically an arbitrary length byte array.
 */
public abstract class Command {

    protected Command() {
    }
    
    public String toString() {
        return this.getClass().getSimpleName();
    }
    
    public abstract boolean executeInSequence();
}
