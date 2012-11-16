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
package org.apache.cloudstack.storage.image.motion;

import org.apache.cloudstack.engine.cloud.entity.api.TemplateEntity;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPoint;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.storage.command.CopyTemplateToPrimaryStorage;
import org.apache.cloudstack.storage.datastore.PrimaryDataStore;
import org.apache.cloudstack.storage.image.TemplateInfo;
import org.apache.cloudstack.storage.to.TemplateTO;
import org.apache.cloudstack.storage.to.VolumeTO;

public class DefaultImageMotionStrategy implements ImageMotionStrategy {

	@Override
	public boolean canHandle(TemplateInfo template, VolumeInfo volume) {
		// TODO Auto-generated method stub
		return true;
	}

	//For default strategy, we will use one of endpoint in volume's datastore
	@Override
	public EndPoint getEndPoint(TemplateInfo template, VolumeInfo volume) {
		PrimaryDataStoreInfo pdi = volume.getDataStore();
		return pdi.getEndPoints().get(0);
	}

	@Override
	public boolean copyTemplate(TemplateInfo template, VolumeInfo volume,
			EndPoint ep) {
		VolumeTO vt = new VolumeTO(volume);
		TemplateTO tt = new TemplateTO(template);
		CopyTemplateToPrimaryStorage copyCommand = new CopyTemplateToPrimaryStorage(tt, vt);
		ep.sendMessage(copyCommand);
		return true;
	}

}
