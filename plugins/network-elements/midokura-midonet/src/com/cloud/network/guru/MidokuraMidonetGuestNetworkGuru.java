/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.cloud.network.guru;

import com.cloud.dc.DataCenter.NetworkType;
import com.cloud.network.PhysicalNetwork;
import com.cloud.offering.NetworkOffering;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import javax.ejb.Local;

/**
 * User: tomoe
 * Date: 8/8/12
 * Time: 10:46 AM
 */

@Component
@Local(value = NetworkGuru.class)
public class MidokuraMidonetGuestNetworkGuru extends GuestNetworkGuru {
    private static final Logger s_logger = Logger.getLogger(MidokuraMidonetGuestNetworkGuru.class);


    @Override
    protected boolean canHandle(NetworkOffering offering, NetworkType networkType,
                                PhysicalNetwork physicalNetwork) {
        // TODO: implement this.
        return false;
    }
}