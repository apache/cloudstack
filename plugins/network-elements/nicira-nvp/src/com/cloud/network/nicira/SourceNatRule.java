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

package com.cloud.network.nicira;

public class SourceNatRule extends NatRule {
    private String toSourceIpAddressMax;
    private String toSourceIpAddressMin;
    private Integer toSourcePort;

    public SourceNatRule() {
        setType("SourceNatRule");
    }

    public String getToSourceIpAddressMax() {
        return toSourceIpAddressMax;
    }

    public void setToSourceIpAddressMax(final String toSourceIpAddressMax) {
        this.toSourceIpAddressMax = toSourceIpAddressMax;
    }

    public String getToSourceIpAddressMin() {
        return toSourceIpAddressMin;
    }

    public void setToSourceIpAddressMin(final String toSourceIpAddressMin) {
        this.toSourceIpAddressMin = toSourceIpAddressMin;
    }

    public Integer getToSourcePort() {
        return toSourcePort;
    }

    public void setToSourcePort(final Integer toSourcePort) {
        this.toSourcePort = toSourcePort;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((toSourceIpAddressMax == null) ? 0 : toSourceIpAddressMax.hashCode());
        result = prime * result + ((toSourceIpAddressMin == null) ? 0 : toSourceIpAddressMin.hashCode());
        result = prime * result + ((toSourcePort == null) ? 0 : toSourcePort.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        SourceNatRule other = (SourceNatRule)obj;
        if (toSourceIpAddressMax == null) {
            if (other.toSourceIpAddressMax != null)
                return false;
        } else if (!toSourceIpAddressMax.equals(other.toSourceIpAddressMax))
            return false;
        if (toSourceIpAddressMin == null) {
            if (other.toSourceIpAddressMin != null)
                return false;
        } else if (!toSourceIpAddressMin.equals(other.toSourceIpAddressMin))
            return false;
        if (toSourcePort == null) {
            if (other.toSourcePort != null)
                return false;
        } else if (!toSourcePort.equals(other.toSourcePort))
            return false;
        return true;
    }

    @Override
    public boolean equalsIgnoreUuid(final Object obj) {
        if (this == obj)
            return true;
        if (!super.equalsIgnoreUuid(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        SourceNatRule other = (SourceNatRule)obj;
        if (toSourceIpAddressMax == null) {
            if (other.toSourceIpAddressMax != null)
                return false;
        } else if (!toSourceIpAddressMax.equals(other.toSourceIpAddressMax))
            return false;
        if (toSourceIpAddressMin == null) {
            if (other.toSourceIpAddressMin != null)
                return false;
        } else if (!toSourceIpAddressMin.equals(other.toSourceIpAddressMin))
            return false;
        if (toSourcePort == null) {
            if (other.toSourcePort != null)
                return false;
        } else if (!toSourcePort.equals(other.toSourcePort))
            return false;
        return true;
    }

}
