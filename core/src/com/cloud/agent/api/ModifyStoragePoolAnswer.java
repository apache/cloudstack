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

import java.util.Map;

import com.cloud.storage.template.TemplateInfo;

/**
 * @author chiradeep
 * 
 */
public class ModifyStoragePoolAnswer extends Answer {
    StoragePoolInfo poolInfo;
    Map<String, TemplateInfo> templateInfo;

    protected ModifyStoragePoolAnswer() {
    }

    public ModifyStoragePoolAnswer(ModifyStoragePoolCommand cmd, long capacityBytes, long availableBytes, Map<String, TemplateInfo> tInfo) {
        super(cmd);
        this.result = true;
        this.poolInfo = new StoragePoolInfo(null, cmd.getPool().getName(),
                cmd.getPool().getHostAddress(), cmd.getPool().getPath(), cmd.getLocalPath(), 
                cmd.getPool().getPoolType(), capacityBytes, availableBytes );
       
        this.templateInfo = tInfo;
    }
    
    public StoragePoolInfo getPoolInfo() {
       return poolInfo;
    }

    public void setPoolInfo(StoragePoolInfo poolInfo) {
        this.poolInfo = poolInfo;
    }


    public Map<String, TemplateInfo> getTemplateInfo() {
        return templateInfo;
    }

    public void setTemplateInfo(Map<String, TemplateInfo> templateInfo) {
        this.templateInfo = templateInfo;
    }

}
