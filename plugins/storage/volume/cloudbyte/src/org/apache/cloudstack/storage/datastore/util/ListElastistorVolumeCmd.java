// Copyright 2012-2013 CloudByte Inc.
package org.apache.cloudstack.storage.datastore.util;

import javax.inject.Inject;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.log4j.Logger;

@APICommand(name = "listElastistorVolume", description = "Lists the volumes of elastistor", responseObject = ListElastistorVolumeResponse.class)
public class ListElastistorVolumeCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(ListElastistorVolumeCmd.class.getName());
    private static final String s_name = "listElastistorVolumeResponse";

    @Inject
    ElastistorVolumeApiService _ElastistorVolumeApiService;

    // ///////////////////////////////////////////////////
    // ////////////// API parameters /////////////////////
    // ///////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ID, type = CommandType.STRING, required = true, description = "the ID of the account")
    private String id;

    // ///////////////////////////////////////////////////
    // ///////////////// Accessors ///////////////////////
    // ///////////////////////////////////////////////////

    public String getId() {
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
    public void execute() {

        ListResponse<ListElastistorVolumeResponse> response = _ElastistorVolumeApiService.listElastistorVolume(this);
        response.setResponseName(getCommandName());
        this.setResponseObject(response);

    }

    @Override
    public long getEntityOwnerId() {
        // TODO Auto-generated method stub
        return 0;
    }
}
