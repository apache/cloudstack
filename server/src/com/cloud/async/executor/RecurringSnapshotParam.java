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

package com.cloud.async.executor;

public class RecurringSnapshotParam extends VMOperationParam {
	private int hourlyMax; 
	private int dailyMax;
	private int weeklyMax;
	private int monthlyMax;

	public RecurringSnapshotParam() {
	}

	public RecurringSnapshotParam(long userId, long vmId, int hourlyMax, int dailyMax, int weeklyMax, int monthlyMax) {
		setUserId(userId);
		setVmId(vmId);
		this.hourlyMax = hourlyMax;
		this.dailyMax = dailyMax;
		this.weeklyMax = weeklyMax;
		this.monthlyMax = monthlyMax;
	}

	public int getHourlyMax() {
		return hourlyMax;
	}

	public void setHourlyMax(int hourlyMax) {
		this.hourlyMax = hourlyMax;
	}

	public int getDailyMax() {
		return dailyMax;
	}

	public void setDailyMax(int dailyMax) {
		this.dailyMax = dailyMax;
	}

	public int getWeeklyMax() {
		return weeklyMax;
	}

	public void setWeeklyMax(int weeklyMax) {
		this.weeklyMax = weeklyMax;
	}

	public int getMonthlyMax() {
		return monthlyMax;
	}

	public void setMonthlyMax(int monthlyMax) {
		this.monthlyMax = monthlyMax;
	}
}
