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

package com.cloud.network.vpc;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.cloudstack.utils.net.cidr.CIDR;
import org.apache.cloudstack.utils.net.cidr.CIDRException;
import org.apache.cloudstack.utils.net.cidr.CIDRFactory;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

import com.cloud.exception.InvalidParameterValueException;

public class OSPFZoneConfig {

    private long zoneId;
    private Protocol protocol;
    private String ospfArea;
    private Short helloInterval;
    private Short deadInterval;
    private Short retransmitInterval;
    private Short transitDelay;
    private Authentication authentication;
    private String password;
    private CIDR[] superCIDRList;
    private Boolean enabled;

    public static final String s_protocol = "protocol";
    public static final String s_area = "area";
    public static final String s_helloInterval = "hellointerval";
    public static final String s_deadInterval = "deadinterval";
    public static final String s_retransmitInterval = "retransmitinterval";
    public static final String s_transitDelay = "transitdelay";
    public static final String s_authentication = "authentication";
    public static final String s_superCIDR = "supercidr";
    public static final String s_password = "password";
    public static final String s_enabled = "enabled";

    public Protocol getProtocol() {
        return protocol;
    }

    public void setProtocol(String str) {
        this.protocol = str == null ? Protocol.OSPF : Protocol.valueOf(str);
    }

    public String getOspfArea() {
        return ospfArea;
    }

    public void setOspfArea(String ospfArea) {
        this.ospfArea = ospfArea == null ? "0" : ospfArea;
    }

    public Short getHelloInterval() {
        return helloInterval;
    }

    public void setHelloInterval(String string) {
        this.helloInterval = string == null ? 10 : Short.valueOf(string);
        if (this.helloInterval > 65535 || this.helloInterval < 1) {
            throw new IllegalArgumentException("The value of hello interval should be from 1 to 65535 and now " + this.helloInterval);
        }
    }

    public Short getDeadInterval() {
        return deadInterval;
    }

    public void setDeadInterval(String deadInterval) {
        this.deadInterval = deadInterval == null ? 40 : Short.valueOf(deadInterval);
        if (this.deadInterval > 65535 || this.helloInterval < 1) {
            throw new IllegalArgumentException("The value of dead interval should be from 1 to 65535 and now " + this.deadInterval);
        }
    }

    public Short getRetransmitInterval() {
        return retransmitInterval;
    }

    public void setRetransmitInterval(String retransmitInterval) {
        this.retransmitInterval = retransmitInterval == null ? 5 : Short.valueOf(retransmitInterval);
        if (this.retransmitInterval > 65535 || this.retransmitInterval < 1) {
            throw new IllegalArgumentException("The value of retransmit interval should be from 1 to 65535 and now " + this.retransmitInterval);
        }
    }

    public Short getTransitDelay() {
        return transitDelay;
    }

    public void setTransitDelay(String transitDelay) {
        this.transitDelay = transitDelay == null ? 1 : Short.valueOf(transitDelay);
        if (this.transitDelay > 65535 || this.transitDelay < 1) {
            throw new IllegalArgumentException("The value of transit rDelay should be from 1 to 65535 and now " + this.retransmitInterval);
        }
    }

    public Authentication getAuthentication() {
        return authentication;
    }

    public void setAuthentication(String authentication) {
        this.authentication = authentication == null ? Authentication.MD5 : Authentication.valueOf(authentication);
    }

    public String getPassword() {
        return password = password == null ? "" : password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public CIDR[] getSuperCIDR() {
        return superCIDRList;
    }

    public void setSuperCIDR(String superCIDR) throws CIDRException {
        this.superCIDRList = superCIDR == null ? new CIDR[0] : CIDRFactory.convertToCIDR(superCIDR.split(","));
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(String enabled) {
        this.enabled = enabled == null ? Boolean.FALSE : Boolean.valueOf(enabled);
    }

    public enum Params {
        PROTOCOL, AREA, HELLO_INTERVAL, DEAD_INTERVAL, RETRANSMIT_INTERVAL, TRANSIT_DELAY, AUTHENTICATION, SUPER_CIDR, PASSWORD, ENABLED
    }

    public enum Protocol {
        OSPF
    }

    public enum Authentication {
        MD5, PLAIN_TEXT
    }

    public OSPFZoneConfig(final Map<String, String> details) {
        this.setProtocol(details.get(Params.PROTOCOL.name()));
        this.setOspfArea(details.get(Params.AREA.name()));
        this.setHelloInterval(details.get(Params.HELLO_INTERVAL.name()));
        this.setDeadInterval(details.get(Params.DEAD_INTERVAL.name()));
        this.setRetransmitInterval(details.get(Params.RETRANSMIT_INTERVAL.name()));
        this.setTransitDelay(details.get(Params.TRANSIT_DELAY.name()));
        this.setAuthentication(details.get(Params.AUTHENTICATION.name()));
        this.setPassword(details.get(Params.PASSWORD.name()));
        String cidrStr = details.get(Params.SUPER_CIDR.name());
        if (cidrStr != null && ! cidrStr.isEmpty()){
            try {
                this.setSuperCIDR(cidrStr);
            } catch (CIDRException e) {
                throw new InvalidParameterValueException("The supercidr in zone config is bad " + cidrStr);
            }
        }
        this.setEnabled(details.get(Params.ENABLED.name()));
    }

    public void setValues(final String protocol, final String ospfArea, final Short helloInterval, final Short deadInterval, final Short retransmitInterval,
            final Short transitDelay, final String authentication, final String quaggaPassword, final String superCIDRList, final Boolean enabled) throws CIDRException {
        if (protocol != null) {
            if ((Protocol.OSPF.name().equals(protocol))) {
                this.protocol = Protocol.valueOf(protocol);
            } else {
                throw new InvalidParameterValueException("The quagga protocal can have values of OSPF only " + protocol);
            }
        }
        if (ospfArea != null) {
            this.ospfArea = ospfArea;
        }
        if (helloInterval != null) {
            this.helloInterval = helloInterval;
        }
        if (deadInterval != null) {
            this.deadInterval = deadInterval;
        }
        if (retransmitInterval != null) {
            this.retransmitInterval = retransmitInterval;
        }
        if (transitDelay != null) {
            this.transitDelay = transitDelay;
        }
        if (authentication != null) {
            if (Authentication.MD5.name().equals(authentication) || Authentication.PLAIN_TEXT.name().equals(authentication)) {
                this.authentication = Authentication.valueOf(authentication);
            } else {
                throw new InvalidParameterValueException("The quagga authentication can have values of MD5 or PLAIN_TEXT and not " + protocol);
            }
        }
        if (quaggaPassword != null) {
            this.password = quaggaPassword;
        }
        if (superCIDRList != null) {
            this.superCIDRList = CIDRFactory.getCIDRList(superCIDRList);
        }
        if (enabled != null) {
            this.enabled = enabled;
        }
    }


    public Map<String, String> getValues() throws CIDRException {
        Map<String, String> details = new HashMap<String, String>();
        if (protocol != null) {
            if ((Protocol.OSPF.name().equals(protocol))) {
                details.put(Params.PROTOCOL.name(), protocol.name());
            } else {
                details.put(Params.PROTOCOL.name(), Protocol.OSPF.name());
            }
        }
        if (ospfArea != null) {
            details.put(Params.AREA.name(), ospfArea);
        }
        if (helloInterval != null) {
            details.put(Params.HELLO_INTERVAL.name(), helloInterval.toString());
        }
        if (deadInterval != null) {
            details.put(Params.DEAD_INTERVAL.name(), deadInterval.toString());
        }
        if (retransmitInterval != null) {
            details.put(Params.RETRANSMIT_INTERVAL.name(), retransmitInterval.toString());
        }
        if (transitDelay != null) {
            details.put(Params.TRANSIT_DELAY.name(), transitDelay.toString());
        }
        if (authentication != null) {
            if (Authentication.MD5.name().equals(authentication) || Authentication.PLAIN_TEXT.name().equals(authentication)) {
                details.put(Params.AUTHENTICATION.name(), authentication.name());
            } else {
                details.put(Params.AUTHENTICATION.name(), Authentication.MD5.name());
            }
        }
        if (password != null) {
            details.put(Params.PASSWORD.name(), password);
        }
        if (!ArrayUtils.isEmpty(superCIDRList)) {
            if (!CIDRFactory.cidrListConsistency(superCIDRList)) {
                throw new InvalidParameterValueException("The cidr list is not consistent " + Arrays.toString(superCIDRList));
            }
            details.put(Params.SUPER_CIDR.name(), StringUtils.join(superCIDRList, ','));

        } else {
            details.put(Params.SUPER_CIDR.name(), "");
        }
        if (enabled != null) {
            details.put(Params.ENABLED.name(), enabled.toString());
        }

        return details;
    }

    @Override
    public String toString() {
        return "OSPFZoneConfig [zoneId=" + zoneId + ", protocol=" + protocol + ", ospfArea=" + ospfArea + ", helloInterval=" + helloInterval + ", deadInterval=" + deadInterval
                + ", retransmitInterval=" + retransmitInterval + ", transitDelay=" + transitDelay + ", authentication=" + authentication + ", password=" + password
                + ", superCIDRList=" + Arrays.toString(superCIDRList) + ", enabled=" + enabled + "]";
    }
}
