package com.cloud.api.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.cloud.api.BaseCmd;
import com.cloud.api.ServerApiException;
import com.cloud.user.Account;
import com.cloud.utils.Pair;
import com.cloud.vm.InstanceGroupVO;

public class CreateVMGroupCmd extends BaseCmd{
    public static final Logger s_logger = Logger.getLogger(CreateVMGroupCmd.class.getName());

    private static final String s_name = "createinstancegroupresponse";
    private static final List<Pair<Enum, Boolean>> s_properties = new ArrayList<Pair<Enum, Boolean>>();

    static {
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ACCOUNT_OBJ, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ACCOUNT, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.DOMAIN_ID, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.NAME, Boolean.TRUE));
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
        Long domainId = (Long)params.get(BaseCmd.Properties.DOMAIN_ID.getName());
        String accountName = (String)params.get(BaseCmd.Properties.ACCOUNT.getName());
        String name = (String)params.get(BaseCmd.Properties.NAME.getName());
        Long accountId = null;

        if (account != null) {
            if (isAdmin(account.getType())) {
                if (domainId != null) {
                    if (!getManagementServer().isChildDomain(account.getDomainId(), domainId)) {
                        throw new ServerApiException(BaseCmd.ACCOUNT_ERROR, "Unable to create vm group in domain " + domainId + ", permission denied.");
                    }
                } else {
                    // the admin must be creating the vm group
                    if (account != null) {
                        accountId = account.getId();
                        domainId = account.getDomainId();
                        accountName = account.getAccountName();
                    }
                }
            } else {
                accountId = account.getId();
                domainId = account.getDomainId();
                accountName = account.getAccountName();
            }
        }

        if (accountId == null) {
            if ((accountName != null) && (domainId != null)) {
                Account userAccount = getManagementServer().findActiveAccount(accountName, domainId);
                if (userAccount != null) {
                    accountId = userAccount.getId();
                    accountName = userAccount.getAccountName();
                } else {
                    throw new ServerApiException(BaseCmd.ACCOUNT_ERROR, "could not find account " + accountName + " in domain " + domainId);
                }
            }
        }

        if (accountId == null) {
            throw new ServerApiException(BaseCmd.ACCOUNT_ERROR, "Unable to create vm group, no account specified.");
        }
        
        //Check if name is already in use by this account
        boolean isNameInUse = getManagementServer().isVmGroupNameInUse(accountId, name);

        if (isNameInUse) {
            throw new ServerApiException(BaseCmd.PARAM_ERROR, "Unable to create vm group, a group with name " + name + " already exisits for account " + accountId);
        }
        
        InstanceGroupVO vmGroup = getManagementServer().createVmGroup(name, accountId);

        List<Pair<String, Object>> embeddedObject = new ArrayList<Pair<String, Object>>();
        
        List<Pair<String, Object>> returnValues = new ArrayList<Pair<String, Object>>();
        returnValues.add(new Pair<String, Object>(BaseCmd.Properties.ID.getName(), Long.toString(vmGroup.getId())));
        returnValues.add(new Pair<String, Object>(BaseCmd.Properties.NAME.getName(), vmGroup.getName()));
        returnValues.add(new Pair<String, Object>(BaseCmd.Properties.CREATED.getName(), vmGroup.getCreated()));
        
        Account accountTemp = getManagementServer().findAccountById(vmGroup.getAccountId());
        if (accountTemp != null) {
        	returnValues.add(new Pair<String, Object>(BaseCmd.Properties.ACCOUNT.getName(), accountTemp.getAccountName()));
        	returnValues.add(new Pair<String, Object>(BaseCmd.Properties.DOMAIN_ID.getName(), accountTemp.getDomainId()));
        	returnValues.add(new Pair<String, Object>(BaseCmd.Properties.DOMAIN.getName(), getManagementServer().findDomainIdById(accountTemp.getDomainId()).getName()));
        }
        embeddedObject.add(new Pair<String, Object>("instancegroup", new Object[] { returnValues } ));
        return embeddedObject;
    }

}
