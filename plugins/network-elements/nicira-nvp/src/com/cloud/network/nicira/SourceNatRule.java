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

    public void setToSourceIpAddressMax(String toSourceIpAddressMax) {
        this.toSourceIpAddressMax = toSourceIpAddressMax;
    }

    public String getToSourceIpAddressMin() {
        return toSourceIpAddressMin;
    }

    public void setToSourceIpAddressMin(String toSourceIpAddressMin) {
        this.toSourceIpAddressMin = toSourceIpAddressMin;
    }

    public Integer getToSourcePort() {
        return toSourcePort;
    }

    public void setToSourcePort(Integer toSourcePort) {
        this.toSourcePort = toSourcePort;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime
                * result
                + ((toSourceIpAddressMax == null) ? 0 : toSourceIpAddressMax
                        .hashCode());
        result = prime
                * result
                + ((toSourceIpAddressMin == null) ? 0 : toSourceIpAddressMin
                        .hashCode());
        result = prime * result
                + ((toSourcePort == null) ? 0 : toSourcePort.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        SourceNatRule other = (SourceNatRule) obj;
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
    public boolean equalsIgnoreUuid(Object obj) {
        if (this == obj)
            return true;
        if (!super.equalsIgnoreUuid(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        SourceNatRule other = (SourceNatRule) obj;
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
