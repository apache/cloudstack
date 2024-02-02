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
package com.cloud.utils.net;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Network protocols and parameters.
 * see <a href="https://www.iana.org/protocols">Protocol Registries</a>
 *
 */
public class NetworkProtocols {

    public enum Option {
        ProtocolNumber, IcmpType;

        public static Option getOption(String value) {
            return Arrays.stream(Option.values())
                    .filter(option -> option.name().equalsIgnoreCase(value))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(String.format("Option " + value + " is not supported. Supported values are %s", Arrays.toString(Option.values()))));
        }
    }

    // Refer to https://www.iana.org/assignments/protocol-numbers/protocol-numbers.xhtml
    public static List<ProtocolNumber> ProtocolNumbers = new ArrayList<>();
    static {
        ProtocolNumbers.add(new ProtocolNumber(0, "HOPOPT", "IPv6 Hop-by-Hop Option"));
        ProtocolNumbers.add(new ProtocolNumber(1, "ICMP", "Internet Control Message"));
        ProtocolNumbers.add(new ProtocolNumber(2, "IGMP", "Internet Group Management"));
        ProtocolNumbers.add(new ProtocolNumber(3, "GGP", "Gateway-to-Gateway"));
        ProtocolNumbers.add(new ProtocolNumber(4, "IPv4", "IPv4 encapsulation"));
        ProtocolNumbers.add(new ProtocolNumber(5, "ST", "Stream"));
        ProtocolNumbers.add(new ProtocolNumber(6, "TCP", "Transmission Control"));
        ProtocolNumbers.add(new ProtocolNumber(7, "CBT", "CBT"));
        ProtocolNumbers.add(new ProtocolNumber(8, "EGP", "Exterior Gateway Protocol"));
        ProtocolNumbers.add(new ProtocolNumber(9, "IGP", "any private interior gateway"));
        ProtocolNumbers.add(new ProtocolNumber(10, "BBN-RCC-MON", "BBN RCC Monitoring"));
        ProtocolNumbers.add(new ProtocolNumber(11, "NVP-II", "Network Voice Protocol"));
        ProtocolNumbers.add(new ProtocolNumber(12, "PUP", "PUP"));
        ProtocolNumbers.add(new ProtocolNumber(13, "ARGUS (deprecated)", "ARGUS"));
        ProtocolNumbers.add(new ProtocolNumber(14, "EMCON", "EMCON"));
        ProtocolNumbers.add(new ProtocolNumber(15, "XNET", "Cross Net Debugger"));
        ProtocolNumbers.add(new ProtocolNumber(16, "CHAOS", "Chaos"));
        ProtocolNumbers.add(new ProtocolNumber(17, "UDP", "User Datagram"));
        ProtocolNumbers.add(new ProtocolNumber(18, "MUX", "Multiplexing"));
        ProtocolNumbers.add(new ProtocolNumber(19, "DCN-MEAS", "DCN Measurement Subsystems"));
        ProtocolNumbers.add(new ProtocolNumber(20, "HMP", "Host Monitoring"));
        ProtocolNumbers.add(new ProtocolNumber(21, "PRM", "Packet Radio Measurement"));
        ProtocolNumbers.add(new ProtocolNumber(22, "XNS-IDP", "XEROX NS IDP"));
        ProtocolNumbers.add(new ProtocolNumber(23, "TRUNK-1", "Trunk-1"));
        ProtocolNumbers.add(new ProtocolNumber(24, "TRUNK-2", "Trunk-2"));
        ProtocolNumbers.add(new ProtocolNumber(25, "LEAF-1", "Leaf-1"));
        ProtocolNumbers.add(new ProtocolNumber(26, "LEAF-2", "Leaf-2"));
        ProtocolNumbers.add(new ProtocolNumber(27, "RDP", "Reliable Data Protocol"));
        ProtocolNumbers.add(new ProtocolNumber(28, "IRTP", "Internet Reliable Transaction"));
        ProtocolNumbers.add(new ProtocolNumber(29, "ISO-TP4", "ISO Transport Protocol Class 4"));
        ProtocolNumbers.add(new ProtocolNumber(30, "NETBLT", "Bulk Data Transfer Protocol"));
        ProtocolNumbers.add(new ProtocolNumber(31, "MFE-NSP", "MFE Network Services Protocol"));
        ProtocolNumbers.add(new ProtocolNumber(32, "MERIT-INP", "MERIT Internodal Protocol"));
        ProtocolNumbers.add(new ProtocolNumber(33, "DCCP", "Datagram Congestion Control Protocol"));
        ProtocolNumbers.add(new ProtocolNumber(34, "3PC", "Third Party Connect Protocol"));
        ProtocolNumbers.add(new ProtocolNumber(35, "IDPR", "Inter-Domain Policy Routing Protocol"));
        ProtocolNumbers.add(new ProtocolNumber(36, "XTP", "XTP"));
        ProtocolNumbers.add(new ProtocolNumber(37, "DDP", "Datagram Delivery Protocol"));
        ProtocolNumbers.add(new ProtocolNumber(38, "IDPR-CMTP", "IDPR Control Message Transport Proto"));
        ProtocolNumbers.add(new ProtocolNumber(39, "TP++", "TP++ Transport Protocol"));
        ProtocolNumbers.add(new ProtocolNumber(40, "IL", "IL Transport Protocol"));
        ProtocolNumbers.add(new ProtocolNumber(41, "IPv6", "IPv6 encapsulation"));
        ProtocolNumbers.add(new ProtocolNumber(42, "SDRP", "Source Demand Routing Protocol"));
        ProtocolNumbers.add(new ProtocolNumber(43, "IPv6-Route", "Routing Header for IPv6"));
        ProtocolNumbers.add(new ProtocolNumber(44, "IPv6-Frag", "Fragment Header for IPv6"));
        ProtocolNumbers.add(new ProtocolNumber(45, "IDRP", "Inter-Domain Routing Protocol"));
        ProtocolNumbers.add(new ProtocolNumber(46, "RSVP", "Reservation Protocol"));
        ProtocolNumbers.add(new ProtocolNumber(47, "GRE", "Generic Routing Encapsulation"));
        ProtocolNumbers.add(new ProtocolNumber(48, "DSR", "Dynamic Source Routing Protocol"));
        ProtocolNumbers.add(new ProtocolNumber(49, "BNA", "BNA"));
        ProtocolNumbers.add(new ProtocolNumber(50, "ESP", "Encap Security Payload"));
        ProtocolNumbers.add(new ProtocolNumber(51, "AH", "Authentication Header"));
        ProtocolNumbers.add(new ProtocolNumber(52, "I-NLSP", "Integrated Net Layer Security TUBA"));
        ProtocolNumbers.add(new ProtocolNumber(53, "SWIPE (deprecated)", "IP with Encryption"));
        ProtocolNumbers.add(new ProtocolNumber(54, "NARP", "NBMA Address Resolution Protocol"));
        ProtocolNumbers.add(new ProtocolNumber(55, "MOBILE", "Minimal IPv4 Encapsulation"));
        ProtocolNumbers.add(new ProtocolNumber(56, "TLSP", "Transport Layer Security Protocol using Kryptonet key management"));
        ProtocolNumbers.add(new ProtocolNumber(57, "SKIP", "SKIP"));
        ProtocolNumbers.add(new ProtocolNumber(58, "IPv6-ICMP", "ICMP for IPv6"));
        ProtocolNumbers.add(new ProtocolNumber(59, "IPv6-NoNxt", "No Next Header for IPv6"));
        ProtocolNumbers.add(new ProtocolNumber(60, "IPv6-Opts", "Destination Options for IPv6"));
        ProtocolNumbers.add(new ProtocolNumber(61, "Any host internal protocol", "Any host internal protocol"));
        ProtocolNumbers.add(new ProtocolNumber(62, "CFTP", "CFTP"));
        ProtocolNumbers.add(new ProtocolNumber(63, "Any local network", "Any local network"));
        ProtocolNumbers.add(new ProtocolNumber(64, "SAT-EXPAK", "SATNET and Backroom EXPAK"));
        ProtocolNumbers.add(new ProtocolNumber(65, "KRYPTOLAN", "Kryptolan"));
        ProtocolNumbers.add(new ProtocolNumber(66, "RVD", "MIT Remote Virtual Disk Protocol"));
        ProtocolNumbers.add(new ProtocolNumber(67, "IPPC", "Internet Pluribus Packet Core"));
        ProtocolNumbers.add(new ProtocolNumber(68, "Any distributed file system", "Any distributed file system"));
        ProtocolNumbers.add(new ProtocolNumber(69, "SAT-MON", "SATNET Monitoring"));
        ProtocolNumbers.add(new ProtocolNumber(70, "VISA", "VISA Protocol"));
        ProtocolNumbers.add(new ProtocolNumber(71, "IPCV", "Internet Packet Core Utility"));
        ProtocolNumbers.add(new ProtocolNumber(72, "CPNX", "Computer Protocol Network Executive"));
        ProtocolNumbers.add(new ProtocolNumber(73, "CPHB", "Computer Protocol Heart Beat"));
        ProtocolNumbers.add(new ProtocolNumber(74, "WSN", "Wang Span Network"));
        ProtocolNumbers.add(new ProtocolNumber(75, "PVP", "Packet Video Protocol"));
        ProtocolNumbers.add(new ProtocolNumber(76, "BR-SAT-MON", "Backroom SATNET Monitoring"));
        ProtocolNumbers.add(new ProtocolNumber(77, "SUN-ND", "SUN ND PROTOCOL-Temporary"));
        ProtocolNumbers.add(new ProtocolNumber(78, "WB-MON", "WIDEBAND Monitoring"));
        ProtocolNumbers.add(new ProtocolNumber(79, "WB-EXPAK", "WIDEBAND EXPAK"));
        ProtocolNumbers.add(new ProtocolNumber(80, "ISO-IP", "ISO Internet Protocol"));
        ProtocolNumbers.add(new ProtocolNumber(81, "VMTP", "VMTP"));
        ProtocolNumbers.add(new ProtocolNumber(82, "SECURE-VMTP", "SECURE-VMTP"));
        ProtocolNumbers.add(new ProtocolNumber(83, "VINES", "VINES"));
        ProtocolNumbers.add(new ProtocolNumber(84, "TTP or IPTM", "Internet Protocol Traffic Manager"));
        ProtocolNumbers.add(new ProtocolNumber(85, "NSFNET-IGP", "NSFNET-IGP"));
        ProtocolNumbers.add(new ProtocolNumber(86, "DGP", "Dissimilar Gateway Protocol"));
        ProtocolNumbers.add(new ProtocolNumber(87, "TCF", "TCF"));
        ProtocolNumbers.add(new ProtocolNumber(88, "EIGRP", "EIGRP"));
        ProtocolNumbers.add(new ProtocolNumber(89, "OSPFIGP", "OSPFIGP"));
        ProtocolNumbers.add(new ProtocolNumber(90, "Sprite-RPC", "Sprite RPC Protocol"));
        ProtocolNumbers.add(new ProtocolNumber(91, "LARP", "Locus Address Resolution Protocol"));
        ProtocolNumbers.add(new ProtocolNumber(92, "MTP", "Multicast Transport Protocol"));
        ProtocolNumbers.add(new ProtocolNumber(93, "AX.25", "AX.25 Frames"));
        ProtocolNumbers.add(new ProtocolNumber(94, "IPIP", "IP-within-IP Encapsulation Protocol"));
        ProtocolNumbers.add(new ProtocolNumber(95, "MICP (deprecated)", "Mobile Internetworking Control Pro."));
        ProtocolNumbers.add(new ProtocolNumber(96, "SCC-SP", "Semaphore Communications Sec. Pro."));
        ProtocolNumbers.add(new ProtocolNumber(97, "ETHERIP", "Ethernet-within-IP Encapsulation"));
        ProtocolNumbers.add(new ProtocolNumber(98, "ENCAP", "Encapsulation Header"));
        ProtocolNumbers.add(new ProtocolNumber(99, "Any private encryption scheme", "Any private encryption scheme"));
        ProtocolNumbers.add(new ProtocolNumber(100, "GMTP", "GMTP"));
        ProtocolNumbers.add(new ProtocolNumber(101, "IFMP", "Ipsilon Flow Management Protocol"));
        ProtocolNumbers.add(new ProtocolNumber(102, "PNNI", "PNNI over IP"));
        ProtocolNumbers.add(new ProtocolNumber(103, "PIM", "Protocol Independent Multicast"));
        ProtocolNumbers.add(new ProtocolNumber(104, "ARIS", "ARIS"));
        ProtocolNumbers.add(new ProtocolNumber(105, "SCPS", "SCPS"));
        ProtocolNumbers.add(new ProtocolNumber(106, "QNX", "QNX"));
        ProtocolNumbers.add(new ProtocolNumber(107, "A/N", "Active Networks"));
        ProtocolNumbers.add(new ProtocolNumber(108, "IPComp", "IP Payload Compression Protocol"));
        ProtocolNumbers.add(new ProtocolNumber(109, "SNP", "Sitara Networks Protocol"));
        ProtocolNumbers.add(new ProtocolNumber(110, "Compaq-Peer", "Compaq Peer Protocol"));
        ProtocolNumbers.add(new ProtocolNumber(111, "IPX-in-IP", "IPX in IP"));
        ProtocolNumbers.add(new ProtocolNumber(112, "VRRP", "Virtual Router Redundancy Protocol"));
        ProtocolNumbers.add(new ProtocolNumber(113, "PGM", "PGM Reliable Transport Protocol"));
        ProtocolNumbers.add(new ProtocolNumber(114, "Any 0-hop protocol", "Any 0-hop protocol"));
        ProtocolNumbers.add(new ProtocolNumber(115, "L2TP", "Layer Two Tunneling Protocol"));
        ProtocolNumbers.add(new ProtocolNumber(116, "DDX", "D-II Data Exchange (DDX)"));
        ProtocolNumbers.add(new ProtocolNumber(117, "IATP", "Interactive Agent Transfer Protocol"));
        ProtocolNumbers.add(new ProtocolNumber(118, "STP", "Schedule Transfer Protocol"));
        ProtocolNumbers.add(new ProtocolNumber(119, "SRP", "SpectraLink Radio Protocol"));
        ProtocolNumbers.add(new ProtocolNumber(120, "UTI", "UTI"));
        ProtocolNumbers.add(new ProtocolNumber(121, "SMP", "Simple Message Protocol"));
        ProtocolNumbers.add(new ProtocolNumber(122, "SM (deprecated)", "Simple Multicast Protocol"));
        ProtocolNumbers.add(new ProtocolNumber(123, "PTP", "Performance Transparency Protocol"));
        ProtocolNumbers.add(new ProtocolNumber(124, "ISIS over IPv4", ""));
        ProtocolNumbers.add(new ProtocolNumber(125, "FIRE", ""));
        ProtocolNumbers.add(new ProtocolNumber(126, "CRTP", "Combat Radio Transport Protocol"));
        ProtocolNumbers.add(new ProtocolNumber(127, "CRUDP", "Combat Radio User Datagram"));
        ProtocolNumbers.add(new ProtocolNumber(128, "SSCOPMCE", ""));
        ProtocolNumbers.add(new ProtocolNumber(129, "IPLT", ""));
        ProtocolNumbers.add(new ProtocolNumber(130, "SPS", "Secure Packet Shield"));
        ProtocolNumbers.add(new ProtocolNumber(131, "PIPE", "Private IP Encapsulation within IP"));
        ProtocolNumbers.add(new ProtocolNumber(132, "SCTP", "Stream Control Transmission Protocol"));
        ProtocolNumbers.add(new ProtocolNumber(133, "FC", "Fibre Channel"));
        ProtocolNumbers.add(new ProtocolNumber(134, "RSVP-E2E-IGNORE", ""));
        ProtocolNumbers.add(new ProtocolNumber(135, "Mobility Header", ""));
        ProtocolNumbers.add(new ProtocolNumber(136, "UDPLite", ""));
        ProtocolNumbers.add(new ProtocolNumber(137, "MPLS-in-IP", ""));
        ProtocolNumbers.add(new ProtocolNumber(138, "manet", "MANET Protocols"));
        ProtocolNumbers.add(new ProtocolNumber(139, "HIP", "Host Identity Protocol"));
        ProtocolNumbers.add(new ProtocolNumber(140, "Shim6", "Shim6 Protocol"));
        ProtocolNumbers.add(new ProtocolNumber(141, "WESP", "Wrapped Encapsulating Security Payload"));
        ProtocolNumbers.add(new ProtocolNumber(142, "ROHC", "Robust Header Compression"));
        ProtocolNumbers.add(new ProtocolNumber(143, "Ethernet", "Ethernet"));
        ProtocolNumbers.add(new ProtocolNumber(144, "AGGFRAG", "AGGFRAG encapsulation payload for ESP"));
        ProtocolNumbers.add(new ProtocolNumber(145, "NSH", "Network Service Header"));
    }
    /**
     * Different Internet Protocol Numbers.
     */
    public static class ProtocolNumber {

        private final Integer number;

        private final String keyword;

        private final String protocol;

        public ProtocolNumber(Integer number, String keyword, String protocol) {
            this.number = number;
            this.keyword = keyword;
            this.protocol = protocol;
        }

        public Integer getNumber() {
            return number;
        }

        public String getKeyword() {
            return keyword;
        }

        public String getProtocol() {
            return protocol;
        }
    }

    // Refer to https://www.iana.org/assignments/icmp-parameters/icmp-parameters.xhtml
    public static List<IcmpType> IcmpTypes = new ArrayList<>();
    static {
        IcmpTypes.add(new IcmpType(0, "Echo Reply"));
        IcmpTypes.add(new IcmpType(3, "Destination Unreachable"));
        IcmpTypes.add(new IcmpType(5, "Redirect"));
        IcmpTypes.add(new IcmpType(8, "Echo"));
        IcmpTypes.add(new IcmpType(9, "Router Advertisement"));
        IcmpTypes.add(new IcmpType(10, "Router Solicitation"));
        IcmpTypes.add(new IcmpType(11, "Time Exceeded"));
        IcmpTypes.add(new IcmpType(12, "Parameter Problem"));
        IcmpTypes.add(new IcmpType(13, "Timestamp"));
        IcmpTypes.add(new IcmpType(14, "Timestamp Reply"));
        IcmpTypes.add(new IcmpType(40, "Photuris"));
        IcmpTypes.add(new IcmpType(42, "Extended Echo Request"));
        IcmpTypes.add(new IcmpType(43, "Extended Echo Reply"));
    }

    /**
     * Different types of ICMP (Internet Control Message Protocol).
     */
    public static class IcmpType {

        private final Integer type;

        private final String description;

        private final List<IcmpCode> icmpCodes = new ArrayList<>();

        public IcmpType(Integer type, String description) {
            this.type = type;
            this.description = description;
        }

        public Integer getType() {
            return type;
        }

        public String getDescription() {
            return description;
        }

        public List<IcmpCode> getIcmpCodes() {
            return icmpCodes;
        }

        public void addIcmpCodes(IcmpCode code) {
            this.icmpCodes.add(code);
        }
    }

    static void addIcmpCode(IcmpCode code) {
        IcmpType type = IcmpTypes.stream().filter(icmpType -> icmpType.getType().equals(code.getType())).findFirst().get();
        type.addIcmpCodes(code);
    }

    public static boolean validateIcmpTypeAndCode(Integer type, Integer code) {
        if (type != null && type != -1) {
            Optional<IcmpType> icmpTypeOptional = IcmpTypes.stream().filter(t -> t.getType().equals(type)).findFirst();
            if (icmpTypeOptional == null || icmpTypeOptional.isEmpty()) {
                return false;
            }
            IcmpType icmpType = icmpTypeOptional.get();
            if (code != null && code != -1) {
                Optional<IcmpCode> icmpCodeOptional = icmpType.getIcmpCodes().stream().filter(c -> c.getCode().equals(code)).findFirst();
                if (icmpCodeOptional == null || icmpCodeOptional.isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }

    static {
        addIcmpCode(new IcmpCode(0, 0, "Echo reply"));
        addIcmpCode(new IcmpCode(3, 0, "Net unreachable"));
        addIcmpCode(new IcmpCode(3, 1, "Host unreachable"));
        addIcmpCode(new IcmpCode(3, 2, "Protocol unreachable"));
        addIcmpCode(new IcmpCode(3, 3, "Port unreachable"));
        addIcmpCode(new IcmpCode(3, 4, "Fragmentation needed and DF set"));
        addIcmpCode(new IcmpCode(3, 5, "Source route failed"));
        addIcmpCode(new IcmpCode(3, 6, "Destination network unknown"));
        addIcmpCode(new IcmpCode(3, 7, "Destination host unknown"));
        addIcmpCode(new IcmpCode(3, 9, "Network administratively prohibited"));
        addIcmpCode(new IcmpCode(3, 10, "Host administratively prohibited"));
        addIcmpCode(new IcmpCode(3, 11, "Network unreachable for ToS"));
        addIcmpCode(new IcmpCode(3, 12, "Host unreachable for ToS"));
        addIcmpCode(new IcmpCode(3, 13, "Communication administratively prohibited"));
        addIcmpCode(new IcmpCode(3, 14, "Host Precedence Violation"));
        addIcmpCode(new IcmpCode(3, 15, "Precedence cutoff in effect"));
        addIcmpCode(new IcmpCode(5, 0, "Redirect Datagram for the Network"));
        addIcmpCode(new IcmpCode(5, 1, "Redirect Datagram for the Host"));
        addIcmpCode(new IcmpCode(5, 2, "Redirect Datagram for the ToS & network"));
        addIcmpCode(new IcmpCode(5, 3, "Redirect Datagram for the ToS & host"));
        addIcmpCode(new IcmpCode(8, 0, "Echo request"));
        addIcmpCode(new IcmpCode(9, 0, "Router advertisement"));
        addIcmpCode(new IcmpCode(9, 16, "Does not route common traffic"));
        addIcmpCode(new IcmpCode(10, 0, "Router solicitation"));
        addIcmpCode(new IcmpCode(11, 0, "TTL expired in transit"));
        addIcmpCode(new IcmpCode(11, 1, "Fragment reassembly time exceeded"));
        addIcmpCode(new IcmpCode(12, 0, "Parameter problem: Pointer indicates the error"));
        addIcmpCode(new IcmpCode(12, 1, "Parameter problem: Missing a required option"));
        addIcmpCode(new IcmpCode(12, 2, "Parameter problem: Bad length"));
        addIcmpCode(new IcmpCode(13, 0, "Timestamp"));
        addIcmpCode(new IcmpCode(14, 0, "Timestamp reply"));
        addIcmpCode(new IcmpCode(40, 0, "Photuris: Security failures"));
        addIcmpCode(new IcmpCode(40, 1, "Photuris: Authentication failed"));
        addIcmpCode(new IcmpCode(40, 2, "Photuris: Decompression failed"));
        addIcmpCode(new IcmpCode(40, 3, "Photuris: Decryption failed"));
        addIcmpCode(new IcmpCode(40, 4, "Photuris: Need authentication"));
        addIcmpCode(new IcmpCode(40, 5, "Photuris: Need authorization"));
    }

    /**
     * Code field of ICMP types.
     */
    public static class IcmpCode {

        private final Integer type;
        private final Integer code;
        private final String description;

        public IcmpCode(Integer type, Integer code, String description) {
            this.type = type;
            this.code = code;
            this.description = description;
        }

        public Integer getType() {
            return type;
        }

        public Integer getCode() {
            return code;
        }

        public String getDescription() {
            return description;
        }
    }
}
