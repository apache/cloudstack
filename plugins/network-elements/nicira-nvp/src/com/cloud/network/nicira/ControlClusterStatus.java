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
package com.cloud.network.nicira;

public class ControlClusterStatus {
	private String cluster_status;
	private Stats node_stats;
	private Stats lqueue_stats;
	private Stats lport_stats;
	private Stats lrouterport_stats;
	private Stats lswitch_stats;
	private Stats zone_stats;
	private Stats lrouter_stats;
	private Stats security_profile_stats;
	
	public String getClusterStatus() {
		return cluster_status;
	}

	public Stats getNodeStats() {
		return node_stats;
	}

	public Stats getLqueueStats() {
		return lqueue_stats;
	}

	public Stats getLportStats() {
		return lport_stats;
	}

	public Stats getLrouterportStats() {
		return lrouterport_stats;
	}

	public Stats getLswitchStats() {
		return lswitch_stats;
	}

	public Stats getZoneStats() {
		return zone_stats;
	}

	public Stats getLrouterStats() {
		return lrouter_stats;
	}

	public Stats getSecurityProfileStats() {
		return security_profile_stats;
	}

	public class Stats {
		private int error_state_count;
		private int registered_count;
		private int active_count;
		
		public int getErrorStateCount() {
			return error_state_count;
		}
		public int getRegisteredCount() {
			return registered_count;
		}
		public int getActiveCount() {
			return active_count;
		}
		
	}
}
