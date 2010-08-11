/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */
package com.cloud.utils.concurrency;

import java.util.Calendar;
import java.util.Date;
import java.util.TimerTask;

/**
 * A test clock which is also a TimerTask. The task calls a Scheduler's poll method
 * every time it is called by a timer to run 
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
        return _minute;
    }
    public int getHour() {
        return _hour;
    }
    public int getDay() {
        return _day;
    }
    public int getWeek() {
        return _week;
    }
    public int getMonth() {
        return _month;
    }
    public int getYear() {
        return _year;
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
        synchronized(this) {
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
