package org.apache.cloudstack.resource;

import com.cloud.network.dao.NetworkVO;
import com.cloud.network.vpc.VpcVO;

import java.util.Objects;

public class NsxOpObject {
    VpcVO vpcVO;
    NetworkVO networkVO;
    long accountId;
    long domainId;
    long zoneId;

    public VpcVO getVpcVO() {
        return vpcVO;
    }

    public void setVpcVO(VpcVO vpcVO) {
        this.vpcVO = vpcVO;
    }

    public NetworkVO getNetworkVO() {
        return networkVO;
    }

    public void setNetworkVO(NetworkVO networkVO) {
        this.networkVO = networkVO;
    }

    public long getAccountId() {
        return accountId;
    }

    public void setAccountId(long accountId) {
        this.accountId = accountId;
    }

    public long getDomainId() {
        return domainId;
    }

    public void setDomainId(long domainId) {
        this.domainId = domainId;
    }

    public long getZoneId() {
        return zoneId;
    }

    public void setZoneId(long zoneId) {
        this.zoneId = zoneId;
    }

    public String getNetworkResourceName() {
        return Objects.nonNull(vpcVO) ? vpcVO.getName() : networkVO.getName();
    }

    public boolean isVpcResource() {
        return Objects.nonNull(vpcVO);
    }

    public long getNetworkResourceId() {
        return Objects.nonNull(vpcVO) ? vpcVO.getId() : networkVO.getId();
    }

    public static final class Builder {
        VpcVO vpcVO;
        NetworkVO networkVO;
        long accountId;
        long domainId;
        long zoneId;

        public Builder() {
            // Default constructor
        }

        public Builder vpcVO(VpcVO vpcVO) {
            this.vpcVO = vpcVO;
            return this;
        }

        public Builder networkVO(NetworkVO networkVO) {
            this.networkVO = networkVO;
            return this;
        }

        public Builder domainId(long domainId) {
            this.domainId = domainId;
            return this;
        }

        public Builder accountId(long accountId) {
            this.accountId = accountId;
            return this;
        }

        public Builder zoneId(long zoneId) {
            this.zoneId = zoneId;
            return this;
        }

        public NsxOpObject build() {
            NsxOpObject object = new NsxOpObject();
            object.setVpcVO(this.vpcVO);
            object.setNetworkVO(this.networkVO);
            object.setDomainId(this.domainId);
            object.setAccountId(this.accountId);
            object.setZoneId(this.zoneId);
            return object;
        }
    }
}
