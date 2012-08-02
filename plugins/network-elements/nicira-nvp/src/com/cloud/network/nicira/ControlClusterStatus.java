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
