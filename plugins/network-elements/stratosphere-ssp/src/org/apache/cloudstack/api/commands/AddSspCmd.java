// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package org.apache.cloudstack.api.commands;
import javax.inject.Inject;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.response.SspResponse;
import org.apache.cloudstack.api.response.ZoneResponse;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.network.element.SspService;

import org.apache.log4j.Logger;

import com.cloud.dc.dao.DataCenterDao;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.host.Host;


@APICommand(name="addStratosphereSsp", responseObject=SspResponse.class, description="Adds stratosphere ssp server")
public class AddSspCmd extends BaseCmd {
    private static final Logger s_logger = Logger.getLogger(AddSspCmd.class.getName());
    @Inject
    SspService _service;
    @Inject
    DataCenterDao _dcDao;

    @Parameter(name=ApiConstants.ZONE_ID, type=CommandType.UUID, entityType=ZoneResponse.class,
            required=true, description="the zone ID")
    private Long zoneId;

    @Parameter(name=ApiConstants.URL, type=CommandType.STRING, required=true, description="stratosphere ssp server url")
    private String url;

    @Parameter(name=ApiConstants.USERNAME, type=CommandType.STRING, required=false, description="stratosphere ssp api username")
    private String username;

    @Parameter(name=ApiConstants.PASSWORD, type=CommandType.STRING, required=false, description="stratosphere ssp api password")
    private String password;

    @Parameter(name=ApiConstants.NAME, type=CommandType.STRING, required=true, description="stratosphere ssp api name")
    private String name; // required because HostVO name field defined as NOT NULL.

    @Parameter(name="tenantuuid", type=CommandType.STRING, required=false, description="stratosphere ssp tenant uuid")
    private String tenantUuid; // required in creating ssp tenant_network

    @Override
    public String getCommandName() {
        return getClass().getAnnotation(APICommand.class).name();
    }

    @Override
    public long getEntityOwnerId() {
        return CallContext.current().getCallingAccount().getId();
    }

    @Override
    public void execute() throws ResourceUnavailableException,
    InsufficientCapacityException, ConcurrentOperationException,
    ResourceAllocationException, NetworkRuleConflictException {
        s_logger.trace("execute");
        Host host = _service.addSspHost(this);
        SspResponse response = new SspResponse();
        response.setResponseName(getCommandName());
        response.setObjectName("ssphost");
        response.setUrl(this.getUrl());
        response.setZoneId(_dcDao.findById(getZoneId()).getUuid());
        response.setHostId(host.getUuid());
        this.setResponseObject(response);
    }

    public Long getZoneId() {
        return zoneId;
    }

    public void setZoneId(Long zoneId) {
        this.zoneId = zoneId;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTenantUuid() {
        return tenantUuid;
    }

    public void setTenantUuid(String tenantUuid) {
        this.tenantUuid = tenantUuid;
    }
}
