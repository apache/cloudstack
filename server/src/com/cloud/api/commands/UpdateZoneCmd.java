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

import com.cloud.api.BaseCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ServerApiException;
import com.cloud.api.BaseCmd.Manager;
import com.cloud.dc.DataCenterVO;
import com.cloud.serializer.Param;
import com.cloud.serializer.SerializerHelper;

@Implementation(method="updateZone", manager=Manager.ConfigManager)
public class UpdateZoneCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(UpdateZoneCmd.class.getName());

    private static final String s_name = "updatezoneresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name="dns1", type=CommandType.STRING)
    private String dns1;

    @Parameter(name="dns2", type=CommandType.STRING)
    private String dns2;

    @Parameter(name="guestcidraddress", type=CommandType.STRING)
    private String guestCidrAddress;

    @Parameter(name="id", type=CommandType.LONG, required=true)
    private Long id;

    @Parameter(name="internaldns1", type=CommandType.STRING)
    private String internalDns1;

    @Parameter(name="internaldns2", type=CommandType.STRING)
    private String internalDns2;

    @Parameter(name="name", type=CommandType.STRING)
    private String zoneName;

    @Parameter(name="vnet", type=CommandType.STRING)
    private String vnet;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getDns1() {
        return dns1;
    }

    public String getDns2() {
        return dns2;
    }

    public String getGuestCidrAddress() {
        return guestCidrAddress;
    }

    public Long getId() {
        return id;
    }

    public String getInternalDns1() {
        return internalDns1;
    }

    public String getInternalDns2() {
        return internalDns2;
    }

    public String getZoneName() {
        return zoneName;
    }

    public String getVnet() {
        return vnet;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////
    

    private DataCenterVO responseObject = null;


    @Override
    public String getName() {
        return s_name;
    }

    @Override
    public String getResponse() 
    {
        UpdateZoneResponse response = new UpdateZoneResponse();
        if (responseObject != null) {
            response.setStatus("true");
            response.setDisplayText("Successfully updated zone");
            response.setId(responseObject.getId());
            response.setGuestCidrAddress(responseObject.getGuestNetworkCidr());
            response.setDns1(responseObject.getDns1());
            response.setDns2(responseObject.getDns2());
            response.setInternalDns1(responseObject.getInternalDns1());
            response.setInternalDns2(responseObject.getInternalDns2());
            response.setZoneName(responseObject.getName());
            response.setVnet(responseObject.getVnet());
        } 
        else 
        {
        	throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to update zone; internal error.");
        }
        
        return SerializerHelper.toSerializedString(responseObject);
    }
    
    public void setResponseObject(DataCenterVO zone) {
        responseObject = zone;
    }
    // helper class for the response object
    private class UpdateZoneResponse 
    {
        @Param(name="status")
        private String status;

        @Param(name="displaytext")
        private String displayText;

		@Param(name="dns1")
        private String dns1;
        
        @Param(name="dns2")
        private String dns2;

		@Param(name="guestcidraddress")
        private String guestCidrAddress;
        
        @Param(name="id")
        private Long id;

        @Param(name="internaldns1")
        private String internalDns1;
        
        @Param(name="internaldns2")
        private String internalDns2;

        @Param(name="zonename")
        private String zoneName;
        
        @Param(name="vnet")
        private String vnet;
        
        public String getGuestCidrAddress() {
			return guestCidrAddress;
		}

		public void setGuestCidrAddress(String guestCidrAddress) {
			this.guestCidrAddress = guestCidrAddress;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getInternalDns1() {
			return internalDns1;
		}

		public void setInternalDns1(String internalDns1) {
			this.internalDns1 = internalDns1;
		}

		public String getInternalDns2() {
			return internalDns2;
		}

		public void setInternalDns2(String internalDns2) {
			this.internalDns2 = internalDns2;
		}

		public String getZoneName() {
			return zoneName;
		}

		public void setZoneName(String zoneName) {
			this.zoneName = zoneName;
		}

		public String getVnet() {
			return vnet;
		}

		public void setVnet(String vnet) {
			this.vnet = vnet;
		}
        
        public String getDns1() {
			return dns1;
		}

		public void setDns1(String dns1) {
			this.dns1 = dns1;
		}

		public String getDns2() {
			return dns2;
		}

		public void setDns2(String dns2) {
			this.dns2 = dns2;
		}
        
        public String getDisplayText() {
            return displayText;
        }

        public void setDisplayText(String displayText) {
            this.displayText = displayText;
        }
        
        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
                
    }
}
