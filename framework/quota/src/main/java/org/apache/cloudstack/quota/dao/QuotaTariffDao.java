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
package org.apache.cloudstack.quota.dao;

import com.cloud.utils.Pair;
import com.cloud.utils.db.GenericDao;

import org.apache.cloudstack.quota.vo.QuotaTariffVO;

import java.util.Date;
import java.util.List;

public interface QuotaTariffDao extends GenericDao<QuotaTariffVO, Long> {

    Pair<List<QuotaTariffVO>, Integer> listQuotaTariffs(Date startDate, Date endDate, Integer usageType, String name, String uuid, boolean listAll, Long startIndex, Long pageSize);

    Pair<List<QuotaTariffVO>, Integer> listQuotaTariffs(Date startDate, Date endDate, Integer usageType, String name, String uuid, boolean listAll, boolean listOnlyRemoved, Long startIndex, Long pageSize, String keyword);

    QuotaTariffVO findByName(String name);

    Boolean updateQuotaTariff(QuotaTariffVO plan);

    QuotaTariffVO addQuotaTariff(QuotaTariffVO plan);
}
