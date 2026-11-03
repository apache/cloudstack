// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// the License.  You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package com.cloud.usage.parser;

import com.cloud.usage.dao.UsageDao;
import com.cloud.user.AccountVO;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.inject.Inject;
import java.util.Date;

public abstract class UsageParser {
    Logger logger = LogManager.getLogger(getClass());

    @Inject
    UsageDao usageDao;

    private void beforeParse(AccountVO account) {
        logger.debug("Parsing all {} usage events for account: [{}]", getParserName(), account);
    }

    public abstract String getParserName();

    protected abstract boolean parse(AccountVO account, Date startDate, Date endDate);

    public boolean doParsing(AccountVO account, Date startDate, Date endDate) {
        beforeParse(account);
        return parse(account, startDate, endDate);
    }
}
