/**
 *  Copyright (C) 2011 Cloud.com, Inc.  All rights reserved.
 */

package com.cloud.baremetal;

public interface ExternalDhcpEntryListener {
	public class DhcpEntryState {
		String _name;
		
		public static final DhcpEntryState add = new DhcpEntryState("add");
		public static final DhcpEntryState old = new DhcpEntryState("old");
		public static final DhcpEntryState del = new DhcpEntryState("del");
		
		public DhcpEntryState(String name) {
			_name = name;
		}
		
		public String getName() {
			return _name;
		}
	}
	
	/**
	 * Notify that DHCP entry state change
	 * @param ip
	 * @param mac
	 * @param DHCP entry state
	 * @return: true means continuous listen on the entry, false cancels the listener
	 */
	public boolean notify(String ip, String mac, DhcpEntryState state, Object userData);
}
