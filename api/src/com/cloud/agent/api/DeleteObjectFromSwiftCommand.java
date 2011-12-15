/**
 * Copyright (C) 2011 Citrix Systems, Inc.  All rights reserved
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

import com.cloud.agent.api.LogLevel.Log4jLevel;
import com.cloud.agent.api.to.SwiftTO;

/**
 * 
 * @author Anthony Xu
 * 
 */

public class DeleteObjectFromSwiftCommand extends Command {
    @LogLevel(Log4jLevel.Off)
    private SwiftTO swift;
    private String container;
    private String object;

    protected DeleteObjectFromSwiftCommand() {
        
    }
   
    public DeleteObjectFromSwiftCommand(SwiftTO swift, String container, String object) {
        this.swift = swift;
        this.container = container;
        this.object = object;
    }

    public SwiftTO getSwift() {
        return this.swift;
    }

    public String getContainer() {
        return container;
    }

    public String getObject() {
        return object;
    }

    @Override
    public boolean executeInSequence() {
        // TODO Auto-generated method stub
        return true;
    }

}