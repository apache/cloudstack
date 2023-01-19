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
package com.cloud.network.brocade;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import com.cloud.network.schema.interfacevlan.InterfaceVlan;
import com.cloud.network.schema.interfacevlan.Interface;
import com.cloud.network.schema.interfacevlan.Vlan;
import com.cloud.network.schema.portprofile.PortProfile;
import com.cloud.network.schema.portprofile.PortProfile.Activate;
import com.cloud.network.schema.portprofile.PortProfile.Static;
import com.cloud.network.schema.portprofile.PortProfileGlobal;
import com.cloud.network.schema.portprofile.VlanProfile;
import com.cloud.network.schema.portprofile.VlanProfile.Switchport;
import com.cloud.network.schema.portprofile.VlanProfile.Switchport.Mode;
import com.cloud.network.schema.portprofile.VlanProfile.Switchport.Trunk;
import com.cloud.network.schema.portprofile.VlanProfile.Switchport.Trunk.Allowed;
import com.cloud.network.schema.portprofile.VlanProfile.SwitchportBasic;
import com.cloud.network.schema.portprofile.VlanProfile.SwitchportBasic.Basic;
import com.cloud.network.schema.showvcs.Output;

public class BrocadeVcsApi {
    protected Logger logger = LogManager.getLogger(getClass());

    private final String _host;
    private final String _adminuser;
    private final String _adminpass;

    protected DefaultHttpClient _client;

    protected HttpRequestBase createMethod(String type, String uri) throws BrocadeVcsApiException {
        String url;
        try {
            url = new URL(Constants.PROTOCOL, _host, Constants.PORT, uri).toString();
        } catch (final MalformedURLException e) {
            logger.error("Unable to build Brocade Switch API URL", e);
            throw new BrocadeVcsApiException("Unable to build Brocade Switch API URL", e);
        }

        if ("post".equalsIgnoreCase(type)) {
            return new HttpPost(url);
        } else if ("get".equalsIgnoreCase(type)) {
            return new HttpGet(url);
        } else if ("delete".equalsIgnoreCase(type)) {
            return new HttpDelete(url);
        } else if ("patch".equalsIgnoreCase(type)) {
            return new HttpPatch(url);
        } else {
            throw new BrocadeVcsApiException("Requesting unknown method type");
        }
    }

    public BrocadeVcsApi(String address, String username, String password) {
        _host = address;
        _adminuser = username;
        _adminpass = password;
        _client = new DefaultHttpClient();
        _client.getCredentialsProvider().setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(_adminuser, _adminpass));

    }

    /*
     * Get Operational Status
     */
    public Output getSwitchStatus() throws BrocadeVcsApiException {
        return executeRetreiveStatus(Constants.STATUS_URI);

    }

    /*
     * Creates a new virtual network.
     */
    public boolean createNetwork(int vlanId, long networkId) throws BrocadeVcsApiException {

        if (createInterfaceVlan(vlanId)) {

            final PortProfile portProfile = createPortProfile(vlanId, networkId);

            if (portProfile != null) {
                return activatePortProfile(portProfile);
            }
        }
        return false;
    }

    /*
     * Activates a port-profile.
     */
    private boolean activatePortProfile(PortProfile portProfile) throws BrocadeVcsApiException {
        final PortProfileGlobal portProfileGlobal = new PortProfileGlobal();
        portProfile.setVlanProfile(null);
        final Activate activate = new Activate();
        portProfile.setActivate(activate);
        portProfileGlobal.setPortProfile(portProfile);

        //activate port-profile
        return executeUpdateObject(portProfileGlobal, Constants.URI);
    }

    /*
     *  Creates AMPP port-profile.
     */
    private PortProfile createPortProfile(int vlanId, long networkId) throws BrocadeVcsApiException {

        final PortProfile portProfile = new PortProfile();
        portProfile.setName(Constants.PORT_PROFILE_NAME_PREFIX + networkId);
        if (executeCreateObject(portProfile, Constants.URI)) {
            if (createVlanSubProfile(vlanId, portProfile)) {
                return portProfile;
            }
        }
        return null;
    }

    /*
     * Create vlan sub-profile for port-profile
     */
    private boolean createVlanSubProfile(int vlanId, PortProfile portProfile) throws BrocadeVcsApiException {
        final VlanProfile vlanProfile = new VlanProfile();
        portProfile.setVlanProfile(vlanProfile);
        if (executeUpdateObject(portProfile, Constants.URI)) {
            return configureVlanSubProfile(vlanId, portProfile);
        }
        return false;
    }

    /*
     * Configures vlan sub-profile for port-profile.
     * - configure L2 mode for vlan sub-profile
     * - configure trunk mode for vlan sub-profile
     * - configure allowed VLANs for vlan sub-profile
     */
    private boolean configureVlanSubProfile(int vlanId, PortProfile portProfile) throws BrocadeVcsApiException {
        final SwitchportBasic switchPortBasic = new SwitchportBasic();
        final Basic basic = new Basic();
        switchPortBasic.setBasic(basic);
        portProfile.getVlanProfile().setSwitchportBasic(switchPortBasic);
        // configure L2 mode for vlan sub-profile
        if (executeUpdateObject(portProfile, Constants.URI)) {
            VlanProfile vlanProfile = new VlanProfile();
            Switchport switchPort = new Switchport();
            final Mode mode = new Mode();
            mode.setVlanMode("trunk");
            switchPort.setMode(mode);
            vlanProfile.setSwitchport(switchPort);
            portProfile.setVlanProfile(vlanProfile);

            // configure trunk mode for vlan sub-profile
            if (executeUpdateObject(portProfile, Constants.URI)) {
                vlanProfile = new VlanProfile();
                switchPort = new Switchport();
                final Trunk trunk = new Trunk();
                final Allowed allowed = new Allowed();
                final Allowed.Vlan allowedVlan = new Allowed.Vlan();
                allowedVlan.setAdd(vlanId);
                allowed.setVlan(allowedVlan);
                trunk.setAllowed(allowed);
                switchPort.setTrunk(trunk);
                vlanProfile.setSwitchport(switchPort);
                portProfile.setVlanProfile(vlanProfile);

                //configure allowed VLANs for vlan sub-profile
                return executeUpdateObject(portProfile, Constants.URI);
            }
        }

        return false;

    }

    /*
     * Creates a vlan interface.
     */
    private boolean createInterfaceVlan(int vlanId) throws BrocadeVcsApiException {
        final InterfaceVlan interfaceVlan = new InterfaceVlan();
        final Interface interfaceObj = new Interface();
        final Vlan vlan = new Vlan();
        vlan.setName(vlanId);
        interfaceObj.setVlan(vlan);
        interfaceVlan.setInterface(interfaceObj);

        return executeUpdateObject(interfaceVlan, Constants.URI);

    }

    /*
     * Associates a MAC address to virtual network.
     */
    public boolean associateMacToNetwork(long networkId, String macAddress) throws BrocadeVcsApiException {

        final PortProfileGlobal portProfileGlobal = new PortProfileGlobal();
        final PortProfile portProfile = new PortProfile();
        portProfile.setName(Constants.PORT_PROFILE_NAME_PREFIX + networkId);
        final Static staticObj = new Static();
        staticObj.setMacAddress(macAddress);
        portProfile.setStatic(staticObj);
        portProfileGlobal.setPortProfile(portProfile);

        //associates a mac address to a port-profile
        return executeUpdateObject(portProfileGlobal, Constants.URI);
    }

    /*
     * Disassociates a MAC address from virtual network.
     */
    public boolean disassociateMacFromNetwork(long networkId, String macAddress) throws BrocadeVcsApiException {

        final PortProfileGlobal portProfileGlobal = new PortProfileGlobal();
        final PortProfile portProfile = new PortProfile();
        portProfile.setName(Constants.PORT_PROFILE_NAME_PREFIX + networkId);
        final Static staticObj = new Static();
        staticObj.setOperation("delete");
        staticObj.setMacAddress(macAddress);
        portProfile.setStatic(staticObj);
        portProfileGlobal.setPortProfile(portProfile);

        //associates a mac address to a port-profile
        return executeUpdateObject(portProfileGlobal, Constants.URI);
    }

    /*
     * Deletes a new virtual network.
     */
    public boolean deleteNetwork(int vlanId, long networkId) throws BrocadeVcsApiException {

        if (deactivatePortProfile(networkId)) {

            if (deletePortProfile(networkId)) {
                return deleteInterfaceVlan(vlanId);
            }
        }
        return false;
    }

    /*
     * Deletes a vlan interface.
     */
    private boolean deleteInterfaceVlan(int vlanId) throws BrocadeVcsApiException {
        final InterfaceVlan interfaceVlan = new InterfaceVlan();
        final Interface interfaceObj = new Interface();
        final Vlan vlan = new Vlan();
        vlan.setOperation("delete");
        vlan.setName(vlanId);
        interfaceObj.setVlan(vlan);
        interfaceVlan.setInterface(interfaceObj);

        return executeUpdateObject(interfaceVlan, Constants.URI);

    }

    /*
     * Deactivates a port-profile.
     */
    private boolean deactivatePortProfile(long networkId) throws BrocadeVcsApiException {
        final PortProfileGlobal portProfileGlobal = new PortProfileGlobal();
        final PortProfile portProfile = new PortProfile();
        portProfile.setName(Constants.PORT_PROFILE_NAME_PREFIX + networkId);
        final Activate activate = new Activate();
        activate.setOperation("delete");
        portProfile.setActivate(activate);
        portProfileGlobal.setPortProfile(portProfile);

        //activate port-profile
        return executeUpdateObject(portProfileGlobal, Constants.URI);
    }

    /*
     *  Deletes AMPP port-profile.
     */
    private boolean deletePortProfile(long networkId) throws BrocadeVcsApiException {

        final PortProfile portProfile = new PortProfile();
        portProfile.setName(Constants.PORT_PROFILE_NAME_PREFIX + networkId);
        portProfile.setOperation("delete");
        //deletes port-profile
        return executeUpdateObject(portProfile, Constants.URI);
    }

    protected <T> boolean executeUpdateObject(T newObject, String uri) throws BrocadeVcsApiException {

        final boolean result = true;

        if (_host == null || _host.isEmpty() || _adminuser == null || _adminuser.isEmpty() || _adminpass == null || _adminpass.isEmpty()) {
            throw new BrocadeVcsApiException("Hostname/credentials are null or empty");
        }

        final HttpPatch pm = (HttpPatch)createMethod("patch", uri);
        pm.setHeader("Accept", "application/vnd.configuration.resource+xml");

        pm.setEntity(new StringEntity(convertToString(newObject), ContentType.APPLICATION_XML));

        final HttpResponse response = executeMethod(pm);

        if (response.getStatusLine().getStatusCode() != HttpStatus.SC_NO_CONTENT) {

            String errorMessage;
            try {
                errorMessage = responseToErrorMessage(response);
            } catch (final IOException e) {
                logger.error("Failed to update object : " + e.getMessage());
                throw new BrocadeVcsApiException("Failed to update object : " + e.getMessage());
            }

            pm.releaseConnection();
            logger.error("Failed to update object : " + errorMessage);
            throw new BrocadeVcsApiException("Failed to update object : " + errorMessage);
        }

        pm.releaseConnection();

        return result;
    }

    protected <T> String convertToString(T object) throws BrocadeVcsApiException {

        final StringWriter stringWriter = new StringWriter();

        try {
            final JAXBContext context = JAXBContext.newInstance(object.getClass());
            final Marshaller marshaller = context.createMarshaller();

            marshaller.marshal(object, stringWriter);

        } catch (final JAXBException e) {
            logger.error("Failed to convert object to string : " + e.getMessage());
            throw new BrocadeVcsApiException("Failed to convert object to string : " + e.getMessage());
        }

        final String str = stringWriter.toString();
        logger.info(str);

        return str;

    }

    protected Output convertToXML(String object) throws BrocadeVcsApiException {

        Output output = null;
        try {
            final JAXBContext context = JAXBContext.newInstance(Output.class);

            final StringReader reader = new StringReader(object);

            final Unmarshaller unmarshaller = context.createUnmarshaller();
            final Object result = unmarshaller.unmarshal(reader);

            if (result instanceof Output) {
                output = (Output)result;
                logger.info(output);
            }

        } catch (final JAXBException e) {
            logger.error("Failed to convert string to object : " + e.getMessage());
            throw new BrocadeVcsApiException("Failed to convert string to object : " + e.getMessage());
        }

        return output;

    }

    protected <T> boolean executeCreateObject(T newObject, String uri) throws BrocadeVcsApiException {
        if (_host == null || _host.isEmpty() || _adminuser == null || _adminuser.isEmpty() || _adminpass == null || _adminpass.isEmpty()) {
            throw new BrocadeVcsApiException("Hostname/credentials are null or empty");
        }

        final boolean result = true;
        final HttpPost pm = (HttpPost)createMethod("post", uri);
        pm.setHeader("Accept", "application/vnd.configuration.resource+xml");
        pm.setEntity(new StringEntity(convertToString(newObject), ContentType.APPLICATION_XML));

        final HttpResponse response = executeMethod(pm);

        if (response.getStatusLine().getStatusCode() != HttpStatus.SC_CREATED) {

            String errorMessage;
            try {
                errorMessage = responseToErrorMessage(response);
            } catch (final IOException e) {
                logger.error("Failed to create object : " + e.getMessage());
                throw new BrocadeVcsApiException("Failed to create object : " + e.getMessage());
            }

            pm.releaseConnection();
            logger.error("Failed to create object : " + errorMessage);
            throw new BrocadeVcsApiException("Failed to create object : " + errorMessage);
        }

        pm.releaseConnection();

        return result;
    }

    protected Output executeRetreiveStatus(String uri) throws BrocadeVcsApiException {
        if (_host == null || _host.isEmpty() || _adminuser == null || _adminuser.isEmpty() || _adminpass == null || _adminpass.isEmpty()) {
            throw new BrocadeVcsApiException("Hostname/credentials are null or empty");
        }

        String readLine = null;
        StringBuffer sb = null;

        final HttpPost pm = (HttpPost)createMethod("post", uri);
        pm.setHeader("Accept", "application/vnd.operational-state.resource+xml");
        pm.setEntity(new StringEntity("<show-vcs></show-vcs>", ContentType.APPLICATION_XML));

        final HttpResponse response = executeMethod(pm);

        if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {

            String errorMessage;
            try {
                errorMessage = responseToErrorMessage(response);
            } catch (final IOException e) {
                logger.error("Failed to retreive status : " + e.getMessage());
                throw new BrocadeVcsApiException("Failed to retreive status : " + e.getMessage());
            }

            pm.releaseConnection();
            logger.error("Failed to retreive status : " + errorMessage);
            throw new BrocadeVcsApiException("Failed to retreive status : " + errorMessage);
        }

        try (BufferedReader br = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), Charset.forName("UTF-8")))) {
            sb = new StringBuffer();

            while ((readLine = br.readLine()) != null) {
                logger.debug(readLine);
                sb.append(readLine);

            }
        } catch (final Exception e) {
            logger.error("Failed to retreive status : " + e.getMessage());
            throw new BrocadeVcsApiException("Failed to retreive status : " + e.getMessage());
        }

        pm.releaseConnection();

        return convertToXML(sb.toString());
    }

    protected void executeDeleteObject(String uri) throws BrocadeVcsApiException {
        if (_host == null || _host.isEmpty() || _adminuser == null || _adminuser.isEmpty() || _adminpass == null || _adminpass.isEmpty()) {
            throw new BrocadeVcsApiException("Hostname/credentials are null or empty");
        }

        final HttpDelete dm = (HttpDelete)createMethod("delete", uri);
        dm.setHeader("Accept", "application/vnd.configuration.resource+xml");

        final HttpResponse response = executeMethod(dm);

        if (response.getStatusLine().getStatusCode() != HttpStatus.SC_NO_CONTENT) {

            String errorMessage;
            try {
                errorMessage = responseToErrorMessage(response);
            } catch (final IOException e) {
                logger.error("Failed to delete object : " + e.getMessage());
                throw new BrocadeVcsApiException("Failed to delete object : " + e.getMessage());
            }

            dm.releaseConnection();
            logger.error("Failed to delete object : " + errorMessage);
            throw new BrocadeVcsApiException("Failed to delete object : " + errorMessage);
        }
        dm.releaseConnection();
    }

    protected HttpResponse executeMethod(HttpRequestBase method) throws BrocadeVcsApiException {
        HttpResponse response = null;
        try {
            response = _client.execute(method);
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_UNAUTHORIZED) {
                method.releaseConnection();
                response = _client.execute(method);
            }
        } catch (final IOException e) {
            logger.error("IOException caught while trying to connect to the Brocade Switch", e);
            method.releaseConnection();
            throw new BrocadeVcsApiException("API call to Brocade Switch Failed", e);
        }

        return response;
    }

    private String responseToErrorMessage(HttpResponse response) throws IOException {

        if ("text/html".equals(response.getEntity().getContentType().getValue())) {

            try (BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), Charset.forName("UTF-8")))) {

                final StringBuffer result = new StringBuffer();
                String line = "";
                while ((line = rd.readLine()) != null) {
                    result.append(line);
                }
                return result.toString();
            }
        }
        return null;
    }

}
