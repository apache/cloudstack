/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cloudstack.storage.driver;

import org.apache.cloudstack.platform.subsystem.api.storage.DataObject;
import org.apache.cloudstack.platform.subsystem.api.storage.DataStore;
import org.apache.cloudstack.platform.subsystem.api.storage.DataStoreEndPoint;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.storage.TemplateProfile;

public class DefaultNfsSecondaryDriver extends AbstractStorageDriver {

	/**
	 * @param ds
	 */
	public DefaultNfsSecondaryDriver(DataStore ds) {
		super(ds);
		// TODO Auto-generated constructor stub
	}

	public String getDriverType() {
		// TODO Auto-generated method stub
		return null;
	}

	public TemplateProfile register(TemplateProfile tp, DataStoreEndPoint ep) {
		// TODO Auto-generated method stub
		return null;
	}

	public DataObject create(DataObject obj) {
		// TODO Auto-generated method stub
		return null;
	}

	public DataObject copy(DataObject src, DataStore dest) {
		// TODO Auto-generated method stub
		return null;
	}

	public DataObject copy(DataObject src, DataObject dest) {
		// TODO Auto-generated method stub
		return null;
	}

	public DataObject move(DataObject src, DataObject dest) {
		// TODO Auto-generated method stub
		return null;
	}

	public Answer sendMessage(DataStoreEndPoint dsep, Command cmd) {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean delete(DataObject obj) {
		// TODO Auto-generated method stub
		return false;
	}

}
