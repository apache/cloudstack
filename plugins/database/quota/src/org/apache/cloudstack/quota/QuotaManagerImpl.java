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
package org.apache.cloudstack.quota;

import java.util.ArrayList;
import java.util.List;

import javax.ejb.Local;
import javax.inject.Inject;

import org.apache.cloudstack.api.command.ListQuotaConfigurationsCmd;
import org.apache.cloudstack.api.response.QuotaConfigurationResponse;
import org.apache.cloudstack.quota.dao.QuotaConfigurationDao;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.utils.Pair;

@Component
@Local(value = QuotaManager.class)
public class QuotaManagerImpl implements QuotaManager {
 private static final Logger s_logger = Logger.getLogger(QuotaManagerImpl.class.getName());


 @Inject
 private QuotaConfigurationDao _quotaConfigurationDao;



 public QuotaManagerImpl() {
     super();
 }

 public QuotaManagerImpl(final QuotaConfigurationDao quotaConfigurationDao) {
     super();
     _quotaConfigurationDao = quotaConfigurationDao;
 }


 @Override
 public List<Class<?>> getCommands() {
     final List<Class<?>> cmdList = new ArrayList<Class<?>>();
     cmdList.add(ListQuotaConfigurationsCmd.class);
     return cmdList;
 }

 @Override
 public Pair<List<QuotaConfigurationVO>, Integer> listConfigurations(final ListQuotaConfigurationsCmd cmd) {
     final Pair<List<QuotaConfigurationVO>, Integer>  result = _quotaConfigurationDao.searchConfigurations();
     return result;
 }


 @Override
 public QuotaConfigurationResponse createQuotaConfigurationResponse(final QuotaConfigurationVO configuration) {
     final QuotaConfigurationResponse response = new QuotaConfigurationResponse();
     response.setUsageType(configuration.getUsageType());
     response.setUsageUnit(configuration.getUsageUnit());
     response.setUsageDiscriminator(configuration.getUsageDiscriminator());
     response.setCurrencyValue(configuration.getCurrencyValue());
     response.setInclude(configuration.getInclude());
     response.setDescription(configuration.getDescription());
     return response;
 }

}
