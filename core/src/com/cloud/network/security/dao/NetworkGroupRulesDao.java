package com.cloud.network.security.dao;

import java.util.List;

import com.cloud.network.security.NetworkGroupRulesVO;
import com.cloud.utils.db.GenericDao;

public interface NetworkGroupRulesDao extends GenericDao<NetworkGroupRulesVO, Long> {
	/**
	 * List a network group and associated ingress rules
	 * @param accountId the account id of the owner of the network group
	 * @param groupName the name of the group for which to list rules
	 * @return the list of ingress rules associated with the network group (and network group info)
	 */
	List<NetworkGroupRulesVO> listNetworkGroupRules(long accountId, String groupName);

	/**
	 * List network groups and associated ingress rules
	 * @param accountId the id of the account for which to list groups and associated rules
	 * @return the list of network groups with associated ingress rules
	 */
	List<NetworkGroupRulesVO> listNetworkGroupRules(long accountId);

    /**
     * List all network groups and associated ingress rules
     * @return the list of network groups with associated ingress rules
     */
    List<NetworkGroupRulesVO> listNetworkGroupRules();

    /**
     * List network groups and associated ingress rules for a particular domain
     * @param domainId the id of the domain for which to list groups and associated rules
     * @param recursive whether or not to recursively search the domain for network groups
     * @return the list of network groups with associated ingress rules
     */
	List<NetworkGroupRulesVO> listNetworkGroupRulesByDomain(long domainId, boolean recursive);
}
