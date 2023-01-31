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

package org.apache.cloudstack.api;

import java.util.Date;

import javax.inject.Inject;

import org.apache.cloudstack.metrics.MetricsService;

public abstract class BaseResourceUsageHistoryCmd extends BaseListCmd {

    @Inject
    protected MetricsService metricsService;

    // ///////////////////////////////////////////////////
    // /// BaseResourceUsageHistoryCmd API parameters ////
    // ///////////////////////////////////////////////////

    @Parameter(name = ApiConstants.START_DATE, type = CommandType.DATE, description = "start date to filter stats."
            + "Use format \"yyyy-MM-dd hh:mm:ss\")")
    private Date startDate;

    @Parameter(name = ApiConstants.END_DATE, type = CommandType.DATE, description = "end date to filter stats."
            + "Use format \"yyyy-MM-dd hh:mm:ss\")")
    private Date endDate;

    // ///////////////////////////////////////////////////
    // ///////////////// Accessors ///////////////////////
    // ///////////////////////////////////////////////////

    public Date getStartDate() {
        return startDate;
    }

    public Date getEndDate() {
        return endDate;
    }
}
