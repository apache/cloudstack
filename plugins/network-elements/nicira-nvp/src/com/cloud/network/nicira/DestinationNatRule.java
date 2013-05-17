package com.cloud.network.nicira;

public class DestinationNatRule extends NatRule {
    private String toDestinationIpAddress;
    private Integer toDestinationPort;
    
    public DestinationNatRule() {
        setType("DestinationNatRule");
    }

    public String getToDestinationIpAddress() {
        return toDestinationIpAddress;
    }


    public void setToDestinationIpAddress(String toDestinationIpAddress) {
        this.toDestinationIpAddress = toDestinationIpAddress;
    }


    public Integer getToDestinationPort() {
        return toDestinationPort;
    }


    public void setToDestinationPort(Integer toDestinationPort) {
        this.toDestinationPort = toDestinationPort;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime
                * result
                + ((toDestinationIpAddress == null) ? 0
                        : toDestinationIpAddress.hashCode());
        result = prime
                * result
                + ((toDestinationPort == null) ? 0 : toDestinationPort
                        .hashCode());
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
        DestinationNatRule other = (DestinationNatRule) obj;
        if (toDestinationIpAddress == null) {
            if (other.toDestinationIpAddress != null)
                return false;
        } else if (!toDestinationIpAddress.equals(other.toDestinationIpAddress))
            return false;
        if (toDestinationPort == null) {
            if (other.toDestinationPort != null)
                return false;
        } else if (!toDestinationPort.equals(other.toDestinationPort))
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
        DestinationNatRule other = (DestinationNatRule) obj;
        if (toDestinationIpAddress == null) {
            if (other.toDestinationIpAddress != null)
                return false;
        } else if (!toDestinationIpAddress.equals(other.toDestinationIpAddress))
            return false;
        if (toDestinationPort == null) {
            if (other.toDestinationPort != null)
                return false;
        } else if (!toDestinationPort.equals(other.toDestinationPort))
            return false;
        return true;
    }

}
