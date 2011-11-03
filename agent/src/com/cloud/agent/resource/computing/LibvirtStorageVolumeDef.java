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

public class LibvirtStorageVolumeDef {
	public enum volFormat {
		RAW("raw"),
		QCOW2("qcow2"),
		DIR("dir");
		private String _format;
		volFormat(String format) {
			_format = format;
		}
		@Override
		public String toString() {
			return _format;
		}
		
		public static volFormat getFormat(String format) {
			if (format == null) {
				return null;
			}
			if (format.equalsIgnoreCase("raw")) {
				return RAW;
			} else if (format.equalsIgnoreCase("qcow2")) {
				return QCOW2;
			}
			return null;
		}
	}
	private String _volName;
	private Long _volSize;
	private volFormat _volFormat;
	private String _backingPath;
	private volFormat _backingFormat;
	
	public LibvirtStorageVolumeDef(String volName, Long size, volFormat format, String tmplPath, volFormat tmplFormat) {
		_volName = volName;
		_volSize = size;
		_volFormat = format;
		_backingPath = tmplPath;
		_backingFormat = tmplFormat;
	}
	
	public volFormat getFormat() {
		return this._volFormat;
	}
	@Override
	public String toString() {
		StringBuilder storageVolBuilder = new StringBuilder();
		storageVolBuilder.append("<volume>\n");
		storageVolBuilder.append("<name>" + _volName + "</name>\n");
		if (_volSize != null) { 
			storageVolBuilder.append("<capacity >" + _volSize + "</capacity>\n");
		}
		storageVolBuilder.append("<target>\n");
		storageVolBuilder.append("<format type='" + _volFormat + "'/>\n");
		storageVolBuilder.append("</target>\n");
		if (_backingPath != null) {
			storageVolBuilder.append("<backingStore>\n");
			storageVolBuilder.append("<path>" + _backingPath + "</path>\n");
			storageVolBuilder.append("<format type='" + _backingFormat + "'/>\n");
			storageVolBuilder.append("</backingStore>\n");
		}
		storageVolBuilder.append("</volume>\n");
		return storageVolBuilder.toString();
	}
	
}
