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
package com.cloud.template;

import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import com.cloud.api.commands.CopyTemplateCmd;
import com.cloud.api.commands.DeleteIsoCmd;
import com.cloud.api.commands.DeleteTemplateCmd;
import com.cloud.api.commands.ExtractIsoCmd;
import com.cloud.api.commands.ExtractTemplateCmd;
import com.cloud.api.commands.ListTemplateOrIsoPermissionsCmd;
import com.cloud.api.commands.RegisterIsoCmd;
import com.cloud.api.commands.RegisterTemplateCmd;
import com.cloud.api.commands.UpdateTemplateOrIsoPermissionsCmd;
import com.cloud.dc.DataCenterVO;
import com.cloud.exception.InternalErrorException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.StorageUnavailableException;
import com.cloud.host.HostVO;
import com.cloud.storage.StoragePool;
import com.cloud.storage.StoragePoolVO;
import com.cloud.storage.VMTemplateHostVO;
import com.cloud.storage.VMTemplateStoragePoolVO;
import com.cloud.storage.VMTemplateVO;
import com.cloud.utils.component.Manager;


@Local(value={TemplateManager.class, TemplateService.class})
public class MockTemplateManagerImpl implements TemplateManager, Manager, TemplateService {

	@Override
	public VirtualMachineTemplate registerTemplate(RegisterTemplateCmd cmd)
			throws URISyntaxException, ResourceAllocationException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public VirtualMachineTemplate registerIso(RegisterIsoCmd cmd)
			throws IllegalArgumentException, ResourceAllocationException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public VirtualMachineTemplate copyTemplate(CopyTemplateCmd cmd)
			throws StorageUnavailableException, ResourceAllocationException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public VirtualMachineTemplate prepareTemplate(long templateId, long zoneId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean detachIso(long vmId) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean attachIso(long isoId, long vmId) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean deleteTemplate(DeleteTemplateCmd cmd) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean deleteIso(DeleteIsoCmd cmd) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Long extract(ExtractIsoCmd cmd) throws InternalErrorException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Long extract(ExtractTemplateCmd cmd) throws InternalErrorException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public VirtualMachineTemplate getTemplate(long templateId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<String> listTemplatePermissions(
			ListTemplateOrIsoPermissionsCmd cmd) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean updateTemplateOrIsoPermissions(
			UpdateTemplateOrIsoPermissionsCmd cmd) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean configure(String name, Map<String, Object> params)
			throws ConfigurationException {
		return true;
	}

	@Override
	public boolean start() {
		return true;
	}

	@Override
	public boolean stop() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public VMTemplateStoragePoolVO prepareTemplateForCreate(
			VMTemplateVO template, StoragePool pool) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean resetTemplateDownloadStateOnPool(
			long templateStoragePoolRefId) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean copy(long userId, VMTemplateVO template, HostVO srcSecHost,
			DataCenterVO srcZone, DataCenterVO dstZone)
			throws StorageUnavailableException, ResourceAllocationException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean delete(long userId, long templateId, Long zoneId) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public List<VMTemplateStoragePoolVO> getUnusedTemplatesInPool(
			StoragePoolVO pool) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void evictTemplateFromStoragePool(
			VMTemplateStoragePoolVO templatePoolVO) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean templateIsDeleteable(VMTemplateHostVO templateHostRef) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public VMTemplateHostVO prepareISOForCreate(VMTemplateVO template,
			StoragePool pool) {
		// TODO Auto-generated method stub
		return null;
	}
}
