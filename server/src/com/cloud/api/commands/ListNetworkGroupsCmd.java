package com.cloud.api.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.cloud.api.BaseCmd;
import com.cloud.api.ServerApiException;
import com.cloud.async.executor.IngressRuleResultObject;
import com.cloud.async.executor.NetworkGroupResultObject;
import com.cloud.network.security.NetworkGroupRulesVO;
import com.cloud.server.Criteria;
import com.cloud.user.Account;
import com.cloud.utils.Pair;
import com.cloud.vm.UserVmVO;

public class ListNetworkGroupsCmd extends BaseCmd {
	public static final Logger s_logger = Logger.getLogger(ListNetworkGroupsCmd.class.getName());

    private static final String s_name = "listnetworkgroupsresponse";
    private static final List<Pair<Enum, Boolean>> s_properties = new ArrayList<Pair<Enum, Boolean>>();

    static {
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ACCOUNT_OBJ, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ACCOUNT, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.DOMAIN_ID, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.NETWORK_GROUP_NAME, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.VIRTUAL_MACHINE_ID, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.KEYWORD, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.PAGE, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.PAGESIZE, Boolean.FALSE));
    }

    public String getName() {
        return s_name;
    }

    public List<Pair<Enum, Boolean>> getProperties() {
        return s_properties;
    }

	@Override
	public List<Pair<String, Object>> execute(Map<String, Object> params) {
        Account account = (Account)params.get(BaseCmd.Properties.ACCOUNT_OBJ.getName());
        String accountName = (String)params.get(BaseCmd.Properties.ACCOUNT.getName());
        Long domainId = (Long)params.get(BaseCmd.Properties.DOMAIN_ID.getName());
        String networkGroup = (String)params.get(BaseCmd.Properties.NETWORK_GROUP_NAME.getName());
        Long vmId = (Long)params.get(BaseCmd.Properties.VIRTUAL_MACHINE_ID.getName());
        String keyword = (String)params.get(BaseCmd.Properties.KEYWORD.getName());
        Integer page = (Integer)params.get(BaseCmd.Properties.PAGE.getName());
        Integer pageSize = (Integer)params.get(BaseCmd.Properties.PAGESIZE.getName());

        Long accountId = null;
        boolean recursive = false;

        // permissions check
        if ((account == null) || isAdmin(account.getType())) {
            if (domainId != null) {
                if ((account != null) && !getManagementServer().isChildDomain(account.getDomainId(), domainId)) {
                    throw new ServerApiException(BaseCmd.ACCOUNT_ERROR, "Unable to list network groups for account " + accountName + " in domain " + domainId + "; permission denied.");
                }
                if (accountName != null) {
                    Account acct = getManagementServer().findActiveAccount(accountName, domainId);
                    if (acct != null) {
                        accountId = acct.getId();
                    } else {
                        throw new ServerApiException(BaseCmd.PARAM_ERROR, "Unable to find account " + accountName + " in domain " + domainId);
                    }
                }
            } else if (vmId != null) {
                UserVmVO userVM = getManagementServer().findUserVMInstanceById(vmId);
                if (userVM == null) {
                    throw new ServerApiException(BaseCmd.PARAM_ERROR, "Unable to list network groups for virtual machine instance " + vmId + "; instance not found.");
                }
                if ((account != null) && !getManagementServer().isChildDomain(account.getDomainId(), userVM.getDomainId())) {
                    throw new ServerApiException(BaseCmd.ACCOUNT_ERROR, "Unable to list network groups for virtual machine instance " + vmId + "; permission denied.");
                }
            } else if (account != null) {
                // either an admin is searching for their own group, or admin is listing all groups and the search needs to be restricted to domain admin's domain
                if (networkGroup != null) {
                    accountId = account.getId();
                } else if (account.getType() != Account.ACCOUNT_TYPE_ADMIN) {
                    domainId = account.getDomainId();
                    recursive = true;
                }
            }
    	} else {
    	    if (vmId != null) {
                UserVmVO userVM = getManagementServer().findUserVMInstanceById(vmId);
                if (userVM == null) {
                    throw new ServerApiException(BaseCmd.PARAM_ERROR, "Unable to list network groups for virtual machine instance " + vmId + "; instance not found.");
                }

                if (account != null) {
                    // check that the user is the owner of the VM (admin case was already verified
                    if (account.getId().longValue() != userVM.getAccountId()) {
                        throw new ServerApiException(BaseCmd.ACCOUNT_ERROR, "Unable to list network groups for virtual machine instance " + vmId + "; permission denied.");
                    }
                }
    	    } else {
                accountId = ((account != null) ? account.getId() : null);
    	    }
    	}

        Long startIndex = Long.valueOf(0);
        int pageSizeNum = 50;
        if (pageSize != null) {
            pageSizeNum = pageSize.intValue();
        }
        if (page != null) {
            int pageNum = page.intValue();
            if (pageNum > 0) {
                startIndex = Long.valueOf(pageSizeNum * (pageNum-1));
            }
        }

        Criteria c = new Criteria("id", Boolean.TRUE, startIndex, Long.valueOf(pageSizeNum));
        c.addCriteria(Criteria.ACCOUNTID, accountId);
        c.addCriteria(Criteria.DOMAINID, domainId);
        c.addCriteria(Criteria.NETWORKGROUP, networkGroup);
        c.addCriteria(Criteria.INSTANCEID, vmId);
        c.addCriteria(Criteria.ISRECURSIVE, recursive);
        c.addCriteria(Criteria.KEYWORD, keyword);

        List<NetworkGroupRulesVO> groups = getManagementServer().searchForNetworkGroupRules(c);
    	List<NetworkGroupResultObject> groupResultObjs = NetworkGroupResultObject.transposeNetworkGroups(groups);
    	Object[] groupDataArray = null;
    	if (groupResultObjs != null) {
            groupDataArray = new Object[groupResultObjs.size()];
            int i = 0;
    	    for (NetworkGroupResultObject groupResultObj : groupResultObjs) {
                List<Pair<String, Object>> groupData = new ArrayList<Pair<String, Object>>();
                groupData.add(new Pair<String, Object>(BaseCmd.Properties.ID.getName(), groupResultObj.getId().toString()));
                groupData.add(new Pair<String, Object>(BaseCmd.Properties.NAME.getName(), groupResultObj.getName()));
                groupData.add(new Pair<String, Object>(BaseCmd.Properties.DESCRIPTION.getName(), groupResultObj.getDescription()));
                groupData.add(new Pair<String, Object>(BaseCmd.Properties.ACCOUNT.getName(), groupResultObj.getAccountName()));
                groupData.add(new Pair<String, Object>(BaseCmd.Properties.DOMAIN_ID.getName(), groupResultObj.getDomainId().toString()));
                groupData.add(new Pair<String, Object>(BaseCmd.Properties.DOMAIN.getName(), getManagementServer().findDomainIdById(groupResultObj.getDomainId()).getName()));

    	        List<IngressRuleResultObject> ingressRules = groupResultObj.getIngressRules();
    	        if ((ingressRules != null) && !ingressRules.isEmpty()) {
                    Object[] ingressDataArray = new Object[ingressRules.size()];
                    int j = 0;
                    for (IngressRuleResultObject ingressRule : ingressRules) {
                        List<Pair<String, Object>> ingressData = new ArrayList<Pair<String, Object>>();

                        ingressData.add(new Pair<String, Object>(BaseCmd.Properties.RULE_ID.getName(), ingressRule.getId().toString()));
                        ingressData.add(new Pair<String, Object>(BaseCmd.Properties.PROTOCOL.getName(), ingressRule.getProtocol()));
                        if ("icmp".equalsIgnoreCase(ingressRule.getProtocol())) {
                            ingressData.add(new Pair<String, Object>(BaseCmd.Properties.ICMP_TYPE.getName(), Integer.valueOf(ingressRule.getStartPort()).toString()));
                            ingressData.add(new Pair<String, Object>(BaseCmd.Properties.ICMP_CODE.getName(), Integer.valueOf(ingressRule.getEndPort()).toString()));
                        } else {
                            ingressData.add(new Pair<String, Object>(BaseCmd.Properties.START_PORT.getName(), Integer.valueOf(ingressRule.getStartPort()).toString()));
                            ingressData.add(new Pair<String, Object>(BaseCmd.Properties.END_PORT.getName(), Integer.valueOf(ingressRule.getEndPort()).toString()));
                        }

                        if (ingressRule.getAllowedNetworkGroup() != null) {
                            ingressData.add(new Pair<String, Object>(BaseCmd.Properties.NETWORK_GROUP_NAME.getName(), ingressRule.getAllowedNetworkGroup()));
                            ingressData.add(new Pair<String, Object>(BaseCmd.Properties.ACCOUNT.getName(), ingressRule.getAllowedNetGroupAcct()));
                        } else if (ingressRule.getAllowedSourceIpCidr() != null) {
                            ingressData.add(new Pair<String, Object>(BaseCmd.Properties.CIDR.getName(), ingressRule.getAllowedSourceIpCidr()));
                        }
                        ingressDataArray[j++] = ingressData;
                    }

                    groupData.add(new Pair<String, Object>("ingressrule", ingressDataArray));
    	        }
    	        groupDataArray[i++] = groupData;
    	    }
    	}

    	List<Pair<String, Object>> groupsTags = new ArrayList<Pair<String, Object>>();
    	if (groupDataArray != null) {
            Pair<String, Object> groupRulesTags = new Pair<String, Object>("networkgroup", groupDataArray);
            groupsTags.add(groupRulesTags);
    	}
		return groupsTags;
	}
}
