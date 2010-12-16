package com.cloud.network.security;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.SecondaryTable;
import javax.persistence.Table;

@Entity
@Table(name=("security_group"))
@SecondaryTable(name="security_ingress_rule", join="left",
        pkJoinColumns={@PrimaryKeyJoinColumn(name="id", referencedColumnName="security_group_id")})
public class SecurityGroupRulesVO implements SecurityGroupRules {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="id")
    private long id;

    @Column(name="name")
    private String name;

    @Column(name="description")
    private String description;

    @Column(name="domain_id")
    private Long domainId;

    @Column(name="account_id")
    private Long accountId;

    @Column(name="account_name")
    private String accountName;

    @Column(name="id", table="security_ingress_rule", insertable=false, updatable=false)
    private Long ruleId;

    @Column(name="start_port", table="security_ingress_rule", insertable=false, updatable=false)
    private int startPort;

    @Column(name="end_port", table="security_ingress_rule", insertable=false, updatable=false)
    private int endPort;

    @Column(name="protocol", table="security_ingress_rule", insertable=false, updatable=false)
    private String protocol;

    @Column(name="allowed_network_id", table="security_ingress_rule", insertable=false, updatable=false, nullable=true)
    private Long allowedNetworkId = null;

    @Column(name="allowed_security_group", table="security_ingress_rule", insertable=false, updatable=false, nullable=true)
    private String allowedSecurityGroup = null;

    @Column(name="allowed_sec_grp_acct", table="security_ingress_rule", insertable=false, updatable=false, nullable=true)
    private String allowedSecGrpAcct = null;

    @Column(name="allowed_ip_cidr", table="security_ingress_rule", insertable=false, updatable=false, nullable=true)
    private String allowedSourceIpCidr = null;

    public SecurityGroupRulesVO() { }

    public SecurityGroupRulesVO(long id, String name, String description, Long domainId, Long accountId, String accountName, Long ruleId, int startPort, int endPort, String protocol, Long allowedNetworkId, String allowedSecurityGroup, String allowedSecGrpAcct, String allowedSourceIpCidr) {
    	this.id = id;
    	this.name = name;
    	this.description = description;
    	this.domainId = domainId;
    	this.accountId = accountId;
    	this.accountName = accountName;
    	this.ruleId = ruleId;
    	this.startPort = startPort;
    	this.endPort = endPort;
    	this.protocol = protocol;
    	this.allowedNetworkId = allowedNetworkId;
    	this.allowedSecurityGroup = allowedSecurityGroup;
    	this.allowedSecGrpAcct = allowedSecGrpAcct;
    	this.allowedSourceIpCidr = allowedSourceIpCidr;
    }

	public long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public Long getDomainId() {
		return domainId;
	}

	public Long getAccountId() {
		return accountId;
	}

	public String getAccountName() {
	    return accountName;
	}

	public Long getRuleId() {
		return ruleId;
	}

	public int getStartPort() {
		return startPort;
	}

	public int getEndPort() {
		return endPort;
	}

	public String getProtocol() {
		return protocol;
	}

	public Long getAllowedNetworkId() {
		return allowedNetworkId;
	}

	public String getAllowedSecurityGroup() {
	    return allowedSecurityGroup;
	}

    public String getAllowedSecGrpAcct() {
        return allowedSecGrpAcct;
    }

	public String getAllowedSourceIpCidr() {
		return allowedSourceIpCidr;
	}
}
