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
package com.cloud.agent.api;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.zip.DeflaterOutputStream;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.log4j.Logger;

import com.cloud.agent.api.LogLevel.Log4jLevel;
import com.cloud.utils.net.NetUtils;


public class SecurityGroupRulesCmd extends Command {
    private static Logger s_logger = Logger.getLogger(SecurityGroupRulesCmd.class);
    public static class IpPortAndProto {
        private String proto;
        private int startPort;
        private int endPort;
        @LogLevel(Log4jLevel.Trace)
        private String [] allowedCidrs;

        public IpPortAndProto() { }

        public IpPortAndProto(String proto, int startPort, int endPort,
                String[] allowedCidrs) {
            super();
            this.proto = proto;
            this.startPort = startPort;
            this.endPort = endPort;
            this.allowedCidrs = allowedCidrs;
        }

        public String[] getAllowedCidrs() {
            return allowedCidrs;
        }

        public void setAllowedCidrs(String[] allowedCidrs) {
            this.allowedCidrs = allowedCidrs;
        }

        public String getProto() {
            return proto;
        }

        public int getStartPort() {
            return startPort;
        }

        public int getEndPort() {
            return endPort;
        }

    }


    String guestIp;
    String vmName;
    String guestMac;
    String signature;
    Long seqNum;
    Long vmId;
    Long msId;
    IpPortAndProto [] ingressRuleSet;
    IpPortAndProto [] egressRuleSet;
    private List<String> secIps;

    public SecurityGroupRulesCmd() {
        super();
    }


    public SecurityGroupRulesCmd(String guestIp, String guestMac, String vmName, Long vmId, String signature, Long seqNum, IpPortAndProto[] ingressRuleSet, IpPortAndProto[] egressRuleSet) {
        super();
        this.guestIp = guestIp;
        this.vmName = vmName;
        this.ingressRuleSet = ingressRuleSet;
        this.egressRuleSet = egressRuleSet;
        this.guestMac = guestMac;
        this.signature = signature;
        this.seqNum = seqNum;
        this.vmId  = vmId;
        if (signature == null) {
            String stringified = stringifyRules();
            this.signature = DigestUtils.md5Hex(stringified);
        }
    }


    public SecurityGroupRulesCmd(String guestIp, String guestMac, String vmName, Long vmId, String signature, Long seqNum, IpPortAndProto[] ingressRuleSet, IpPortAndProto[] egressRuleSet, List<String> secIps) {
        super();
        this.guestIp = guestIp;
        this.vmName = vmName;
        this.ingressRuleSet = ingressRuleSet;
        this.egressRuleSet = egressRuleSet;
        this.guestMac = guestMac;
        this.signature = signature;
        this.seqNum = seqNum;
        this.vmId  = vmId;
        if (signature == null) {
            String stringified = stringifyRules();
            this.signature = DigestUtils.md5Hex(stringified);
        }
        this.secIps = secIps;
    }

    @Override
    public boolean executeInSequence() {
        return true;
    }


    public IpPortAndProto[] getIngressRuleSet() {
        return ingressRuleSet;
    }


    public void setIngressRuleSet(IpPortAndProto[] ingressRuleSet) {
        this.ingressRuleSet = ingressRuleSet;
    }

    public IpPortAndProto[] getEgressRuleSet() {
        return egressRuleSet;
    }


    public void setEgressRuleSet(IpPortAndProto[] egressRuleSet) {
        this.egressRuleSet = egressRuleSet;
    }

    public String getGuestIp() {
        return guestIp;
    }

    public List<String> getSecIps() {
        return secIps;
    }


    public String getVmName() {
        return vmName;
    }

    public String stringifyRules() {
        StringBuilder ruleBuilder = new StringBuilder();
        for (SecurityGroupRulesCmd.IpPortAndProto ipPandP: getIngressRuleSet()) {
            ruleBuilder.append("I:").append(ipPandP.getProto()).append(":").append(ipPandP.getStartPort()).append(":").append(ipPandP.getEndPort()).append(":");
            for (String cidr: ipPandP.getAllowedCidrs()) {
                ruleBuilder.append(cidr).append(",");
            }
            ruleBuilder.append("NEXT");
            ruleBuilder.append(" ");
        }
        for (SecurityGroupRulesCmd.IpPortAndProto ipPandP: getEgressRuleSet()) {
            ruleBuilder.append("E:").append(ipPandP.getProto()).append(":").append(ipPandP.getStartPort()).append(":").append(ipPandP.getEndPort()).append(":");
            for (String cidr: ipPandP.getAllowedCidrs()) {
                ruleBuilder.append(cidr).append(",");
            }
            ruleBuilder.append("NEXT");
            ruleBuilder.append(" ");
        }
        return ruleBuilder.toString();
    }

    //convert cidrs in the form "a.b.c.d/e" to "hexvalue of 32bit ip/e"
    private String compressCidr(String cidr) {
        String [] toks = cidr.split("/");
        long ipnum = NetUtils.ip2Long(toks[0]);
        return Long.toHexString(ipnum) + "/" + toks[1];
    }


    public String getSecIpsString() {
        StringBuilder sb = new StringBuilder();
        List<String> ips = getSecIps();
        if (ips == null) {
            return "0:";
        } else {
            for (String ip : ips) {
                sb.append(ip).append(":");
            }
        }
        return sb.toString();
    }


    public String stringifyCompressedRules() {
        StringBuilder ruleBuilder = new StringBuilder();
        for (SecurityGroupRulesCmd.IpPortAndProto ipPandP : getIngressRuleSet()) {
            ruleBuilder.append("I:").append(ipPandP.getProto()).append(":").append(ipPandP.getStartPort()).append(":").append(ipPandP.getEndPort()).append(":");
            for (String cidr: ipPandP.getAllowedCidrs()) {
                //convert cidrs in the form "a.b.c.d/e" to "hexvalue of 32bit ip/e"
                ruleBuilder.append(compressCidr(cidr)).append(",");
            }
            ruleBuilder.append("NEXT");
            ruleBuilder.append(" ");
        }
        for (SecurityGroupRulesCmd.IpPortAndProto ipPandP : getEgressRuleSet()) {
            ruleBuilder.append("E:").append(ipPandP.getProto()).append(":").append(ipPandP.getStartPort()).append(":").append(ipPandP.getEndPort()).append(":");
            for (String cidr: ipPandP.getAllowedCidrs()) {
                //convert cidrs in the form "a.b.c.d/e" to "hexvalue of 32bit ip/e"
                ruleBuilder.append(compressCidr(cidr)).append(",");
            }
            ruleBuilder.append("NEXT");
            ruleBuilder.append(" ");
        }
        return ruleBuilder.toString();
    }

    /*
     * Compress the security group rules using zlib compression to allow the call to the hypervisor
     * to scale beyond 8k cidrs.
     */
    public String compressStringifiedRules() {
        StringBuilder ruleBuilder = new StringBuilder();
        for (SecurityGroupRulesCmd.IpPortAndProto ipPandP: getIngressRuleSet()) {
            ruleBuilder.append("I:").append(ipPandP.getProto()).append(":").append(ipPandP.getStartPort()).append(":").append(ipPandP.getEndPort()).append(":");
            for (String cidr: ipPandP.getAllowedCidrs()) {
                ruleBuilder.append(cidr).append(",");
            }
            ruleBuilder.append("NEXT");
            ruleBuilder.append(" ");
        }
        for (SecurityGroupRulesCmd.IpPortAndProto ipPandP: getEgressRuleSet()) {
            ruleBuilder.append("E:").append(ipPandP.getProto()).append(":").append(ipPandP.getStartPort()).append(":").append(ipPandP.getEndPort()).append(":");
            for (String cidr: ipPandP.getAllowedCidrs()) {
                ruleBuilder.append(cidr).append(",");
            }
            ruleBuilder.append("NEXT");
            ruleBuilder.append(" ");
        }
        String stringified = ruleBuilder.toString();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            //Note : not using GZipOutputStream since that is for files
            //GZipOutputStream gives a different header, although the compression is the same
            DeflaterOutputStream dzip = new DeflaterOutputStream(out);
            dzip.write(stringified.getBytes());
            dzip.close();
        } catch (IOException e) {
            s_logger.warn("Exception while compressing security group rules");
            return null;
        }
        return Base64.encodeBase64String(out.toByteArray());
    }

    public String getSignature() {
        return signature;
    }


    public String getGuestMac() {
        return guestMac;
    }


    public Long getSeqNum() {
        return seqNum;
    }


    public Long getVmId() {
        return vmId;
    }

    public int getTotalNumCidrs() {
        //useful for logging
        int count = 0;
        for (IpPortAndProto i: ingressRuleSet) {
            count += i.allowedCidrs.length;
        }
        for (IpPortAndProto i: egressRuleSet) {
            count += i.allowedCidrs.length;
        }
        return count;
    }

    public void setMsId(long msId) {
        this.msId = msId;
    }

    public Long getMsId() {
        return msId;
    }

}
