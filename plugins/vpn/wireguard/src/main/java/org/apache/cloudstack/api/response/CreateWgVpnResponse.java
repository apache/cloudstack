package org.apache.cloudstack.api.response;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;

@SuppressWarnings("unused")
public class CreateWgVpnResponse extends BaseResponse {

    @SerializedName(ApiConstants.ID)
    @Param(description = "the ID of the new wireguard vpn tunnel")
    private String id;

    @SerializedName(ApiConstants.PUBLIC_IP_ID)
    @Param(description = "the public ip address of the vpn server")
    private String publicIpId;

    @SerializedName(ApiConstants.PUBLIC_IP)
    @Param(description = "the public ip address of the vpn server")
    private String publicIp;

    @SerializedName("networkid")
    @Param(description = "id of the network accessible by this VPN")
    private String NetworkId;

    @SerializedName("publicport")
    @Param(description = "the public port used by this vpn server")
    private Integer PublicPort;

    @SerializedName("accountid")
    @Param(description = "the id of the account of the vpn")
    private String AccountId;

    @SerializedName(ApiConstants.ACCOUNT)
    @Param(description = "the account of the wireguard vpn")
    private String accountName;

    @SerializedName(ApiConstants.DOMAIN_ID)
    @Param(description = "the domain id of the account of the wireguard vpn")
    private String domainId;

    @SerializedName(ApiConstants.DOMAIN)
    @Param(description = "the domain name of the account of the wireguard vpn")
    private String domainName;

    @SerializedName(ApiConstants.FOR_DISPLAY)
    @Param(description = "is vpn for display to the regular user", since = "4.4", authorized = {RoleType.Admin})
    private Boolean forDisplay;

    @SerializedName("ip4enable")
    @Param(description = "does the vpn support ipv4 addresses")
    private Boolean IP4Enable;

    @SerializedName("ip4range")
    @Param(description = "IPv4 network to use internally for the vpn (CIDR notation)")
    private String IP4Range;

    @SerializedName("ip6enable")
    @Param(description = "does the vpn support ipv6 addresses")
    private Boolean IP6Enable;

    @SerializedName("ip6range")
    @Param(description = "IPv6 network to use internally for the vpn (CIDR notation)")
    private String IP6Range;

    @SerializedName(ApiConstants.STATE)
    @Param(description = "the state of the wireguard server")
    private String state;

    @SerializedName("publickey")
    @Param(description = "the server's public Key")
    private String ServerPublicKey;

    public void setId(String id) {
        this.id = id;
    }

    public void setPublicIpId(String publicIpId) {
        this.publicIpId = publicIpId;
    }

    public void setPublicIp(String publicIp) {
        this.publicIp = publicIp;
    }

    public void setNetworkId(String networkId) {
        NetworkId = networkId;
    }

    public void setPublicPort(Integer publicPort) {
        PublicPort = publicPort;
    }

    public void setAccountId(String accountId) {
        AccountId = accountId;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public void setDomainId(String domainId) {
        this.domainId = domainId;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    public void setForDisplay(Boolean forDisplay) {
        this.forDisplay = forDisplay;
    }

    public void setIP4Enable(Boolean IP4Enable) {
        this.IP4Enable = IP4Enable;
    }

    public void setIP4Range(String IP4Range) {
        this.IP4Range = IP4Range;
    }

    public void setIP6Enable(Boolean IP6Enable) {
        this.IP6Enable = IP6Enable;
    }

    public void setIP6Range(String IP6Range) {
        this.IP6Range = IP6Range;
    }

    public void setState(String state) {
        this.state = state;
    }

    public void setServerPublicKey(String serverPublicKey) {
        ServerPublicKey = serverPublicKey;
    }
}
