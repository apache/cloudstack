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
package com.cloud.hypervisor.vmware.util;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;
import javax.net.ssl.HttpsURLConnection;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.handler.MessageContext;


import com.vmware.vim25.DynamicProperty;
import com.vmware.vim25.InvalidCollectorVersionFaultMsg;
import com.vmware.vim25.InvalidPropertyFaultMsg;
import com.vmware.vim25.LocalizedMethodFault;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.ObjectSpec;
import com.vmware.vim25.ObjectUpdate;
import com.vmware.vim25.ObjectUpdateKind;
import com.vmware.vim25.PropertyChange;
import com.vmware.vim25.PropertyChangeOp;
import com.vmware.vim25.PropertyFilterSpec;
import com.vmware.vim25.PropertyFilterUpdate;
import com.vmware.vim25.PropertySpec;
import com.vmware.vim25.RuntimeFaultFaultMsg;
import com.vmware.vim25.SelectionSpec;
import com.vmware.vim25.ServiceContent;
import com.vmware.vim25.TaskInfoState;
import com.vmware.vim25.TraversalSpec;
import com.vmware.vim25.UpdateSet;
import com.vmware.vim25.VimPortType;
import com.vmware.vim25.VimService;
import com.vmware.vim25.ObjectContent;

/**
 * A wrapper class to handle Vmware vsphere connection and disconnection.
 *
 * DISCLAIMER: This code is partly copied from sample codes that come along with Vmware web service 5.1 SDK.
 *
 */
public class VmwareClient {

    private static class TrustAllTrustManager implements javax.net.ssl.TrustManager, javax.net.ssl.X509TrustManager {

        @Override
        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
            return null;
        }

        @Override
        public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) throws java.security.cert.CertificateException {
            return;
        }

        @Override
        public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) throws java.security.cert.CertificateException {
            return;
        }
    }

    private static void trustAllHttpsCertificates() throws Exception {
        // Create a trust manager that does not validate certificate chains:
        javax.net.ssl.TrustManager[] trustAllCerts = new javax.net.ssl.TrustManager[1];
        javax.net.ssl.TrustManager tm = new TrustAllTrustManager();
        trustAllCerts[0] = tm;
        javax.net.ssl.SSLContext sc = javax.net.ssl.SSLContext.getInstance("SSL");
        javax.net.ssl.SSLSessionContext sslsc = sc.getServerSessionContext();
        sslsc.setSessionTimeout(0);
        sc.init(null, trustAllCerts, null);
        javax.net.ssl.HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
    }

    private ManagedObjectReference SVC_INST_REF = new ManagedObjectReference();
    private ManagedObjectReference propCollectorRef;
    private ManagedObjectReference rootRef;
    private VimService vimService;
    private VimPortType vimPort;
    private ServiceContent serviceContent;
    private String serviceCookie;
    private final String SVC_INST_NAME = "ServiceInstance";

    private boolean isConnected = false;

    public VmwareClient(String name) {

    }

    /**
     * Establishes session with the virtual center server.
     *
     * @throws Exception
     *             the exception
     */
    public void connect(String url, String userName, String password) throws Exception {

        HostnameVerifier hv = new HostnameVerifier() {
            @Override
            public boolean verify(String urlHostName, SSLSession session) {
                return true;
            }
        };
        trustAllHttpsCertificates();
        HttpsURLConnection.setDefaultHostnameVerifier(hv);

        SVC_INST_REF.setType(SVC_INST_NAME);
        SVC_INST_REF.setValue(SVC_INST_NAME);

        vimService = new VimService();
        vimPort = vimService.getVimPort();
        Map<String, Object> ctxt = ((BindingProvider) vimPort).getRequestContext();

        ctxt.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, url);
        ctxt.put(BindingProvider.SESSION_MAINTAIN_PROPERTY, true);

        ctxt.put("com.sun.xml.internal.ws.request.timeout", 600000);
        ctxt.put("com.sun.xml.internal.ws.connect.timeout", 600000);

        serviceContent = vimPort.retrieveServiceContent(SVC_INST_REF);

        // Extract a cookie. See vmware sample program com.vmware.httpfileaccess.GetVMFiles
        Map<String, List<String>> headers = (Map<String, List<String>>) ((BindingProvider) vimPort)
                .getResponseContext().get(MessageContext.HTTP_RESPONSE_HEADERS);
        List<String> cookies = (List<String>) headers.get("Set-cookie");
        String cookieValue = cookies.get(0);
        StringTokenizer tokenizer = new StringTokenizer(cookieValue, ";");
        cookieValue = tokenizer.nextToken();
        String pathData = "$" + tokenizer.nextToken();
        serviceCookie = "$Version=\"1\"; " + cookieValue + "; " + pathData;

        vimPort.login(serviceContent.getSessionManager(), userName, password, null);
        isConnected = true;

        propCollectorRef = serviceContent.getPropertyCollector();
        rootRef = serviceContent.getRootFolder();
    }

    /**
     * Disconnects the user session.
     *
     * @throws Exception
     */
    public void disconnect() throws Exception {
        if (isConnected) {
            vimPort.logout(serviceContent.getSessionManager());
        }
        isConnected = false;
    }

    /**
     * @return Service instance
     */
    public VimPortType getService() {
        return vimPort;
    }

    /**
     * @return Service instance content
     */
    public ServiceContent getServiceContent() {
        return serviceContent;
    }

    /**
     * @return cookie used in service connection
     */
    public String getServiceCookie() {
        return serviceCookie;
    }

    /**
     * @return Service property collector
     */
    public ManagedObjectReference getPropCol() {
        return propCollectorRef;
    }

    /**
     * @return Root folder
     */
    public ManagedObjectReference getRootFolder() {
        return rootRef;
    }

    /**
     * Get the property value of a managed object.
     *
     * @param mor
     *            managed object reference
     * @param propertyName
     *            property name.
     * @return property value.
     * @throws Exception
     *             in case of error.
     */
    public Object getDynamicProperty(ManagedObjectReference mor, String propertyName) throws Exception {
        List<String> props = new ArrayList<String>();
        props.add(propertyName);
        List<ObjectContent> objContent = this.retrieveMoRefProperties(mor, props);

        Object propertyValue = null;
        if (objContent != null && objContent.size() > 0) {
            List<DynamicProperty> dynamicProperty = objContent.get(0).getPropSet();
            if (dynamicProperty != null && dynamicProperty.size() > 0) {
                DynamicProperty dp = dynamicProperty.get(0);
                propertyValue = dp.getVal();
                /*
                 * If object is ArrayOfXXX object, then get the XXX[] by
                 * invoking getXXX() on the object.
                 * For Ex:
                 * ArrayOfManagedObjectReference.getManagedObjectReference()
                 * returns ManagedObjectReference[] array.
                 */
                Class dpCls = propertyValue.getClass();
                String dynamicPropertyName = dpCls.getName();
                if (dynamicPropertyName.indexOf("ArrayOf") != -1) {
                    String methodName = "get"
                            + dynamicPropertyName
                                    .substring(dynamicPropertyName.indexOf("ArrayOf") + "ArrayOf".length(), dynamicPropertyName.length());

                    Method getMorMethod = dpCls.getDeclaredMethod(methodName, null);
                    propertyValue = getMorMethod.invoke(propertyValue, (Object[]) null);
                }
            }
        }
        return propertyValue;
    }

    private List<ObjectContent> retrieveMoRefProperties(ManagedObjectReference mObj, List<String> props) throws Exception {
        PropertySpec pSpec = new PropertySpec();
        pSpec.setAll(false);
        pSpec.setType(mObj.getType());
        pSpec.getPathSet().addAll(props);

        ObjectSpec oSpec = new ObjectSpec();
        oSpec.setObj(mObj);
        oSpec.setSkip(false);
        PropertyFilterSpec spec = new PropertyFilterSpec();
        spec.getPropSet().add(pSpec);
        spec.getObjectSet().add(oSpec);
        List<PropertyFilterSpec> specArr = new ArrayList<PropertyFilterSpec>();
        specArr.add(spec);

        return vimPort.retrieveProperties(propCollectorRef, specArr);
    }

    /**
     * This method returns a boolean value specifying whether the Task is
     * succeeded or failed.
     *
     * @param task
     *            ManagedObjectReference representing the Task.
     *
     * @return boolean value representing the Task result.
     * @throws InvalidCollectorVersionFaultMsg
     * @throws RuntimeFaultFaultMsg
     * @throws InvalidPropertyFaultMsg
     */
    public boolean waitForTask(ManagedObjectReference task) throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg, InvalidCollectorVersionFaultMsg {

        boolean retVal = false;

        // info has a property - state for state of the task
        Object[] result = waitForValues(task, new String[] { "info.state", "info.error" }, new String[] { "state" }, new Object[][] { new Object[] {
                TaskInfoState.SUCCESS, TaskInfoState.ERROR } });

        if (result[0].equals(TaskInfoState.SUCCESS)) {
            retVal = true;
        }
        if (result[1] instanceof LocalizedMethodFault) {
            throw new RuntimeException(((LocalizedMethodFault) result[1]).getLocalizedMessage());
        }
        return retVal;
    }

    /**
     * Handle Updates for a single object. waits till expected values of
     * properties to check are reached Destroys the ObjectFilter when done.
     *
     * @param objmor
     *            MOR of the Object to wait for
     * @param filterProps
     *            Properties list to filter
     * @param endWaitProps
     *            Properties list to check for expected values these be
     *            properties of a property in the filter properties list
     * @param expectedVals
     *            values for properties to end the wait
     * @return true indicating expected values were met, and false otherwise
     * @throws RuntimeFaultFaultMsg
     * @throws InvalidPropertyFaultMsg
     * @throws InvalidCollectorVersionFaultMsg
     */
    private Object[] waitForValues(ManagedObjectReference objmor, String[] filterProps, String[] endWaitProps, Object[][] expectedVals)
            throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg, InvalidCollectorVersionFaultMsg {
        // version string is initially null
        String version = "";
        Object[] endVals = new Object[endWaitProps.length];
        Object[] filterVals = new Object[filterProps.length];

        PropertyFilterSpec spec = new PropertyFilterSpec();
        ObjectSpec oSpec = new ObjectSpec();
        oSpec.setObj(objmor);
        oSpec.setSkip(Boolean.FALSE);
        spec.getObjectSet().add(oSpec);

        PropertySpec pSpec = new PropertySpec();
        pSpec.getPathSet().addAll(Arrays.asList(filterProps));
        pSpec.setType(objmor.getType());
        spec.getPropSet().add(pSpec);

        ManagedObjectReference filterSpecRef = vimPort.createFilter(propCollectorRef, spec, true);

        boolean reached = false;

        UpdateSet updateset = null;
        List<PropertyFilterUpdate> filtupary = null;
        List<ObjectUpdate> objupary = null;
        List<PropertyChange> propchgary = null;
        while (!reached) {
            updateset = vimPort.waitForUpdates(propCollectorRef, version);
            if (updateset == null || updateset.getFilterSet() == null) {
                continue;
            }
            version = updateset.getVersion();

            // Make this code more general purpose when PropCol changes later.
            filtupary = updateset.getFilterSet();

            for (PropertyFilterUpdate filtup : filtupary) {
                objupary = filtup.getObjectSet();
                for (ObjectUpdate objup : objupary) {
                    // TODO: Handle all "kind"s of updates.
                    if (objup.getKind() == ObjectUpdateKind.MODIFY || objup.getKind() == ObjectUpdateKind.ENTER
                            || objup.getKind() == ObjectUpdateKind.LEAVE) {
                        propchgary = objup.getChangeSet();
                        for (PropertyChange propchg : propchgary) {
                            updateValues(endWaitProps, endVals, propchg);
                            updateValues(filterProps, filterVals, propchg);
                        }
                    }
                }
            }

            Object expctdval = null;
            // Check if the expected values have been reached and exit the loop
            // if done.
            // Also exit the WaitForUpdates loop if this is the case.
            for (int chgi = 0; chgi < endVals.length && !reached; chgi++) {
                for (int vali = 0; vali < expectedVals[chgi].length && !reached; vali++) {
                    expctdval = expectedVals[chgi][vali];

                    reached = expctdval.equals(endVals[chgi]) || reached;
                }
            }
        }

        // Destroy the filter when we are done.
        vimPort.destroyPropertyFilter(filterSpecRef);
        return filterVals;
    }

    private void updateValues(String[] props, Object[] vals, PropertyChange propchg) {
        for (int findi = 0; findi < props.length; findi++) {
            if (propchg.getName().lastIndexOf(props[findi]) >= 0) {
                if (propchg.getOp() == PropertyChangeOp.REMOVE) {
                    vals[findi] = "";
                } else {
                    vals[findi] = propchg.getVal();
                }
            }
        }
    }

    private SelectionSpec getSelectionSpec(String name) {
        SelectionSpec genericSpec = new SelectionSpec();
        genericSpec.setName(name);
        return genericSpec;
     }

    /*
     * @return An array of SelectionSpec covering VM, Host, Resource pool,
     * Cluster Compute Resource and Datastore.
     */
    private List<SelectionSpec> constructCompleteTraversalSpec() {
       // ResourcePools to VM: RP -> VM
       TraversalSpec rpToVm = new TraversalSpec();
       rpToVm.setName("rpToVm");
       rpToVm.setType("ResourcePool");
       rpToVm.setPath("vm");
       rpToVm.setSkip(Boolean.FALSE);

       // VirtualApp to VM: vApp -> VM
       TraversalSpec vAppToVM = new TraversalSpec();
       vAppToVM.setName("vAppToVM");
       vAppToVM.setType("VirtualApp");
       vAppToVM.setPath("vm");

       // Host to VM: HostSystem -> VM
       TraversalSpec hToVm = new TraversalSpec();
       hToVm.setType("HostSystem");
       hToVm.setPath("vm");
       hToVm.setName("hToVm");
       hToVm.getSelectSet().add(getSelectionSpec("VisitFolders"));
       hToVm.setSkip(Boolean.FALSE);

       // DataCenter to DataStore: DC -> DS
       TraversalSpec dcToDs = new TraversalSpec();
       dcToDs.setType("Datacenter");
       dcToDs.setPath("datastore");
       dcToDs.setName("dcToDs");
       dcToDs.setSkip(Boolean.FALSE);

       // Recurse through all ResourcePools
       TraversalSpec rpToRp = new TraversalSpec();
       rpToRp.setType("ResourcePool");
       rpToRp.setPath("resourcePool");
       rpToRp.setSkip(Boolean.FALSE);
       rpToRp.setName("rpToRp");
       rpToRp.getSelectSet().add(getSelectionSpec("rpToRp"));

       TraversalSpec crToRp = new TraversalSpec();
       crToRp.setType("ComputeResource");
       crToRp.setPath("resourcePool");
       crToRp.setSkip(Boolean.FALSE);
       crToRp.setName("crToRp");
       crToRp.getSelectSet().add(getSelectionSpec("rpToRp"));

       TraversalSpec crToH = new TraversalSpec();
       crToH.setSkip(Boolean.FALSE);
       crToH.setType("ComputeResource");
       crToH.setPath("host");
       crToH.setName("crToH");

       TraversalSpec dcToHf = new TraversalSpec();
       dcToHf.setSkip(Boolean.FALSE);
       dcToHf.setType("Datacenter");
       dcToHf.setPath("hostFolder");
       dcToHf.setName("dcToHf");
       dcToHf.getSelectSet().add(getSelectionSpec("VisitFolders"));

       TraversalSpec vAppToRp = new TraversalSpec();
       vAppToRp.setName("vAppToRp");
       vAppToRp.setType("VirtualApp");
       vAppToRp.setPath("resourcePool");
       vAppToRp.getSelectSet().add(getSelectionSpec("rpToRp"));

       TraversalSpec dcToVmf = new TraversalSpec();
       dcToVmf.setType("Datacenter");
       dcToVmf.setSkip(Boolean.FALSE);
       dcToVmf.setPath("vmFolder");
       dcToVmf.setName("dcToVmf");
       dcToVmf.getSelectSet().add(getSelectionSpec("VisitFolders"));

       // For Folder -> Folder recursion
       TraversalSpec visitFolders = new TraversalSpec();
       visitFolders.setType("Folder");
       visitFolders.setPath("childEntity");
       visitFolders.setSkip(Boolean.FALSE);
       visitFolders.setName("VisitFolders");
       List<SelectionSpec> sspecarrvf = new ArrayList<SelectionSpec>();
       sspecarrvf.add(getSelectionSpec("crToRp"));
       sspecarrvf.add(getSelectionSpec("crToH"));
       sspecarrvf.add(getSelectionSpec("dcToVmf"));
       sspecarrvf.add(getSelectionSpec("dcToHf"));
       sspecarrvf.add(getSelectionSpec("vAppToRp"));
       sspecarrvf.add(getSelectionSpec("vAppToVM"));
       sspecarrvf.add(getSelectionSpec("dcToDs"));
       sspecarrvf.add(getSelectionSpec("hToVm"));
       sspecarrvf.add(getSelectionSpec("rpToVm"));
       sspecarrvf.add(getSelectionSpec("VisitFolders"));

       visitFolders.getSelectSet().addAll(sspecarrvf);

       List<SelectionSpec> resultspec = new ArrayList<SelectionSpec>();
       resultspec.add(visitFolders);
       resultspec.add(crToRp);
       resultspec.add(crToH);
       resultspec.add(dcToVmf);
       resultspec.add(dcToHf);
       resultspec.add(vAppToRp);
       resultspec.add(vAppToVM);
       resultspec.add(dcToDs);
       resultspec.add(hToVm);
       resultspec.add(rpToVm);
       resultspec.add(rpToRp);

       return resultspec;
    }


    /**
     * Get the ManagedObjectReference for an item under the
     * specified root folder that has the type and name specified.
     *
     * @param root a root folder if available, or null for default
     * @param type type of the managed object
     * @param name name to match
     *
     * @return First ManagedObjectReference of the type / name pair found
     */
    public ManagedObjectReference getDecendentMoRef(ManagedObjectReference root, String type, String name) throws Exception {
        if (name == null || name.length() == 0) {
            return null;
        }

        // Create PropertySpecs
        PropertySpec pSpec = new PropertySpec();
        pSpec.setType(type);
        pSpec.setAll(false);
        pSpec.getPathSet().add(name);

        ObjectSpec oSpec = new ObjectSpec();
        oSpec.setObj(root);
        oSpec.setSkip(false);
        oSpec.getSelectSet().addAll(constructCompleteTraversalSpec());

        PropertyFilterSpec spec = new PropertyFilterSpec();
        spec.getPropSet().add(pSpec);
        spec.getObjectSet().add(oSpec);
        List<PropertyFilterSpec> specArr = new ArrayList<PropertyFilterSpec>();
        specArr.add(spec);

        List<ObjectContent> ocary = vimPort.retrieveProperties(propCollectorRef, specArr);

        if (ocary == null || ocary.size() == 0) {
            return null;
        }

        // filter through retrieved objects to get the first match.
        for (ObjectContent oc : ocary) {
            ManagedObjectReference mor = oc.getObj();
            List<DynamicProperty> propary = oc.getPropSet();
            if (type == null || type.equals(mor.getType())) {
                if (propary.size() > 0) {
                    String propval = (String) propary.get(0).getVal();
                    if (propval != null && name.equals(propval))
                        return mor;
                }
            }
        }
        return null;
    }

    /**
     * Get a MORef from the property returned.
     *
     * @param objMor Object to get a reference property from
     * @param propName name of the property that is the MORef
     * @return the ManagedObjectReference for that property.
     */
    public ManagedObjectReference getMoRefProp(ManagedObjectReference objMor, String propName) throws Exception {
       Object props = getDynamicProperty(objMor, propName);
       ManagedObjectReference propmor = null;
       if (!props.getClass().isArray()) {
          propmor = (ManagedObjectReference)props;
       }
       return propmor;
    }
}
