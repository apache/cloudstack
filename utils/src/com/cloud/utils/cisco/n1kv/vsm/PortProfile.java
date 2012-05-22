package com.cloud.utils.cisco.n1kv.vsm;

import com.cloud.utils.cisco.n1kv.vsm.VsmCommand.BindingType;
import com.cloud.utils.cisco.n1kv.vsm.VsmCommand.PortProfileType;
import com.cloud.utils.cisco.n1kv.vsm.VsmCommand.SwitchPortMode;

public class PortProfile {
    public PortProfileType type;
    public SwitchPortMode mode;
    public BindingType binding;
    public String profileName;
    public String inputPolicyMap;
    public String outputPolicyMap;
    public String vlan;
    public boolean status;
    public int maxPorts;

    PortProfile() {
        profileName = null;
        inputPolicyMap = null;
        outputPolicyMap = null;
        vlan = null;
        status = false;
        maxPorts = 32;
        type = PortProfileType.none;
        mode = SwitchPortMode.none;
        binding = BindingType.none;
    }
}
