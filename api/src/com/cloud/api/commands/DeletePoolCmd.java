package com.cloud.api.commands;

import org.apache.log4j.Logger;

import com.cloud.api.ApiConstants;
import com.cloud.api.BaseCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ServerApiException;
import com.cloud.api.response.SuccessResponse;
import com.cloud.storage.StoragePool;
import com.cloud.storage.StoragePoolStatus;
import com.cloud.user.Account;

@Implementation(description = "Deletes a storage pool.", responseObject = SuccessResponse.class)
public class DeletePoolCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(DeletePoolCmd.class.getName());
    private static final String s_name = "deletestoragepoolresponse";

    // ///////////////////////////////////////////////////
    // ////////////// API parameters /////////////////////
    // ///////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ID, type = CommandType.LONG, required = true, description = "Storage pool id")
    private Long id;

    // ///////////////////////////////////////////////////
    // ///////////////// Accessors ///////////////////////
    // ///////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    // ///////////////////////////////////////////////////
    // ///////////// API Implementation///////////////////
    // ///////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

    @Override
    public void execute() {
        boolean result = _storageService.deletePool(this);
        if (result) {
            SuccessResponse response = new SuccessResponse(getCommandName());
            this.setResponseObject(response);
        } else {
            StoragePool pool = _storageService.getStoragePool(id);
            if (pool != null && pool.getStatus() == StoragePoolStatus.Removed) {
                throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to finish storage pool removal. The storage pool will not be used but cleanup is needed");
            } else {
                throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to delete storage pool");
            }
        }
    }
}
