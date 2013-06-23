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
package com.cloud.hypervisor.vmware.mo;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.cloud.hypervisor.vmware.util.VmwareContext;
import com.vmware.vim25.CustomFieldStringValue;
import com.vmware.vim25.DatastoreInfo;
import com.vmware.vim25.DynamicProperty;
import com.vmware.vim25.HostNasVolumeSpec;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.NasDatastoreInfo;
import com.vmware.vim25.ObjectContent;
import com.vmware.vim25.ObjectSpec;
import com.vmware.vim25.PropertyFilterSpec;
import com.vmware.vim25.PropertySpec;
import com.vmware.vim25.SelectionSpec;
import com.vmware.vim25.TraversalSpec;

public class HostDatastoreSystemMO extends BaseMO {

	public HostDatastoreSystemMO(VmwareContext context, ManagedObjectReference morHostDatastore) {
		super(context, morHostDatastore);
	}

	public HostDatastoreSystemMO(VmwareContext context, String morType, String morValue) {
		super(context, morType, morValue);
	}

	public ManagedObjectReference findDatastore(String name) throws Exception {
		// added cloud.com specific name convention, we will use custom field "cloud.uuid" as datastore name as well
		CustomFieldsManagerMO cfmMo = new CustomFieldsManagerMO(_context,
			_context.getServiceContent().getCustomFieldsManager());
		int key = cfmMo.getCustomFieldKey("Datastore", CustomFieldConstants.CLOUD_UUID);
		assert(key != 0);

		List<ObjectContent> ocs = getDatastorePropertiesOnHostDatastoreSystem(
			new String[] { "name", String.format("value[%d]", key) });
		if(ocs != null) {
			for(ObjectContent oc : ocs) {
				if(oc.getPropSet().get(0).getVal().equals(name))
					return oc.getObj();

				if(oc.getPropSet().size() > 1) {
					DynamicProperty prop = oc.getPropSet().get(1);
					if(prop != null && prop.getVal() != null) {
						if(prop.getVal() instanceof CustomFieldStringValue) {
							String val = ((CustomFieldStringValue)prop.getVal()).getValue();
							if(val.equalsIgnoreCase(name))
								return oc.getObj();
						}
					}
				}
			}
		}
		return null;
	}

	// storeUrl in nfs://host/exportpath format
	public ManagedObjectReference findDatastoreByUrl(String storeUrl) throws Exception {
		assert(storeUrl != null);

		List<ManagedObjectReference> datastores = getDatastores();
		if(datastores != null && datastores.size() > 0) {
			for(ManagedObjectReference morDatastore : datastores) {
				NasDatastoreInfo info = getNasDatastoreInfo(morDatastore);
				if(info != null) {
					URI uri = new URI(storeUrl);
					String vmwareStyleUrl = "netfs://" + uri.getHost() + "/" + uri.getPath() + "/";
					if(info.getUrl().equals(vmwareStyleUrl))
						return morDatastore;
				}
			}
		}

		return null;
	}

	// TODO this is a hacking helper method, when we can pass down storage pool info along with volume
	// we should be able to find the datastore by name
	public ManagedObjectReference findDatastoreByExportPath(String exportPath) throws Exception {
		assert(exportPath != null);

		List<ManagedObjectReference> datastores = getDatastores();
		if(datastores != null && datastores.size() > 0) {
			for(ManagedObjectReference morDatastore : datastores) {
				DatastoreMO dsMo = new DatastoreMO(_context, morDatastore);
				if(dsMo.getInventoryPath().equals(exportPath))
					return morDatastore;

				NasDatastoreInfo info = getNasDatastoreInfo(morDatastore);
				if(info != null) {
					String vmwareUrl = info.getUrl();
					if(vmwareUrl.charAt(vmwareUrl.length() - 1) == '/')
						vmwareUrl = vmwareUrl.substring(0, vmwareUrl.length() - 1);

					URI uri = new URI(vmwareUrl);
					if(uri.getPath().equals("/" + exportPath))
						return morDatastore;
				}
			}
		}

		return null;
	}

	public boolean deleteDatastore(String name) throws Exception {
		ManagedObjectReference morDatastore = findDatastore(name);
		if(morDatastore != null) {
			_context.getService().removeDatastore(_mor, morDatastore);
			return true;
		}
		return false;
	}

	public ManagedObjectReference createNfsDatastore(String host, int port,
		String exportPath, String uuid) throws Exception {

		HostNasVolumeSpec spec = new HostNasVolumeSpec();
		spec.setRemoteHost(host);
		spec.setRemotePath(exportPath);
		spec.setType("nfs");
		spec.setLocalPath(uuid);

		// readOnly/readWrite
		spec.setAccessMode("readWrite");
		return _context.getService().createNasDatastore(_mor, spec);
	}

	public List<ManagedObjectReference> getDatastores() throws Exception {
		return (List<ManagedObjectReference>)_context.getVimClient().getDynamicProperty(
			_mor, "datastore");
	}

	public DatastoreInfo getDatastoreInfo(ManagedObjectReference morDatastore) throws Exception {
		return (DatastoreInfo)_context.getVimClient().getDynamicProperty(morDatastore, "info");
	}

	public NasDatastoreInfo getNasDatastoreInfo(ManagedObjectReference morDatastore) throws Exception {
		DatastoreInfo info = (DatastoreInfo)_context.getVimClient().getDynamicProperty(morDatastore, "info");
		if(info instanceof NasDatastoreInfo)
			return (NasDatastoreInfo)info;
		return null;
	}

	public List<ObjectContent> getDatastorePropertiesOnHostDatastoreSystem(String[] propertyPaths) throws Exception {

		PropertySpec pSpec = new PropertySpec();
		pSpec.setType("Datastore");
		pSpec.getPathSet().addAll(Arrays.asList(propertyPaths));

	    TraversalSpec hostDsSys2DatastoreTraversal = new TraversalSpec();
	    hostDsSys2DatastoreTraversal.setType("HostDatastoreSystem");
	    hostDsSys2DatastoreTraversal.setPath("datastore");
	    hostDsSys2DatastoreTraversal.setName("hostDsSys2DatastoreTraversal");

	    ObjectSpec oSpec = new ObjectSpec();
	    oSpec.setObj(_mor);
	    oSpec.setSkip(Boolean.TRUE);
	    oSpec.getSelectSet().add(hostDsSys2DatastoreTraversal);

	    PropertyFilterSpec pfSpec = new PropertyFilterSpec();
	    pfSpec.getPropSet().add(pSpec);
	    pfSpec.getObjectSet().add(oSpec);
        List<PropertyFilterSpec> pfSpecArr = new ArrayList<PropertyFilterSpec>();
        pfSpecArr.add(pfSpec);

	    return _context.getService().retrieveProperties(
	    	_context.getPropertyCollector(), pfSpecArr);
	}
}
