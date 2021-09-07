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

import com.cloud.hypervisor.vmware.util.VmwareContext;
import com.vmware.vim25.CustomFieldStringValue;
import com.vmware.vim25.DatastoreInfo;
import com.vmware.vim25.DynamicProperty;
import com.vmware.vim25.HostNasVolumeSpec;
import com.vmware.vim25.HostResignatureRescanResult;
import com.vmware.vim25.HostScsiDisk;
import com.vmware.vim25.HostUnresolvedVmfsResignatureSpec;
import com.vmware.vim25.HostUnresolvedVmfsVolume;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.NasDatastoreInfo;
import com.vmware.vim25.ObjectContent;
import com.vmware.vim25.ObjectSpec;
import com.vmware.vim25.PropertyFilterSpec;
import com.vmware.vim25.PropertySpec;
import com.vmware.vim25.RetrieveOptions;
import com.vmware.vim25.RetrieveResult;
import com.vmware.vim25.SelectionSpec;
import com.vmware.vim25.TraversalSpec;
import com.vmware.vim25.VmfsDatastoreCreateSpec;
import com.vmware.vim25.VmfsDatastoreExpandSpec;
import com.vmware.vim25.VmfsDatastoreOption;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class HostDatastoreSystemMO extends BaseMO {

    public HostDatastoreSystemMO(VmwareContext context, ManagedObjectReference morHostDatastore) {
        super(context, morHostDatastore);
    }

    public HostDatastoreSystemMO(VmwareContext context, String morType, String morValue) {
        super(context, morType, morValue);
    }

    public ManagedObjectReference findDatastore(String name) throws Exception {
        ManagedObjectReference morDatastore = findSpecificDatastore(name);
        if (morDatastore == null) {
            morDatastore = findDatastoreCluster(name);
        }
        return morDatastore;
    }

    public ManagedObjectReference findSpecificDatastore(String name) throws Exception {
        // added Apache CloudStack specific name convention, we will use custom field "cloud.uuid" as datastore name as well
        CustomFieldsManagerMO cfmMo = new CustomFieldsManagerMO(_context, _context.getServiceContent().getCustomFieldsManager());
        int key = cfmMo.getCustomFieldKey("Datastore", CustomFieldConstants.CLOUD_UUID);
        assert (key != 0);

        List<ObjectContent> ocs = getDatastorePropertiesOnHostDatastoreSystem(new String[] {"name", String.format("value[%d]", key)});
        if (ocs != null) {
            for (ObjectContent oc : ocs) {
                if (oc.getPropSet().get(0).getVal().equals(name))
                    return oc.getObj();

                if (oc.getPropSet().size() > 1) {
                    DynamicProperty prop = oc.getPropSet().get(1);
                    if (prop != null && prop.getVal() != null) {
                        if (prop.getVal() instanceof CustomFieldStringValue) {
                            String val = ((CustomFieldStringValue)prop.getVal()).getValue();
                            if (val.equalsIgnoreCase(name))
                                return oc.getObj();
                        }
                    }
                }
            }
        }
        return null;
    }

    public ManagedObjectReference findDatastoreCluster(String name) throws Exception {
        // added Apache CloudStack specific name convention, we will use custom field "cloud.uuid" as datastore name as well
        CustomFieldsManagerMO cfmMo = new CustomFieldsManagerMO(_context, _context.getServiceContent().getCustomFieldsManager());
        int key = cfmMo.getCustomFieldKey("StoragePod", CustomFieldConstants.CLOUD_UUID);
        assert (key != 0);

        List<ObjectContent> ocs = getDatastoreClusterPropertiesOnHostDatastoreSystem(new String[] {"name", String.format("value[%d]", key)});
        if (ocs != null) {
            for (ObjectContent oc : ocs) {
                if (oc.getPropSet().get(0).getVal().equals(name))
                    return oc.getObj();

                if (oc.getPropSet().size() > 1) {
                    DynamicProperty prop = oc.getPropSet().get(1);
                    if (prop != null && prop.getVal() != null) {
                        if (prop.getVal() instanceof CustomFieldStringValue) {
                            String val = ((CustomFieldStringValue)prop.getVal()).getValue();
                            if (val.equalsIgnoreCase(name))
                                return oc.getObj();
                        }
                    }
                }
            }
        }
        return null;
    }

    public List<HostUnresolvedVmfsVolume> queryUnresolvedVmfsVolumes() throws Exception {
        return _context.getService().queryUnresolvedVmfsVolumes(_mor);
    }

    public List<VmfsDatastoreOption> queryVmfsDatastoreExpandOptions(DatastoreMO datastoreMO) throws Exception {
        return _context.getService().queryVmfsDatastoreExpandOptions(_mor, datastoreMO.getMor());
    }

    public void expandVmfsDatastore(DatastoreMO datastoreMO, VmfsDatastoreExpandSpec vmfsDatastoreExpandSpec) throws Exception {
        _context.getService().expandVmfsDatastore(_mor, datastoreMO.getMor(), vmfsDatastoreExpandSpec);
    }

    // storeUrl in nfs://host/exportpath format
    public ManagedObjectReference findDatastoreByUrl(String storeUrl) throws Exception {
        assert (storeUrl != null);

        List<ManagedObjectReference> datastores = getDatastores();
        if (datastores != null && datastores.size() > 0) {
            for (ManagedObjectReference morDatastore : datastores) {
                NasDatastoreInfo info = getNasDatastoreInfo(morDatastore);
                if (info != null) {
                    URI uri = new URI(storeUrl);
                    String vmwareStyleUrl = "netfs://" + uri.getHost() + "/" + uri.getPath() + "/";
                    if (info.getUrl().equals(vmwareStyleUrl))
                        return morDatastore;
                }
            }
        }

        return null;
    }

    public ManagedObjectReference findDatastoreByName(String datastoreName) throws Exception {
        assert (datastoreName != null);

        List<ManagedObjectReference> datastores = getDatastores();

        if (datastores != null) {
            for (ManagedObjectReference morDatastore : datastores) {
                DatastoreInfo info = getDatastoreInfo(morDatastore);

                if (info != null) {
                    if (info.getName().equals(datastoreName))
                        return morDatastore;
                }
            }
        }

        return null;
    }

    // TODO this is a hacking helper method, when we can pass down storage pool info along with volume
    // we should be able to find the datastore by name
    public ManagedObjectReference findDatastoreByExportPath(String exportPath) throws Exception {
        assert (exportPath != null);

        List<ManagedObjectReference> datastores = getDatastores();
        if (datastores != null && datastores.size() > 0) {
            for (ManagedObjectReference morDatastore : datastores) {
                DatastoreMO dsMo = new DatastoreMO(_context, morDatastore);
                if (dsMo.getInventoryPath().equals(exportPath))
                    return morDatastore;

                NasDatastoreInfo info = getNasDatastoreInfo(morDatastore);
                if (info != null) {
                    String vmwareUrl = info.getUrl();
                    if (vmwareUrl.charAt(vmwareUrl.length() - 1) == '/')
                        vmwareUrl = vmwareUrl.substring(0, vmwareUrl.length() - 1);

                    URI uri = new URI(vmwareUrl);
                    if (uri.getPath().equals("/" + exportPath))
                        return morDatastore;
                }
            }
        }

        return null;
    }

    public List<HostScsiDisk> queryAvailableDisksForVmfs() throws Exception {
        return _context.getService().queryAvailableDisksForVmfs(_mor, null);
    }

    public ManagedObjectReference createVmfsDatastore(String datastoreName, HostScsiDisk hostScsiDisk) throws Exception {
        // just grab the first instance of VmfsDatastoreOption
        VmfsDatastoreOption vmfsDatastoreOption = _context.getService().queryVmfsDatastoreCreateOptions(_mor, hostScsiDisk.getDevicePath(), 5).get(0);

        VmfsDatastoreCreateSpec vmfsDatastoreCreateSpec = (VmfsDatastoreCreateSpec)vmfsDatastoreOption.getSpec();

        // set the name of the datastore to be created
        vmfsDatastoreCreateSpec.getVmfs().setVolumeName(datastoreName);

        return _context.getService().createVmfsDatastore(_mor, vmfsDatastoreCreateSpec);
    }

    public boolean deleteDatastore(String name) throws Exception {
        ManagedObjectReference morDatastore = findDatastore(name);
        if (morDatastore != null) {
            _context.getService().removeDatastore(_mor, morDatastore);
            return true;
        }
        return false;
    }

    public ManagedObjectReference createNfsDatastore(String host, int port, String exportPath, String uuid) throws Exception {

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
        return _context.getVimClient().getDynamicProperty(_mor, "datastore");
    }

    public DatastoreInfo getDatastoreInfo(ManagedObjectReference morDatastore) throws Exception {
        return (DatastoreInfo)_context.getVimClient().getDynamicProperty(morDatastore, "info");
    }

    public NasDatastoreInfo getNasDatastoreInfo(ManagedObjectReference morDatastore) throws Exception {
        DatastoreInfo info = (DatastoreInfo)_context.getVimClient().getDynamicProperty(morDatastore, "info");
        if (info instanceof NasDatastoreInfo)
            return (NasDatastoreInfo)info;
        return null;
    }

    public HostResignatureRescanResult resignatureUnresolvedVmfsVolume(HostUnresolvedVmfsResignatureSpec resolutionSpec) throws Exception {
        ManagedObjectReference task = _context.getService().resignatureUnresolvedVmfsVolumeTask(_mor, resolutionSpec);

        boolean result = _context.getVimClient().waitForTask(task);

        if (result) {
            _context.waitForTaskProgressDone(task);

            TaskMO taskMO = new TaskMO(_context, task);

            return (HostResignatureRescanResult)taskMO.getTaskInfo().getResult();
        } else {
            throw new Exception("Unable to register vm due to " + TaskMO.getTaskFailureInfo(_context, task));
        }
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

        return _context.getService().retrieveProperties(_context.getPropertyCollector(), pfSpecArr);
    }

    public List<ObjectContent> getDatastoreClusterPropertiesOnHostDatastoreSystem(String[] propertyPaths) throws Exception {
        ManagedObjectReference retVal = null;
        // Create Property Spec
        PropertySpec propertySpec = new PropertySpec();
        propertySpec.setAll(Boolean.FALSE);
        propertySpec.setType("StoragePod");
        propertySpec.getPathSet().addAll(Arrays.asList(propertyPaths));

        // Now create Object Spec
        ObjectSpec objectSpec = new ObjectSpec();
        objectSpec.setObj(getContext().getRootFolder());
        objectSpec.setSkip(Boolean.TRUE);
        objectSpec.getSelectSet().addAll(
                Arrays.asList(getStorageTraversalSpec()));

        // Create PropertyFilterSpec using the PropertySpec and ObjectPec
        // created above.
        PropertyFilterSpec propertyFilterSpec = new PropertyFilterSpec();
        propertyFilterSpec.getPropSet().add(propertySpec);
        propertyFilterSpec.getObjectSet().add(objectSpec);

        List<PropertyFilterSpec> listpfs = new ArrayList<PropertyFilterSpec>();
        listpfs.add(propertyFilterSpec);
        return retrievePropertiesAllObjects(listpfs);
    }

    private SelectionSpec[] getStorageTraversalSpec() {
        // create a traversal spec that start from root folder

        SelectionSpec ssFolders = new SelectionSpec();
        ssFolders.setName("visitFolders");

        TraversalSpec datacenterSpec = new TraversalSpec();
        datacenterSpec.setName("dcTodf");
        datacenterSpec.setType("Datacenter");
        datacenterSpec.setPath("datastoreFolder");
        datacenterSpec.setSkip(Boolean.FALSE);
        datacenterSpec.getSelectSet().add(ssFolders);

        TraversalSpec visitFolder = new TraversalSpec();
        visitFolder.setType("Folder");
        visitFolder.setName("visitFolders");
        visitFolder.setPath("childEntity");
        visitFolder.setSkip(Boolean.FALSE);

        List<SelectionSpec> ssSpecList = new ArrayList<SelectionSpec>();
        ssSpecList.add(datacenterSpec);
        ssSpecList.add(ssFolders);

        visitFolder.getSelectSet().addAll(ssSpecList);
        return (new SelectionSpec[]{visitFolder});
    }

    private List<ObjectContent> retrievePropertiesAllObjects(
            List<PropertyFilterSpec> listpfs) throws Exception {

        RetrieveOptions propObjectRetrieveOpts = new RetrieveOptions();

        List<ObjectContent> listobjcontent = new ArrayList<ObjectContent>();

        RetrieveResult rslts =
                getContext().getService().retrievePropertiesEx(getContext().getServiceContent().getPropertyCollector(), listpfs,
                        propObjectRetrieveOpts);
        if (rslts != null && rslts.getObjects() != null
                && !rslts.getObjects().isEmpty()) {
            listobjcontent.addAll(rslts.getObjects());
        }
        String token = null;
        if (rslts != null && rslts.getToken() != null) {
            token = rslts.getToken();
        }
        while (token != null && !token.isEmpty()) {
            rslts =
                    getContext().getService().continueRetrievePropertiesEx(getContext().getServiceContent().getPropertyCollector(), token);
            token = null;
            if (rslts != null) {
                token = rslts.getToken();
                if (rslts.getObjects() != null && !rslts.getObjects().isEmpty()) {
                    listobjcontent.addAll(rslts.getObjects());
                }
            }
        }
        return listobjcontent;
    }

}
