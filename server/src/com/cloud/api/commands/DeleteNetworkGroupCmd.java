package com.cloud.api.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.cloud.api.BaseCmd;
import com.cloud.api.ServerApiException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceInUseException;
import com.cloud.network.security.NetworkGroupVO;
import com.cloud.user.Account;
import com.cloud.utils.Pair;

public class DeleteNetworkGroupCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(DeleteNetworkGroupCmd.class.getName());

    private static final String s_name = "deletenetworkgroupresponse";
    private static final List<Pair<Enum, Boolean>> s_properties = new ArrayList<Pair<Enum, Boolean>>();

    static {
        //s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.USER_ID, Boolean.FALSE));
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
        //Long userId = (Long)params.get(BaseCmd.Properties.USER_ID.getName());
        String accountName = (String)params.get(BaseCmd.Properties.ACCOUNT.getName());
        String name = (String)params.get(BaseCmd.Properties.NAME.getName());

        Long accountId = null;
        if ((account == null) || isAdmin(account.getType())) {
            if ((accountName != null) && (domainId != null)) {
                // if it's an admin account, do a quick permission check
                if ((account != null) && !getManagementServer().isChildDomain(account.getDomainId(), domainId)) {
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("Unable to find rules network group " + name + ", permission denied.");
                    }
                    throw new ServerApiException(BaseCmd.ACCOUNT_ERROR, "Unable to network group " + name + ", permission denied.");
                }

                Account groupOwner = getManagementServer().findActiveAccount(accountName, domainId);
                if (groupOwner == null) {
                    throw new ServerApiException(BaseCmd.PARAM_ERROR, "Unable to find account " + accountName + " in domain " + domainId);
                }
                accountId = groupOwner.getId();
            } else {
                if (account != null) {
                    accountId = account.getId();
                    domainId = account.getDomainId();
                }
            }
        } else {
            if (account != null) {
                accountId = account.getId();
                domainId = account.getDomainId();
            }
        }

        if (accountId == null) {
            throw new ServerApiException(BaseCmd.PARAM_ERROR, "Unable to find account for network group " + name + "; failed to delete group.");
        }

        NetworkGroupVO sg = getManagementServer().findNetworkGroupByName(accountId, name);
        if (sg == null) {
            throw new ServerApiException(BaseCmd.PARAM_ERROR, "Unable to find network group " + name + "; failed to delete group.");
        }

        try {
            getManagementServer().deleteNetworkGroup(sg.getId(), accountId);
        } catch (ResourceInUseException ex) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Failed to delete network group " + name + " for account " + accountId + ", group is not empty.");
            }
            throw new ServerApiException(BaseCmd.PARAM_ERROR, "Unable to delete network group " + name + "; group is not empty.");
        } catch (PermissionDeniedException pde) {
        	if (s_logger.isDebugEnabled()) {
                s_logger.debug("Failed to delete network group " + name + " for account " + accountId + ", default group cannot be deleted");
            }
            throw new ServerApiException(BaseCmd.PARAM_ERROR, "Unable to delete network group " + name + "; default group cannot be deleted");
        }

        List<Pair<String, Object>> returnValues = new ArrayList<Pair<String, Object>>();
        returnValues.add(new Pair<String, Object>(BaseCmd.Properties.SUCCESS.getName(), "true"));
        return returnValues;
    }
}
