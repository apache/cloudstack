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
package org.apache.cloudstack.thrift;

import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import org.apache.cloudstack.thrift.api.CloudStack.Iface;
import org.apache.cloudstack.thrift.api.Zone;
import org.apache.thrift.TException;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

public class CloudStackHandler implements Iface {

    @Inject
    DataCenterDao _dcDao;

    @Override
    public boolean ping() throws TException {
        return true;
    }

    @Override
    public List<Zone> listZones() throws TException {
        List<DataCenterVO> dcs = _dcDao.listAllIncludingRemoved();
        List<Zone> zones = new ArrayList<Zone>();
        for (DataCenter dc: dcs) {
            zones.add(new Zone(dc.getUuid(), dc.getName()));
        }
        return zones;
    }
}
