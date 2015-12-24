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

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.utils.net.NetUtils;
import com.cloud.utils.net.cidr.BadCIDRException;
import com.cloud.utils.net.cidr.CIDR;

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

    public void setProtocol(Protocol protocol) {
        this.protocol = protocol;
    }

    public String getOspfArea() {
        return ospfArea;
    }

    public void setOspfArea(String ospfArea) {
        this.ospfArea = ospfArea;
    }

    public Short getHelloInterval() {
        return helloInterval;
    }

    public void setHelloInterval(Short helloInterval) {
        this.helloInterval = helloInterval;
    }

    public Short getDeadInterval() {
        return deadInterval;
    }

    public void setDeadInterval(Short deadInterval) {
        this.deadInterval = deadInterval;
    }

    public Short getRetransmitInterval() {
        return retransmitInterval;
    }

    public void setRetransmitInterval(Short retransmitInterval) {
        this.retransmitInterval = retransmitInterval;
    }

    public Short getTransitDelay() {
        return transitDelay;
    }

    public void setTransitDelay(Short transitDelay) {
        this.transitDelay = transitDelay;
    }

    public Authentication getAuthentication() {
        return authentication;
    }

    public void setAuthentication(Authentication authentication) {
        this.authentication = authentication;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public CIDR[] getSuperCIDR() {
        return superCIDRList;
    }

    public void setSuperCIDR(CIDR[] superCIDR) {
        this.superCIDRList = superCIDR;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
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

    public OSPFZoneConfig() {
    }

    public void setDefaultValues(final Map<String, String> details) throws BadCIDRException {
        this.protocol = details.get(Params.PROTOCOL.name()) == null ? Protocol.OSPF : Protocol.valueOf(details.get(Params.PROTOCOL.name()));
        this.ospfArea = details.get(Params.AREA.name()) == null ? "0" : details.get(Params.AREA.name());
        this.helloInterval = Short.valueOf(details.get(Params.HELLO_INTERVAL.name()) == null ? "10" : details.get(Params.HELLO_INTERVAL.name()));
        this.deadInterval = Short.valueOf(details.get(Params.DEAD_INTERVAL.name()) == null ? "40" : details.get(Params.DEAD_INTERVAL.name()));
        this.retransmitInterval = Short.valueOf(details.get(Params.RETRANSMIT_INTERVAL.name()) == null ? "5" : details.get(Params.RETRANSMIT_INTERVAL.name()));
        this.transitDelay = Short.valueOf(details.get(Params.TRANSIT_DELAY.name()) == null ? "1" : details.get(Params.TRANSIT_DELAY.name()));
        this.authentication = details.get(Params.AUTHENTICATION.name()) == null ? Authentication.MD5 : Authentication.valueOf(details.get(Params.AUTHENTICATION.name()));
        this.password = details.get(Params.PASSWORD.name()) == null ? "" : details.get(Params.PASSWORD.name());
        this.superCIDRList = details.get(Params.SUPER_CIDR.name()) == null ? new CIDR[0] : NetUtils.convertToCIDR(details.get(Params.SUPER_CIDR.name()).split(","));
        this.enabled = details.get(Params.ENABLED.name()) == null ? Boolean.FALSE : Boolean.valueOf(details.get(Params.ENABLED.name()));
    }

    public void setValues(final String protocol, final String ospfArea, final Short helloInterval, final Short deadInterval, final Short retransmitInterval,
            final Short transitDelay, final String authentication, final String quaggaPassword, final String superCIDRList, final Boolean enabled) throws BadCIDRException {
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
            String[] cidr_list = superCIDRList.split(",");
            for (String cidr : cidr_list) {
                if (!NetUtils.isValidCIDR(cidr)) {
                    throw new InvalidParameterValueException("The super CIDR is not a valid cidr " + cidr);
                }
            }
            CIDR[] v_cidr = NetUtils.convertToCIDR(cidr_list);
            if (!NetUtils.cidrListConsistency(v_cidr)) {
                throw new InvalidParameterValueException("The cidr list is not consistent " + Arrays.toString(cidr_list));
            }
            this.superCIDRList = v_cidr;
        }
        if (enabled != null) {
            this.enabled = enabled;
        }
    }

    public void setValues(Map<String, String> details) throws BadCIDRException {
        Protocol protocol = Protocol.valueOf(details.get(Params.PROTOCOL.name()));
        String ospfArea = details.get(Params.AREA.name());
        Short helloInterval = Short.valueOf(details.get(Params.HELLO_INTERVAL.name()));
        Short deadInterval = Short.valueOf(details.get(Params.DEAD_INTERVAL.name()));
        Short retransmitInterval = Short.valueOf(details.get(Params.RETRANSMIT_INTERVAL.name()));
        Short transitDelay = Short.valueOf(details.get(Params.TRANSIT_DELAY.name()));
        Authentication authentication = Authentication.valueOf(details.get(Params.AUTHENTICATION.name()));
        String quaggaPassword = details.get(Params.PASSWORD.name());
        String superCIDRList = details.get(Params.SUPER_CIDR.name());
        Boolean enabled = Boolean.valueOf(details.get(Params.ENABLED.name()));

        if (protocol != null) {
            if (Protocol.OSPF == protocol) {
                this.protocol = protocol;
            } else {
                throw new InvalidParameterValueException("The quagga protocal can have values of OSPF  " + protocol);
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
            if (Authentication.MD5 == authentication || Authentication.PLAIN_TEXT == authentication) {
                this.authentication = authentication;
            } else {
                throw new InvalidParameterValueException("The quagga authentication can have values of MD5 or PLAIN_TEXT and not " + authentication);
            }
        }
        if (quaggaPassword != null) {
            this.password = quaggaPassword;
        }
        if (superCIDRList != null) {
            String[] cidr_list = superCIDRList.split(",");
            for (String cidr : cidr_list) {
                if (!NetUtils.isValidCIDR(cidr)) {
                    throw new InvalidParameterValueException("The super CIDR is not a valid cidr " + cidr);
                }
            }
            CIDR[] v_cidr = NetUtils.convertToCIDR(cidr_list);
            if (!NetUtils.cidrListConsistency(v_cidr)) {
                throw new InvalidParameterValueException("The cidr list is not consistent " + Arrays.toString(cidr_list));
            }
            this.superCIDRList = v_cidr;
        }
        if (enabled != null) {
            this.enabled = enabled;
        }
    }

    public Map<String, String> getValues() throws BadCIDRException {
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
            if (!NetUtils.cidrListConsistency(superCIDRList)) {
                throw new InvalidParameterValueException("The cidr list is not consistent " + Arrays.toString(superCIDRList));
            }
            details.put(Params.SUPER_CIDR.name(), StringUtils.join(superCIDRList, ','));

        } else {
            details.put(Params.SUPER_CIDR.name(), "200.100.0.0/20");
        }
        if (enabled != null) {
            details.put(Params.ENABLED.name(), enabled.toString());
        }

        return details;
    }

}
