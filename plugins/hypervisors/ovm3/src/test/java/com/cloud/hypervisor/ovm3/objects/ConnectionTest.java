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

package com.cloud.hypervisor.ovm3.objects;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.common.XmlRpcHttpRequestConfigImpl;
import org.apache.xmlrpc.common.XmlRpcStreamConfig;
import org.apache.xmlrpc.common.XmlRpcStreamRequestConfig;
import org.apache.xmlrpc.parser.XmlRpcResponseParser;
import org.apache.xmlrpc.util.SAXParsers;
import org.junit.Test;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

/*
 * This is a stub for XML parsing into result sets, it also contains test for
 * Connection
 */
public class ConnectionTest extends Connection {
    XmlTestResultTest results = new XmlTestResultTest();
    String result;
    List<String> multiRes = new ArrayList<String>();
    String hostIp;
    private Map<String, String> methodResponse = new HashMap<String, String>();

    public ConnectionTest() {
    }

    @Override
    public Object callTimeoutInSec(String method, List<?> params, int timeout,
            boolean debug) throws XmlRpcException {
        XmlRpcStreamConfig config = new XmlRpcHttpRequestConfigImpl();
        XmlRpcClient client = new XmlRpcClient();
        client.setTypeFactory(new RpcTypeFactory(client));
        XmlRpcResponseParser parser = new XmlRpcResponseParser(
                (XmlRpcStreamRequestConfig) config, client.getTypeFactory());
        XMLReader xr = SAXParsers.newXMLReader();
        xr.setContentHandler(parser);
        try {
            String result = null;
            if (getMethodResponse(method) != null) {
                result = getMethodResponse(method);
                logger.debug("methodresponse call: " + method + " - " + params);
                logger.trace("methodresponse reply: " + result);
            }
            if (result == null && multiRes.size() >= 0) {
                result = getResult();
                logger.debug("getresult call: " + method + " - " + params);
                logger.trace("getresult reply: " + result);
            }
            xr.parse(new InputSource(new StringReader(result)));
        } catch (Exception e) {
            throw new XmlRpcException("Exception: " + e.getMessage(), e);
        }
        if (parser.getErrorCode() != 0) {
            throw new XmlRpcException("Fault received[" + parser.getErrorCode()
                    + "]: " + parser.getErrorMessage());
        }
        return parser.getResult();
    }

    public void setMethodResponse(String method, String response) {
        methodResponse.put(method, response);
    }

    public String getMethodResponse(String method) {
        if (methodResponse.containsKey(method)) {
            return methodResponse.get(method);
        }
        return null;
    }

    public void removeMethodResponse(String method) {
        if (methodResponse.containsKey(method)) {
            methodResponse.remove(method);
        }
    }

    public void setResult(String res) {
        multiRes = new ArrayList<String>();
        multiRes.add(0, res);
    }

    public void setResult(List<String> l) {
        multiRes = new ArrayList<String>();
        multiRes.addAll(l);
    }

    public void setNull() {
        multiRes = new ArrayList<String>();
        multiRes.add(0, null);
    }

    /* result chainsing */
    public void addResult(String e) {
        multiRes.add(e);
    }

    public void addNull() {
        multiRes.add(null);
    }

    public String getResult() {
        return popResult();
    }

    public String popResult() {
        String res = multiRes.get(0);
        if (multiRes.size() > 1)
            multiRes.remove(0);
        return res;
    }

    public List<String> resultList() {
        return multiRes;
    }

    @Test
    public void testConnection() {
        String host = "ovm-1";
        String user = "admin";
        String pass = "password";
        Integer port = 8899;
        List<?> emptyParams = new ArrayList<Object>();
        Connection con = new Connection(host, port, user, pass);
        results.basicStringTest(con.getIp(), host);
        results.basicStringTest(con.getUserName(), user);
        results.basicStringTest(con.getPassword(), pass);
        results.basicIntTest(con.getPort(), port);
        try {
            con.callTimeoutInSec("ping", emptyParams, 1);
            // con.call("ping", emptyParams, 1, false);
        } catch (XmlRpcException e) {
            // TODO Auto-generated catch block
            System.out.println("Exception: " + e);
        }
        new Connection(host, user, pass);
    }
}
