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

public class CheckOnVmAnswer extends Answer {
    boolean determined;
    boolean alive;
    
    protected CheckOnVmAnswer() {
    }
    
    public CheckOnVmAnswer(CheckOnVmCommand cmd, Boolean alive) {
        this(cmd, alive, null);
    }
    
    public CheckOnVmAnswer(CheckOnVmCommand cmd, Boolean alive, String details) {
        super(cmd, true, details);
        if (alive == null) {
            determined = false;
        } else {
            determined = true;
            this.alive = alive;
        }
    }
    
    public CheckOnVmAnswer(CheckOnVmCommand cmd, Exception e) {
        super(cmd, e);
    }

    public boolean isDetermined() {
        return determined;
    }

    public boolean isAlive() {
        return alive;
    }
}
