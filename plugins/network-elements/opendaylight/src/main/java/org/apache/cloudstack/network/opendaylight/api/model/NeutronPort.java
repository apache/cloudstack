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

package org.apache.cloudstack.network.opendaylight.api.model;

import java.util.List;
import java.util.UUID;

public class NeutronPort {

    private UUID id;
    private String name;
    private String tenantId;
    private UUID networkId;
    private String macAddress;
    private UUID deviceId;
    private boolean adminStateUp;
    private String status;
    private List<String> fixedIps;

    public NeutronPort() {
    }

    public NeutronPort(final UUID id, final String name, final String tenantId, final UUID networkId, final String macAddress, final UUID deviceId, final boolean adminStateUp,
            final String status, final List<String> fixedIps) {
        this.id = id;
        this.name = name;
        this.tenantId = tenantId;
        this.networkId = networkId;
        this.macAddress = macAddress;
        this.deviceId = deviceId;
        this.adminStateUp = adminStateUp;
        this.status = status;
        this.fixedIps = fixedIps;
    }

    public UUID getId() {
        return id;
    }

    public void setId(final UUID uuid) {
        id = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(final String tenantId) {
        this.tenantId = tenantId;
    }

    public UUID getNetworkId() {
        return networkId;
    }

    public void setNetworkId(final UUID networkId) {
        this.networkId = networkId;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public void setMacAddress(final String macAddress) {
        this.macAddress = macAddress;
    }

    public UUID getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(final UUID deviceId) {
        this.deviceId = deviceId;
    }

    public boolean isAdminStateUp() {
        return adminStateUp;
    }

    public void setAdminStateUp(final boolean adminStateUp) {
        this.adminStateUp = adminStateUp;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(final String status) {
        this.status = status;
    }

    public List<String> getFixedIps() {
        return fixedIps;
    }

    public void setFixedIps(final List<String> fixedIps) {
        this.fixedIps = fixedIps;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (adminStateUp ? 1231 : 1237);
        result = prime * result + (deviceId == null ? 0 : deviceId.hashCode());
        result = prime * result + (macAddress == null ? 0 : macAddress.hashCode());
        result = prime * result + (name == null ? 0 : name.hashCode());
        result = prime * result + (networkId == null ? 0 : networkId.hashCode());
        result = prime * result + (status == null ? 0 : status.hashCode());
        result = prime * result + (tenantId == null ? 0 : tenantId.hashCode());
        result = prime * result + (id == null ? 0 : id.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        NeutronPort other = (NeutronPort) obj;
        if (adminStateUp != other.adminStateUp) {
            return false;
        }
        if (deviceId == null) {
            if (other.deviceId != null) {
                return false;
            }
        } else if (!deviceId.equals(other.deviceId)) {
            return false;
        }
        if (macAddress == null) {
            if (other.macAddress != null) {
                return false;
            }
        } else if (!macAddress.equals(other.macAddress)) {
            return false;
        }
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }
        if (networkId == null) {
            if (other.networkId != null) {
                return false;
            }
        } else if (!networkId.equals(other.networkId)) {
            return false;
        }
        if (status == null) {
            if (other.status != null) {
                return false;
            }
        } else if (!status.equals(other.status)) {
            return false;
        }
        if (tenantId == null) {
            if (other.tenantId != null) {
                return false;
            }
        } else if (!tenantId.equals(other.tenantId)) {
            return false;
        }
        if (id == null) {
            if (other.id != null) {
                return false;
            }
        } else if (!id.equals(other.id)) {
            return false;
        }
        return true;
    }

    public boolean equalsIgnoreUuid(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        NeutronPort other = (NeutronPort) obj;
        if (adminStateUp != other.adminStateUp) {
            return false;
        }
        if (deviceId == null) {
            if (other.deviceId != null) {
                return false;
            }
        } else if (!deviceId.equals(other.deviceId)) {
            return false;
        }
        if (macAddress == null) {
            if (other.macAddress != null) {
                return false;
            }
        } else if (!macAddress.equals(other.macAddress)) {
            return false;
        }
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }
        if (networkId == null) {
            if (other.networkId != null) {
                return false;
            }
        } else if (!networkId.equals(other.networkId)) {
            return false;
        }
        if (status == null) {
            if (other.status != null) {
                return false;
            }
        } else if (!status.equals(other.status)) {
            return false;
        }
        if (tenantId == null) {
            if (other.tenantId != null) {
                return false;
            }
        } else if (!tenantId.equals(other.tenantId)) {
            return false;
        }
        return true;
    }
}
