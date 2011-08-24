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
