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

package com.cloud.agent.api.guru;

import com.cloud.agent.api.CmdBuilder;
import com.cloud.agent.api.Command;

public class ReserveVmInterfaceVspCommand extends Command {

    private final String _nicUuid;
    private final String _nicMacAddress;
    private final String _networkUuid;
    private final boolean _isL3Network;
    private final boolean _isSharedNetwork;
    private final String _vpcUuid;
    private final String _networkDomainUuid;
    private final String _networksAccountUuid;
    private final boolean _isDomainRouter;
    private final String _domainRouterIp;
    private final String _vmInstanceName;
    private final String _vmUuid;
    private final String _vmUserName;
    private final String _vmUserDomainName;
    private final boolean _useStaticIp;
    private final String _staticIp;
    private final String _staticNatIpUuid;
    private final String _staticNatIpAddress;
    private final boolean _isStaticNatIpAllocated;
    private final boolean _isOneToOneNat;
    private final String _staticNatVlanUuid;
    private final String _staticNatVlanGateway;
    private final String _staticNatVlanNetmask;

    private ReserveVmInterfaceVspCommand(String nicUuid, String nicMacAddress, String networkUuid, boolean isL3Network, boolean isSharedNetwork, String vpcUuid, String networkDomainUuid,
            String networksAccountUuid, boolean isDomainRouter, String domainRouterIp, String vmInstanceName, String vmUuid, String vmUserName, String vmUserDomainName,
            boolean useStaticIp, String staticIp, String staticNatIpUuid, String staticNatIpAddress, boolean isStaticNatIpAllocated, boolean isOneToOneNat, String staticNatVlanUuid,
            String staticNatVlanGateway, String staticNatVlanNetmask) {
        super();
        this._nicUuid = nicUuid;
        this._nicMacAddress = nicMacAddress;
        this._networkUuid = networkUuid;
        this._isL3Network = isL3Network;
        this._isSharedNetwork = isSharedNetwork;
        this._vpcUuid = vpcUuid;
        this._networkDomainUuid = networkDomainUuid;
        this._networksAccountUuid = networksAccountUuid;
        this._isDomainRouter = isDomainRouter;
        this._domainRouterIp = domainRouterIp;
        this._vmInstanceName = vmInstanceName;
        this._vmUuid = vmUuid;
        this._vmUserName = vmUserName;
        this._vmUserDomainName = vmUserDomainName;
        this._useStaticIp = useStaticIp;
        this._staticIp = staticIp;
        this._staticNatIpUuid = staticNatIpUuid;
        this._staticNatIpAddress = staticNatIpAddress;
        this._isStaticNatIpAllocated = isStaticNatIpAllocated;
        this._isOneToOneNat = isOneToOneNat;
        this._staticNatVlanUuid = staticNatVlanUuid;
        this._staticNatVlanGateway = staticNatVlanGateway;
        this._staticNatVlanNetmask = staticNatVlanNetmask;
    }

    public String getNicUuid() {
        return _nicUuid;
    }

    public String getNicMacAddress() {
        return _nicMacAddress;
    }

    public String getNetworkUuid() {
        return _networkUuid;
    }

    public boolean isL3Network() {
        return _isL3Network;
    }

    public boolean isSharedNetwork() {
        return _isSharedNetwork;
    }

    public String getVpcUuid() {
        return _vpcUuid;
    }

    public String getNetworkDomainUuid() {
        return _networkDomainUuid;
    }

    public String getNetworksAccountUuid() {
        return _networksAccountUuid;
    }

    public boolean isDomainRouter() {
        return _isDomainRouter;
    }

    public String getDomainRouterIp() {
        return _domainRouterIp;
    }

    public String getVmInstanceName() {
        return _vmInstanceName;
    }

    public String getVmUuid() {
        return _vmUuid;
    }

    public String getVmUserName() {
        return _vmUserName;
    }

    public String getVmUserDomainName() {
        return _vmUserDomainName;
    }

    public boolean useStaticIp() {
        return _useStaticIp;
    }

    public String getStaticIp() {
        return _staticIp;
    }

    public String getStaticNatIpUuid() {
        return _staticNatIpUuid;
    }

    public String getStaticNatIpAddress() {
        return _staticNatIpAddress;
    }

    public boolean isStaticNatIpAllocated() {
        return _isStaticNatIpAllocated;
    }

    public boolean isOneToOneNat() {
        return _isOneToOneNat;
    }

    public String getStaticNatVlanUuid() {
        return _staticNatVlanUuid;
    }

    public String getStaticNatVlanGateway() {
        return _staticNatVlanGateway;
    }

    public String getStaticNatVlanNetmask() {
        return _staticNatVlanNetmask;
    }

    public static class Builder implements CmdBuilder<ReserveVmInterfaceVspCommand> {
        private String _nicUuid;
        private String _nicMacAddress;
        private String _networkUuid;
        private boolean _isL3Network;
        private boolean _isSharedNetwork;
        private String _vpcUuid;
        private String _networkDomainUuid;
        private String _networksAccountUuid;
        private boolean _isDomainRouter;
        private String _domainRouterIp;
        private String _vmInstanceName;
        private String _vmUuid;
        private String _vmUserName;
        private String _vmUserDomainName;
        private boolean _useStaticIp;
        private String _staticIp;
        private String _staticNatIpUuid;
        private String _staticNatIpAddress;
        private boolean _isStaticNatIpAllocated;
        private boolean _isOneToOneNat;
        private String _staticNatVlanUuid;
        private String _staticNatVlanGateway;
        private String _staticNatVlanNetmask;

        public Builder nicUuid(String nicUuid) {
            this._nicUuid = nicUuid;
            return this;
        }

        public Builder nicMacAddress(String nicMacAddress) {
            this._nicMacAddress = nicMacAddress;
            return this;
        }

        public Builder networkUuid(String networkUuid) {
            this._networkUuid = networkUuid;
            return this;
        }

        public Builder isL3Network(boolean isL3Network) {
            this._isL3Network = isL3Network;
            return this;
        }

        public Builder isSharedNetwork(boolean isSharedNetwork) {
            this._isSharedNetwork = isSharedNetwork;
            return this;
        }

        public Builder vpcUuid(String vpcUuid) {
            this._vpcUuid = vpcUuid;
            return this;
        }

        public Builder networkDomainUuid(String networkDomainUuid) {
            this._networkDomainUuid = networkDomainUuid;
            return this;
        }

        public Builder networksAccountUuid(String networksAccountUuid) {
            this._networksAccountUuid = networksAccountUuid;
            return this;
        }

        public Builder isDomainRouter(boolean isDomainRouter) {
            this._isDomainRouter = isDomainRouter;
            return this;
        }

        public Builder domainRouterIp(String domainRouterIp) {
            this._domainRouterIp = domainRouterIp;
            return this;
        }

        public Builder vmInstanceName(String vmInstanceName) {
            this._vmInstanceName = vmInstanceName;
            return this;
        }

        public Builder vmUuid(String vmUuid) {
            this._vmUuid = vmUuid;
            return this;
        }

        public Builder vmUserName(String vmUserName) {
            this._vmUserName = vmUserName;
            return this;
        }

        public Builder vmUserDomainName(String vmUserDomainName) {
            this._vmUserDomainName = vmUserDomainName;
            return this;
        }

        public Builder useStaticIp(boolean useStaticIp) {
            this._useStaticIp = useStaticIp;
            return this;
        }

        public Builder staticIp(String staticIp) {
            this._staticIp = staticIp;
            return this;
        }

        public Builder staticNatIpUuid(String staticNatIpUuid) {
            this._staticNatIpUuid = staticNatIpUuid;
            return this;
        }

        public Builder staticNatIpAddress(String staticNatIpAddress) {
            this._staticNatIpAddress = staticNatIpAddress;
            return this;
        }

        public Builder isStaticNatIpAllocated(boolean isStaticNatIpAllocated) {
            this._isStaticNatIpAllocated = isStaticNatIpAllocated;
            return this;
        }

        public Builder isOneToOneNat(boolean isOneToOneNat) {
            this._isOneToOneNat = isOneToOneNat;
            return this;
        }

        public Builder staticNatVlanUuid(String staticNatVlanUuid) {
            this._staticNatVlanUuid = staticNatVlanUuid;
            return this;
        }

        public Builder staticNatVlanGateway(String staticNatVlanGateway) {
            this._staticNatVlanGateway = staticNatVlanGateway;
            return this;
        }

        public Builder staticNatVlanNetmask(String staticNatVlanNetmask) {
            this._staticNatVlanNetmask = staticNatVlanNetmask;
            return this;
        }

        @Override
        public ReserveVmInterfaceVspCommand build() {
            return new ReserveVmInterfaceVspCommand(_nicUuid, _nicMacAddress, _networkUuid, _isL3Network, _isSharedNetwork, _vpcUuid, _networkDomainUuid, _networksAccountUuid,
                    _isDomainRouter, _domainRouterIp, _vmInstanceName, _vmUuid, _vmUserName, _vmUserDomainName, _useStaticIp, _staticIp, _staticNatIpUuid, _staticNatIpAddress,
                    _isStaticNatIpAllocated, _isOneToOneNat, _staticNatVlanUuid, _staticNatVlanGateway, _staticNatVlanNetmask);
        }
    }

    @Override
    public boolean executeInSequence() {
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ReserveVmInterfaceVspCommand)) return false;
        if (!super.equals(o)) return false;

        ReserveVmInterfaceVspCommand that = (ReserveVmInterfaceVspCommand) o;

        if (_isDomainRouter != that._isDomainRouter) return false;
        if (_isL3Network != that._isL3Network) return false;
        if (_isOneToOneNat != that._isOneToOneNat) return false;
        if (_isSharedNetwork != that._isSharedNetwork) return false;
        if (_isStaticNatIpAllocated != that._isStaticNatIpAllocated) return false;
        if (_useStaticIp != that._useStaticIp) return false;
        if (_domainRouterIp != null ? !_domainRouterIp.equals(that._domainRouterIp) : that._domainRouterIp != null)
            return false;
        if (_networkDomainUuid != null ? !_networkDomainUuid.equals(that._networkDomainUuid) : that._networkDomainUuid != null)
            return false;
        if (_networkUuid != null ? !_networkUuid.equals(that._networkUuid) : that._networkUuid != null) return false;
        if (_networksAccountUuid != null ? !_networksAccountUuid.equals(that._networksAccountUuid) : that._networksAccountUuid != null)
            return false;
        if (_nicMacAddress != null ? !_nicMacAddress.equals(that._nicMacAddress) : that._nicMacAddress != null)
            return false;
        if (_nicUuid != null ? !_nicUuid.equals(that._nicUuid) : that._nicUuid != null) return false;
        if (_staticIp != null ? !_staticIp.equals(that._staticIp) : that._staticIp != null) return false;
        if (_staticNatIpAddress != null ? !_staticNatIpAddress.equals(that._staticNatIpAddress) : that._staticNatIpAddress != null)
            return false;
        if (_staticNatIpUuid != null ? !_staticNatIpUuid.equals(that._staticNatIpUuid) : that._staticNatIpUuid != null)
            return false;
        if (_staticNatVlanGateway != null ? !_staticNatVlanGateway.equals(that._staticNatVlanGateway) : that._staticNatVlanGateway != null)
            return false;
        if (_staticNatVlanNetmask != null ? !_staticNatVlanNetmask.equals(that._staticNatVlanNetmask) : that._staticNatVlanNetmask != null)
            return false;
        if (_staticNatVlanUuid != null ? !_staticNatVlanUuid.equals(that._staticNatVlanUuid) : that._staticNatVlanUuid != null)
            return false;
        if (_vmInstanceName != null ? !_vmInstanceName.equals(that._vmInstanceName) : that._vmInstanceName != null)
            return false;
        if (_vmUserDomainName != null ? !_vmUserDomainName.equals(that._vmUserDomainName) : that._vmUserDomainName != null)
            return false;
        if (_vmUserName != null ? !_vmUserName.equals(that._vmUserName) : that._vmUserName != null) return false;
        if (_vmUuid != null ? !_vmUuid.equals(that._vmUuid) : that._vmUuid != null) return false;
        if (_vpcUuid != null ? !_vpcUuid.equals(that._vpcUuid) : that._vpcUuid != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (_nicUuid != null ? _nicUuid.hashCode() : 0);
        result = 31 * result + (_nicMacAddress != null ? _nicMacAddress.hashCode() : 0);
        result = 31 * result + (_networkUuid != null ? _networkUuid.hashCode() : 0);
        result = 31 * result + (_isL3Network ? 1 : 0);
        result = 31 * result + (_isSharedNetwork ? 1 : 0);
        result = 31 * result + (_vpcUuid != null ? _vpcUuid.hashCode() : 0);
        result = 31 * result + (_networkDomainUuid != null ? _networkDomainUuid.hashCode() : 0);
        result = 31 * result + (_networksAccountUuid != null ? _networksAccountUuid.hashCode() : 0);
        result = 31 * result + (_isDomainRouter ? 1 : 0);
        result = 31 * result + (_domainRouterIp != null ? _domainRouterIp.hashCode() : 0);
        result = 31 * result + (_vmInstanceName != null ? _vmInstanceName.hashCode() : 0);
        result = 31 * result + (_vmUuid != null ? _vmUuid.hashCode() : 0);
        result = 31 * result + (_vmUserName != null ? _vmUserName.hashCode() : 0);
        result = 31 * result + (_vmUserDomainName != null ? _vmUserDomainName.hashCode() : 0);
        result = 31 * result + (_useStaticIp ? 1 : 0);
        result = 31 * result + (_staticIp != null ? _staticIp.hashCode() : 0);
        result = 31 * result + (_staticNatIpUuid != null ? _staticNatIpUuid.hashCode() : 0);
        result = 31 * result + (_staticNatIpAddress != null ? _staticNatIpAddress.hashCode() : 0);
        result = 31 * result + (_isStaticNatIpAllocated ? 1 : 0);
        result = 31 * result + (_isOneToOneNat ? 1 : 0);
        result = 31 * result + (_staticNatVlanUuid != null ? _staticNatVlanUuid.hashCode() : 0);
        result = 31 * result + (_staticNatVlanGateway != null ? _staticNatVlanGateway.hashCode() : 0);
        result = 31 * result + (_staticNatVlanNetmask != null ? _staticNatVlanNetmask.hashCode() : 0);
        return result;
    }
}
