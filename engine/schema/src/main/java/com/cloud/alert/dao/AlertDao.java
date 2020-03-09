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
package com.cloud.alert.dao;

import java.util.Date;
import java.util.List;

import com.cloud.alert.AlertVO;
import com.cloud.utils.db.GenericDao;

public interface AlertDao extends GenericDao<AlertVO, Long> {
    AlertVO getLastAlert(short type, long dataCenterId, Long podId, Long clusterId);

    // This is for backward compatibility
    AlertVO getLastAlert(short type, long dataCenterId, Long podId);

    public boolean deleteAlert(List<Long> ids, String type, Date startDate, Date endDate, Long zoneId);

    public boolean archiveAlert(List<Long> ids, String type, Date startDate, Date endDate, Long zoneId);

    public List<AlertVO> listOlderAlerts(Date oldTime);
}
