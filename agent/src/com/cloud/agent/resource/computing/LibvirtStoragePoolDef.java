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

package com.cloud.agent.resource.computing;

public class LibvirtStoragePoolDef {
	public enum poolType {
		ISCSI("iscsi"),
		NETFS("netfs"),
		LOGICAL("logical"),
		DIR("dir");
		String _poolType;
		poolType(String poolType) {
			_poolType = poolType;
		}
		@Override
		public String toString() {
			return _poolType;
		}
	}
	private poolType _poolType;
	private String _poolName;
	private String _uuid;
	private String _sourceHost;
	private String _sourceDir;
	private String _targetPath;
	
	public LibvirtStoragePoolDef(poolType type, String poolName, String uuid, String host, String dir, String targetPath) {
		_poolType = type;
		_poolName = poolName;
		_uuid = uuid;
		_sourceHost = host;
		_sourceDir = dir;
		_targetPath = targetPath;
	}
	
	public String getPoolName() {
	    return _poolName;
	}
	
	public poolType getPoolType() {
	    return _poolType;
	}
	
	public String getSourceHost() {
	    return _sourceHost;
	}
	
	public String getSourceDir() {
	    return _sourceDir;
	}
	
	public String getTargetPath() {
	    return _targetPath;
	}
	
    @Override
	public String toString() {
		StringBuilder storagePoolBuilder = new StringBuilder();
		storagePoolBuilder.append("<pool type='" + _poolType + "'>\n");
		storagePoolBuilder.append("<name>" + _poolName + "</name>\n");
		if (_uuid != null)
			storagePoolBuilder.append("<uuid>" + _uuid + "</uuid>\n");
		if (_poolType == poolType.NETFS) {
			storagePoolBuilder.append("<source>\n");
			storagePoolBuilder.append("<host name='" + _sourceHost + "'/>\n");
			storagePoolBuilder.append("<dir path='" + _sourceDir + "'/>\n");
			storagePoolBuilder.append("</source>\n");
		}
		storagePoolBuilder.append("<target>\n");
		storagePoolBuilder.append("<path>" + _targetPath + "</path>\n");
		storagePoolBuilder.append("</target>\n");
		storagePoolBuilder.append("</pool>\n");
		return storagePoolBuilder.toString();
	}
}
