package com.cloud.api.commands;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.cloud.api.BaseCmd;
import com.cloud.api.ServerApiException;
import com.cloud.domain.DomainVO;
import com.cloud.host.HostVO;
import com.cloud.server.Criteria;
import com.cloud.user.Account;
import com.cloud.utils.Pair;
import com.cloud.vm.InstanceGroupVO;

public class ListVMGroupsCmd extends BaseCmd{
    public static final Logger s_logger = Logger.getLogger(ListVMGroupsCmd.class.getName());

    private static final String s_name = "listinstancegroupsresponse";
    private static final List<Pair<Enum, Boolean>> s_properties = new ArrayList<Pair<Enum, Boolean>>();

    static {
    	s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ID, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.NAME, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ACCOUNT, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.DOMAIN_ID, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ACCOUNT_OBJ, Boolean.FALSE));
    }

    @Override
	public String getName() {
        return s_name;
    }
    @Override
	public List<Pair<Enum, Boolean>> getProperties() {
        return s_properties;
    }

    @Override
    public List<Pair<String, Object>> execute(Map<String, Object> params) {
    	Long id = (Long)params.get(BaseCmd.Properties.ID.getName());
        Account account = (Account)params.get(BaseCmd.Properties.ACCOUNT_OBJ.getName());
        String accountName = (String)params.get(BaseCmd.Properties.ACCOUNT.getName());
        Long domainId = (Long)params.get(BaseCmd.Properties.DOMAIN_ID.getName());
        String name = (String) params.get(BaseCmd.Properties.NAME.getName());
        String keyword = (String)params.get(BaseCmd.Properties.KEYWORD.getName());
        Integer page = (Integer)params.get(BaseCmd.Properties.PAGE.getName());
        Integer pageSize = (Integer)params.get(BaseCmd.Properties.PAGESIZE.getName());
        Long accountId = null;
        Boolean isAdmin = false;

        if ((account == null) || isAdmin(account.getType())) {
            isAdmin = true;
            if (domainId != null) {
                if ((account != null) && !getManagementServer().isChildDomain(account.getDomainId(), domainId)) {
                    throw new ServerApiException(BaseCmd.PARAM_ERROR, "Invalid domain id (" + domainId + ") given, unable to list vm groups.");
                }

                if (accountName != null) {
                    account = getManagementServer().findActiveAccount(accountName, domainId);
                    if (account == null) {
                        throw new ServerApiException(BaseCmd.ACCOUNT_ERROR, "Unable to find account " + accountName + " in domain " + domainId);
                    }
                    accountId = account.getId();
                }
            } else {
                domainId = ((account == null) ? DomainVO.ROOT_DOMAIN : account.getDomainId());
            }
        } else {
            accountName = account.getAccountName();
            accountId = account.getId();
            domainId = account.getDomainId();
        }

        Long[] accountIds = null;
        if (accountId != null) {
            accountIds = new Long[1];
            accountIds[0] = accountId;
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
        
        if (keyword != null) {
        	c.addCriteria(Criteria.KEYWORD, keyword);
        } else {
        	c.addCriteria(Criteria.ID, id);
            c.addCriteria(Criteria.NAME, name);

            // ignore these search requests if it's not an admin
            if (isAdmin == true) {
    	        c.addCriteria(Criteria.DOMAINID, domainId);
            } 
        }

        c.addCriteria(Criteria.ACCOUNTID, accountIds);
        c.addCriteria(Criteria.ISADMIN, isAdmin); 

        List<? extends InstanceGroupVO> vmGroups = getManagementServer().searchForVmGroups(c);

        if (vmGroups == null) {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "unable to find vm groups for account id " + accountName.toString());
        }

        Object[] vmTag = new Object[vmGroups.size()];
        int i = 0;

        for (InstanceGroupVO vmGroup : vmGroups) {
        	
            List<Pair<String, Object>> vmData = new ArrayList<Pair<String, Object>>();

            vmData.add(new Pair<String, Object>(BaseCmd.Properties.ID.getName(), Long.toString(vmGroup.getId())));
            vmData.add(new Pair<String, Object>(BaseCmd.Properties.NAME.getName(), vmGroup.getName()));
            vmData.add(new Pair<String, Object>(BaseCmd.Properties.CREATED.getName(), vmGroup.getCreated()));

            Account acct = getManagementServer().findAccountById(Long.valueOf(vmGroup.getAccountId()));
            if (acct != null) {
                vmData.add(new Pair<String, Object>(BaseCmd.Properties.ACCOUNT.getName(), acct.getAccountName()));
                vmData.add(new Pair<String, Object>(BaseCmd.Properties.DOMAIN_ID.getName(), Long.toString(acct.getDomainId())));
                vmData.add(new Pair<String, Object>(BaseCmd.Properties.DOMAIN.getName(), getManagementServer().findDomainIdById(acct.getDomainId()).getName()));
            }
            
            vmTag[i++] = vmData;
        }
        List<Pair<String, Object>> returnTags = new ArrayList<Pair<String, Object>>();
        Pair<String, Object> vmTags = new Pair<String, Object>("instancegroup", vmTag);
        returnTags.add(vmTags);
        return returnTags;
    }

}
