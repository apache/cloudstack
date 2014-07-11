// Copyright 2012-2013 CloudByte Inc.
package org.apache.cloudstack.storage.datastore.util;

import javax.inject.Inject;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.log4j.Logger;

@APICommand(name = "listElastistorInterface", description = "Lists the network Interfaces of elastistor", responseObject = ListElastistorVolumeResponse.class)
public class ListElastistorInterfaceCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(ListElastistorInterfaceCmd.class.getName());
    private static final String s_name = "listElastistorInterfaceResponse";

    @Inject
    ElastistorVolumeApiService _ElastistorVolumeApiService;

    // ///////////////////////////////////////////////////
    // ////////////// API parameters /////////////////////
    // ///////////////////////////////////////////////////

    @Parameter(name = "controllerid", type = CommandType.STRING, description = "controller id")
    private String controllerid;

    // ///////////////////////////////////////////////////
    // ///////////////// Accessors ///////////////////////
    // ///////////////////////////////////////////////////

    public String getControllerId() {
        return controllerid;
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

        ListResponse<ListElastistorInterfaceResponse> response = _ElastistorVolumeApiService.listElastistorInterfaces(this);
        response.setResponseName(getCommandName());
        this.setResponseObject(response);

    }

    @Override
    public long getEntityOwnerId() {
        // TODO Auto-generated method stub
        return 0;
    }
}
