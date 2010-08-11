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
package com.cloud.host;

/**
 * @author chiradeep
 *
 */
public final class HostInfo {
	public static final String HYPERVISOR_VERSION = "Hypervisor.Version"; //tricky since KVM has userspace version and kernel version
	public static final String HOST_OS = "Host.OS"; //Fedora, XenServer, Ubuntu, etc
	public static final String HOST_OS_VERSION = "Host.OS.Version"; //12, 5.5, 9.10, etc
	public static final String HOST_OS_KERNEL_VERSION = "Host.OS.Kernel.Version"; //linux-2.6.31 etc
	
}
