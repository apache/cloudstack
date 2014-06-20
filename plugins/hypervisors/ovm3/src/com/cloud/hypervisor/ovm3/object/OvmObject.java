/*******************************************************************************
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.cloud.hypervisor.ovm3.object;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.xmlrpc.XmlRpcException;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class OvmObject {
    public static Connection client = null;
    public static Vector<?> emptyParams = new Vector<Object>();

    /* remove dashes from uuids */
    public String deDash(String str) {
        final String x = str.replaceAll("-", "");
        return x;
    }

    /* generate a uuid */
    public String newUuid() {
        final String uuid = UUID.randomUUID().toString();
        return uuid;
    }

    /* generate a uuid */
    public String newUuid(String str) {
        final String uuid = UUID.nameUUIDFromBytes(str.getBytes()).toString();
        return uuid;
    }

    /* capture most of the calls here */
    public static Object callWrapper(String call) throws XmlRpcException {
        try {
            Object res = client.call(call, emptyParams);
            return res;
        } catch (XmlRpcException e) {
            throw new XmlRpcException(e.getMessage());
        }
    }

    /* nice try but doesn't work like that .. */
    @SafeVarargs
    public static <T> Object callWrapper(String call, T... args)
            throws XmlRpcException {
        Vector<T> params = new Vector<T>();
        for (T param : args) {
            params.add(param);
        }
        // return
        Object res = client.call(call, params);
        return res;
    }

    /* returns a single string */
    public HashMap<String, Long> callMap(String call) throws XmlRpcException {
        HashMap<String, Long> result = (HashMap<String, Long>) callWrapper(call);
        return result;
    }

    public <T> String callString(String call, T... args) throws XmlRpcException {
        Object result = callWrapper(call, args);
        if (result == null) {
            return null;
        }
        if (result instanceof String)
            return result.toString();
        if (result instanceof Integer)
            return result.toString();
        if (result instanceof Long)
            return result.toString();
        if (result instanceof HashMap)
            return result.toString();

        Object[] results = (Object[]) result;

        if (results.length == 0)
            // return results[0].toString();
            return null;

        if (results.length == 1)
            return results[0].toString();

        return null;
    }

    /* was String, Object before */
    public <E> Map<String, E> xmlToMap(String path, Document xmlDocument)
            throws XPathExpressionException {
        XPathFactory factory = javax.xml.xpath.XPathFactory.newInstance();
        XPath xPath = factory.newXPath();
        // capabilities, date_time etc
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
                    // System.out.println("multiball");
                    /*
                     * for (int i = 0; i < nodeListFor2.getLength(); i++) {
                     * String node = nodeListFor2.item(i).getNodeName(); String
                     * element = nodeListFor2.item(i).getTextContent();
                     * System.out.println("rnode: " + rnode + " -> node " + node
                     * + " ---> " + element); myMap.put(node, element); }
                     */
                } else {
                    String element = nodeListFor.item(index).getTextContent();
                    // System.out.println("rnode " + rnode + " ---> " +
                    // element);
                    myMap.put(rnode, (E) element);
                }
            }
        }
        return myMap;
    }

    public List<String> xmlToList(String path, Document xmlDocument)
            throws XPathExpressionException {
        List<String> list = new ArrayList<String>();
        XPathFactory factory = javax.xml.xpath.XPathFactory.newInstance();
        XPath xPath = factory.newXPath();

        XPathExpression xPathExpression = xPath.compile(path);
        NodeList nodeList = (NodeList) xPathExpression.evaluate(xmlDocument,
                XPathConstants.NODESET);

        for (int ind = 0; ind < nodeList.getLength(); ind++) {
            // System.out.println(nodeList.item(ind).getTextContent());
            if (!nodeList.item(ind).getTextContent().isEmpty()) {
                list.add("" + nodeList.item(ind).getTextContent());
            } else {
                list.add("" + nodeList.item(ind).getNodeValue());
            }
        }
        return list;
    }

    public String xmlToString(String path, Document xmlDocument)
            throws XPathExpressionException {

        XPathFactory factory = javax.xml.xpath.XPathFactory.newInstance();
        XPath xPath = factory.newXPath();

        XPathExpression xPathExpression = xPath.compile(path);
        NodeList nodeList = (NodeList) xPathExpression.evaluate(xmlDocument,
                XPathConstants.NODESET);
        // put a try in here too, so we can get the subbies
        String x = nodeList.item(0).getTextContent();
        return x;
    }

    public Document prepParse(String input)
            throws ParserConfigurationException, Exception, IOException {
        DocumentBuilderFactory builderfactory = DocumentBuilderFactory
                .newInstance();
        builderfactory.setNamespaceAware(true);

        DocumentBuilder builder = builderfactory.newDocumentBuilder();
        Document xmlDocument = builder.parse(new InputSource(new StringReader(
                (String) input)));
        return xmlDocument;
    }
    /*
     * returns a list of strings public <T> ArrayList<String> call(String call,
     * T... args) throws XmlRpcException { ArrayList<String> data = new
     * ArrayList<String>(); Object[] result = (Object[]) callWrapper(call,
     * args);
     *
     * if (result[result.length] != null) return null;
     *
     * for(Object x : result) { data.add(x.toString()); } return data; }
     */
}
