//Licensed to the Apache Software Foundation (ASF) under one
//or more contributor license agreements.  See the NOTICE file
//distributed with this work for additional information
//regarding copyright ownership.  The ASF licenses this file
//to you under the Apache License, Version 2.0 (the
//"License"); you may not use this file except in compliance
//with the License.  You may obtain a copy of the License at
//
//http://www.apache.org/licenses/LICENSE-2.0
//
//Unless required by applicable law or agreed to in writing,
//software distributed under the License is distributed on an
//"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
//KIND, either express or implied.  See the License for the
//specific language governing permissions and limitations
//under the License.
package org.apache.cloudstack.api.command.user.event;

import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.EventResponse;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.context.CallContext;

import com.cloud.event.Event;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.user.Account;

@APICommand(name = "archiveEvents", description = "Archive one or more events.", responseObject = SuccessResponse.class, entityType = {Event.class},
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class ArchiveEventsCmd extends BaseCmd {

    public static final Logger s_logger = Logger.getLogger(ArchiveEventsCmd.class.getName());


    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.IDS,
               type = CommandType.LIST,
               collectionType = CommandType.UUID,
               entityType = EventResponse.class,
               description = "the IDs of the events")
    private List<Long> ids;

    @Parameter(name = ApiConstants.END_DATE, type = CommandType.DATE, description = "end date range to archive events"
        + " (including) this date (use format \"yyyy-MM-dd\" or the new format \"yyyy-MM-ddThh:mm:ss\")")
    private Date endDate;

    @Parameter(name = ApiConstants.START_DATE, type = CommandType.DATE, description = "start date range to archive events"
        + " (including) this date (use format \"yyyy-MM-dd\" or the new format \"yyyy-MM-ddThh:mm:ss\")")
    private Date startDate;

    @Parameter(name = ApiConstants.TYPE, type = CommandType.STRING, description = "archive by event type")
    private String type;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public List<Long> getIds() {
        return ids;
    }

    public Date getEndDate() {
        return endDate;
    }

    public Date getStartDate() {
        return startDate;
    }

    public String getType() {
        return type;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public long getEntityOwnerId() {
        Account account = CallContext.current().getCallingAccount();
        if (account != null) {
            return account.getId();
        }
        return Account.ACCOUNT_ID_SYSTEM;
    }

    @Override
    public void execute() {
        if (ids == null && type == null && endDate == null) {
            throw new InvalidParameterValueException("either ids, type or enddate must be specified");
        } else if (startDate != null && endDate == null) {
            throw new InvalidParameterValueException("enddate must be specified with startdate parameter");
        }
        boolean result = _mgr.archiveEvents(this);
        if (result) {
            SuccessResponse response = new SuccessResponse(getCommandName());
            setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Unable to archive Events, one or more parameters has invalid values");
        }
    }
}
