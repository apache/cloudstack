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
package org.apache.cloudstack.quota.dao;

import java.util.Map;

import javax.ejb.Local;
import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;
import org.apache.cloudstack.quota.vo.ServiceOfferingVO;

import com.cloud.event.UsageEventVO;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.TransactionLegacy;
import com.cloud.utils.exception.CloudRuntimeException;

@Component
@Local(value = { ServiceOfferingDao.class })
@DB()
public class ServiceOfferingDaoImpl extends GenericDaoBase<ServiceOfferingVO, Long> implements ServiceOfferingDao {
    protected static final Logger s_logger = Logger.getLogger(ServiceOfferingDaoImpl.class);

    @Inject
    UserVmDetailsDao userVmDetailsDao;

    @Override
    public ServiceOfferingVO findServiceOffering(final Long vmId, final long serviceOfferingId) {
        final short opendb = TransactionLegacy.currentTxn().getDatabaseId();
        TransactionLegacy txn = TransactionLegacy.open(TransactionLegacy.CLOUD_DB);
        ServiceOfferingVO result;
        try {
            result = findById(vmId, serviceOfferingId);
        } finally {
            txn.close();
        }
        TransactionLegacy.open(opendb).close();
        return result;
    }

    public ServiceOfferingVO findById(Long vmId, long serviceOfferingId) {
        ServiceOfferingVO offering = super.findById(serviceOfferingId);
        if (offering.isDynamic()) {
            offering.setDynamicFlag(true);
            if (vmId == null) {
                throw new CloudRuntimeException("missing argument vmId");
            }
            Map<String, String> dynamicOffering = userVmDetailsDao.listDetailsKeyPairs(vmId);
            return getcomputeOffering(offering, dynamicOffering);
        }
        return offering;
    }

    public ServiceOfferingVO getcomputeOffering(ServiceOfferingVO serviceOffering, Map<String, String> customParameters) {
        ServiceOfferingVO dummyoffering = new ServiceOfferingVO(serviceOffering);
        dummyoffering.setDynamicFlag(true);
        if (customParameters.containsKey(UsageEventVO.DynamicParameters.cpuNumber.name())) {
            dummyoffering.setCpu(Integer.parseInt(customParameters.get(UsageEventVO.DynamicParameters.cpuNumber.name())));
        }
        if (customParameters.containsKey(UsageEventVO.DynamicParameters.cpuSpeed.name())) {
            dummyoffering.setSpeed(Integer.parseInt(customParameters.get(UsageEventVO.DynamicParameters.cpuSpeed.name())));
        }
        if (customParameters.containsKey(UsageEventVO.DynamicParameters.memory.name())) {
            dummyoffering.setRamSize(Integer.parseInt(customParameters.get(UsageEventVO.DynamicParameters.memory.name())));
        }
        return dummyoffering;
    }

}
