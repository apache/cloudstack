// Copyright 2012 Citrix Systems, Inc. Licensed under the
// Apache License, Version 2.0 (the "License"); you may not use this
// file except in compliance with the License.  Citrix Systems, Inc.
// reserves all rights not expressly granted by the License.
// You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// 
package com.cloud.network;

public interface CiscoNexusVSMDevice {
	
    // This tells us whether the VSM is currently enabled or disabled. We may
    // need this if we would like to carry out any sort of maintenance on the
    // VSM or CS.
    public enum VSMDeviceState {
    	Enabled,
    	Disabled
    }    
    
    // This tells us whether the VSM is currently configured with a standby (HA)
    // or does not have any standby (Standalone).
    public enum VSMConfigMode {
        Standalone,
        HA
    }
    
    // This tells us whether the VSM is currently a primary or a standby VSM.
    public enum VSMConfigState {
        Primary,
        Standby
    }

    long getId();
    
    public String getvsmName();
    
    public long getHostId();
    
    public String getUserName();
    
    public String getPassword();

    public String getipaddr();
    
    public int getManagementVlan();
    
    public int getControlVlan();
    
    public int getPacketVlan();

    public int getStorageVlan();
    
    public long getvsmDomainId();
    
    public VSMConfigMode getvsmConfigMode();
    
    public VSMConfigState getvsmConfigState();
    
    public VSMDeviceState getvsmDeviceState();
    
    public String getUuid();
    
}
