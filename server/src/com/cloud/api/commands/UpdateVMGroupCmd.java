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

public class UpdateVMGroupCmd extends BaseCmd{

    private static final String s_name = "updateinstancegroupresponse";
    public static final Logger s_logger = Logger.getLogger(UpdateVMGroupCmd.class.getName());
    private static final List<Pair<Enum, Boolean>> s_properties = new ArrayList<Pair<Enum, Boolean>>();

    static {
    	s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ID, Boolean.TRUE));
    	s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.NAME, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ACCOUNT_OBJ, Boolean.FALSE));
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
        Long groupId = (Long)params.get(BaseCmd.Properties.ID.getName());
        String name = (String)params.get(BaseCmd.Properties.NAME.getName());

        // Verify input parameters
        InstanceGroupVO group = getManagementServer().findVmGroupById(groupId.longValue());
        if (group == null) {
        	throw new ServerApiException(BaseCmd.PARAM_ERROR, "unable to find a vm group with id " + groupId);
        }
        
        if (account != null) {
        	Account tempAccount = getManagementServer().findAccountById(group.getAccountId());
            if (!isAdmin(account.getType()) && (account.getId() != group.getAccountId())) {
                throw new ServerApiException(BaseCmd.PARAM_ERROR, "unable to find a group with id " + groupId + " for this account");
            } else if (!getManagementServer().isChildDomain(account.getDomainId(), tempAccount.getDomainId())) {
                throw new ServerApiException(BaseCmd.PARAM_ERROR, "Invalid group id (" + groupId + ") given, unable to update the group.");
            }
        }
        
        //Check if name is already in use by this account (exclude this group)
        boolean isNameInUse = getManagementServer().isVmGroupNameInUse(group.getAccountId(), name);

        if (isNameInUse && !group.getName().equals(name)) {
            throw new ServerApiException(BaseCmd.PARAM_ERROR, "Unable to update vm group, a group with name " + name + " already exisits for account");
        }
        
    	InstanceGroupVO vmGroup = getManagementServer().updateVmGroup(groupId, name);
        List<Pair<String, Object>> embeddedObject = new ArrayList<Pair<String, Object>>();
        
        List<Pair<String, Object>> returnValues = new ArrayList<Pair<String, Object>>();
        returnValues.add(new Pair<String, Object>(BaseCmd.Properties.ID.getName(), Long.toString(vmGroup.getId())));
        returnValues.add(new Pair<String, Object>(BaseCmd.Properties.NAME.getName(), vmGroup.getName()));
        
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
