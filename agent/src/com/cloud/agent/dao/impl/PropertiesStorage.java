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
package com.cloud.agent.dao.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;

import javax.ejb.Local;

import org.apache.log4j.Logger;

import com.cloud.agent.dao.StorageComponent;
import com.cloud.utils.PropertiesUtil;

/**
 * Uses Properties to implement storage.
 * 
 * @config {@table || Param Name | Description | Values | Default || || path |
 *         path to the properties _file | String | db/db.properties || * }
 **/
@Local(value = { StorageComponent.class })
public class PropertiesStorage implements StorageComponent {
	private static final Logger s_logger = Logger
			.getLogger(PropertiesStorage.class);
	Properties _properties = new Properties();
	File _file;
	String _name;

	@Override
	public synchronized String get(String key) {
		return _properties.getProperty(key);
	}

	@Override
	public synchronized void persist(String key, String value) {
		_properties.setProperty(key, value);
		FileOutputStream output = null;
		try {
			output = new FileOutputStream(_file);
			_properties.store(output, _name);
			output.flush();
			output.close();
		} catch (FileNotFoundException e) {
			s_logger.error("Who deleted the file? ", e);
		} catch (IOException e) {
			s_logger.error("Uh-oh: ", e);
		} finally {
			if (output != null) {
				try {
					output.close();
				} catch (IOException e) {
					// ignore.
				}
			}
		}
	}

	@Override
	public boolean configure(String name, Map<String, Object> params) {
		_name = name;
		String path = (String) params.get("path");
		if (path == null) {
			path = "agent.properties";
		}

		File file = PropertiesUtil.findConfigFile(path);
		if (file == null) {
			file = new File(path);
			try {
				if (!file.createNewFile()) {
					s_logger.error("Unable to create _file: "
							+ file.getAbsolutePath());
					return false;
				}
			} catch (IOException e) {
				s_logger.error(
						"Unable to create _file: " + file.getAbsolutePath(), e);
				return false;
			}
		}

		try {
			_properties.load(new FileInputStream(file));
			_file = file;
		} catch (FileNotFoundException e) {
			s_logger.error("How did we get here? ", e);
			return false;
		} catch (IOException e) {
			s_logger.error("IOException: ", e);
			return false;
		}

		return true;
	}

	@Override
	public String getName() {
		return _name;
	}

	@Override
	public boolean start() {
		return true;
	}

	@Override
	public boolean stop() {
		return true;
	}

}
