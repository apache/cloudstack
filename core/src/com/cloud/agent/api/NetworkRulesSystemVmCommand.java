package com.cloud.agent.api;

import com.cloud.vm.VirtualMachine;

public class NetworkRulesSystemVmCommand extends Command {
    /**
     * Copyright (C) 2010 Cloud.com, Inc. All rights reserved.
     *
     * This software is licensed under the GNU General Public License v3 or later.
     *
     * It is free software: you can redistribute it and/or modify
     * it under the terms of the GNU General Public License as published by
     * the Free Software Foundation, either version 3 of the License, or any later version.
     * This program is distributed in the hope that it will be useful,
     * but WITHOUT ANY WARRANTY; without even the implied warranty of
     * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
     * GNU General Public License for more details.
     *
     * You should have received a copy of the GNU General Public License
     * along with this program. If not, see <http://www.gnu.org/licenses/>.
     *
     */
    
     private String vmName;
     private long vmId;
     private String pubIp;
     private String mac;
     private VirtualMachine.Type type;
    
     protected NetworkRulesSystemVmCommand() {
    
     }
    
     public NetworkRulesSystemVmCommand(String vmName, VirtualMachine.Type type) {
         this.vmName = vmName;
     }
     
     public NetworkRulesSystemVmCommand(String vmName, long vmId, String publicIP, String mac, VirtualMachine.Type type) {
         this.vmName = vmName;
         this.vmId = vmId;
         this.pubIp = publicIP;
         this.mac = mac;
         this.type = type;
     }
    
     public String getVmName() {
         return vmName;
     }
     
     public long getVmId() {
         return vmId;
     }
     
     public String getIp() {
         return pubIp;
     }
    
     public String getMac() {
         return mac;
     }
     
     public VirtualMachine.Type getType() {
         return type;
     }
     @Override
     public boolean executeInSequence() {
     return false;
     }
}
