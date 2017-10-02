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

package org.apache.cloudstack.network.contrail.management;

import static com.cloud.utils.AutoCloseableUtil.closeAutoCloseable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;

import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.deploy.DeploymentPlan;
import com.cloud.network.Network;
import com.cloud.network.Networks.BroadcastDomainType;
import com.cloud.network.Networks.Mode;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.dao.NetworkVO;
import com.cloud.offering.NetworkOffering;
import com.cloud.user.Account;
import com.cloud.utils.PropertiesUtil;
/**
 * ManagementNetworkGuru
 *
 * Replace the default management network strategy (PodBasedNetworkGuru) by using a Isolated network for management
 * traffic.
 */
@Component
public class ManagementNetworkGuru extends ContrailGuru {
    private static final Logger s_logger = Logger.getLogger(ManagementNetworkGuru.class);
    private static final TrafficType[] TrafficTypes = {TrafficType.Management};

    private final String configuration = "contrail.properties";
    private String _mgmtCidr;
    private String _mgmtGateway;

    @Override
    public String getName() {
        return "ManagementNetworkGuru";
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        File configFile = PropertiesUtil.findConfigFile(configuration);
        FileInputStream inputFile = null;

        try {
            if (null == configFile) {
                throw new FileNotFoundException("Configuration file was not found!");
            }
            inputFile = new FileInputStream(configFile);
        } catch (FileNotFoundException e) {
            s_logger.error(e.getMessage());
            throw new ConfigurationException(e.getMessage());
        }

        final Properties configProps = new Properties();
        try {
            configProps.load(inputFile);
        } catch (IOException e) {
            s_logger.error(e.getMessage());
            throw new ConfigurationException(e.getMessage());
        } finally {
            closeAutoCloseable(inputFile, "error closing config file");
        }
        _mgmtCidr = configProps.getProperty("management.cidr");
        _mgmtGateway = configProps.getProperty("management.gateway");
        s_logger.info("Management network " + _mgmtCidr + " gateway: " + _mgmtGateway);
        return true;
    }

    @Override
    public TrafficType[] getSupportedTrafficType() {
        return TrafficTypes;
    }

    @Override
    public boolean isMyTrafficType(TrafficType type) {
        for (TrafficType t : TrafficTypes) {
            if (t == type) {
                return true;
            }
        }
        return false;
    }

    private boolean canHandle(NetworkOffering offering) {
        TrafficType type = offering.getTrafficType();
        return (isMyTrafficType(type));
    }

    @Override
    public Network design(NetworkOffering offering, DeploymentPlan plan, Network userSpecified, Account owner) {

        if (!canHandle(offering)) {
            return null;
        }
        NetworkVO network =
            new NetworkVO(offering.getTrafficType(), Mode.Dhcp, BroadcastDomainType.Lswitch, offering.getId(), Network.State.Allocated, plan.getDataCenterId(),
                plan.getPhysicalNetworkId(), offering.getRedundantRouter());
        if (_mgmtCidr != null) {
            network.setCidr(_mgmtCidr);
            network.setGateway(_mgmtGateway);
        }
        s_logger.debug("Allocated network " + userSpecified.getName() + (network.getCidr() == null ? "" : " subnet: " + network.getCidr()));
        return network;
    }

}
