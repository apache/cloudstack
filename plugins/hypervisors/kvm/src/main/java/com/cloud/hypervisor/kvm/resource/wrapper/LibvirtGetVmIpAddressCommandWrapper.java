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

import java.util.ArrayList;
import java.util.List;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.GetVmIpAddressCommand;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.utils.net.NetUtils;
import com.cloud.utils.script.Script;

@ResourceWrapper(handles =  GetVmIpAddressCommand.class)
public final class LibvirtGetVmIpAddressCommandWrapper extends CommandWrapper<GetVmIpAddressCommand, Answer, LibvirtComputingResource> {


    @Override
    public Answer execute(final GetVmIpAddressCommand command, final LibvirtComputingResource libvirtComputingResource) {
        String ip = null;
        boolean result = false;
        String vmName = command.getVmName();
        String sanitizedVmName = sanitizeBashCommandArgument(vmName);
        String networkCidr = command.getVmNetworkCidr();
        List<String[]> commands = new ArrayList<>();
        final String virt_ls_path = Script.getExecutableAbsolutePath("virt-ls");
        final String virt_cat_path = Script.getExecutableAbsolutePath("virt-cat");
        final String virt_win_reg_path = Script.getExecutableAbsolutePath("virt-win-reg");
        final String tail_path = Script.getExecutableAbsolutePath("tail");
        final String grep_path = Script.getExecutableAbsolutePath("grep");
        final String awk_path = Script.getExecutableAbsolutePath("awk");
        final String sed_path = Script.getExecutableAbsolutePath("sed");
        if(!command.isWindows()) {
            //List all dhcp lease files inside guestVm
            commands.add(new String[]{virt_ls_path, sanitizedVmName, "/var/lib/dhclient/"});
            commands.add(new String[]{grep_path, ".*\\*.leases"});
            String leasesList = Script.executePipedCommands(commands, 0).second();
            if(leasesList != null) {
                String[] leasesFiles = leasesList.split("\n");
                for(String leaseFile : leasesFiles){
                    //Read from each dhclient lease file inside guest Vm using virt-cat libguestfs utility
                    commands = new ArrayList<>();
                    commands.add(new String[]{virt_cat_path, sanitizedVmName, "/var/lib/dhclient/" + leaseFile});
                    commands.add(new String[]{tail_path, "-16"});
                    commands.add(new String[]{grep_path, "fixed-address"});
                    commands.add(new String[]{awk_path, "{print $2}"});
                    commands.add(new String[]{sed_path, "-e", "s/;//"});
                    String ipAddr = Script.executePipedCommands(commands, 0).second();
                    // Check if the IP belongs to the network
                    if((ipAddr != null) && NetUtils.isIpWithInCidrRange(ipAddr, networkCidr)) {
                        ip = ipAddr;
                        break;
                    }
                    logger.debug("GetVmIp: "+ vmName + " Ip: "+ipAddr+" does not belong to network "+networkCidr);
                }
            }
        } else {
            // For windows, read from guest Vm registry using virt-win-reg libguestfs ulitiy. Registry Path: HKEY_LOCAL_MACHINE\SYSTEM\ControlSet001\Services\Tcpip\Parameters\Interfaces\<service>\DhcpIPAddress
            commands = new ArrayList<>();
            commands.add(new String[]{virt_win_reg_path, "--unsafe-printable-strings", sanitizedVmName, "HKEY_LOCAL_MACHINE\\SYSTEM\\ControlSet001\\Services\\Tcpip\\Parameters\\Interfaces"});
            commands.add(new String[]{grep_path, "DhcpIPAddress"});
            commands.add(new String[]{awk_path, "-F", ":", "{print $2}"});
            commands.add(new String[]{sed_path, "-e", "s/^\"//", "-e", "s/\"$//"});
            String ipList = Script.executePipedCommands(commands, 0).second();
            if(ipList != null) {
                logger.debug("GetVmIp: "+ vmName + "Ips: "+ipList);
                String[] ips = ipList.split("\n");
                for (String ipAddr : ips){
                    // Check if the IP belongs to the network
                    if((ipAddr != null) && NetUtils.isIpWithInCidrRange(ipAddr, networkCidr)){
                        ip = ipAddr;
                        break;
                    }
                    logger.debug("GetVmIp: "+ vmName + " Ip: "+ipAddr+" does not belong to network "+networkCidr);
                }
            }
        }
        if(ip != null){
            result = true;
            logger.debug("GetVmIp: "+ vmName + " Found Ip: "+ip);
        }
        return new Answer(command, result, ip);
    }
}
