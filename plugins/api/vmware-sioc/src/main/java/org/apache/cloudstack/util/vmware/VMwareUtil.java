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
package org.apache.cloudstack.util.vmware;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSessionContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.WebServiceException;

import org.apache.cloudstack.util.LoginInfo;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import com.cloud.hypervisor.vmware.mo.VirtualMachineDiskInfoBuilder;
import com.vmware.vim25.DynamicProperty;
import com.vmware.vim25.InvalidCollectorVersionFaultMsg;
import com.vmware.vim25.InvalidPropertyFaultMsg;
import com.vmware.vim25.LocalizedMethodFault;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.ObjectContent;
import com.vmware.vim25.ObjectSpec;
import com.vmware.vim25.ObjectUpdate;
import com.vmware.vim25.ObjectUpdateKind;
import com.vmware.vim25.PropertyChange;
import com.vmware.vim25.PropertyChangeOp;
import com.vmware.vim25.PropertyFilterSpec;
import com.vmware.vim25.PropertyFilterUpdate;
import com.vmware.vim25.PropertySpec;
import com.vmware.vim25.RetrieveOptions;
import com.vmware.vim25.RetrieveResult;
import com.vmware.vim25.RuntimeFaultFaultMsg;
import com.vmware.vim25.SelectionSpec;
import com.vmware.vim25.ServiceContent;
import com.vmware.vim25.TaskInfoState;
import com.vmware.vim25.TraversalSpec;
import com.vmware.vim25.UpdateSet;
import com.vmware.vim25.VimPortType;
import com.vmware.vim25.VimService;
import com.vmware.vim25.VirtualDevice;
import com.vmware.vim25.VirtualDeviceBackingInfo;
import com.vmware.vim25.VirtualDisk;
import com.vmware.vim25.VirtualDiskFlatVer2BackingInfo;
import com.vmware.vim25.VirtualIDEController;
import com.vmware.vim25.VirtualMachineConfigSpec;
import com.vmware.vim25.VirtualSCSIController;

public class VMwareUtil {
    protected static Logger LOGGER = LogManager.getLogger(VMwareUtil.class);

    private VMwareUtil() {}

    public static class VMwareConnection {
        private VimPortType _vimPortType;
        private ServiceContent _serviceContent;

        VMwareConnection(VimPortType vimPortType, ServiceContent serviceContent) {
            _vimPortType = vimPortType;
            _serviceContent = serviceContent;
        }

        VimPortType getVimPortType() {
            return _vimPortType;
        }

        ServiceContent getServiceContent() {
            return _serviceContent;
        }
    }

    public static VMwareConnection getVMwareConnection(LoginInfo loginInfo) throws Exception {
        trustAllHttpsCertificates();

        HostnameVerifier hv = new HostnameVerifier() {
            @Override
            public boolean verify(String urlHostName, SSLSession session) {
                return true;
            }
        };

        HttpsURLConnection.setDefaultHostnameVerifier(hv);

        ManagedObjectReference serviceInstanceRef = new ManagedObjectReference();

        final String serviceInstanceName = "ServiceInstance";

        serviceInstanceRef.setType(serviceInstanceName);
        serviceInstanceRef.setValue(serviceInstanceName);

        VimService vimService = new VimService();

        VimPortType vimPortType = vimService.getVimPort();

        Map<String, Object> ctxt = ((BindingProvider)vimPortType).getRequestContext();

        ctxt.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, "https://" + loginInfo.getHost() + "/sdk");
        ctxt.put(BindingProvider.SESSION_MAINTAIN_PROPERTY, true);

        ServiceContent serviceContent = vimPortType.retrieveServiceContent(serviceInstanceRef);

        vimPortType.login(serviceContent.getSessionManager(), loginInfo.getUsername(), loginInfo.getPassword(), null);

        return new VMwareConnection(vimPortType, serviceContent);
    }

    public static void closeVMwareConnection(VMwareConnection connection) throws Exception {
        if (connection != null) {
            connection.getVimPortType().logout(connection.getServiceContent().getSessionManager());
        }
    }

    public static Map<String, ManagedObjectReference> getVms(VMwareConnection connection) throws Exception {
        Map<String, ManagedObjectReference> nameToVm = new HashMap<>();

        ManagedObjectReference rootFolder = connection.getServiceContent().getRootFolder();

        TraversalSpec tSpec = getVMTraversalSpec();

        PropertySpec propertySpec = new PropertySpec();

        propertySpec.setAll(Boolean.FALSE);
        propertySpec.getPathSet().add("name");
        propertySpec.setType("VirtualMachine");

        ObjectSpec objectSpec = new ObjectSpec();

        objectSpec.setObj(rootFolder);
        objectSpec.setSkip(Boolean.TRUE);
        objectSpec.getSelectSet().add(tSpec);

        PropertyFilterSpec propertyFilterSpec = new PropertyFilterSpec();

        propertyFilterSpec.getPropSet().add(propertySpec);
        propertyFilterSpec.getObjectSet().add(objectSpec);

        List<PropertyFilterSpec> lstPfs = new ArrayList<>(1);

        lstPfs.add(propertyFilterSpec);

        VimPortType vimPortType = connection.getVimPortType();
        ManagedObjectReference propertyCollector = connection.getServiceContent().getPropertyCollector();

        List<ObjectContent> lstObjectContent = retrievePropertiesAllObjects(lstPfs, vimPortType, propertyCollector);

        if (lstObjectContent != null) {
            for (ObjectContent oc : lstObjectContent) {
                ManagedObjectReference mor = oc.getObj();
                List<DynamicProperty> dps = oc.getPropSet();
                String vmName = null;

                if (dps != null) {
                    for (DynamicProperty dp : dps) {
                        vmName = (String)dp.getVal();
                    }
                }

                if (vmName != null) {
                    nameToVm.put(vmName, mor);
                }
            }
        }

        return nameToVm;
    }

    public static Map<String, Object> getEntityProps(VMwareConnection connection, ManagedObjectReference entityMor, String[] props)
            throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg {
        Map<String, Object> retVal = new HashMap<>();

        PropertySpec propertySpec = new PropertySpec();

        propertySpec.setAll(Boolean.FALSE);
        propertySpec.setType(entityMor.getType());
        propertySpec.getPathSet().addAll(Arrays.asList(props));

        ObjectSpec objectSpec = new ObjectSpec();

        objectSpec.setObj(entityMor);

        // Create PropertyFilterSpec using the PropertySpec and ObjectPec created above.
        PropertyFilterSpec propertyFilterSpec = new PropertyFilterSpec();

        propertyFilterSpec.getPropSet().add(propertySpec);
        propertyFilterSpec.getObjectSet().add(objectSpec);

        List<PropertyFilterSpec> propertyFilterSpecs = new ArrayList<>();

        propertyFilterSpecs.add(propertyFilterSpec);

        RetrieveResult rslts = connection.getVimPortType().retrievePropertiesEx(connection.getServiceContent().getPropertyCollector(),
                propertyFilterSpecs, new RetrieveOptions());
        List<ObjectContent> listobjcontent = new ArrayList<>();

        if (rslts != null && rslts.getObjects() != null && !rslts.getObjects().isEmpty()) {
            listobjcontent.addAll(rslts.getObjects());
        }

        String token = null;

        if (rslts != null && rslts.getToken() != null) {
            token = rslts.getToken();
        }

        while (token != null && !token.isEmpty()) {
            rslts = connection.getVimPortType().continueRetrievePropertiesEx(connection.getServiceContent().getPropertyCollector(),
                    token);

            token = null;

            if (rslts != null) {
                token = rslts.getToken();

                if (rslts.getObjects() != null && !rslts.getObjects().isEmpty()) {
                    listobjcontent.addAll(rslts.getObjects());
                }
            }
        }

        for (ObjectContent oc : listobjcontent) {
            List<DynamicProperty> dps = oc.getPropSet();

            if (dps != null) {
                for (DynamicProperty dp : dps) {
                    retVal.put(dp.getName(), dp.getVal());
                }
            }
        }

        return retVal;
    }

    public static ManagedObjectReference reconfigureVm(VMwareConnection connection, ManagedObjectReference morVm,
            VirtualMachineConfigSpec vmcs) throws Exception {
        return connection.getVimPortType().reconfigVMTask(morVm, vmcs);
    }

    public static VirtualMachineDiskInfoBuilder getDiskInfoBuilder(List<VirtualDevice> devices) throws Exception {
        VirtualMachineDiskInfoBuilder builder = new VirtualMachineDiskInfoBuilder();

        if (devices != null) {
            for (VirtualDevice device : devices) {
                if (device instanceof VirtualDisk) {
                    VirtualDisk virtualDisk = (VirtualDisk)device;
                    VirtualDeviceBackingInfo backingInfo = virtualDisk.getBacking();

                    if (backingInfo instanceof VirtualDiskFlatVer2BackingInfo) {
                        VirtualDiskFlatVer2BackingInfo diskBackingInfo = (VirtualDiskFlatVer2BackingInfo)backingInfo;

                        String deviceBusName = VMwareUtil.getDeviceBusName(devices, virtualDisk);

                        while (diskBackingInfo != null) {
                            builder.addDisk(deviceBusName, diskBackingInfo.getFileName());

                            diskBackingInfo = diskBackingInfo.getParent();
                        }
                    }
                }
            }
        }

        return builder;
    }

    public static String getDeviceBusName(List<VirtualDevice> allDevices, VirtualDisk disk) throws Exception {
        for (VirtualDevice device : allDevices) {
            if (device.getKey() == disk.getControllerKey()) {
                if (device instanceof VirtualIDEController) {
                    return String.format("ide%d:%d", ((VirtualIDEController)device).getBusNumber(), disk.getUnitNumber());
                } else if (device instanceof VirtualSCSIController) {
                    return String.format("scsi%d:%d", ((VirtualSCSIController)device).getBusNumber(), disk.getUnitNumber());
                } else {
                    throw new Exception("The device controller is not supported.");
                }
            }
        }

        throw new Exception("The device controller could not be located.");
    }

    public static boolean waitForTask(VMwareConnection connection, ManagedObjectReference task) throws Exception {
        try {
            Object[] result = waitForValues(connection, task, new String[] { "info.state", "info.error" }, new String[] { "state" },
                    new Object[][] { new Object[] { TaskInfoState.SUCCESS, TaskInfoState.ERROR } });

            if (result[0].equals(TaskInfoState.SUCCESS)) {
                return true;
            }

            if (result[1] instanceof LocalizedMethodFault) {
                throw new Exception(((LocalizedMethodFault)result[1]).getLocalizedMessage());
            }
        } catch (WebServiceException we) {
            LOGGER.debug("Cancelling vCenter task because the task failed with the following error: " + we.getLocalizedMessage());

            connection.getVimPortType().cancelTask(task);

            throw new Exception("The vCenter task failed due to the following error: " + we.getLocalizedMessage());
        }

        return false;
    }

    private static Object[] waitForValues(VMwareConnection connection, ManagedObjectReference morObj, String[] filterProps,
            String[] endWaitProps, Object[][] expectedVals) throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg,
            InvalidCollectorVersionFaultMsg {
        String version = "";
        Object[] endVals = new Object[endWaitProps.length];
        Object[] filterVals = new Object[filterProps.length];

        PropertyFilterSpec spec = new PropertyFilterSpec();

        ObjectSpec oSpec = new ObjectSpec();

        oSpec.setObj(morObj);
        oSpec.setSkip(Boolean.FALSE);

        spec.getObjectSet().add(oSpec);

        PropertySpec pSpec = new PropertySpec();

        pSpec.getPathSet().addAll(Arrays.asList(filterProps));
        pSpec.setType(morObj.getType());

        spec.getPropSet().add(pSpec);

        ManagedObjectReference propertyCollector = connection.getServiceContent().getPropertyCollector();
        ManagedObjectReference filterSpecRef = connection.getVimPortType().createFilter(propertyCollector, spec, true);

        boolean reached = false;

        UpdateSet updateSet;
        List<PropertyFilterUpdate> lstPropertyFilterUpdates;
        List<ObjectUpdate> lstObjectUpdates;
        List<PropertyChange> lstPropertyChanges;

        while (!reached) {
            updateSet = connection.getVimPortType().waitForUpdates(propertyCollector, version);

            if (updateSet == null || updateSet.getFilterSet() == null) {
                continue;
            }

            version = updateSet.getVersion();

            lstPropertyFilterUpdates = updateSet.getFilterSet();

            for (PropertyFilterUpdate propertyFilterUpdate : lstPropertyFilterUpdates) {
                lstObjectUpdates = propertyFilterUpdate.getObjectSet();

                for (ObjectUpdate objUpdate : lstObjectUpdates) {
                    if (objUpdate.getKind() == ObjectUpdateKind.MODIFY || objUpdate.getKind() == ObjectUpdateKind.ENTER ||
                            objUpdate.getKind() == ObjectUpdateKind.LEAVE) {
                        lstPropertyChanges = objUpdate.getChangeSet();

                        for (PropertyChange propchg : lstPropertyChanges) {
                            updateValues(endWaitProps, endVals, propchg);
                            updateValues(filterProps, filterVals, propchg);
                        }
                    }
                }
            }

            Object expectedValue;

            // Check if the expected values have been reached and exit the loop if done.
            // Also, exit the WaitForUpdates loop if this is the case.
            for (int chgi = 0; chgi < endVals.length && !reached; chgi++) {
                for (int vali = 0; vali < expectedVals[chgi].length && !reached; vali++) {
                    expectedValue = expectedVals[chgi][vali];

                    reached = expectedValue.equals(endVals[chgi]) || reached;
                }
            }
        }

        // Destroy the filter when we are done.
        connection.getVimPortType().destroyPropertyFilter(filterSpecRef);

        return filterVals;
    }

    private static void updateValues(String[] props, Object[] vals, PropertyChange propertyChange) {
        for (int findi = 0; findi < props.length; findi++) {
            if (propertyChange.getName().lastIndexOf(props[findi]) >= 0) {
                if (propertyChange.getOp() == PropertyChangeOp.REMOVE) {
                    vals[findi] = "";
                } else {
                    vals[findi] = propertyChange.getVal();
                }
            }
        }
    }

    private static List<ObjectContent> retrievePropertiesAllObjects(List<PropertyFilterSpec> lstPfs,
            VimPortType vimPortType, ManagedObjectReference propCollectorRef) throws Exception {
        List<ObjectContent> lstObjectContent = new ArrayList<>();

        RetrieveOptions retrieveOptions = new RetrieveOptions();

        RetrieveResult rslts = vimPortType.retrievePropertiesEx(propCollectorRef, lstPfs, retrieveOptions);

        if (rslts != null && rslts.getObjects() != null && rslts.getObjects().size() > 0) {
            List<ObjectContent> lstOc = new ArrayList<>();

            for (ObjectContent oc : rslts.getObjects()) {
                lstOc.add(oc);
            }

            lstObjectContent.addAll(lstOc);
        }

        String token = null;

        if (rslts != null && rslts.getToken() != null) {
            token = rslts.getToken();
        }

        while (token != null && !token.isEmpty()) {
            rslts = vimPortType.continueRetrievePropertiesEx(propCollectorRef, token);
            token = null;

            if (rslts != null) {
                token = rslts.getToken();

                if (rslts.getObjects() != null && rslts.getObjects().size() > 0) {
                    List<ObjectContent> lstOc = new ArrayList<>();

                    for (ObjectContent oc : rslts.getObjects()) {
                        lstOc.add(oc);
                    }

                    lstObjectContent.addAll(lstOc);
                }
            }
        }

        return lstObjectContent;
    }

    private static TraversalSpec getVMTraversalSpec() {
        // Create a TraversalSpec that starts from the 'root' objects
        // and traverses the inventory tree to get to the VirtualMachines.
        // Build the traversal specs bottoms up

        // TraversalSpec to get to the VM in a vApp
        TraversalSpec vAppToVM = new TraversalSpec();

        vAppToVM.setName("vAppToVM");
        vAppToVM.setType("VirtualApp");
        vAppToVM.setPath("vm");

        // TraversalSpec for vApp to vApp
        TraversalSpec vAppToVApp = new TraversalSpec();

        vAppToVApp.setName("vAppToVApp");
        vAppToVApp.setType("VirtualApp");
        vAppToVApp.setPath("resourcePool");

        // SelectionSpec for vApp-to-vApp recursion
        SelectionSpec vAppRecursion = new SelectionSpec();

        vAppRecursion.setName("vAppToVApp");

        // SelectionSpec to get to a VM in the vApp
        SelectionSpec vmInVApp = new SelectionSpec();

        vmInVApp.setName("vAppToVM");

        // SelectionSpec for both vApp to vApp and vApp to VM
        List<SelectionSpec> vAppToVMSS = new ArrayList<>();

        vAppToVMSS.add(vAppRecursion);
        vAppToVMSS.add(vmInVApp);

        vAppToVApp.getSelectSet().addAll(vAppToVMSS);

        // This SelectionSpec is used for recursion for Folder recursion
        SelectionSpec sSpec = new SelectionSpec();

        sSpec.setName("VisitFolders");

        // Traversal to get to the vmFolder from DataCenter
        TraversalSpec dataCenterToVMFolder = new TraversalSpec();

        dataCenterToVMFolder.setName("DataCenterToVMFolder");
        dataCenterToVMFolder.setType("Datacenter");
        dataCenterToVMFolder.setPath("vmFolder");
        dataCenterToVMFolder.setSkip(false);

        dataCenterToVMFolder.getSelectSet().add(sSpec);

        // TraversalSpec to get to the DataCenter from rootFolder
        TraversalSpec traversalSpec = new TraversalSpec();

        traversalSpec.setName("VisitFolders");
        traversalSpec.setType("Folder");
        traversalSpec.setPath("childEntity");
        traversalSpec.setSkip(false);

        List<SelectionSpec> sSpecArr = new ArrayList<>();

        sSpecArr.add(sSpec);
        sSpecArr.add(dataCenterToVMFolder);
        sSpecArr.add(vAppToVM);
        sSpecArr.add(vAppToVApp);

        traversalSpec.getSelectSet().addAll(sSpecArr);

        return traversalSpec;
    }

    private static void trustAllHttpsCertificates() throws Exception {
        // Create a trust manager that does not validate certificate chains:
        TrustManager[] trustAllCerts = new TrustManager[1];

        TrustManager tm = new TrustAllTrustManager();

        trustAllCerts[0] = tm;

        SSLContext sc = SSLContext.getInstance("SSL");

        SSLSessionContext sslsc = sc.getServerSessionContext();

        sslsc.setSessionTimeout(0);

        sc.init(null, trustAllCerts, null);

        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
     }

    private static class TrustAllTrustManager implements TrustManager, X509TrustManager {
        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return null;
        }

        @Override
        public void checkServerTrusted(X509Certificate[] certs, String authType) throws CertificateException {
        }

        @Override
        public void checkClientTrusted(X509Certificate[] certs, String authType) throws CertificateException {
        }
    }
}
