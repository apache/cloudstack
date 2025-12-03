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

package com.xensource.xenapi;


import org.apache.ws.commons.util.NamespaceContextImpl;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.apache.xmlrpc.client.XmlRpcHttpClientConfig;
import org.apache.xmlrpc.common.TypeFactory;
import org.apache.xmlrpc.common.TypeFactoryImpl;
import org.apache.xmlrpc.common.XmlRpcStreamConfig;
import org.apache.xmlrpc.parser.MapParser;
import org.apache.xmlrpc.parser.RecursiveTypeParserImpl;
import org.apache.xmlrpc.parser.TypeParser;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.namespace.QName;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

public class ConnectionNew extends Connection {
    /**
     * The version of the bindings that this class belongs to.
     *
     * @deprecated This field is not used any more.
     */
    private APIVersion apiVersion;


    /**
     * Updated when Session.login_with_password() is called.
     */
    @Override
    public APIVersion getAPIVersion()
    {
        return apiVersion;
    }

    /**
     * The opaque reference to the session used by this connection
     */
    private String sessionReference;

    /**
     * As seen by the xmlrpc library. From our point of view it's a server.
     */
    private final XmlRpcClient client;

    /**
     * Creates a connection to a particular server using a given url. This object can then be passed
     * in to any other API calls.
     *
     * Note this constructor does NOT call Session.loginWithPassword; the programmer is responsible for calling it,
     * passing the Connection as a parameter. No attempt to connect to the server is made until login is called.
     *
     * When this constructor is used, a call to dispose() will do nothing. The programmer is responsible for manually
     * logging out the Session.
     *
     * @param url The URL of the server to connect to
     * @param replyTimeout The reply timeout for xml-rpc calls in seconds
     * @param connTimeout The connection timeout for xml-rpc calls in seconds
     */
    public ConnectionNew(URL url, int replyTimeout, int connTimeout)
    {
        super(url, replyTimeout, connTimeout);
        this.client = getClientFromURL(url, replyTimeout, connTimeout);
    }

    private XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();

    @Override
    public XmlRpcClientConfigImpl getConfig()
    {
        return config;
    }

    static class CustomMapParser extends RecursiveTypeParserImpl {

        private int level = 0;
        private StringBuffer nameBuffer = new StringBuffer();
        private Object nameObject;
        private Map map;
        private boolean inName;
        private boolean inValue;
        private boolean doneValue;

        public CustomMapParser(XmlRpcStreamConfig pConfig, NamespaceContextImpl pContext, TypeFactory pFactory) {
            super(pConfig, pContext, pFactory);
        }

        protected void addResult(Object pResult) throws SAXException {
            if (this.inName) {
                this.nameObject = pResult;
            } else {
                if (this.nameObject == null) {
                    throw new SAXParseException("Invalid state: Expected name", this.getDocumentLocator());
                }

                this.map.put(this.nameObject, pResult);
            }

        }

        public void startDocument() throws SAXException {
            super.startDocument();
            this.level = 0;
            this.map = new HashMap();
            this.inValue = this.inName = false;
        }

        public void characters(char[] pChars, int pOffset, int pLength) throws SAXException {
            if (this.inName && !this.inValue) {
                this.nameBuffer.append(pChars, pOffset, pLength);
            } else {
                super.characters(pChars, pOffset, pLength);
            }

        }

        public void ignorableWhitespace(char[] pChars, int pOffset, int pLength) throws SAXException {
            if (this.inName) {
                this.characters(pChars, pOffset, pLength);
            } else {
                super.ignorableWhitespace(pChars, pOffset, pLength);
            }

        }

        public void startElement(String pURI, String pLocalName, String pQName, Attributes pAttrs) throws SAXException {
            switch (this.level++) {
                case 0:
                    if (!"".equals(pURI) || !"struct".equals(pLocalName)) {
                        throw new SAXParseException("Expected struct, got " + new QName(pURI, pLocalName), this.getDocumentLocator());
                    }
                    break;
                case 1:
                    if (!"".equals(pURI) || !"member".equals(pLocalName)) {
                        throw new SAXParseException("Expected member, got " + new QName(pURI, pLocalName), this.getDocumentLocator());
                    }

                    this.doneValue = this.inName = this.inValue = false;
                    this.nameObject = null;
                    this.nameBuffer.setLength(0);
                    break;
                case 2:
                    if (this.doneValue) {
                        throw new SAXParseException("Expected /member, got " + new QName(pURI, pLocalName), this.getDocumentLocator());
                    }

                    if ("".equals(pURI) && "name".equals(pLocalName)) {
                        if (this.nameObject != null) {
                            throw new SAXParseException("Expected value, got " + new QName(pURI, pLocalName), this.getDocumentLocator());
                        }

                        this.inName = true;
                    } else if ("".equals(pURI) && "value".equals(pLocalName)) {
                        if (this.nameObject == null) {
                            throw new SAXParseException("Expected name, got " + new QName(pURI, pLocalName), this.getDocumentLocator());
                        }

                        this.inValue = true;
                        this.startValueTag();
                    }
                    break;
                case 3:
                    if (this.inName && "".equals(pURI) && "value".equals(pLocalName)) {
                        if (!this.cfg.isEnabledForExtensions()) {
                            throw new SAXParseException("Expected /name, got " + new QName(pURI, pLocalName), this.getDocumentLocator());
                        }

                        this.inValue = true;
                        this.startValueTag();
                    } else {
                        super.startElement(pURI, pLocalName, pQName, pAttrs);
                    }
                    break;
                default:
                    super.startElement(pURI, pLocalName, pQName, pAttrs);
            }

        }

        public void endElement(String pURI, String pLocalName, String pQName) throws SAXException {
            switch (--this.level) {
                case 0:
                    this.setResult(this.map);
                case 1:
                    break;
                case 2:
                    if (this.inName) {
                        this.inName = false;
                        if (this.nameObject == null) {
                            this.nameObject = this.nameBuffer.toString();
                        } else {
                            for(int i = 0; i < this.nameBuffer.length(); ++i) {
                                if (!Character.isWhitespace(this.nameBuffer.charAt(i))) {
                                    throw new SAXParseException("Unexpected non-whitespace character in member name", this.getDocumentLocator());
                                }
                            }
                        }
                    } else if (this.inValue) {
                        this.endValueTag();
                        this.doneValue = true;
                    }
                    break;
                case 3:
                    if (this.inName && this.inValue && "".equals(pURI) && "value".equals(pLocalName)) {
                        this.endValueTag();
                    } else {
                        super.endElement(pURI, pLocalName, pQName);
                    }
                    break;
                default:
                    super.endElement(pURI, pLocalName, pQName);
            }

        }
    }

    private XmlRpcClient getClientFromURL(URL url, int replyWait, int connWait)
    {
        config.setTimeZone(TimeZone.getTimeZone("UTC"));
        config.setServerURL(url);
        config.setReplyTimeout(replyWait * 1000);
        config.setConnectionTimeout(connWait * 1000);
        XmlRpcClient client = new XmlRpcClient();
        client.setConfig(config);
        client.setTypeFactory(new TypeFactoryImpl(client) {
            @Override
            public TypeParser getParser(XmlRpcStreamConfig pConfig, NamespaceContextImpl pContext, String pURI, String pLocalName) {
                TypeParser parser = super.getParser(pConfig, pContext, pURI, pLocalName);
                if (parser instanceof MapParser) {
                    return new CustomMapParser(pConfig, pContext, this);
                }
                return parser;
            }
        });
        return client;
    }

    @Override
    public String getSessionReference()
    {
        return this.sessionReference;
    }

    @Override
    protected Map dispatch(String methodCall, Object[] methodParams) throws XmlRpcException, Types.XenAPIException
    {
        Map response = (Map) client.execute(methodCall, methodParams);

        if (methodCall.equals("session.login_with_password") &&
                response.get("Status").equals("Success"))
        {
            Session session = Types.toSession(response.get("Value"));
            sessionReference = session.ref;
            setAPIVersion(session);
        }
        else if (methodCall.equals("session.slave_local_login_with_password") &&
                response.get("Status").equals("Success"))
        {
            sessionReference = Types.toSession(response.get("Value")).ref;
            apiVersion = APIVersion.latest();
        }
        else if (methodCall.equals("session.logout"))
        {
            // Work around a bug in XenServer 5.0 and below.
            // session.login_with_password should have rejected us with
            // HOST_IS_SLAVE, but instead we don't find out until later.
            // We don't want to leak the session, so we need to log out
            // this session from the master instead.
            if (response.get("Status").equals("Failure"))
            {
                Object[] error = (Object[]) response.get("ErrorDescription");
                if (error.length == 2 && error[0].equals("HOST_IS_SLAVE"))
                {
                    try
                    {
                        XmlRpcHttpClientConfig clientConfig = (XmlRpcHttpClientConfig)client.getClientConfig();
                        URL client_url = clientConfig.getServerURL();
                        URL masterUrl = new URL(client_url.getProtocol(), (String)error[1], client_url.getPort(), client_url.getFile());

                        Connection tmp_conn = new Connection(masterUrl, sessionReference, clientConfig.getReplyTimeout(), clientConfig.getConnectionTimeout());

                        Session.logout(tmp_conn);
                    }
                    catch (Exception ex)
                    {
                        // Ignore
                    }
                }
            }

            this.sessionReference = null;
        }

        return Types.checkResponse(response);
    }


    private void setAPIVersion(Session session) throws Types.XenAPIException, XmlRpcException
    {
        try
        {
            long major = session.getThisHost(this).getAPIVersionMajor(this);
            long minor = session.getThisHost(this).getAPIVersionMinor(this);
            apiVersion = APIVersion.fromMajorMinor(major, minor);
        }
        catch (Types.BadServerResponse exn)
        {
            apiVersion = APIVersion.UNKNOWN;
        }
    }
}
