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
package com.cloud.network;

import org.apache.log4j.Logger;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.network.dao.PortProfileDaoImpl;
import com.cloud.network.dao.PortProfileVO;
import com.cloud.network.dao.PortProfileVO.BindingType;
import com.cloud.network.dao.PortProfileVO.PortType;
import com.cloud.utils.db.DB;

public class PortProfileManagerImpl {

    private PortProfileDaoImpl _portProfileDao;

    private static final org.apache.log4j.Logger s_logger = Logger.getLogger(PortProfileManagerImpl.class);

    public PortProfileManagerImpl() {
        _portProfileDao = new PortProfileDaoImpl();
    }

    @DB
    public PortProfileVO addPortProfile(String portProfName, long vsmId, int vlanId, PortType pType, BindingType bType) {

        // In this function, we create a port profile record in the port_profile table.
        // First, check if a port profile with the given name already exists. If it does, throw an exception.

        if (_portProfileDao.findByName(portProfName) != null) {
            s_logger.info("Port Profile with specified name: " + portProfName + " already exists");
            throw new InvalidParameterValueException("Port Profile with specified name: " + portProfName + " already exists");
        }
        // Check if the VSM id is a valid one.

        // TODO: Should we also check whether a port profile for the specified vlanId already exists, and if so,
        // fail this function? Do we want to enforce such a 1:1 mapping b/w port profile and vlanId?

        // Else, go ahead and create the port profile.
        PortProfileVO portProfileObj = new PortProfileVO(portProfName, vsmId, vlanId, pType, bType);

        return _portProfileDao.persist(portProfileObj);
    }

    @DB
    public PortProfileVO addPortProfile(String portProfName, long vsmId, int lowVlanId, int highVlanId, PortType pType, BindingType bType) {

        // In this function, we create a port profile record in the port_profile table.

        // First, check if a port profile with the given name already exists. If it does, throw an exception.
        PortProfileVO portProfileObj;

        portProfileObj = _portProfileDao.findByName(portProfName);

        if (portProfileObj != null) {
            s_logger.info("Port Profile with specified name: " + portProfName + " already exists");
            throw new InvalidParameterValueException("Port Profile with specified name: " + portProfName + " already exists");
        }

        // Next, check if there is any existing port profile that uses a VLAN ID range that clashes with the
        // range passed to this function. If so, throw an exception.

        if (_portProfileDao.doesVlanRangeClash(lowVlanId, highVlanId) == true) {
            s_logger.info("Port Profile's vlanId range clashes with an existing Port Profile's");
            throw new InvalidParameterValueException("Port Profile's vlanId range clashes with an existing Port Profile's");
        }

        // Else, go ahead and create the port profile.
        portProfileObj = new PortProfileVO(portProfName, vsmId, lowVlanId, highVlanId, pType, bType);

        return _portProfileDao.persist(portProfileObj);
    }

    @DB
    public boolean deletePortProfile(long portProfileId) {
        PortProfileVO ppObj = _portProfileDao.findById(portProfileId);
        if (ppObj == null) {
            // This entry is already not present. Return success.
            return true;
        }

        //Else, remove it.
        // TODO: Should we be putting any checks here before removing
        // the port profile record from the db?

        // Remove the VSM entry in CiscoNexusVSMDeviceVO's table.
        return _portProfileDao.remove(portProfileId);
    }
}
