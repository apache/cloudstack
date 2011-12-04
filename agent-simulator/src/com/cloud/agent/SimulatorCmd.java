/**
 * *  Copyright (C) 2011 Citrix Systems, Inc.  All rights reserved
*
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

package com.cloud.agent;

import java.io.Serializable;

public class SimulatorCmd implements Serializable {
	private static final long serialVersionUID = 1L;
	
	private String testCase = "DEFAULT";
	
	public SimulatorCmd(String testCase) {
		this.testCase = testCase; 
	}
	
	public String getTestCase() {
		return testCase;
	}
	
	public void setTestCase(String testCase) {
		this.testCase = testCase;
	}
}
