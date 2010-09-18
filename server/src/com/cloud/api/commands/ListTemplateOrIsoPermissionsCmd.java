package com.cloud.api.commands;

import java.util.List;

import org.apache.log4j.Logger;

import com.cloud.api.ApiDBUtils;
import com.cloud.api.BaseListCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.response.ApiResponseSerializer;
import com.cloud.api.response.TemplatePermissionsResponse;
import com.cloud.storage.VMTemplateVO;
import com.cloud.user.Account;
import com.cloud.user.UserContext;

@Implementation(method="listTemplatePermissions")
public class ListTemplateOrIsoPermissionsCmd extends BaseListCmd {
	public Logger s_logger = getLogger();
    protected String s_name = getResponseName();

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name="account", type=CommandType.STRING)
    private String accountName;

    @Parameter(name="domainid", type=CommandType.LONG)
    private Long domainId;

    @Parameter(name="id", type=CommandType.LONG, required=true)
    private Long id;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getAccountName() {
        return accountName;
    }

    public Long getDomainId() {
        return domainId;
    }

    public Long getId() {
        return id;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getName() {
        return s_name;
    }

    @Override @SuppressWarnings("unchecked")
    public String getResponse() {
        List<String> accountNames = (List<String>)getResponseObject();
        Account account = (Account)UserContext.current().getAccountObject();
        boolean isAdmin = ((account == null) || isAdmin(account.getType()));
        Long templateOwnerDomain = null;
        VMTemplateVO template = ApiDBUtils.findTemplateById(id);
        if (isAdmin) {
            // FIXME:  we have just template id and need to get template owner from that
            Account templateOwner = ApiDBUtils.findAccountById(template.getAccountId());
            if (templateOwner != null) {
                templateOwnerDomain = templateOwner.getDomainId();
            }
        }

        TemplatePermissionsResponse response = new TemplatePermissionsResponse();
        response.setId(template.getId());
        response.setPublicTemplate(template.isPublicTemplate());
        if (isAdmin && (templateOwnerDomain != null)) {
            response.setDomainId(templateOwnerDomain);
        }

        response.setAccountNames(accountNames);

        response.setResponseName(getName());
        return ApiResponseSerializer.toSerializedString(response);
    }
    
    protected boolean templateIsCorrectType(VMTemplateVO template) {
    	return true;
    }
    
    protected String getResponseName() {
    	return "updatetemplateorisopermissionsresponse";
    }
    
    public String getMediaType() {
    	return "templateOrIso";
    }
    
    protected Logger getLogger() {
    	return Logger.getLogger(UpdateTemplateOrIsoPermissionsCmd.class.getName());    
    }
}
