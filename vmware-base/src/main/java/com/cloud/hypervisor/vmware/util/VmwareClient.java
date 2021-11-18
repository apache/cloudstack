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
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.Handler;
import javax.xml.ws.handler.HandlerResolver;
import javax.xml.ws.handler.PortInfo;


import org.apache.cloudstack.utils.security.SSLUtils;
import org.apache.cloudstack.utils.security.SecureSSLSocketFactory;
import com.vmware.pbm.PbmPortType;
import com.vmware.pbm.PbmService;
import com.vmware.pbm.PbmServiceInstanceContent;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.w3c.dom.Element;

import com.vmware.vim25.DynamicProperty;
import com.vmware.vim25.InvalidCollectorVersionFaultMsg;
import com.vmware.vim25.InvalidPropertyFaultMsg;
import com.vmware.vim25.LocalizedMethodFault;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.MethodFault;
import com.vmware.vim25.ObjectContent;
import com.vmware.vim25.ObjectSpec;
import com.vmware.vim25.ObjectUpdate;
import com.vmware.vim25.ObjectUpdateKind;
import com.vmware.vim25.PropertyChange;
import com.vmware.vim25.PropertyChangeOp;
import com.vmware.vim25.PropertyFilterSpec;
import com.vmware.vim25.PropertyFilterUpdate;
import com.vmware.vim25.PropertySpec;
import com.vmware.vim25.RequestCanceled;
import com.vmware.vim25.RuntimeFaultFaultMsg;
import com.vmware.vim25.SelectionSpec;
import com.vmware.vim25.ServiceContent;
import com.vmware.vim25.TaskInfo;
import com.vmware.vim25.TaskInfoState;
import com.vmware.vim25.TraversalSpec;
import com.vmware.vim25.UpdateSet;
import com.vmware.vim25.VimPortType;
import com.vmware.vim25.VimService;
import com.vmware.vim25.WaitOptions;

/**
 * A wrapper class to handle Vmware vsphere connection and disconnection.
 *
 * DISCLAIMER: This code is partly copied from sample codes that come along with Vmware web service 5.1 SDK.
 *
 */
public class VmwareClient {
    private static final Logger s_logger = Logger.getLogger(VmwareClient.class);

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

    static {
        try {
            trustAllHttpsCertificates();
            HostnameVerifier hv = new HostnameVerifier() {
                @Override
                public boolean verify(String urlHostName, SSLSession session) {
                    return true;
                }
            };
            HttpsURLConnection.setDefaultHostnameVerifier(hv);

            vimService = new VimService();
            pbmService = new PbmService();
        } catch (Exception e) {
            s_logger.info("[ignored]"
                    + "failed to trust all certificates blindly: ", e);
        }
    }

    private static void trustAllHttpsCertificates() throws Exception {
        // Create a trust manager that does not validate certificate chains:
        javax.net.ssl.TrustManager[] trustAllCerts = new javax.net.ssl.TrustManager[1];
        javax.net.ssl.TrustManager tm = new TrustAllTrustManager();
        trustAllCerts[0] = tm;
        javax.net.ssl.SSLContext sc = SSLUtils.getSSLContext();
        javax.net.ssl.SSLSessionContext sslsc = sc.getServerSessionContext();
        sslsc.setSessionTimeout(0);
        sc.init(null, trustAllCerts, null);
        javax.net.ssl.HttpsURLConnection.setDefaultSSLSocketFactory(new SecureSSLSocketFactory(sc));
    }

    private final ManagedObjectReference svcInstRef = new ManagedObjectReference();
    private final ManagedObjectReference pbmSvcInstRef = new ManagedObjectReference();

    private static VimService vimService;
    private static PbmService pbmService;
    private PbmServiceInstanceContent pbmServiceContent;
    private VimPortType vimPort;
    private PbmPortType pbmPort;
    private static final String PBM_SERVICE_INSTANCE_TYPE = "PbmServiceInstance";
    private static final String PBM_SERVICE_INSTANCE_VALUE = "ServiceInstance";

    private String serviceCookie;
    private final static String SVC_INST_NAME = "ServiceInstance";
    private int vCenterSessionTimeout = 1200000; // Timeout in milliseconds

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
        svcInstRef.setType(SVC_INST_NAME);
        svcInstRef.setValue(SVC_INST_NAME);

        vimPort = vimService.getVimPort();
        Map<String, Object> ctxt = ((BindingProvider)vimPort).getRequestContext();

        ctxt.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, url);
        ctxt.put(BindingProvider.SESSION_MAINTAIN_PROPERTY, true);

        ctxt.put("com.sun.xml.internal.ws.request.timeout", vCenterSessionTimeout);
        ctxt.put("com.sun.xml.internal.ws.connect.timeout", vCenterSessionTimeout);

        ServiceContent serviceContent = vimPort.retrieveServiceContent(svcInstRef);

        // Extract a cookie. See vmware sample program com.vmware.httpfileaccess.GetVMFiles
        @SuppressWarnings("unchecked")
        Map<String, List<String>> headers = (Map<String, List<String>>)((BindingProvider)vimPort).getResponseContext().get(MessageContext.HTTP_RESPONSE_HEADERS);
        List<String> cookies = headers.get("Set-cookie");

        vimPort.login(serviceContent.getSessionManager(), userName, password, null);

        if (cookies == null) {
            // Get the cookie from the response header. See vmware sample program com.vmware.httpfileaccess.GetVMFiles
            @SuppressWarnings("unchecked")
            Map<String, List<String>> responseHeaders = (Map<String, List<String>>)((BindingProvider)vimPort).getResponseContext().get(MessageContext.HTTP_RESPONSE_HEADERS);
            cookies = responseHeaders.get("Set-cookie");
            if (cookies == null) {
                String msg = "Login successful, but failed to get server cookies from url :[" + url + "]";
                s_logger.error(msg);
                throw new Exception(msg);
            }
        }

        String cookieValue = cookies.get(0);
        StringTokenizer tokenizer = new StringTokenizer(cookieValue, ";");
        cookieValue = tokenizer.nextToken();
        String pathData = "$" + tokenizer.nextToken();
        serviceCookie = "$Version=\"1\"; " + cookieValue + "; " + pathData;
        Map<String, List<String>> map = new HashMap<String, List<String>>();
        map.put("Cookie", Collections.singletonList(serviceCookie));
        ((BindingProvider)vimPort).getRequestContext().put(MessageContext.HTTP_REQUEST_HEADERS, map);
        pbmConnect(url, cookieValue);
        isConnected = true;
    }

    private void pbmConnect(String url, String cookieValue) throws Exception {
        URI uri = new URI(url);
        String pbmurl = "https://" + uri.getHost() + "/pbm";
        String[] tokens = cookieValue.split("=");
        String extractedCookie = tokens[1];

        HandlerResolver soapHandlerResolver = new HandlerResolver() {
            @Override
            public List<Handler> getHandlerChain(PortInfo portInfo) {
                VcenterSessionHandler VcSessionHandler = new VcenterSessionHandler(extractedCookie);
                List<Handler> handlerChain = new ArrayList<Handler>();
                handlerChain.add((Handler)VcSessionHandler);
                return handlerChain;
            }
        };
        pbmService.setHandlerResolver(soapHandlerResolver);

        pbmSvcInstRef.setType(PBM_SERVICE_INSTANCE_TYPE);
        pbmSvcInstRef.setValue(PBM_SERVICE_INSTANCE_VALUE);
        pbmPort = pbmService.getPbmPort();
        Map<String, Object> pbmCtxt = ((BindingProvider)pbmPort).getRequestContext();
        pbmCtxt.put(BindingProvider.SESSION_MAINTAIN_PROPERTY, true);
        pbmCtxt.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, pbmurl);
    }

    /**
     * Disconnects the user session.
     *
     * @throws Exception
     */
    public void disconnect() throws Exception {
        if (isConnected) {
            vimPort.logout(getServiceContent().getSessionManager());
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

        try {
            return vimPort.retrieveServiceContent(svcInstRef);
        } catch (RuntimeFaultFaultMsg e) {
        }
        return null;
    }

    /**
     * @return PBM service instance
     */
    public PbmPortType getPbmService() {
        return pbmPort;
    }

    /**
     * @return Service instance content
     */
    public PbmServiceInstanceContent getPbmServiceContent() {
        try {
            return pbmPort.pbmRetrieveServiceContent(pbmSvcInstRef);
        } catch (com.vmware.pbm.RuntimeFaultFaultMsg e) {
        }
        return null;
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
        return getServiceContent().getPropertyCollector();
    }

    /**
     * @return Root folder
     */
    public ManagedObjectReference getRootFolder() {
        return getServiceContent().getRootFolder();
    }

    public boolean validate() {
        //
        // There is no official API to validate an open vCenter API session. This is hacking way to tell if
        // an open vCenter API session is still valid for making calls.
        //
        // It will give false result if there really does not exist data-center in the inventory, however, I consider
        // this really is not possible in production deployment
        //

        // Create PropertySpecs
        PropertySpec pSpec = new PropertySpec();
        pSpec.setType("Datacenter");
        pSpec.setAll(false);
        pSpec.getPathSet().add("name");

        ObjectSpec oSpec = new ObjectSpec();
        oSpec.setObj(getRootFolder());
        oSpec.setSkip(false);
        oSpec.getSelectSet().addAll(constructCompleteTraversalSpec());

        PropertyFilterSpec spec = new PropertyFilterSpec();
        spec.getPropSet().add(pSpec);
        spec.getObjectSet().add(oSpec);
        List<PropertyFilterSpec> specArr = new ArrayList<PropertyFilterSpec>();
        specArr.add(spec);

        try {
            List<ObjectContent> ocary = vimPort.retrieveProperties(getPropCol(), specArr);
            if (ocary != null && ocary.size() > 0)
                return true;
        } catch (Exception e) {
            return false;
        }

        return false;
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
    @SuppressWarnings("unchecked")
    public <T> T getDynamicProperty(ManagedObjectReference mor, String propertyName) throws Exception {
        List<String> props = new ArrayList<String>();
        props.add(propertyName);
        List<ObjectContent> objContent = retrieveMoRefProperties(mor, props);

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
                    String methodName = "get" + dynamicPropertyName.substring(dynamicPropertyName.indexOf("ArrayOf") + "ArrayOf".length(), dynamicPropertyName.length());

                    Method getMorMethod = dpCls.getDeclaredMethod(methodName, null);
                    propertyValue = getMorMethod.invoke(propertyValue, (Object[])null);
                }
            }
        }
        return (T)propertyValue;
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

        return vimPort.retrieveProperties(getPropCol(), specArr);
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
    public boolean waitForTask(ManagedObjectReference task) throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg, InvalidCollectorVersionFaultMsg, Exception {

        boolean retVal = false;

        try {
            // info has a property - state for state of the task
            Object[] result = waitForValues(task, new String[] { "info.state", "info.error" }, new String[] { "state" }, new Object[][] { new Object[] {
                    TaskInfoState.SUCCESS, TaskInfoState.ERROR } });

            if (result != null && result.length == 2) { //result for 2 properties: info.state, info.error
                if (result[0].equals(TaskInfoState.SUCCESS)) {
                    retVal = true;
                }
                if (result[1] instanceof LocalizedMethodFault) {
                    throw new RuntimeException(((LocalizedMethodFault)result[1]).getLocalizedMessage());
                }
            }
        } catch (WebServiceException we) {
            s_logger.warn("Session to vCenter failed with: " + we.getLocalizedMessage());

            TaskInfo taskInfo = (TaskInfo)getDynamicProperty(task, "info");
            if (!taskInfo.isCancelable()) {
                s_logger.warn("vCenter task: " + taskInfo.getName() + "(" + taskInfo.getKey() + ")" + " will continue to run on vCenter because the task cannot be cancelled");
                throw new RuntimeException(we.getLocalizedMessage());
            }

            s_logger.debug("Cancelling vCenter task: " + taskInfo.getName() + "(" + taskInfo.getKey() + ")");
            getService().cancelTask(task);

            // Since task cancellation is asynchronous, wait for the task to be cancelled
            Object[] result = waitForValues(task, new String[] {"info.state", "info.error"}, new String[] {"state"},
                    new Object[][] {new Object[] {TaskInfoState.SUCCESS, TaskInfoState.ERROR}});

            if (result != null && result.length == 2) { //result for 2 properties: info.state, info.error
                if (result[0].equals(TaskInfoState.SUCCESS)) {
                    s_logger.warn("Failed to cancel vCenter task: " + taskInfo.getName() + "(" + taskInfo.getKey() + ")" + " and the task successfully completed");
                    retVal = true;
                }

                if (result[1] instanceof LocalizedMethodFault) {
                    MethodFault fault = ((LocalizedMethodFault)result[1]).getFault();
                    if (fault instanceof RequestCanceled) {
                        s_logger.debug("vCenter task " + taskInfo.getName() + "(" + taskInfo.getKey() + ")" + " was successfully cancelled");
                        throw new RuntimeException(we.getLocalizedMessage());
                    }
                } else {
                    throw new RuntimeException(((LocalizedMethodFault)result[1]).getLocalizedMessage());
                }
            }
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
    private synchronized Object[] waitForValues(ManagedObjectReference objmor, String[] filterProps, String[] endWaitProps, Object[][] expectedVals) throws InvalidPropertyFaultMsg,
    RuntimeFaultFaultMsg, InvalidCollectorVersionFaultMsg {
        // version string is initially null
        String version = "";
        Object[] endVals = new Object[endWaitProps.length];
        Object[] filterVals = new Object[filterProps.length];
        String stateVal = null;

        PropertyFilterSpec spec = new PropertyFilterSpec();
        ObjectSpec oSpec = new ObjectSpec();
        oSpec.setObj(objmor);
        oSpec.setSkip(Boolean.FALSE);
        spec.getObjectSet().add(oSpec);

        PropertySpec pSpec = new PropertySpec();
        pSpec.getPathSet().addAll(Arrays.asList(filterProps));
        pSpec.setType(objmor.getType());
        spec.getPropSet().add(pSpec);

        ManagedObjectReference propertyCollector = getPropCol();
        ManagedObjectReference filterSpecRef = vimPort.createFilter(propertyCollector, spec, true);

        boolean reached = false;

        UpdateSet updateset = null;
        List<PropertyFilterUpdate> filtupary = null;
        List<ObjectUpdate> objupary = null;
        List<PropertyChange> propchgary = null;
        while (!reached) {
            updateset = vimPort.waitForUpdatesEx(propertyCollector, version, new WaitOptions());
            if (updateset == null || updateset.getFilterSet() == null) {
                continue;
            }
            version = updateset.getVersion();

            // Make this code more general purpose when PropCol changes later.
            filtupary = updateset.getFilterSet();

            for (PropertyFilterUpdate filtup : filtupary) {
                objupary = filtup.getObjectSet();
                for (ObjectUpdate objup : objupary) {
                    if (objup.getKind() == ObjectUpdateKind.MODIFY || objup.getKind() == ObjectUpdateKind.ENTER || objup.getKind() == ObjectUpdateKind.LEAVE) {
                        propchgary = objup.getChangeSet();
                        for (PropertyChange propchg : propchgary) {
                            updateValues(endWaitProps, endVals, propchg);
                            updateValues(filterProps, filterVals, propchg);
                        }
                    }
                }
            }

            Object expctdval = null;
            // Check if the expected values have been reached and exit the loop if done.
            // Also exit the WaitForUpdates loop if this is the case.
            for (int chgi = 0; chgi < endVals.length && !reached; chgi++) {
                for (int vali = 0; vali < expectedVals[chgi].length && !reached; vali++) {
                    expctdval = expectedVals[chgi][vali];

                    if (endVals[chgi] == null) {
                        // Do nothing
                    } else if (endVals[chgi].toString().contains("val: null")) {
                        // Handle JAX-WS De-serialization issue, by parsing nodes
                        Element stateElement = (Element) endVals[chgi];
                        if (stateElement != null && stateElement.getFirstChild() != null) {
                            stateVal = stateElement.getFirstChild().getTextContent();
                            reached = expctdval.toString().equalsIgnoreCase(stateVal) || reached;
                        }
                    } else {
                        reached = expctdval.equals(endVals[chgi]) || reached;
                        stateVal = "filtervals";
                    }
                }
            }
        }

        // Destroy the filter when we are done.
        vimPort.destroyPropertyFilter(filterSpecRef);

        Object[] retVal = filterVals;
        if (stateVal != null && stateVal.equalsIgnoreCase("success")) {
            retVal = new Object[] { TaskInfoState.SUCCESS, null };
        }

        return retVal;
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

        try {
            // Create PropertySpecs
            PropertySpec pSpec = new PropertySpec();
            pSpec.setType(type);
            pSpec.setAll(false);
            pSpec.getPathSet().add("name");

            ObjectSpec oSpec = new ObjectSpec();
            oSpec.setObj(root);
            oSpec.setSkip(false);
            oSpec.getSelectSet().addAll(constructCompleteTraversalSpec());

            PropertyFilterSpec spec = new PropertyFilterSpec();
            spec.getPropSet().add(pSpec);
            spec.getObjectSet().add(oSpec);
            List<PropertyFilterSpec> specArr = new ArrayList<PropertyFilterSpec>();
            specArr.add(spec);

            ManagedObjectReference propCollector = getPropCol();
            List<ObjectContent> ocary = vimPort.retrieveProperties(propCollector, specArr);

            if (ocary == null || ocary.size() == 0) {
                return null;
            }

            // filter through retrieved objects to get the first match.
            for (ObjectContent oc : ocary) {
                ManagedObjectReference mor = oc.getObj();
                List<DynamicProperty> propary = oc.getPropSet();
                if (type == null || type.equals(mor.getType())) {
                    if (propary.size() > 0) {
                        String propval = (String)propary.get(0).getVal();
                        if (propval != null && name.equalsIgnoreCase(propval))
                            return mor;
                    }
                }
            }
        } catch (InvalidPropertyFaultMsg invalidPropertyException) {
            s_logger.debug("Failed to get Vmware ManagedObjectReference for name: " + name + " and type: " + type + " due to " + invalidPropertyException.getMessage());
            throw invalidPropertyException;
        } catch (RuntimeFaultFaultMsg runtimeFaultException) {
            s_logger.debug("Failed to get Vmware ManagedObjectReference for name: " + name + " and type: " + type + " due to " + runtimeFaultException.getMessage());
            throw runtimeFaultException;
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

    public void setVcenterSessionTimeout(int vCenterSessionTimeout) {
        this.vCenterSessionTimeout = vCenterSessionTimeout;
    }

    public int getVcenterSessionTimeout() {
        return vCenterSessionTimeout;
    }

    public void cancelTask(ManagedObjectReference task) throws Exception {
        TaskInfo info = (TaskInfo)(getDynamicProperty(task, "info"));
        if (info == null) {
            s_logger.warn("Unable to get the task info, so couldn't cancel the task");
            return;
        }

        String taskName = StringUtils.isNotBlank(info.getName()) ? info.getName() : "Unknown";
        taskName += "(" + info.getKey() + ")";

        String entityName = StringUtils.isNotBlank(info.getEntityName()) ? info.getEntityName() : "";

        if (info.getState().equals(TaskInfoState.SUCCESS)) {
            s_logger.debug(taskName + " task successfully completed for the entity " + entityName + ", can't cancel it");
            return;
        }

        if (info.getState().equals(TaskInfoState.ERROR)) {
            s_logger.debug(taskName + " task execution failed for the entity " + entityName + ", can't cancel it");
            return;
        }

        s_logger.debug(taskName + " task pending for the entity " + entityName + ", trying to cancel");
        if (!info.isCancelable()) {
            s_logger.warn(taskName + " task will continue to run on vCenter because it can't be cancelled");
            return;
        }

        s_logger.debug("Cancelling task " + taskName + " of the entity " + entityName);
        getService().cancelTask(task);

        // Since task cancellation is asynchronous, wait for the task to be cancelled
        Object[] result = waitForValues(task, new String[] {"info.state", "info.error"}, new String[] {"state"},
                new Object[][] {new Object[] {TaskInfoState.SUCCESS, TaskInfoState.ERROR}});

        if (result != null && result.length == 2) { //result for 2 properties: info.state, info.error
            if (result[0].equals(TaskInfoState.SUCCESS)) {
                s_logger.warn("Failed to cancel" + taskName + " task of the entity " + entityName + ", the task successfully completed");
            }

            if (result[1] instanceof LocalizedMethodFault) {
                MethodFault fault = ((LocalizedMethodFault)result[1]).getFault();
                if (fault instanceof RequestCanceled) {
                    s_logger.debug(taskName + " task of the entity " + entityName + " was successfully cancelled");
                }
            } else {
                s_logger.warn("Couldn't cancel " + taskName + " task of the entity " + entityName + " due to " + ((LocalizedMethodFault)result[1]).getLocalizedMessage());
            }
        }
    }
}
