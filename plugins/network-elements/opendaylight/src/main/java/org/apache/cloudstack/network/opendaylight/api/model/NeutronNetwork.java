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

import java.util.UUID;

import com.google.gson.annotations.SerializedName;

public class NeutronNetwork {

    private UUID id;
    private String name;
    private boolean shared;
    private String tenantId;
    @SerializedName("provider:network_type")
    private String networkType;
    @SerializedName("provider:segmentation_id")
    private Integer segmentationId;

    public NeutronNetwork() {
    }

    public NeutronNetwork(final UUID id, final String name, final boolean shared, final String tenantId, final String networkType, final Integer segmentationId) {
        this.id = id;
        this.name = name;
        this.shared = shared;
        this.tenantId = tenantId;
        this.networkType = networkType;
        this.segmentationId = segmentationId;
    }

    public UUID getId() {
        return id;
    }

    public void setId(final UUID uuid) {
        id = uuid;
    }

    public String getNetworkType() {
        return networkType;
    }

    public void setNetworkType(final String networkType) {
        this.networkType = networkType;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public boolean isShared() {
        return shared;
    }

    public void setShared(final boolean shared) {
        this.shared = shared;
    }

    public Integer getSegmentationId() {
        return segmentationId;
    }

    public void setSegmentationId(final Integer segmentationId) {
        this.segmentationId = segmentationId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(final String tenantId) {
        this.tenantId = tenantId;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (name == null ? 0 : name.hashCode());
        result = prime * result + (networkType == null ? 0 : networkType.hashCode());
        result = prime * result + (segmentationId == null ? 0 : segmentationId.hashCode());
        result = prime * result + (shared ? 1231 : 1237);
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
        NeutronNetwork other = (NeutronNetwork) obj;
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }
        if (networkType == null) {
            if (other.networkType != null) {
                return false;
            }
        } else if (!networkType.equals(other.networkType)) {
            return false;
        }
        if (segmentationId == null) {
            if (other.segmentationId != null) {
                return false;
            }
        } else if (!segmentationId.equals(other.segmentationId)) {
            return false;
        }
        if (shared != other.shared) {
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
        NeutronNetwork other = (NeutronNetwork) obj;
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }
        if (networkType == null) {
            if (other.networkType != null) {
                return false;
            }
        } else if (!networkType.equals(other.networkType)) {
            return false;
        }
        if (segmentationId == null) {
            if (other.segmentationId != null) {
                return false;
            }
        } else if (!segmentationId.equals(other.segmentationId)) {
            return false;
        }
        if (shared != other.shared) {
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

    @Override
    public String toString() {
        return "NeutronNetwork [uuid=" + id + ", networkType=" + networkType + ", name=" + name + ", shared=" + shared + ", segmentationId=" + segmentationId + ", tenantId="
                + tenantId + "]";
    }
}
