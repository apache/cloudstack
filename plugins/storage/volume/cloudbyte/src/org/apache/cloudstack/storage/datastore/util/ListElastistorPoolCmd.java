// Copyright 2012-2013 CloudByte Inc.
package org.apache.cloudstack.storage.datastore.util;

import javax.inject.Inject;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.BaseCmd.CommandType;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.log4j.Logger;

@APICommand(name = "listElastistorPool", description = "Lists the pools of elastistor",
        responseObject = ListElastistorPoolResponse.class)
public class ListElastistorPoolCmd extends BaseCmd {
    public static final Logger  s_logger = Logger.getLogger(ListElastistorPoolCmd.class.getName());
    private static final String s_name   = "listElastistorPoolResponse";

    @Inject
    ElastistorVolumeApiService  _ElastistorVolumeApiService;

    // ///////////////////////////////////////////////////
    // ////////////// API parameters /////////////////////
    // ///////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ID, type = CommandType.LONG, description = "the ID of the Pool")
    private Long                id;

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
    public void execute() {

        ListResponse<ListElastistorPoolResponse> response = _ElastistorVolumeApiService.listElastistorPools(this);
        response.setResponseName(getCommandName());
        this.setResponseObject(response);

    }

    @Override
    public long getEntityOwnerId() {
        // TODO Auto-generated method stub
        return 0;
    }
}
