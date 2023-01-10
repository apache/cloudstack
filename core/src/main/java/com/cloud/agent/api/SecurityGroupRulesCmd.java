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

package com.cloud.agent.api;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.DeflaterOutputStream;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;

import com.cloud.agent.api.LogLevel.Log4jLevel;
import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.utils.net.NetUtils;

public class SecurityGroupRulesCmd extends Command {
    private static final String CIDR_LENGTH_SEPARATOR = "/";
    public static final char RULE_TARGET_SEPARATOR = ',';
    public static final char RULE_COMMAND_SEPARATOR = ';';
    protected static final String EGRESS_RULE = "E:";
    protected static final String INGRESS_RULE = "I:";

    private final String guestIp;
    private final String guestIp6;
    private final String vmName;
    private final String guestMac;
    private final String signature;
    private final Long seqNum;
    private final Long vmId;
    private Long msId;
    private List<IpPortAndProto> ingressRuleSet;
    private List<IpPortAndProto> egressRuleSet;
    private final List<String> secIps;
    private VirtualMachineTO vmTO;

    public static class IpPortAndProto {
        private final String proto;
        private final int startPort;
        private final int endPort;
        @LogLevel(Log4jLevel.Trace)
        private List<String> allowedCidrs;

        public IpPortAndProto(final String proto, final int startPort, final int endPort, final String... allowedCidrs) {
            super();
            this.proto = proto;
            this.startPort = startPort;
            this.endPort = endPort;
            setAllowedCidrs(allowedCidrs);
        }

        public List<String> getAllowedCidrs() {
            return allowedCidrs;
        }

        public void setAllowedCidrs(final String... allowedCidrs) {
            this.allowedCidrs = new ArrayList<String>();
            for (final String allowedCidr : allowedCidrs) {
                this.allowedCidrs.add(allowedCidr);
            }
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

    public SecurityGroupRulesCmd(
            final String guestIp,
            final String guestIp6,
            final String guestMac,
            final String vmName,
            final Long vmId,
            final String signature,
            final Long seqNum,
            final IpPortAndProto[] ingressRuleSet,
            final IpPortAndProto[] egressRuleSet,
            final List<String> secIps) {
        this.guestIp = guestIp;
        this.guestIp6 = guestIp6;
        this.vmName = vmName;
        setIngressRuleSet(ingressRuleSet);
        this.setEgressRuleSet(egressRuleSet);
        this.guestMac = guestMac;
        this.seqNum = seqNum;
        this.vmId = vmId;
        if (signature == null) {
            final String stringified = stringifyRules();
            this.signature = DigestUtils.md5Hex(stringified);
        } else {
            this.signature = signature;
        }
        this.secIps = secIps;
    }

    @Override
    public boolean executeInSequence() {
        return true;
    }

    public List<IpPortAndProto> getIngressRuleSet() {
        return ingressRuleSet;
    }

    public void setIngressRuleSet(final IpPortAndProto... ingressRuleSet) {
        this.ingressRuleSet = new ArrayList<IpPortAndProto>();
        for(final IpPortAndProto rule: ingressRuleSet) {
            this.ingressRuleSet.add(rule);
        }
    }

    public List<IpPortAndProto> getEgressRuleSet() {
        return egressRuleSet;
    }

    public void setEgressRuleSet(final IpPortAndProto... egressRuleSet) {
        this.egressRuleSet = new ArrayList<IpPortAndProto>();
        for(final IpPortAndProto rule: egressRuleSet) {
            this.egressRuleSet.add(rule);
        }
    }

    public String getGuestIp() {
        return guestIp;
    }

    public String getGuestIp6() {
        return guestIp6;
    }

    public List<String> getSecIps() {
        return secIps;
    }

    public String getVmName() {
        return vmName;
    }

    private String compressCidrToHexRepresentation(final String cidr) {
        final String[] toks = cidr.split(CIDR_LENGTH_SEPARATOR);
        final long ipnum = NetUtils.ip2Long(toks[0]);
        return Long.toHexString(ipnum) + CIDR_LENGTH_SEPARATOR + toks[1];
    }

    public String getSecIpsString() {
        final StringBuilder sb = new StringBuilder();
        final List<String> ips = getSecIps();
        if (ips == null) {
            sb.append("0").append(RULE_COMMAND_SEPARATOR);
        } else {
            for (final String ip : ips) {
                sb.append(ip).append(RULE_COMMAND_SEPARATOR);
            }
        }
        return sb.toString();
    }

    public String stringifyRules() {
        final StringBuilder ruleBuilder = new StringBuilder();
        stringifyRulesFor(getIngressRuleSet(), INGRESS_RULE, false, ruleBuilder);
        stringifyRulesFor(getEgressRuleSet(), EGRESS_RULE, false, ruleBuilder);
        return ruleBuilder.toString();
    }

    public String stringifyCompressedRules() {
        final StringBuilder ruleBuilder = new StringBuilder();
        stringifyRulesFor(getIngressRuleSet(), INGRESS_RULE, true, ruleBuilder);
        stringifyRulesFor(getEgressRuleSet(), EGRESS_RULE, true, ruleBuilder);
        return ruleBuilder.toString();
    }

    private void stringifyRulesFor(final List<IpPortAndProto> ipPortAndProtocols, final String inOrEgress, final boolean compressed, final StringBuilder ruleBuilder) {
        for (final IpPortAndProto ipPandP : ipPortAndProtocols) {
            ruleBuilder.append(inOrEgress).append(ipPandP.getProto()).append(RULE_COMMAND_SEPARATOR).append(ipPandP.getStartPort()).append(RULE_COMMAND_SEPARATOR)
                    .append(ipPandP.getEndPort()).append(RULE_COMMAND_SEPARATOR);
            for (final String cidr : ipPandP.getAllowedCidrs()) {
                ruleBuilder.append(represent(cidr, compressed)).append(RULE_TARGET_SEPARATOR);
            }
            ruleBuilder.append("NEXT ");
        }
    }

    private String represent(final String cidr, final boolean compressed) {
        if (compressed) {
            return compressCidrToHexRepresentation(cidr);
        } else {
            return cidr;
        }
    }

    /**
     * Compress the security group rules using zlib compression to allow the call to the hypervisor
     * to scale beyond 8k cidrs.
     * Note : not using {@see GZipOutputStream} since that is for files, using {@see DeflaterOutputStream} instead.
     * {@see GZipOutputStream} gives a different header, although the compression is the same
     */
    public String compressStringifiedRules() {
        final String stringified = stringifyRules();
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        String encodedResult = null;
        try {
            final DeflaterOutputStream dzip = new DeflaterOutputStream(out);
            dzip.write(stringified.getBytes());
            dzip.close();
            encodedResult = Base64.encodeBase64String(out.toByteArray());
        } catch (final IOException e) {
            logger.warn("Exception while compressing security group rules");
        }
        return encodedResult;
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

    public void setVmTO(VirtualMachineTO vmTO) {
        this.vmTO = vmTO;
    }

    public VirtualMachineTO getVmTO() {
        return vmTO;
    }

    /**
     * used for logging
     * @return the number of Cidrs in the in and egress rule sets for this security group rules command.
     */
    public int getTotalNumCidrs() {
        int count = 0;
        for (final IpPortAndProto i : ingressRuleSet) {
            count += i.allowedCidrs.size();
        }
        for (final IpPortAndProto i : egressRuleSet) {
            count += i.allowedCidrs.size();
        }
        return count;
    }

    public void setMsId(final long msId) {
        this.msId = msId;
    }

    public Long getMsId() {
        return msId;
    }

}
