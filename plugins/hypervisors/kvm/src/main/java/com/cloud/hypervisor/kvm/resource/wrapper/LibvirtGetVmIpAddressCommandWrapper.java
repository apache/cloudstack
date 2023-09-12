//
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
//

package com.cloud.hypervisor.kvm.resource.wrapper;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.GetVmIpAddressCommand;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.utils.net.NetUtils;
import com.cloud.utils.script.Script;
import org.apache.log4j.Logger;

@ResourceWrapper(handles =  GetVmIpAddressCommand.class)
public final class LibvirtGetVmIpAddressCommandWrapper extends CommandWrapper<GetVmIpAddressCommand, Answer, LibvirtComputingResource> {

    private static final Logger s_logger = Logger.getLogger(LibvirtGetVmIpAddressCommandWrapper.class);

    @Override
    public Answer execute(final GetVmIpAddressCommand command, final LibvirtComputingResource libvirtComputingResource) {
        String ip = null;
        boolean result = false;
        String networkCidr = command.getVmNetworkCidr();
        if(!command.isWindows()) {
            //List all dhcp lease files inside guestVm
            String leasesList = Script.runSimpleBashScript(new StringBuilder().append("virt-ls ").append(command.getVmName())
                    .append(" /var/lib/dhclient/ | grep .*\\*.leases").toString());
            if(leasesList != null) {
                String[] leasesFiles = leasesList.split("\n");
                for(String leaseFile : leasesFiles){
                    //Read from each dhclient lease file inside guest Vm using virt-cat libguestfs ulitiy
                    String ipAddr = Script.runSimpleBashScript(new StringBuilder().append("virt-cat ").append(command.getVmName())
                            .append(" /var/lib/dhclient/" + leaseFile + " | tail -16 | grep 'fixed-address' | awk '{print $2}' | sed -e 's/;//'").toString());
                    // Check if the IP belongs to the network
                    if((ipAddr != null) && NetUtils.isIpWithInCidrRange(ipAddr, networkCidr)){
                        ip = ipAddr;
                        break;
                    }
                    s_logger.debug("GetVmIp: "+command.getVmName()+ " Ip: "+ipAddr+" does not belong to network "+networkCidr);
                }
            }
        } else {
            // For windows, read from guest Vm registry using virt-win-reg libguestfs ulitiy. Registry Path: HKEY_LOCAL_MACHINE\SYSTEM\ControlSet001\Services\Tcpip\Parameters\Interfaces\<service>\DhcpIPAddress
            String ipList = Script.runSimpleBashScript(new StringBuilder().append("virt-win-reg --unsafe-printable-strings ").append(command.getVmName())
                    .append(" 'HKEY_LOCAL_MACHINE\\SYSTEM\\ControlSet001\\Services\\Tcpip\\Parameters\\Interfaces' | grep DhcpIPAddress | awk -F : '{print $2}' | sed -e 's/^\"//' -e 's/\"$//'").toString());
            if(ipList != null) {
                s_logger.debug("GetVmIp: "+command.getVmName()+ "Ips: "+ipList);
                String[] ips = ipList.split("\n");
                for (String ipAddr : ips){
                    // Check if the IP belongs to the network
                    if((ipAddr != null) && NetUtils.isIpWithInCidrRange(ipAddr, networkCidr)){
                        ip = ipAddr;
                        break;
                    }
                    s_logger.debug("GetVmIp: "+command.getVmName()+ " Ip: "+ipAddr+" does not belong to network "+networkCidr);
                }
            }
        }
        if(ip != null){
            result = true;
            s_logger.debug("GetVmIp: "+command.getVmName()+ " Found Ip: "+ip);
        }
        return new Answer(command, result, ip);
    }
}
