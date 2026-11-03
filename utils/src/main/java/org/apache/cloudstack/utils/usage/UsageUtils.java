//
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
//

package org.apache.cloudstack.utils.usage;

import com.cloud.utils.DateUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class UsageUtils {
    protected static Logger logger = LogManager.getLogger(UsageUtils.class);

    public static final int USAGE_AGGREGATION_RANGE_MIN = 1;

    public static Date getNextJobExecutionTime(TimeZone usageTimeZone, String jobExecTimeConfig) {
        return getJobExecutionTime(usageTimeZone, jobExecTimeConfig, true);
    }

    public static Date getPreviousJobExecutionTime(TimeZone usageTimeZone, String jobExecTimeConfig) {
        return getJobExecutionTime(usageTimeZone, jobExecTimeConfig, false);
    }

    protected static Date getJobExecutionTime(TimeZone usageTimeZone, String jobExecTimeConfig, boolean next) {
        String[] execTimeSegments = jobExecTimeConfig.split(":");
        if (execTimeSegments.length != 2) {
            logger.warn("Unable to parse configuration 'usage.stats.job.exec.time'.");
            return null;
        }
        int hourOfDay;
        int minutes;
        try {
            hourOfDay = Integer.parseInt(execTimeSegments[0]);
            minutes = Integer.parseInt(execTimeSegments[1]);
        } catch (NumberFormatException e) {
            logger.warn("Unable to parse configuration 'usage.stats.job.exec.time' due to non-numeric values in [{}].", jobExecTimeConfig, e);
            return null;
        }

        Date currentDate = DateUtil.currentGMTTime();
        Calendar jobExecTime = Calendar.getInstance(usageTimeZone);
        jobExecTime.setTime(currentDate);
        jobExecTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
        jobExecTime.set(Calendar.MINUTE, minutes);
        jobExecTime.set(Calendar.SECOND, 0);
        jobExecTime.set(Calendar.MILLISECOND, 0);

        if (next && jobExecTime.getTime().before(currentDate)) {
            jobExecTime.add(Calendar.DAY_OF_YEAR, 1);
        } else if (!next && jobExecTime.getTime().after(currentDate)) {
            jobExecTime.add(Calendar.DAY_OF_YEAR, -1);
        }

        return jobExecTime.getTime();
    }
}
