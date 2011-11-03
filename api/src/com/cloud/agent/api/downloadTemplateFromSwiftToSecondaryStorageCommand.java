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

public class downloadTemplateFromSwiftToSecondaryStorageCommand extends Command {
    @LogLevel(Log4jLevel.Off)
    private SwiftTO swift;
    private String secondaryStorageUrl;

    private Long dcId;
    private Long accountId;
    private Long templateId;

    protected downloadTemplateFromSwiftToSecondaryStorageCommand() {
        
    }
   
    public downloadTemplateFromSwiftToSecondaryStorageCommand(SwiftTO swift, String secondaryStorageUrl, Long dcId, Long accountId, Long templateId, int wait) {

        this.swift = swift;
        this.secondaryStorageUrl = secondaryStorageUrl;
        this.dcId = dcId;
        this.accountId = accountId;
        this.templateId = templateId;
        setWait(wait);
    }

    public SwiftTO getSwift() {
        return this.swift;
    }

    public void setSwift(SwiftTO swift) {
        this.swift = swift;
    }

    public String getSecondaryStorageUrl() {
        return secondaryStorageUrl;
    }

    public Long getDcId() {
        return dcId;
    }

    public Long getAccountId() {
        return accountId;
    }

    public Long getTemplateId() {
        return templateId;
    }

    @Override
    public boolean executeInSequence() {
        // TODO Auto-generated method stub
        return true;
    }

}