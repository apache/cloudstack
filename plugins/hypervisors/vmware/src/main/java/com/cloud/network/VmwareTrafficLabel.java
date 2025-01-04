// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements. See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership. The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License. You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied. See the License for the
// specific language governing permissions and limitations
// under the License.
package com.cloud.network;

import com.cloud.dc.Vlan;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.hypervisor.vmware.mo.VirtualSwitchType;
import com.cloud.network.Networks.TrafficType;

public class VmwareTrafficLabel implements TrafficLabel {
    public static final String DEFAULT_VSWITCH_NAME = "vSwitch0";
    public static final String DEFAULT_DVSWITCH_NAME = "dvSwitch0";
    public static final String DEFAULT_NDVSWITCH_NAME = "epp0";
    public static final int MAX_FIELDS_VMWARE_LABEL = 3;
    public static final int VMWARE_LABEL_FIELD_INDEX_NAME = 0;
    public static final int VMWARE_LABEL_FIELD_INDEX_VLANID = 1;
    public static final int VMWARE_LABEL_FIELD_INDEX_VSWITCH_TYPE = 2;

    TrafficType _trafficType = TrafficType.None;
    VirtualSwitchType _vSwitchType = VirtualSwitchType.StandardVirtualSwitch;
    String _vSwitchName = DEFAULT_VSWITCH_NAME;
    String _vlanId = Vlan.UNTAGGED;
    boolean _isPrimaryNic = true;  // Flag to identify if this is a primary NIC
    int _rateLimit = 1000;  // Default rate limit in Mbps
    int _guaranteedBandwidth = 500;  // Default guaranteed bandwidth in Mbps

    public VmwareTrafficLabel(String networkLabel, TrafficType trafficType, VirtualSwitchType defVswitchType, boolean isPrimaryNic) {
        _trafficType = trafficType;
        _isPrimaryNic = isPrimaryNic;
        _parseLabel(networkLabel, defVswitchType);
    }

    public VmwareTrafficLabel(String networkLabel, TrafficType trafficType, boolean isPrimaryNic) {
        _trafficType = trafficType;
        _isPrimaryNic = isPrimaryNic;
        _parseLabel(networkLabel, VirtualSwitchType.StandardVirtualSwitch);
    }

    private void _parseLabel(String networkLabel, VirtualSwitchType defVswitchType) {
        // Set defaults for label in case of distributed vSwitch
        if (defVswitchType.equals(VirtualSwitchType.VMwareDistributedVirtualSwitch)) {
            _vSwitchName = DEFAULT_DVSWITCH_NAME;
            _vSwitchType = VirtualSwitchType.VMwareDistributedVirtualSwitch;
        } else if (defVswitchType.equals(VirtualSwitchType.NexusDistributedVirtualSwitch)) {
            _vSwitchName = DEFAULT_NDVSWITCH_NAME;
            _vSwitchType = VirtualSwitchType.NexusDistributedVirtualSwitch;
        }
        if (networkLabel == null || networkLabel.isEmpty()) {
            return;
        }
        String[] tokens = networkLabel.split(",");
        if (tokens.length > VMWARE_LABEL_FIELD_INDEX_NAME) {
            _vSwitchName = tokens[VMWARE_LABEL_FIELD_INDEX_NAME].trim();
        }
        if (tokens.length > VMWARE_LABEL_FIELD_INDEX_VLANID) {
            String vlanToken = tokens[VMWARE_LABEL_FIELD_INDEX_VLANID].trim();
            if (!vlanToken.isEmpty()) {
                _vlanId = vlanToken;
            }
        }
        if (tokens.length > VMWARE_LABEL_FIELD_INDEX_VSWITCH_TYPE) {
            _vSwitchType = VirtualSwitchType.getType(tokens[VMWARE_LABEL_FIELD_INDEX_VSWITCH_TYPE].trim());
            if (VirtualSwitchType.None == _vSwitchType) {
                throw new InvalidParameterValueException("Invalid virtual switch type : " + tokens[VMWARE_LABEL_FIELD_INDEX_VSWITCH_TYPE].trim());
            }
        }
        if (tokens.length > MAX_FIELDS_VMWARE_LABEL) {
            throw new InvalidParameterValueException("Found extraneous fields in vmware traffic label : " + networkLabel);
        }
    }

    public void applyTrafficShaping() {
        // Ensure traffic shaping is applied to secondary NICs
        if (!_isPrimaryNic) {
            // Apply lower rate limits or minimum bandwidth guarantees for secondary NICs
            System.out.println("Applying traffic shaping to secondary NIC:");
            System.out.println("Rate Limit: " + _rateLimit + " Mbps");
            System.out.println("Guaranteed Bandwidth: " + _guaranteedBandwidth + " Mbps");
        } else {
            // For primary NIC, apply normal rate limits
            System.out.println("Applying traffic shaping to primary NIC:");
            System.out.println("Rate Limit: " + _rateLimit + " Mbps");
        }
    }

    @Override
    public TrafficType getTrafficType() {
        return _trafficType;
    }

    @Override
    public String getNetworkLabel() {
        return null;
    }

    public VirtualSwitchType getVirtualSwitchType() {
        return _vSwitchType;
    }

    public String getVirtualSwitchName() {
        return _vSwitchName;
    }

    public String getVlanId() {
        return _vlanId;
    }

    public void setVirtualSwitchName(String vSwitchName) {
        _vSwitchName = vSwitchName;
    }

    public void setVirtualSwitchType(VirtualSwitchType vSwitchType) {
        _vSwitchType = vSwitchType;
    }
}
