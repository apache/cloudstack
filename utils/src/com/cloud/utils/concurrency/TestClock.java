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

package com.cloud.utils.concurrency;

import java.util.Calendar;
import java.util.Date;
import java.util.TimerTask;

/**
 * A test clock which is also a TimerTask. The task calls a Scheduler's poll method
 *
 */
public class TestClock extends TimerTask {
    private int _minute = 0;
    private int _hour = 0;
    private int _day = 0;
    private int _week = 0;
    private int _month = 0;
    private int _year = 0;
    private Calendar _cal = null;
    private final int _minutesPerHour;
    private final int _hoursPerDay;
    private final int _daysPerWeek;
    private final int _daysPerMonth;
    private final int _weeksPerMonth;
    private final int _monthsPerYear;
    private final Scheduler _scheduler;

    public TestClock(Scheduler scheduler, int minutesPerHour, int hoursPerDay, int daysPerWeek, int daysPerMonth, int weeksPerMonth, int monthsPerYear) {
        _minutesPerHour = minutesPerHour;
        _hoursPerDay = hoursPerDay;
        _daysPerWeek = daysPerWeek;
        _daysPerMonth = daysPerMonth;
        _weeksPerMonth = weeksPerMonth;
        _monthsPerYear = monthsPerYear;
        _cal = Calendar.getInstance();
        _year = _cal.get(Calendar.YEAR);
        _month = _cal.get(Calendar.MONTH);
        _day = _cal.get(Calendar.DAY_OF_MONTH);
        _week = _cal.get(Calendar.WEEK_OF_MONTH);
        _hour = _cal.get(Calendar.HOUR_OF_DAY);
        _minute = _cal.get(Calendar.MINUTE);
        _scheduler = scheduler;
    }

    public int getMinute() {
        synchronized (this) {
            return _minute;
        }
    }

    public int getHour() {
        synchronized (this) {
            return _hour;
        }
    }

    public int getDay() {
        synchronized (this) {
            return _day;
        }
    }

    public int getWeek() {
        synchronized (this) {
            return _week;
        }
    }

    public int getMonth() {
        synchronized (this) {
            return _month;
        }
    }

    public int getYear() {
        synchronized (this) {
            return _year;
        }
    }

    public int getMinutesPerHour() {
        return _minutesPerHour;
    }

    public int getHoursPerDay() {
        return _hoursPerDay;
    }

    public int getDaysPerMonth() {
        return _daysPerMonth;
    }

    public int getDaysPerWeek() {
        return _daysPerWeek;
    }

    public int getWeeksPerMonth() {
        return _weeksPerMonth;
    }

    public int getMonthsPerYear() {
        return _monthsPerYear;
    }

    @Override
    public void run() {
        synchronized (this) {
            _minute++;
            if ((_minute > 0) && ((_minute % _minutesPerHour) == 0)) {
                _minute = 0;
                _hour++;
            }

            if ((_hour > 0) && ((_hour % _hoursPerDay) == 0)) {
                _hour = 0;
                _day++;
            }

            if ((_day > 0) && ((_day % _daysPerWeek) == 0)) {
                _week++;
            }

            if ((_day > 0) && ((_day % _daysPerMonth) == 0)) {
                _day = 0;
                _week = 0;
                _month++;
            }

            if ((_month > 0) && ((_month % _monthsPerYear) == 0)) {
                _month = 0;
                _year++;
            }
            if (_scheduler != null) {
                // XXX: Creating new date is hugely inefficient for every minute.
                // Later the time in the database will be changed to currentTimeInMillis.
                // Then we can use System.getCurrentTimeInMillis() which is damn cheap.
                _cal.set(_year, _month, _day, _hour, _minute);
                Date currentTimestamp = _cal.getTime();
                _scheduler.poll(currentTimestamp);
            }
        }
    }
}
