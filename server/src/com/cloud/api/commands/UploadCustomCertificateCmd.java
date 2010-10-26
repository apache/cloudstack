/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */
package com.cloud.api.commands;

import org.apache.log4j.Logger;

import com.cloud.api.BaseAsyncCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.response.StatusResponse;
import com.cloud.event.EventTypes;
import com.cloud.user.Account;

@Implementation(method="uploadCertificate")
public class UploadCustomCertificateCmd extends BaseAsyncCmd {
    public static final Logger s_logger = Logger.getLogger(UploadCustomCertificateCmd.class.getName());

    private static final String s_name = "uploadcustomcertificateresponse";

    @Parameter(name="path", type=CommandType.STRING)
    private String path;

    public String getPath() {
        return path;
    }


    @Override @SuppressWarnings("unchecked")
    public StatusResponse getResponse() {
        Boolean status = (Boolean)getResponseObject();

        StatusResponse response = new StatusResponse();
        response.setStatus(status);
        response.setResponseName(getName());
        return response;
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_VOLUME_CREATE;
    }

    @Override
    public String getEventDescription() {
        return  ("Uploading custom certificate to the db, and applying it to the cpvm");
    }
    
    @Override
    public String getName() {
        return s_name;
    }
    
    public static String getResultObjectName() {
    	return "volume";
    }

    @Override
    public long getAccountId() {
        return Account.ACCOUNT_ID_SYSTEM; // no account info given, parent this command to SYSTEM so ERROR events are tracked
    }

}
