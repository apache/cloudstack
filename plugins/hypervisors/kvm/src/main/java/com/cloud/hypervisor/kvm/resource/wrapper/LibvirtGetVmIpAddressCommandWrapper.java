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
import com.cloud.utils.Pair;
import com.cloud.utils.net.NetUtils;
import com.cloud.utils.script.Script;

@ResourceWrapper(handles =  GetVmIpAddressCommand.class)
public final class LibvirtGetVmIpAddressCommandWrapper extends CommandWrapper<GetVmIpAddressCommand, Answer, LibvirtComputingResource> {


    static String virsh_path = null;
    static String virt_win_reg_path = null;
    static String grep_path = null;
    static String awk_path = null;
    static String sed_path = null;
    static String virt_ls_path = null;
    static String virt_cat_path = null;
    static String tail_path = null;

    static void init() {
        virt_ls_path = Script.getExecutableAbsolutePath("virt-ls");
        virt_cat_path = Script.getExecutableAbsolutePath("virt-cat");
        virt_win_reg_path = Script.getExecutableAbsolutePath("virt-win-reg");
        tail_path = Script.getExecutableAbsolutePath("tail");
        grep_path = Script.getExecutableAbsolutePath("grep");
        awk_path = Script.getExecutableAbsolutePath("awk");
        sed_path = Script.getExecutableAbsolutePath("sed");
        virsh_path = Script.getExecutableAbsolutePath("virsh");
    }

    @Override
    public Answer execute(final GetVmIpAddressCommand command, final LibvirtComputingResource libvirtComputingResource) {
        String ip = null;
        boolean result = false;
        String vmName = command.getVmName();
        if (!NetUtils.verifyDomainNameLabel(vmName, true)) {
            return new Answer(command, result, ip);
        }

        String sanitizedVmName = sanitizeBashCommandArgument(vmName);
        String networkCidr = command.getVmNetworkCidr();
        String macAddress = command.getMacAddress();

        init();

        ip = ipFromDomIf(sanitizedVmName, networkCidr, macAddress);

        if (ip == null && networkCidr != null) {
            if(!command.isWindows()) {
                ip = ipFromDhcpLeaseFile(sanitizedVmName, networkCidr);
            } else {
                ip = ipFromWindowsRegistry(sanitizedVmName, networkCidr);
            }
        }

        if(ip != null){
            result = true;
            logger.debug("GetVmIp: "+ vmName + " Found Ip: "+ip);
        } else {
            logger.warn("GetVmIp: "+ vmName + " IP not found.");
        }

        return new Answer(command, result, ip);
    }

    private String ipFromDomIf(String sanitizedVmName, String networkCidr, String macAddress) {
        String ip = null;
        List<String[]> commands = new ArrayList<>();
        commands.add(new String[]{virsh_path, "domifaddr", sanitizedVmName, "--source", "agent"});
        Pair<Integer,String> response = executePipedCommands(commands, 0);
        if (response != null) {
            String output = response.second();
            Pair<String, String> ipAddresses = getIpAddresses(output, macAddress);
            String ipv4 = ipAddresses.first();
            if (networkCidr == null || NetUtils.isIpWithInCidrRange(ipv4, networkCidr)) {
                ip = ipv4;
            }
        } else {
            logger.error("ipFromDomIf: Command execution failed for VM: " + sanitizedVmName);
        }
        return ip;
    }

    private Pair<String, String> getIpAddresses(String output, String macAddress) {
        String ipv4 = null;
        String ipv6 = null;
        boolean found = false;
        String[] lines = output.split("\n");
        for (String line : lines) {
            String[] parts = line.replaceAll(" +", " ").trim().split(" ");
            if (parts.length < 4) {
                continue;
            }
            String device = parts[0];
            String mac = parts[1];
            if (found) {
                if (!device.equals("-") || !mac.equals("-")) {
                    break;
                }
            } else if (!mac.equals(macAddress)) {
                continue;
            }
            found = true;
            String ipFamily = parts[2];
            String ipPart = parts[3].split("/")[0];
            if (ipFamily.equals("ipv4")) {
                ipv4 = ipPart;
            } else if (ipFamily.equals("ipv6")) {
                ipv6 = ipPart;
            }
        }
        logger.debug(String.format("Found ipv4: %s and ipv6: %s with mac address %s", ipv4, ipv6, macAddress));
        return new Pair<>(ipv4, ipv6);
    }

    private String ipFromDhcpLeaseFile(String sanitizedVmName, String networkCidr) {
        String ip = null;
        List<String[]> commands = new ArrayList<>();
        commands.add(new String[]{virt_ls_path, sanitizedVmName, "/var/lib/dhclient/"});
        commands.add(new String[]{grep_path, ".*\\*.leases"});
        Pair<Integer,String> response = executePipedCommands(commands, 0);

        if(response != null && response.second() != null) {
            String leasesList = response.second();
            String[] leasesFiles = leasesList.split("\n");
            for(String leaseFile : leasesFiles){
                commands = new ArrayList<>();
                commands.add(new String[]{virt_cat_path, sanitizedVmName, "/var/lib/dhclient/" + leaseFile});
                commands.add(new String[]{tail_path, "-16"});
                commands.add(new String[]{grep_path, "fixed-address"});
                commands.add(new String[]{awk_path, "{print $2}"});
                commands.add(new String[]{sed_path, "-e", "s/;//"});
                String ipAddr = executePipedCommands(commands, 0).second();
                if((ipAddr != null) && NetUtils.isIpWithInCidrRange(ipAddr, networkCidr)) {
                    ip = ipAddr;
                    break;
                }
                logger.debug("GetVmIp: "+ sanitizedVmName + " Ip: "+ipAddr+" does not belong to network "+networkCidr);
            }
        } else {
            logger.error("ipFromDhcpLeaseFile: Command execution failed for VM: " + sanitizedVmName);
        }
        return ip;
    }

    private String ipFromWindowsRegistry(String sanitizedVmName, String networkCidr) {
        String ip = null;
        List<String[]> commands = new ArrayList<>();
        commands.add(new String[]{virt_win_reg_path, "--unsafe-printable-strings", sanitizedVmName, "HKEY_LOCAL_MACHINE\\SYSTEM\\ControlSet001\\Services\\Tcpip\\Parameters\\Interfaces"});
        commands.add(new String[]{grep_path, "DhcpIPAddress"});
        commands.add(new String[]{awk_path, "-F", ":", "{print $2}"});
        commands.add(new String[]{sed_path, "-e", "s/^\"//", "-e", "s/\"$//"});
        Pair<Integer,String> pair = executePipedCommands(commands, 0);
        if(pair != null && pair.second() != null) {
            String ipList = pair.second();
            ipList = ipList.replaceAll("\"", "");
            logger.debug("GetVmIp: "+ sanitizedVmName + "Ips: "+ipList);
            String[] ips = ipList.split("\n");
            for (String ipAddr : ips){
                if((ipAddr != null) && NetUtils.isIpWithInCidrRange(ipAddr, networkCidr)){
                    ip = ipAddr;
                    break;
                }
                logger.debug("GetVmIp: "+ sanitizedVmName + " Ip: "+ipAddr+" does not belong to network "+networkCidr);
            }
        } else {
            logger.error("ipFromWindowsRegistry: Command execution failed for VM: " + sanitizedVmName);
        }
        return ip;
    }

    static Pair<Integer, String> executePipedCommands(List<String[]> commands, long timeout) {
        return Script.executePipedCommands(commands, timeout);
    }
}
