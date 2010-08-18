package com.cloud.api.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.cloud.api.BaseCmd;
import com.cloud.api.ServerApiException;
import com.cloud.domain.Domain;
import com.cloud.storage.VMTemplateVO;
import com.cloud.user.Account;
import com.cloud.utils.Pair;

public class ListTemplateOrIsoPermissionsCmd extends BaseCmd {
	public Logger s_logger = getLogger();
    protected static final List<Pair<Enum, Boolean>> s_properties = new ArrayList<Pair<Enum, Boolean>>();
    protected String s_name = getResponseName();

    static {
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ACCOUNT_OBJ, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ACCOUNT, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.DOMAIN_ID, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ID, Boolean.TRUE));
    }

    @Override
    public String getName() {
        return s_name;
    }
    @Override
    public List<Pair<Enum, Boolean>> getProperties() {
        return s_properties;
    }
    
    protected boolean templateIsCorrectType(VMTemplateVO template) {
    	return true;
    }
    
    protected String getResponseName() {
    	return "updatetemplateorisopermissionsresponse";
    }
    
    protected String getMediaType() {
    	return "templateOrIso";
    }
    
    protected Logger getLogger() {
    	return Logger.getLogger(UpdateTemplateOrIsoPermissionsCmd.class.getName());    
    }

    @Override
    public List<Pair<String, Object>> execute(Map<String, Object> params) {
        Long id = (Long)params.get(BaseCmd.Properties.ID.getName());
        Account account = (Account)params.get(BaseCmd.Properties.ACCOUNT_OBJ.getName());
        String acctName = (String)params.get(BaseCmd.Properties.ACCOUNT.getName());
        Long domainId = (Long)params.get(BaseCmd.Properties.DOMAIN_ID.getName());
        Long accountId = null;

        if ((account == null) || account.getType() == Account.ACCOUNT_TYPE_ADMIN) {
            // validate domainId before proceeding
            if (domainId != null) {
                if ((account != null) && !getManagementServer().isChildDomain(account.getDomainId(), domainId)) {
                    throw new ServerApiException(BaseCmd.PARAM_ERROR, "Invalid domain id (" + domainId + ") given, unable to list " + getMediaType() + " permissions.");
                }
                if (acctName != null) {
                    Account userAccount = getManagementServer().findAccountByName(acctName, domainId);
                    if (userAccount != null) {
                        accountId = userAccount.getId();
                    } else {
                        throw new ServerApiException(BaseCmd.ACCOUNT_ERROR, "Unable to find account " + acctName + " in domain " + domainId);
                    }
                }
            }
        } else {
            accountId = account.getId();
        }

        VMTemplateVO template = getManagementServer().findTemplateById(id.longValue());
        if (template == null || !templateIsCorrectType(template)) {
            throw new ServerApiException(BaseCmd.PARAM_ERROR, "unable to find " + getMediaType() + " with id " + id);
        }

        if (accountId != null && !template.isPublicTemplate()) {
        	if (account.getType() == Account.ACCOUNT_TYPE_NORMAL && template.getAccountId() != accountId) {
        		throw new ServerApiException(BaseCmd.ACCOUNT_ERROR, "unable to list permissions for " + getMediaType() + " with id " + id);
        	} else if (account.getType() == Account.ACCOUNT_TYPE_DOMAIN_ADMIN) {
        		Domain accountDomain = getManagementServer().findDomainIdById(account.getDomainId());
        		Account templateAccount = getManagementServer().findAccountById(template.getAccountId());
        		Domain templateDomain = getManagementServer().findDomainIdById(templateAccount.getDomainId());        			
            	if (!templateDomain.getPath().contains(accountDomain.getPath())) {
            		throw new ServerApiException(BaseCmd.ACCOUNT_ERROR, "unable to list permissions for " + getMediaType() + " with id " + id);
            	}
        	}                                    
        }

        if (id == Long.valueOf(1)) {
            throw new ServerApiException(BaseCmd.PARAM_ERROR, "unable to list permissions for " + getMediaType() + " with id " + id);
        }

        List<String> accountNames = getManagementServer().listTemplatePermissions(id);

        boolean isAdmin = ((account == null) || isAdmin(account.getType()));
        Long templateOwnerDomain = null;
        if (isAdmin) {
            Account templateOwner = getManagementServer().findAccountById(template.getAccountId());
            if (templateOwner != null) {
                templateOwnerDomain = templateOwner.getDomainId();
            }
        }

        List<Pair<String, Object>> embeddedObject = new ArrayList<Pair<String, Object>>();
        List<Pair<String, Object>> returnValues = new ArrayList<Pair<String, Object>>();
        returnValues.add(new Pair<String, Object>(BaseCmd.Properties.ID.getName(), template.getId().toString()));
        returnValues.add(new Pair<String, Object>(BaseCmd.Properties.IS_PUBLIC.getName(), Boolean.valueOf(template.isPublicTemplate()).toString()));
        if (isAdmin && (templateOwnerDomain != null)) {
            returnValues.add(new Pair<String, Object>(BaseCmd.Properties.DOMAIN_ID.getName(), templateOwnerDomain.toString()));
        }
        if ((accountNames != null) && !accountNames.isEmpty()) {
            for (String accountName : accountNames) {
                returnValues.add(new Pair<String, Object>(BaseCmd.Properties.ACCOUNT.getName(), accountName));
            }
        }
        embeddedObject.add(new Pair<String, Object>(getMediaType() + "permission", new Object[] { returnValues } ));
        return embeddedObject;
    }
}
