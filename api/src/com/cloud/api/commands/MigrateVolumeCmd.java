package com.cloud.api.commands;

import com.cloud.api.ApiConstants;
import com.cloud.api.BaseAsyncCmd;
import com.cloud.api.BaseCmd;
import com.cloud.api.IdentityMapper;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ServerApiException;
import com.cloud.api.response.VolumeResponse;
import com.cloud.event.EventTypes;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.storage.Volume;
import com.cloud.user.Account;


@Implementation(description="Migrate volume", responseObject=VolumeResponse.class)
public class MigrateVolumeCmd extends BaseAsyncCmd {
	private static final String s_name = "migratevolumeresponse";
	
	 /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @IdentityMapper(entityTableName="volumes")
    @Parameter(name=ApiConstants.VOLUME_ID, type=CommandType.LONG, required=true, description="the ID of the volume")
    private Long volumeId;

    @IdentityMapper(entityTableName="storage_pool")
    @Parameter(name=ApiConstants.STORAGE_ID, type=CommandType.LONG, required=false, description="destination storage pool ID to migrate the volume to")
    private Long storageId;
    
    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getVolumeId() {
        return volumeId;
    }

    public Long getStoragePoolId() {
    	return storageId;
    }
    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }
    
    @Override
    public long getEntityOwnerId() {
    	  Volume volume = _entityMgr.findById(Volume.class, getVolumeId());
          if (volume != null) {
              return volume.getAccountId();
          }

          return Account.ACCOUNT_ID_SYSTEM; // no account info given, parent this command to SYSTEM so ERROR events are tracked
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_VOLUME_MIGRATE;
    }

    @Override
    public String getEventDescription() {
        return  "Attempting to migrate volume Id: " + getVolumeId() + " to storage pool Id: "+ getStoragePoolId();
    }
    
    
    @Override
    public void execute(){
    	Volume result;
		try {
			result = _storageService.migrateVolume(getVolumeId(), getStoragePoolId());
			 if (result != null) {
	             VolumeResponse response = _responseGenerator.createVolumeResponse(result);
	             response.setResponseName(getCommandName());
	             this.setResponseObject(response);
			 }
		} catch (ConcurrentOperationException e) {
			throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to migrate volume: ");
		}
    	
    }

}
