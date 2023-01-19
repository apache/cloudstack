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

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.apache.xmlrpc.XmlRpcException;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class OvmObject {
    private volatile Connection client;
    private static List<?> emptyParams = new ArrayList<Object>();
    protected Logger logger = LogManager.getLogger(getClass());
    private boolean success = false;

    public OvmObject() {
    }

    public Connection getClient() {
        return client;
    }

    public synchronized void setClient(Connection c) {
        client = c;
    }

    /* remove dashes from uuids */
    public String deDash(String str) {
        return str.replaceAll("-", "");
    }
    /* generate a uuid */
    public String newUuid() {
        return UUID.randomUUID().toString();
    }

    /* generate a uuid */
    public String newUuid(String str) {
        return UUID.nameUUIDFromBytes(str.getBytes(Charset.defaultCharset())).toString();
    }

    /* capture most of the calls here */
    public Object callWrapper(String call) throws Ovm3ResourceException {
        try {
            return client.call(call, emptyParams);
        } catch (XmlRpcException e) {
            String msg = "Client call " + call  + " to " +  client.getIp() + " went wrong: " + e.getMessage();
            throw new Ovm3ResourceException(msg, e);
        }
    }

    public void setSuccess(boolean s) {
        success = s;
    }

    public Boolean getSuccess() {
        return success;
    }

    /* nice try but doesn't work like that .. */
    @SafeVarargs
    public final <T> Object callWrapper(String call, T... args)
            throws Ovm3ResourceException {
        List<T> params = new ArrayList<T>();
        for (T param : args) {
            params.add(param);
        }
        try {
            return client.call(call, params);
        } catch (XmlRpcException e) {
            String msg = "Client call " + call  + " to " +  client.getIp() + " with " + params + " went wrong: " + e.getMessage();
            throw new Ovm3ResourceException(msg, e);
        }
    }

    /* should check on nil ? */
    @SafeVarargs
    public final <T> Boolean nullCallWrapper(String call, Boolean nullReturn, T... args) throws Ovm3ResourceException {
        Object x = callWrapper(call, args);
        if (x == null) {
            return nullReturn;
        } else if (!nullReturn) {
            return true;
        }
        return false;
    }

    @SafeVarargs
    public final <T> Boolean nullIsFalseCallWrapper(String call, T... args) throws Ovm3ResourceException {
        return nullCallWrapper(call, false, args);
    }
    @SafeVarargs
    public final <T> Boolean nullIsTrueCallWrapper(String call, T... args) throws Ovm3ResourceException {
        return nullCallWrapper(call, true, args);
    }

    /* returns a single string */
    public Map<String, Long> callMap(String call) throws Ovm3ResourceException {
        return (HashMap<String, Long>) callWrapper(call);
    }

    public <T> String callString(String call, T... args) throws Ovm3ResourceException {
        Object result = callWrapper(call, args);
        if (result == null) {
            return null;
        }
        if (result instanceof String || result instanceof Integer || result instanceof Long || result instanceof HashMap) {
            return result.toString();
        }

        Object[] results = (Object[]) result;

        if (results.length == 0) {
            return null;
        }
        if (results.length == 1) {
            return results[0].toString();
        }
        return null;
    }

    /* was String, Object before */
    public <E> Map<String, E> xmlToMap(String path, Document xmlDocument)
            throws Ovm3ResourceException {
        XPathFactory factory = javax.xml.xpath.XPathFactory.newInstance();
        XPath xPath = factory.newXPath();
        try {
            XPathExpression xPathExpression = xPath.compile(path);
            NodeList nodeList = (NodeList) xPathExpression.evaluate(xmlDocument,
                    XPathConstants.NODESET);
            Map<String, E> myMap = new HashMap<String, E>();
            for (int ind = 0; ind < nodeList.getLength(); ind++) {
                NodeList nodeListFor = nodeList.item(ind).getChildNodes();
                for (int index = 0; index < nodeListFor.getLength(); index++) {
                    String rnode = nodeListFor.item(index).getNodeName();
                    NodeList nodeListFor2 = nodeListFor.item(index).getChildNodes();
                    if (nodeListFor2.getLength() > 1) {
                        /* Do we need to figure out all the sub elements here and put them in a map? */
                    } else {
                        String element = nodeListFor.item(index).getNodeValue();
                        myMap.put(rnode, (E) element);
                    }
                }
            }
            return myMap;
        } catch (XPathExpressionException e) {
            throw new Ovm3ResourceException("Problem parsing XML to Map:", e);
        }
    }

    public List<String> xmlToList(String path, Document xmlDocument)
            throws Ovm3ResourceException {
        List<String> list = new ArrayList<String>();
        XPathFactory factory = javax.xml.xpath.XPathFactory.newInstance();
        XPath xPath = factory.newXPath();
        try {
            XPathExpression xPathExpression = xPath.compile(path);
            NodeList nodeList = (NodeList) xPathExpression.evaluate(xmlDocument,
                    XPathConstants.NODESET);
            for (int ind = 0; ind < nodeList.getLength(); ind++) {
                if (!nodeList.item(ind).getNodeValue().isEmpty()) {
                    list.add("" + nodeList.item(ind).getNodeValue());
                } else {
                    list.add("" + nodeList.item(ind).getNodeValue());
                }
            }
            return list;
        } catch (XPathExpressionException e) {
            throw new Ovm3ResourceException("Problem parsing XML to List: ", e);
        }
    }

    public String xmlToString(String path, Document xmlDocument)
            throws Ovm3ResourceException {
        XPathFactory factory = javax.xml.xpath.XPathFactory.newInstance();
        XPath xPath = factory.newXPath();
        try {
            XPathExpression xPathExpression = xPath.compile(path);
            NodeList nodeList = (NodeList) xPathExpression.evaluate(xmlDocument,
                    XPathConstants.NODESET);
            return nodeList.item(0).getNodeValue();
        } catch (NullPointerException e) {
            logger.info("Got no items back from parsing, returning null: " + e);
            return null;
        } catch (XPathExpressionException e) {
            throw new Ovm3ResourceException("Problem parsing XML to String: ", e);
        }
    }

    public Document prepParse(String input)
            throws Ovm3ResourceException {
        DocumentBuilderFactory builderfactory = DocumentBuilderFactory
                .newInstance();
        builderfactory.setNamespaceAware(true);

        DocumentBuilder builder;
        try {
            builder = builderfactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new Ovm3ResourceException("Unable to create document Builder: ", e);
        }
        Document xmlDocument;
        try {
            xmlDocument = builder.parse(new InputSource(new StringReader(
                    input)));
        } catch (SAXException | IOException e) {
            logger.info(e.getClass() + ": ", e);
            throw new Ovm3ResourceException("Unable to parse XML: ", e);
        }
        return xmlDocument;
    }
}
