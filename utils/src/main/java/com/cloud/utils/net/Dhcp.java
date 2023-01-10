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

package com.cloud.utils.net;

import java.util.Arrays;

public class Dhcp {
    public enum DhcpOptionCode {
        NETMASK(1, "netmask"),
        TIME_OFFSET(2, "time-offset"),
        ROUTER(3, "router"),
        TIME_SERVER(4, "time-server"),
        DNS_SERVER(6, "dns-server"),
        logger_SERVER(7, "log-server"),
        LPR_SERVER(9, "lpr-server"),
        HOSTNAME(12, "hostname"),
        BOOT_FILE_SIZE(13, "boot-file-size"),
        DOMAIN_NAME(15, "domain-name"),
        SWAP_SERVER(16, "swap-server"),
        ROOT_PATH(17, "root-path"),
        EXTENSION_PATH(18, "extension-path"),
        IP_FORWARD_ENABLE(19, "ip-forward-enable"),
        NON_LOCAL_SOURCE_ROUTING(20, "non-local-source-routing"),
        POLICY_FILTER(21, "policy-filter"),
        MAX_DATAGRAM_REASSEMBLY(22, "max-datagram-reassembly"),
        DEFAULT_TTL(23, "default-ttl"),
        MTU(26, "mtu"),
        ALL_SUBNETS_LOCAL(27, "all-subnets-local"),
        BROADCAST(28, "broadcast"),
        ROUTER_DISCOVERY(31, "router-discovery"),
        ROUTER_SOLICITATION(32, "router-solicitation"),
        STATIC_ROUTE(33, "static-route"),
        TRAILER_ENCAPSULATION(34, "trailer-encapsulation"),
        ARP_TIMEOUT(35, "arp-timeout"),
        ETHERNET_ENCAP(36, "ethernet-encap"),
        TCP_TTL(37, "tcp-ttl"),
        TCP_KEEPALIVE(38, "tcp-keepalive"),
        NIS_DOMAIN(40, "nis-domain"),
        NIS_SERVER(41, "nis-server"),
        NTP_SERVER(42, "ntp-server"),
        VENDOR_ENCAP(43, "vendor-encap"),
        NETBIOS_NS(44, "netbios-ns"),
        NETBIOS_DD(45, "netbios-dd"),
        NETBIOS_NODETYPE(46, "netbios-nodetype"),
        NETBIOS_SCOPE(47, "netbios-scope"),
        X_WINDOWS_FS(48, "x-windows-fs"),
        X_WINDOWS_DM(49, "x-windows-dm"),
        REQUESTED_ADDRESS(50, "requested-address"),
        LEASE_TIME(51, "lease-time"),
        OPTION_OVERLOAD(52, "option-overload"),
        MESSAGE_TYPE(53, "message-type"),
        SERVER_IDENTIFIER(54, "server-identifier"),
        PARAMETER_REQUEST(55, "parameter-request"),
        MESSAGE(56, "message"),
        MAX_MESSAGE_SIZE(57, "max-message-size"),
        T1(58, "T1"),
        T2(59, "T2"),
        VENDOR_CLASS(60, "vendor-class"),
        CLIENT_ID(61, "client-id"),
        NISPLUS_DOMAIN(64, "nis+-domain"),
        NISPLUS_SERVER(65, "nis+-server"),
        TFTP_SERVER(66, "tftp-server"),
        BOOTFILE_NAME(67, "bootfile-name"),
        MOBILE_IP_HOME(68, "mobile-ip-home"),
        SMTP_SERVER(69, "smtp-server"),
        POP3_SERVER(70, "pop3-server"),
        NNTP_SERVER(71, "nntp-server"),
        IRC_SERVER(74, "irc-server"),
        USER_CLASS(77, "user-class"),
        CLIENT_ARCH(93, "client-arch"),
        CLIENT_INTERFACE_ID(94, "client-interface-id"),
        CLIENT_MACHINE_ID(97, "client-machine-id"),
        URL(114, "url"),
        DOMAIN_SEARCH(119, "domain-search"),
        SIP_SERVER(120, "sip-server"),
        CLASSLESS_STATIC_ROUTE(121, "classless-static-route"),
        VENDOR_ID_ENCAP(125, "vendor-id-encap"),
        SERVER_IP_ADDRESS(255, "server-ip-address");

        private int code;
        private String name;

        DhcpOptionCode(int code, String name){
            this.code = code;
            this.name = name;
        }

        public int getCode() {
            return code;
        }

        public String getName() { return name; }

        public static DhcpOptionCode valueOfInt(int code) {
            return Arrays.stream(DhcpOptionCode.values())
                    .filter(option -> option.getCode() == code)
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Dhcp option code " + code + " not supported."));
        }

        public static DhcpOptionCode valueOfString(String name) {
            return Arrays.stream(DhcpOptionCode.values())
                    .filter(option -> option.getName().equals(name))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Dhcp option " + name + " not supported."));
        }
    }
}
