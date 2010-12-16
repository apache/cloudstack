package com.cloud.network.security.dao;

import java.util.List;

import com.cloud.network.security.SecurityGroupRulesVO;
import com.cloud.utils.db.GenericDao;

public interface SecurityGroupRulesDao extends GenericDao<SecurityGroupRulesVO, Long> {
	/**
	 * List a security group and associated ingress rules
	 * @param accountId the account id of the owner of the security group
	 * @param groupName the name of the group for which to list rules
	 * @return the list of ingress rules associated with the security group (and security group info)
	 */
	List<SecurityGroupRulesVO> listSecurityGroupRules(long accountId, String groupName);

	/**
	 * List security groups and associated ingress rules
	 * @param accountId the id of the account for which to list groups and associated rules
	 * @return the list of security groups with associated ingress rules
	 */
	List<SecurityGroupRulesVO> listSecurityGroupRules(long accountId);

    /**
     * List all security groups and associated ingress rules
     * @return the list of security groups with associated ingress rules
     */
    List<SecurityGroupRulesVO> listSecurityGroupRules();
    
    /**
     * List all security rules belonging to the specific group
     * @return the security group with associated ingress rules
     */
    List<SecurityGroupRulesVO> listSecurityRulesByGroupId(long groupId);
}
