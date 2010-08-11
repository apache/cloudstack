package com.cloud.api.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;

import com.cloud.api.BaseCmd;
import com.cloud.api.Parameter;
import com.cloud.api.ServerApiException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.storage.VMTemplateVO;
import com.cloud.user.Account;
import com.cloud.utils.Pair;

public abstract class UpdateTemplateOrIsoPermissionsCmd extends BaseCmd {
	public Logger s_logger = getLogger();
    protected static final List<Pair<Enum, Boolean>> s_properties = new ArrayList<Pair<Enum, Boolean>>();
    protected String s_name = getResponseName();

    static {
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ACCOUNT_OBJ, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.USER_ID, Boolean.FALSE));

        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ACCOUNT_NAMES, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ID, Boolean.TRUE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.IS_FEATURED, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.IS_PUBLIC, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.OP, Boolean.FALSE));
    }

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name="accounts", type=CommandType.LIST, collectionType=CommandType.STRING)
    private List<String> accountNames;

    @Parameter(name="id", type=CommandType.LONG, required=true)
    private Long id;

    @Parameter(name="isfeatured", type=CommandType.BOOLEAN)
    private Boolean featured;

    @Parameter(name="ispublic", type=CommandType.BOOLEAN)
    private Boolean isPublic;

    @Parameter(name="op", type=CommandType.STRING)
    private String operation;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public List<String> getAccountNames() {
        return accountNames;
    }

    public Long getId() {
        return id;
    }

    public Boolean isFeatured() {
        return featured;
    }

    public Boolean isPublic() {
        return isPublic;
    }

    public String getOperation() {
        return operation;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

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
        Boolean isPublic = (Boolean)params.get(BaseCmd.Properties.IS_PUBLIC.getName());
        Boolean isFeatured = (Boolean)params.get(BaseCmd.Properties.IS_FEATURED.getName());
        String accoutNames = (String)params.get(BaseCmd.Properties.ACCOUNT_NAMES.getName());
        String operation = (String)params.get(BaseCmd.Properties.OP.getName());

        Boolean publishTemplateResult = Boolean.FALSE;

        VMTemplateVO template = getManagementServer().findTemplateById(id.longValue());
        if (template == null || !templateIsCorrectType(template)) {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "unable to find " + getMediaType() + " with id " + id);
        }

        if (account != null) {
            if (!isAdmin(account.getType()) && (template.getAccountId() != account.getId())) {
                throw new ServerApiException(BaseCmd.ACCOUNT_ERROR, "unable to update permissions for " + getMediaType() + " with id " + id);
            } else if (account.getType() != Account.ACCOUNT_TYPE_ADMIN) {
                Long templateOwnerDomainId = getManagementServer().findDomainIdByAccountId(template.getAccountId());
                if (!getManagementServer().isChildDomain(account.getDomainId(), templateOwnerDomainId)) {
                    throw new ServerApiException(BaseCmd.ACCOUNT_ERROR, "Unable to update permissions for " + getMediaType() + " with id " + id);
                }
            }
        }

        if (id == Long.valueOf(1)) {
            throw new ServerApiException(BaseCmd.PARAM_ERROR, "unable to update permissions for " + getMediaType() + " with id " + id);
        }
        
        boolean isAdmin = ((account == null) || isAdmin(account.getType()));
        boolean allowPublicUserTemplates = Boolean.parseBoolean(getManagementServer().getConfigurationValue("allow.public.user.templates"));        
        if (!isAdmin && !allowPublicUserTemplates && isPublic != null && isPublic) {
        	throw new ServerApiException(BaseCmd.PARAM_ERROR, "Only private " + getMediaType() + "s can be created.");
        }

        // package up the accountNames as a list
        List<String> accountNameList = new ArrayList<String>();
        if (accoutNames != null) {
            if ((operation == null) || (!operation.equalsIgnoreCase("add") && !operation.equalsIgnoreCase("remove") && !operation.equalsIgnoreCase("reset"))) {
                throw new ServerApiException(BaseCmd.PARAM_ERROR, "Invalid operation on accounts, the operation must be either 'add' or 'remove' in order to modify launch permissions." +
                        "  Given operation is: '" + operation + "'");
            }
            StringTokenizer st = new StringTokenizer(accoutNames, ",");
            while (st.hasMoreTokens()) {
                accountNameList.add(st.nextToken());
            }
        }

        try {
            publishTemplateResult = getManagementServer().updateTemplatePermissions(id, operation, isPublic, isFeatured, accountNameList);
        } catch (InvalidParameterValueException ex) {
            throw new ServerApiException(BaseCmd.PARAM_ERROR, "Failed to update " + getMediaType() + " permissions for template " + template.getName() + ":  internal error.");
        } catch (PermissionDeniedException ex) {
            throw new ServerApiException(BaseCmd.ACCOUNT_ERROR, "Failed to update " + getMediaType() + " permissions for template " + template.getName() + ":  internal error.");
        } catch (Exception ex) {
             s_logger.error("Exception editing template", ex);
             throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to update " + getMediaType() + " permissions for template " + template.getName() + ":  internal error.");
        }

        List<Pair<String, Object>> returnValues = new ArrayList<Pair<String, Object>>();
        returnValues.add(new Pair<String, Object>(BaseCmd.Properties.SUCCESS.getName(), publishTemplateResult.toString()));
        return returnValues;
    }
}
