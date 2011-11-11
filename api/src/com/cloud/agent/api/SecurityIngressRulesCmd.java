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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.DeflaterOutputStream;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.log4j.Logger;

import com.cloud.agent.api.LogLevel.Log4jLevel;
import com.cloud.utils.net.NetUtils;


public class SecurityIngressRulesCmd extends Command {
    private static Logger s_logger = Logger.getLogger(SecurityIngressRulesCmd.class);
    public static class IpPortAndProto {
        String proto;
        int startPort;
        int endPort;
        @LogLevel(Log4jLevel.Trace)
        String [] allowedCidrs;

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
    IpPortAndProto [] ruleSet;

    public SecurityIngressRulesCmd() {
        super();
    }


    public SecurityIngressRulesCmd(String guestIp, String guestMac, String vmName, Long vmId, String signature, Long seqNum, IpPortAndProto[] ruleSet) {
        super();
        this.guestIp = guestIp;
        this.vmName = vmName;
        this.ruleSet = ruleSet;
        this.guestMac = guestMac;
        this.signature = signature;
        this.seqNum = seqNum;
        this.vmId  = vmId;
        if (signature == null) {
            String stringified = stringifyRules();
            this.signature = DigestUtils.md5Hex(stringified);
        }
    }


    @Override
    public boolean executeInSequence() {
        return true;
    }


    public IpPortAndProto[] getRuleSet() {
        return ruleSet;
    }


    public void setRuleSet(IpPortAndProto[] ruleSet) {
        this.ruleSet = ruleSet;
    }


    public String getGuestIp() {
        return guestIp;
    }


    public String getVmName() {
        return vmName;
    }

    public String stringifyRules() {
        StringBuilder ruleBuilder = new StringBuilder();
        for (SecurityIngressRulesCmd.IpPortAndProto ipPandP: getRuleSet()) {
            ruleBuilder.append(ipPandP.getProto()).append(":").append(ipPandP.getStartPort()).append(":").append(ipPandP.getEndPort()).append(":");
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
    
    
    public String stringifyCompressedRules() {
        StringBuilder ruleBuilder = new StringBuilder();
        for (SecurityIngressRulesCmd.IpPortAndProto ipPandP: getRuleSet()) {
            ruleBuilder.append(ipPandP.getProto()).append(":").append(ipPandP.getStartPort()).append(":").append(ipPandP.getEndPort()).append(":");
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
     * Compress the ingress rules using zlib compression to allow the call to the hypervisor
     * to scale beyond 8k cidrs.
     */
    public String compressStringifiedRules() {
        StringBuilder ruleBuilder = new StringBuilder();
        for (SecurityIngressRulesCmd.IpPortAndProto ipPandP: getRuleSet()) {
            ruleBuilder.append(ipPandP.getProto()).append(":").append(ipPandP.getStartPort()).append(":").append(ipPandP.getEndPort()).append(":");
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
            s_logger.warn("Exception while compressing ingress rules");
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
        for (IpPortAndProto i: ruleSet) {
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
