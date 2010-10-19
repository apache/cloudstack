package com.cloud.network.security.dao;

import java.util.List;

import com.cloud.network.security.SecurityGroupRulesVO;
import com.cloud.utils.db.GenericDao;

public interface NetworkGroupRulesDao extends GenericDao<SecurityGroupRulesVO, Long> {
	/**
	 * List a network group and associated ingress rules
	 * @param accountId the account id of the owner of the network group
	 * @param groupName the name of the group for which to list rules
	 * @return the list of ingress rules associated with the network group (and network group info)
	 */
	List<SecurityGroupRulesVO> listNetworkGroupRules(long accountId, String groupName);

	/**
	 * List network groups and associated ingress rules
	 * @param accountId the id of the account for which to list groups and associated rules
	 * @return the list of network groups with associated ingress rules
	 */
	List<SecurityGroupRulesVO> listNetworkGroupRules(long accountId);

    /**
     * List all network groups and associated ingress rules
     * @return the list of network groups with associated ingress rules
     */
    List<SecurityGroupRulesVO> listNetworkGroupRules();
}
