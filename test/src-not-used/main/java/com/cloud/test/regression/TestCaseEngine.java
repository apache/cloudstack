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
package com.cloud.test.regression;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class TestCaseEngine {

    protected Logger logger = LogManager.getLogger(getClass());
    public static String s_fileName = "../metadata/adapter.xml";
    public static HashMap<String, String> s_globalParameters = new HashMap<String, String>();
    protected static HashMap<String, String> s_componentMap = new HashMap<String, String>();
    protected static HashMap<String, ArrayList<String>> s_inputFile = new HashMap<String, ArrayList<String>>();
    protected static String s_testCaseName = new String();
    protected static ArrayList<String> s_keys = new ArrayList<String>();
    private static ThreadLocal<Object> s_result = new ThreadLocal<Object>();
    public static int s_numThreads = 1;
    public static boolean s_repeat = false;
    public static boolean s_printUrl = false;
    public static String s_type = "All";
    public static boolean s_isSanity = false;
    public static boolean s_isRegression = false;
    private static int s_failure = 0;

    public static void main(String args[]) {

        // Parameters
        List<String> argsList = Arrays.asList(args);
        Iterator<String> iter = argsList.iterator();
        while (iter.hasNext()) {
            String arg = iter.next();
            // is stress?
            if (arg.equals("-t")) {
                s_numThreads = Integer.parseInt(iter.next());
            }
            // do you want to print url for all commands?
            if (arg.equals("-p")) {
                s_printUrl = true;
            }

            //type of the test: sanity, regression, all (default)
            if (arg.equals("-type")) {
                s_type = iter.next();
            }

            if (arg.equals("-repeat")) {
                s_repeat = Boolean.valueOf(iter.next());
            }

            if (arg.equals("-filename")) {
                s_fileName = iter.next();
            }
        }

        if (s_type.equalsIgnoreCase("sanity"))
            s_isSanity = true;
        else if (s_type.equalsIgnoreCase("regression"))
            s_isRegression = true;

        try {
            // parse adapter.xml file to get list of tests to execute
            File file = new File(s_fileName);
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(file);
            doc.getDocumentElement().normalize();
            Element root = doc.getDocumentElement();

            // set global parameters
            setGlobalParams(root);

            // populate _componentMap
            setComponent(root);

            // set error to 0 by default

            // execute test
            for (int i = 0; i < s_numThreads; i++) {
                if (s_numThreads > 1) {
                    logger.info("STARTING STRESS TEST IN " + s_numThreads + " THREADS");
                } else {
                    logger.info("STARTING FUNCTIONAL TEST");
                }
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        do {
                            if (s_numThreads == 1) {
                                try {
                                    for (String key : s_keys) {
                                        Class<?> c = Class.forName(s_componentMap.get(key));
                                        TestCase component = (TestCase)c.newInstance();
                                        executeTest(key, c, component);
                                    }
                                } catch (Exception ex1) {
                                    logger.error(ex1);
                                } finally {
                                    if (s_failure > 0) {
                                        System.exit(1);
                                    }
                                }
                            } else {
                                Random ran = new Random();
                                Integer randomNumber = Math.abs(ran.nextInt(s_keys.size()));
                                try {
                                    String key = s_keys.get(randomNumber);
                                    Class<?> c = Class.forName(s_componentMap.get(key));
                                    TestCase component = (TestCase)c.newInstance();
                                    executeTest(key, c, component);
                                } catch (Exception e) {
                                    logger.error("Error in thread ", e);
                                }
                            }
                        } while (s_repeat);
                    }
                }).start();
            }

        } catch (Exception exc) {
            logger.error(exc);
        }
    }

    public static void setGlobalParams(Element rootElement) {
        NodeList globalParam = rootElement.getElementsByTagName("globalparam");
        Element parameter = (Element)globalParam.item(0);
        NodeList paramLst = parameter.getElementsByTagName("param");

        for (int i = 0; i < paramLst.getLength(); i++) {
            Element paramElement = (Element)paramLst.item(i);

            if (paramElement.getNodeType() == Node.ELEMENT_NODE) {
                Element itemElement = paramElement;
                NodeList itemName = itemElement.getElementsByTagName("name");
                Element itemNameElement = (Element)itemName.item(0);
                NodeList itemVariable = itemElement.getElementsByTagName("variable");
                Element itemVariableElement = (Element)itemVariable.item(0);
                s_globalParameters.put(itemVariableElement.getTextContent(), itemNameElement.getTextContent());
            }
        }
    }

    public static void setComponent(Element rootElement) {
        NodeList testLst = rootElement.getElementsByTagName("test");
        for (int j = 0; j < testLst.getLength(); j++) {
            Element testElement = (Element)testLst.item(j);

            if (testElement.getNodeType() == Node.ELEMENT_NODE) {
                Element itemElement = testElement;

                // get test case name
                NodeList testCaseNameList = itemElement.getElementsByTagName("testname");
                if (testCaseNameList != null) {
                    s_testCaseName = ((Element)testCaseNameList.item(0)).getTextContent();
                }

                if (s_isSanity == true && !s_testCaseName.equals("SANITY TEST"))
                    continue;
                else if (s_isRegression == true && !(s_testCaseName.equals("SANITY TEST") || s_testCaseName.equals("REGRESSION TEST")))
                    continue;

                // set class name
                NodeList className = itemElement.getElementsByTagName("class");
                if ((className.getLength() == 0) || (className == null)) {
                    s_componentMap.put(s_testCaseName, "com.cloud.test.regression.VMApiTest");
                } else {
                    String name = ((Element)className.item(0)).getTextContent();
                    s_componentMap.put(s_testCaseName, name);
                }

                // set input file name
                NodeList inputFileNameLst = itemElement.getElementsByTagName("filename");
                s_inputFile.put(s_testCaseName, new ArrayList<String>());
                for (int k = 0; k < inputFileNameLst.getLength(); k++) {
                    String inputFileName = ((Element)inputFileNameLst.item(k)).getTextContent();
                    s_inputFile.get(s_testCaseName).add(inputFileName);
                }
            }
        }

        //If sanity test required, make sure that SANITY TEST componennt got loaded
        if (s_isSanity == true && s_componentMap.size() == 0) {
            logger.error("FAILURE!!! Failed to load SANITY TEST component. Verify that the test is uncommented in adapter.xml");
            System.exit(1);
        }

        if (s_isRegression == true && s_componentMap.size() != 2) {
            logger.error("FAILURE!!! Failed to load SANITY TEST or REGRESSION TEST components. Verify that these tests are uncommented in adapter.xml");
            System.exit(1);
        }

        // put all keys from _componentMap to the ArrayList
        Set<?> set = s_componentMap.entrySet();
        Iterator<?> it = set.iterator();
        while (it.hasNext()) {
            Map.Entry<?, ?> me = (Map.Entry<?, ?>)it.next();
            String key = (String)me.getKey();
            s_keys.add(key);
        }

    }

    public static boolean executeTest(String key, Class<?> c, TestCase component) {
        boolean finalResult = false;
        try {
            logger.info("Starting \"" + key + "\" test...\n\n");

            // set global parameters
            HashMap<String, String> updateParam = new HashMap<String, String>();
            updateParam.putAll(s_globalParameters);
            component.setParam(updateParam);

            // set DB ip address
            component.setConn(s_globalParameters.get("dbPassword"));

            // set commands list
            component.setCommands();

            // set input file
            if (s_inputFile.get(key) != null) {
                component.setInputFile(s_inputFile.get(key));
            }

            // set test case name
            if (key != null) {
                component.setTestCaseName(s_testCaseName);
            }

            // execute method
            s_result.set(component.executeTest());
            if (s_result.get().toString().equals("false")) {
                logger.error("FAILURE!!! Test \"" + key + "\" failed\n\n\n");
                s_failure++;
            } else {
                finalResult = true;
                logger.info("SUCCESS!!! Test \"" + key + "\" passed\n\n\n");
            }

        } catch (Exception ex) {
            logger.error("error during test execution ", ex);
        }
        return finalResult;
    }
}
