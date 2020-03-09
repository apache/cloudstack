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
package com.cloud.test.stress;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.log4j.Logger;
import org.apache.log4j.NDC;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.trilead.ssh2.ChannelCondition;
import com.trilead.ssh2.Connection;
import com.trilead.ssh2.SCPClient;
import com.trilead.ssh2.Session;

import com.cloud.utils.exception.CloudRuntimeException;

public class StressTestDirectAttach {
    private static long sleepTime = 180000L; // default 0
    private static boolean cleanUp = true;
    public static final Logger s_logger = Logger.getLogger(TestClientWithAPI.class.getName());
    private static boolean repeat = true;
    private static String[] users = null;
    private static boolean internet = false;
    private static ThreadLocal<String> s_linuxIP = new ThreadLocal<String>();
    private static ThreadLocal<String> s_linuxVmId = new ThreadLocal<String>();
    private static ThreadLocal<String> s_linuxVmId1 = new ThreadLocal<String>();
    private static ThreadLocal<String> s_linuxPassword = new ThreadLocal<String>();
    private static ThreadLocal<String> s_windowsIP = new ThreadLocal<String>();
    private static ThreadLocal<String> s_secretKey = new ThreadLocal<String>();
    private static ThreadLocal<String> s_apiKey = new ThreadLocal<String>();
    private static ThreadLocal<Long> s_userId = new ThreadLocal<Long>();
    private static ThreadLocal<String> s_account = new ThreadLocal<String>();
    private static ThreadLocal<String> s_domainRouterId = new ThreadLocal<String>();
    private static ThreadLocal<String> s_newVolume = new ThreadLocal<String>();
    private static ThreadLocal<String> s_newVolume1 = new ThreadLocal<String>();
    private static DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    private static int usageIterator = 1;
    private static int numThreads = 1;
    private static int wait = 5000;
    private static String accountName = null;
    private static String zoneId = "1";
    private static String serviceOfferingId = "13";
    private static String diskOfferingId = "11";
    private static String diskOfferingId1 = "12";

    private static final int MAX_RETRY_LINUX = 10;
    private static final int MAX_RETRY_WIN = 10;

    public static void main(String[] args) {
        String host = "http://localhost";
        String port = "8092";
        String devPort = "8080";
        String apiUrl = "/client/api";

        try {
            // Parameters
            List<String> argsList = Arrays.asList(args);
            Iterator<String> iter = argsList.iterator();
            while (iter.hasNext()) {
                String arg = iter.next();
                // host
                if (arg.equals("-h")) {
                    host = "http://" + iter.next();
                }

                if (arg.equals("-p")) {
                    port = iter.next();
                }
                if (arg.equals("-dp")) {
                    devPort = iter.next();
                }

                if (arg.equals("-t")) {
                    numThreads = Integer.parseInt(iter.next());
                }

                if (arg.equals("-s")) {
                    sleepTime = Long.parseLong(iter.next());
                }
                if (arg.equals("-a")) {
                    accountName = iter.next();
                }

                if (arg.equals("-c")) {
                    cleanUp = Boolean.parseBoolean(iter.next());
                    if (!cleanUp)
                        sleepTime = 0L; // no need to wait if we don't ever
                    // cleanup
                }

                if (arg.equals("-r")) {
                    repeat = Boolean.parseBoolean(iter.next());
                }

                if (arg.equals("-i")) {
                    internet = Boolean.parseBoolean(iter.next());
                }

                if (arg.equals("-w")) {
                    wait = Integer.parseInt(iter.next());
                }

                if (arg.equals("-z")) {
                    zoneId = iter.next();
                }

                if (arg.equals("-so")) {
                    serviceOfferingId = iter.next();
                }

            }

            final String server = host + ":" + port + "/";
            final String developerServer = host + ":" + devPort + apiUrl;
            s_logger.info("Starting test against server: " + server + " with " + numThreads + " thread(s)");
            if (cleanUp)
                s_logger.info("Clean up is enabled, each test will wait " + sleepTime + " ms before cleaning up");

            for (int i = 0; i < numThreads; i++) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        do {
                            String username = null;
                            try {
                                long now = System.currentTimeMillis();
                                Random ran = new Random();
                                username = Math.abs(ran.nextInt()) + "-user";
                                NDC.push(username);

                                s_logger.info("Starting test for the user " + username);
                                int response = executeDeployment(server, developerServer, username);
                                boolean success = false;
                                String reason = null;

                                if (response == 200) {
                                    success = true;
                                    if (internet) {
                                        s_logger.info("Deploy successful...waiting 5 minute before SSH tests");
                                        Thread.sleep(300000L); // Wait 60
                                        // seconds so
                                        // the windows VM
                                        // can boot up and do a sys prep.

                                        s_logger.info("Begin Linux SSH test for account " + s_account.get());
                                        reason = sshTest(s_linuxIP.get(), s_linuxPassword.get());

                                        if (reason == null) {
                                            s_logger.info("Linux SSH test successful for account " + s_account.get());
                                        }
                                    }
                                    if (reason == null) {
                                        if (internet) {
                                            s_logger.info("Windows SSH test successful for account " + s_account.get());
                                        } else {
                                            s_logger.info("deploy test successful....now cleaning up");
                                            if (cleanUp) {
                                                s_logger.info("Waiting " + sleepTime + " ms before cleaning up vms");
                                                Thread.sleep(sleepTime);
                                            } else {
                                                success = true;
                                            }
                                        }

                                        if (usageIterator >= numThreads) {
                                            int eventsAndBillingResponseCode = executeEventsAndBilling(server, developerServer);
                                            s_logger.info("events and usage records command finished with response code: " + eventsAndBillingResponseCode);
                                            usageIterator = 1;

                                        } else {
                                            s_logger.info("Skipping events and usage records for this user: usageIterator " + usageIterator + " and number of Threads " +
                                                numThreads);
                                            usageIterator++;
                                        }

                                        if ((users == null) && (accountName == null)) {
                                            s_logger.info("Sending cleanup command");
                                            int cleanupResponseCode = executeCleanup(server, developerServer, username);
                                            s_logger.info("cleanup command finished with response code: " + cleanupResponseCode);
                                            success = (cleanupResponseCode == 200);
                                        } else {
                                            s_logger.info("Sending stop DomR / destroy VM command");
                                            int stopResponseCode = executeStop(server, developerServer, username);
                                            s_logger.info("stop(destroy) command finished with response code: " + stopResponseCode);
                                            success = (stopResponseCode == 200);
                                        }

                                    } else {
                                        // Just stop but don't destroy the
                                        // VMs/Routers
                                        s_logger.info("SSH test failed for account " + s_account.get() + "with reason '" + reason + "', stopping VMs");
                                        int stopResponseCode = executeStop(server, developerServer, username);
                                        s_logger.info("stop command finished with response code: " + stopResponseCode);
                                        success = false; // since the SSH test
                                        // failed, mark the
                                        // whole test as
                                        // failure
                                    }
                                } else {
                                    // Just stop but don't destroy the
                                    // VMs/Routers
                                    s_logger.info("Deploy test failed with reason '" + reason + "', stopping VMs");
                                    int stopResponseCode = executeStop(server, developerServer, username);
                                    s_logger.info("stop command finished with response code: " + stopResponseCode);
                                    success = false; // since the deploy test
                                    // failed, mark the
                                    // whole test as failure
                                }

                                if (success) {
                                    s_logger.info("***** Completed test for user : " + username + " in " + ((System.currentTimeMillis() - now) / 1000L) + " seconds");

                                } else {
                                    s_logger.info("##### FAILED test for user : " + username + " in " + ((System.currentTimeMillis() - now) / 1000L) +
                                        " seconds with reason : " + reason);
                                }
                                s_logger.info("Sleeping for " + wait + " seconds before starting next iteration");
                                Thread.sleep(wait);
                            } catch (Exception e) {
                                s_logger.warn("Error in thread", e);
                                try {
                                    int stopResponseCode = executeStop(server, developerServer, username);
                                    s_logger.info("stop response code: " + stopResponseCode);
                                } catch (Exception e1) {
                                    s_logger.info("[ignored]"
                                            + "error executing stop during stress test: " + e1.getLocalizedMessage());
                                }
                            } finally {
                                NDC.clear();
                            }
                        } while (repeat);
                    }
                }).start();
            }
        } catch (Exception e) {
            s_logger.error(e);
        }
    }

    public static Map<String, List<String>> getMultipleValuesFromXML(InputStream is, String[] tagNames) {
        Map<String, List<String>> returnValues = new HashMap<String, List<String>>();
        try {
            DocumentBuilder docBuilder = factory.newDocumentBuilder();
            Document doc = docBuilder.parse(is);
            Element rootElement = doc.getDocumentElement();
            for (int i = 0; i < tagNames.length; i++) {
                NodeList targetNodes = rootElement.getElementsByTagName(tagNames[i]);
                if (targetNodes.getLength() <= 0) {
                    s_logger.error("no " + tagNames[i] + " tag in XML response...returning null");
                } else {
                    List<String> valueList = new ArrayList<String>();
                    for (int j = 0; j < targetNodes.getLength(); j++) {
                        Node node = targetNodes.item(j);
                        valueList.add(node.getTextContent());
                    }
                    returnValues.put(tagNames[i], valueList);
                }
            }
        } catch (Exception ex) {
            s_logger.error(ex);
        }
        return returnValues;
    }

    public static Map<String, String> getSingleValueFromXML(InputStream is, String[] tagNames) {
        Map<String, String> returnValues = new HashMap<String, String>();
        try {
            DocumentBuilder docBuilder = factory.newDocumentBuilder();
            Document doc = docBuilder.parse(is);
            Element rootElement = doc.getDocumentElement();

            for (int i = 0; i < tagNames.length; i++) {
                NodeList targetNodes = rootElement.getElementsByTagName(tagNames[i]);
                if (targetNodes.getLength() <= 0) {
                    s_logger.error("no " + tagNames[i] + " tag in XML response...returning null");
                } else {
                    returnValues.put(tagNames[i], targetNodes.item(0).getTextContent());
                }
            }
        } catch (Exception ex) {
            s_logger.error("error processing XML", ex);
        }
        return returnValues;
    }

    public static Map<String, String> getSingleValueFromXML(Element rootElement, String[] tagNames) {
        Map<String, String> returnValues = new HashMap<String, String>();
        if (rootElement == null) {
            s_logger.error("Root element is null, can't get single value from xml");
            return null;
        }
        try {
            for (int i = 0; i < tagNames.length; i++) {
                NodeList targetNodes = rootElement.getElementsByTagName(tagNames[i]);
                if (targetNodes.getLength() <= 0) {
                    s_logger.error("no " + tagNames[i] + " tag in XML response...returning null");
                } else {
                    returnValues.put(tagNames[i], targetNodes.item(0).getTextContent());
                }
            }
        } catch (Exception ex) {
            s_logger.error("error processing XML", ex);
        }
        return returnValues;
    }

    private static List<String> getNonSourceNatIPs(InputStream is) {
        List<String> returnValues = new ArrayList<String>();
        try {
            DocumentBuilder docBuilder = factory.newDocumentBuilder();
            Document doc = docBuilder.parse(is);
            Element rootElement = doc.getDocumentElement();
            NodeList allocatedIpAddrNodes = rootElement.getElementsByTagName("publicipaddress");
            for (int i = 0; i < allocatedIpAddrNodes.getLength(); i++) {
                Node allocatedIpAddrNode = allocatedIpAddrNodes.item(i);
                NodeList childNodes = allocatedIpAddrNode.getChildNodes();
                String ipAddress = null;
                boolean isSourceNat = true; // assume it's source nat until we
                // find otherwise
                for (int j = 0; j < childNodes.getLength(); j++) {
                    Node n = childNodes.item(j);
                    if ("ipaddress".equals(n.getNodeName())) {
                        ipAddress = n.getTextContent();
                    } else if ("issourcenat".equals(n.getNodeName())) {
                        isSourceNat = Boolean.parseBoolean(n.getTextContent());
                    }
                }
                if ((ipAddress != null) && !isSourceNat) {
                    returnValues.add(ipAddress);
                }
            }
        } catch (Exception ex) {
            s_logger.error(ex);
        }
        return returnValues;
    }

    private static List<String> getSourceNatIPs(InputStream is) {
        List<String> returnValues = new ArrayList<String>();
        try {
            DocumentBuilder docBuilder = factory.newDocumentBuilder();
            Document doc = docBuilder.parse(is);
            Element rootElement = doc.getDocumentElement();
            NodeList allocatedIpAddrNodes = rootElement.getElementsByTagName("publicipaddress");
            for (int i = 0; i < allocatedIpAddrNodes.getLength(); i++) {
                Node allocatedIpAddrNode = allocatedIpAddrNodes.item(i);
                NodeList childNodes = allocatedIpAddrNode.getChildNodes();
                String ipAddress = null;
                boolean isSourceNat = false; // assume it's *not* source nat until we find otherwise
                for (int j = 0; j < childNodes.getLength(); j++) {
                    Node n = childNodes.item(j);
                    if ("ipaddress".equals(n.getNodeName())) {
                        ipAddress = n.getTextContent();
                    } else if ("issourcenat".equals(n.getNodeName())) {
                        isSourceNat = Boolean.parseBoolean(n.getTextContent());
                    }
                }
                if ((ipAddress != null) && isSourceNat) {
                    returnValues.add(ipAddress);
                }
            }
        } catch (Exception ex) {
            s_logger.error(ex);
        }
        return returnValues;
    }

    private static String executeRegistration(String server, String username, String password) throws HttpException, IOException {
        String url = server + "?command=registerUserKeys&id=" + s_userId.get().toString();
        s_logger.info("registering: " + username);
        String returnValue = null;
        HttpClient client = new HttpClient();
        HttpMethod method = new GetMethod(url);
        int responseCode = client.executeMethod(method);
        if (responseCode == 200) {
            InputStream is = method.getResponseBodyAsStream();
            Map<String, String> requestKeyValues = getSingleValueFromXML(is, new String[] {"apikey", "secretkey"});
            s_apiKey.set(requestKeyValues.get("apikey"));
            returnValue = requestKeyValues.get("secretkey");
        } else {
            s_logger.error("registration failed with error code: " + responseCode);
        }
        return returnValue;
    }

    private static Integer executeDeployment(String server, String developerServer, String username) throws HttpException, IOException {
        // test steps:
        // - create user
        // - deploy Windows VM
        // - deploy Linux VM
        // - associate IP address
        // - create two IP forwarding rules
        // - create load balancer rule
        // - list IP forwarding rules
        // - list load balancer rules

        // -----------------------------
        // CREATE USER
        // -----------------------------
        String encodedUsername = URLEncoder.encode(username, "UTF-8");
        String encryptedPassword = createMD5Password(username);
        String encodedPassword = URLEncoder.encode(encryptedPassword, "UTF-8");

        String url =
            server + "?command=createUser&username=" + encodedUsername + "&password=" + encodedPassword +
                "&firstname=Test&lastname=Test&email=test@vmops.com&domainId=1&accounttype=0";
        if (accountName != null) {
            url =
                server + "?command=createUser&username=" + encodedUsername + "&password=" + encodedPassword +
                    "&firstname=Test&lastname=Test&email=test@vmops.com&domainId=1&accounttype=0&account=" + accountName;
        }
        HttpClient client = new HttpClient();
        HttpMethod method = new GetMethod(url);
        int responseCode = client.executeMethod(method);
        long userId = -1;
        if (responseCode == 200) {
            InputStream is = method.getResponseBodyAsStream();
            Map<String, String> userIdValues = getSingleValueFromXML(is, new String[] {"id", "account"});
            String userIdStr = userIdValues.get("id");
            s_logger.info("created user " + username + " with id " + userIdStr);
            if (userIdStr != null) {
                userId = Long.parseLong(userIdStr);
                s_userId.set(userId);
                s_account.set(userIdValues.get("account"));
                if (userId == -1) {
                    s_logger.error("create user (" + username + ") failed to retrieve a valid user id, aborting depolyment test");
                    return -1;
                }
            }
        } else {
            s_logger.error("create user test failed for user " + username + " with error code :" + responseCode);
            return responseCode;
        }

        s_secretKey.set(executeRegistration(server, username, username));

        if (s_secretKey.get() == null) {
            s_logger.error("FAILED to retrieve secret key during registration, skipping user: " + username);
            return -1;
        } else {
            s_logger.info("got secret key: " + s_secretKey.get());
            s_logger.info("got api key: " + s_apiKey.get());
        }

        // ---------------------------------
        // CREATE NETWORK GROUP AND ADD INGRESS RULE TO IT
        // ---------------------------------
        String networkAccount = null;
        if (accountName != null) {
            networkAccount = accountName;
        } else {
            networkAccount = encodedUsername;
        }
        String encodedApiKey = URLEncoder.encode(s_apiKey.get(), "UTF-8");
        String requestToSign = "apikey=" + encodedApiKey + "&command=createSecurityGroup&name=" + encodedUsername;
        requestToSign = requestToSign.toLowerCase();
        String signature = signRequest(requestToSign, s_secretKey.get());
        String encodedSignature = URLEncoder.encode(signature, "UTF-8");
        url = developerServer + "?command=createSecurityGroup&name=" + encodedUsername + "&apikey=" + encodedApiKey + "&signature=" + encodedSignature;
        method = new GetMethod(url);
        responseCode = client.executeMethod(method);
        if (responseCode == 200) {
            InputStream is = method.getResponseBodyAsStream();
            Map<String, String> values = getSingleValueFromXML(is, new String[] {"id"});

            if (values.get("id") == null) {
                s_logger.info("Create network rule response code: 401");
                return 401;
            } else {
                s_logger.info("Create security group response code: " + responseCode);
            }
        } else {
            s_logger.error("Create security group failed with error code: " + responseCode + ". Following URL was sent: " + url);
            return responseCode;
        }

        String encodedCidr = URLEncoder.encode("192.168.1.143/32", "UTF-8");
        url =
            server + "?command=authorizeSecurityGroupIngress&cidrlist=" + encodedCidr + "&endport=22&" + "securitygroupname=" + encodedUsername +
                "&protocol=tcp&startport=22&account=" + networkAccount + "&domainid=1";

        method = new GetMethod(url);
        responseCode = client.executeMethod(method);
        if (responseCode == 200) {
            InputStream input = method.getResponseBodyAsStream();
            Element el = queryAsyncJobResult(server, input);
            Map<String, String> values = getSingleValueFromXML(el, new String[] {"id"});

            if (values.get("id") == null) {
                s_logger.info("Authorise security group ingress response code: 401");
                return 401;
            } else {
                s_logger.info("Authorise security group ingress response code: " + responseCode);
            }
        } else {
            s_logger.error("Authorise security group ingress failed with error code: " + responseCode + ". Following URL was sent: " + url);
            return responseCode;
        }

        // ---------------------------------
        // DEPLOY LINUX VM
        // ---------------------------------
        {
            long templateId = 2;
            String encodedZoneId = URLEncoder.encode("" + zoneId, "UTF-8");
            String encodedServiceOfferingId = URLEncoder.encode("" + serviceOfferingId, "UTF-8");
            String encodedTemplateId = URLEncoder.encode("" + templateId, "UTF-8");
            encodedApiKey = URLEncoder.encode(s_apiKey.get(), "UTF-8");
            requestToSign =
                "apikey=" + encodedApiKey + "&command=deployVirtualMachine&securitygrouplist=" + encodedUsername + "&serviceofferingid=" + encodedServiceOfferingId +
                    "&templateid=" + encodedTemplateId + "&zoneid=" + encodedZoneId;
            requestToSign = requestToSign.toLowerCase();
            signature = signRequest(requestToSign, s_secretKey.get());
            encodedSignature = URLEncoder.encode(signature, "UTF-8");
            url =
                developerServer + "?command=deployVirtualMachine&securitygrouplist=" + encodedUsername + "&zoneid=" + encodedZoneId + "&serviceofferingid=" +
                    encodedServiceOfferingId + "&templateid=" + encodedTemplateId + "&apikey=" + encodedApiKey + "&signature=" + encodedSignature;

            method = new GetMethod(url);
            responseCode = client.executeMethod(method);
            if (responseCode == 200) {
                InputStream input = method.getResponseBodyAsStream();
                Element el = queryAsyncJobResult(server, input);
                Map<String, String> values = getSingleValueFromXML(el, new String[] {"id", "ipaddress"});

                if ((values.get("ipaddress") == null) || (values.get("id") == null)) {
                    s_logger.info("deploy linux vm response code: 401");
                    return 401;
                } else {
                    s_logger.info("deploy linux vm response code: " + responseCode);
                    long linuxVMId = Long.parseLong(values.get("id"));
                    s_logger.info("got linux virtual machine id: " + linuxVMId);
                    s_linuxVmId.set(values.get("id"));
                    s_linuxIP.set(values.get("ipaddress"));
                    s_linuxPassword.set("rs-ccb35ea5");
                }
            } else {
                s_logger.error("deploy linux vm failed with error code: " + responseCode + ". Following URL was sent: " + url);
                return responseCode;
            }
        }

        //Create a new volume
        {
            url = server + "?command=createVolume&diskofferingid=" + diskOfferingId + "&zoneid=" + zoneId + "&name=newvolume&account=" + s_account.get() + "&domainid=1";
            s_logger.info("Creating volume....");
            client = new HttpClient();
            method = new GetMethod(url);
            responseCode = client.executeMethod(method);
            if (responseCode == 200) {
                InputStream input = method.getResponseBodyAsStream();
                Element el = queryAsyncJobResult(server, input);
                Map<String, String> values = getSingleValueFromXML(el, new String[] {"id"});

                if (values.get("id") == null) {
                    s_logger.info("create volume response code: 401");
                    return 401;
                } else {
                    s_logger.info("create volume response code: " + responseCode);
                    String volumeId = values.get("id");
                    s_logger.info("got volume id: " + volumeId);
                    s_newVolume.set(volumeId);
                }
            } else {
                s_logger.error("create volume failed with error code: " + responseCode + ". Following URL was sent: " + url);
                return responseCode;
            }
        }

        //attach a new volume to the vm
        {
            url = server + "?command=attachVolume&id=" + s_newVolume.get() + "&virtualmachineid=" + s_linuxVmId.get();
            s_logger.info("Attaching volume with id " + s_newVolume.get() + " to the vm " + s_linuxVmId.get());
            client = new HttpClient();
            method = new GetMethod(url);
            responseCode = client.executeMethod(method);
            s_logger.info("Attach data volume response code: " + responseCode);
            if (responseCode == 200) {
                InputStream input = method.getResponseBodyAsStream();
                Element el = queryAsyncJobResult(server, input);
                Map<String, String> values = getSingleValueFromXML(el, new String[] {"id"});

                if (values.get("id") == null) {
                    s_logger.info("Attach volume response code: 401");
                    return 401;
                } else {
                    s_logger.info("Attach volume response code: " + responseCode);
                }
            } else {
                s_logger.error("Attach volume failed with error code: " + responseCode + ". Following URL was sent: " + url);
                return responseCode;
            }
        }

        //DEPLOY SECOND VM, ADD VOLUME TO IT

        // ---------------------------------
        // DEPLOY another linux vm
        // ---------------------------------
        {
            long templateId = 2;
            String encodedZoneId = URLEncoder.encode("" + zoneId, "UTF-8");
            String encodedServiceOfferingId = URLEncoder.encode("" + serviceOfferingId, "UTF-8");
            String encodedTemplateId = URLEncoder.encode("" + templateId, "UTF-8");
            encodedApiKey = URLEncoder.encode(s_apiKey.get(), "UTF-8");
            requestToSign =
                "apikey=" + encodedApiKey + "&command=deployVirtualMachine&securitygrouplist=" + encodedUsername + "&serviceofferingid=" + encodedServiceOfferingId +
                    "&templateid=" + encodedTemplateId + "&zoneid=" + encodedZoneId;
            requestToSign = requestToSign.toLowerCase();
            signature = signRequest(requestToSign, s_secretKey.get());
            encodedSignature = URLEncoder.encode(signature, "UTF-8");
            url =
                developerServer + "?command=deployVirtualMachine&securitygrouplist=" + encodedUsername + "&zoneid=" + encodedZoneId + "&serviceofferingid=" +
                    encodedServiceOfferingId + "&templateid=" + encodedTemplateId + "&apikey=" + encodedApiKey + "&signature=" + encodedSignature;

            method = new GetMethod(url);
            responseCode = client.executeMethod(method);
            if (responseCode == 200) {
                InputStream input = method.getResponseBodyAsStream();
                Element el = queryAsyncJobResult(server, input);
                Map<String, String> values = getSingleValueFromXML(el, new String[] {"id", "ipaddress"});

                if ((values.get("ipaddress") == null) || (values.get("id") == null)) {
                    s_logger.info("deploy linux vm response code: 401");
                    return 401;
                } else {
                    s_logger.info("deploy linux vm response code: " + responseCode);
                    long linuxVMId = Long.parseLong(values.get("id"));
                    s_logger.info("got linux virtual machine id: " + linuxVMId);
                    s_linuxVmId1.set(values.get("id"));
                }
            } else {
                s_logger.error("deploy linux vm failed with error code: " + responseCode + ". Following URL was sent: " + url);
                return responseCode;
            }
        }

        //Create a new volume
        {
            url = server + "?command=createVolume&diskofferingid=" + diskOfferingId1 + "&zoneid=" + zoneId + "&name=newvolume1&account=" + s_account.get() + "&domainid=1";
            s_logger.info("Creating volume....");
            client = new HttpClient();
            method = new GetMethod(url);
            responseCode = client.executeMethod(method);
            if (responseCode == 200) {
                InputStream input = method.getResponseBodyAsStream();
                Element el = queryAsyncJobResult(server, input);
                Map<String, String> values = getSingleValueFromXML(el, new String[] {"id"});

                if (values.get("id") == null) {
                    s_logger.info("create volume response code: 401");
                    return 401;
                } else {
                    s_logger.info("create volume response code: " + responseCode);
                    String volumeId = values.get("id");
                    s_logger.info("got volume id: " + volumeId);
                    s_newVolume1.set(volumeId);
                }
            } else {
                s_logger.error("create volume failed with error code: " + responseCode + ". Following URL was sent: " + url);
                return responseCode;
            }
        }

        //attach a new volume to the vm
        {
            url = server + "?command=attachVolume&id=" + s_newVolume1.get() + "&virtualmachineid=" + s_linuxVmId1.get();
            s_logger.info("Attaching volume with id " + s_newVolume1.get() + " to the vm " + s_linuxVmId1.get());
            client = new HttpClient();
            method = new GetMethod(url);
            responseCode = client.executeMethod(method);
            s_logger.info("Attach data volume response code: " + responseCode);
            if (responseCode == 200) {
                InputStream input = method.getResponseBodyAsStream();
                Element el = queryAsyncJobResult(server, input);
                Map<String, String> values = getSingleValueFromXML(el, new String[] {"id"});

                if (values.get("id") == null) {
                    s_logger.info("Attach volume response code: 401");
                    return 401;
                } else {
                    s_logger.info("Attach volume response code: " + responseCode);
                }
            } else {
                s_logger.error("Attach volume failed with error code: " + responseCode + ". Following URL was sent: " + url);
                return responseCode;
            }
        }
        return 200;
    }

    private static int executeCleanup(String server, String developerServer, String username) throws HttpException, IOException {
        // test steps:
        // - get user
        // - delete user

        // -----------------------------
        // GET USER
        // -----------------------------
        String userId = s_userId.get().toString();
        String encodedUserId = URLEncoder.encode(userId, "UTF-8");
        String url = server + "?command=listUsers&id=" + encodedUserId;
        s_logger.info("Cleaning up resources for user: " + userId + " with url " + url);
        HttpClient client = new HttpClient();
        HttpMethod method = new GetMethod(url);
        int responseCode = client.executeMethod(method);
        s_logger.info("get user response code: " + responseCode);
        if (responseCode == 200) {
            InputStream is = method.getResponseBodyAsStream();
            Map<String, String> userInfo = getSingleValueFromXML(is, new String[] {"username", "id", "account"});
            if (!username.equals(userInfo.get("username"))) {
                s_logger.error("get user failed to retrieve requested user, aborting cleanup test" + ". Following URL was sent: " + url);
                return -1;
            }

        } else {
            s_logger.error("get user failed with error code: " + responseCode + ". Following URL was sent: " + url);
            return responseCode;
        }

        // -----------------------------
        // UPDATE USER
        // -----------------------------
        {
            url = server + "?command=updateUser&id=" + userId + "&firstname=delete&lastname=me";
            client = new HttpClient();
            method = new GetMethod(url);
            responseCode = client.executeMethod(method);
            s_logger.info("update user response code: " + responseCode);
            if (responseCode == 200) {
                InputStream is = method.getResponseBodyAsStream();
                Map<String, String> success = getSingleValueFromXML(is, new String[] {"success"});
                s_logger.info("update user..success? " + success.get("success"));
            } else {
                s_logger.error("update user failed with error code: " + responseCode + ". Following URL was sent: " + url);
                return responseCode;
            }
        }

        // -----------------------------
        // Execute reboot/stop/start commands for the VMs before deleting the account - made to exercise xen
        // -----------------------------

        //Reboot centos VM
        String encodedApiKey = URLEncoder.encode(s_apiKey.get(), "UTF-8");
        String requestToSign = "apikey=" + encodedApiKey + "&command=rebootVirtualMachine&id=" + s_linuxVmId.get();
        requestToSign = requestToSign.toLowerCase();
        String signature = signRequest(requestToSign, s_secretKey.get());
        String encodedSignature = URLEncoder.encode(signature, "UTF-8");

        url = developerServer + "?command=rebootVirtualMachine&id=" + s_linuxVmId.get() + "&apikey=" + encodedApiKey + "&signature=" + encodedSignature;
        client = new HttpClient();
        method = new GetMethod(url);
        responseCode = client.executeMethod(method);
        s_logger.info("Reboot VM response code: " + responseCode);
        if (responseCode == 200) {
            InputStream input = method.getResponseBodyAsStream();
            Element el = queryAsyncJobResult(server, input);
            Map<String, String> success = getSingleValueFromXML(el, new String[] {"success"});
            s_logger.info("VM was rebooted with the status: " + success.get("success"));
        } else {
            s_logger.error(" VM test failed with error code: " + responseCode + ". Following URL was sent: " + url);
            return responseCode;
        }

        //Stop centos VM
        requestToSign = "apikey=" + encodedApiKey + "&command=stopVirtualMachine&id=" + s_linuxVmId.get();
        requestToSign = requestToSign.toLowerCase();
        signature = signRequest(requestToSign, s_secretKey.get());
        encodedSignature = URLEncoder.encode(signature, "UTF-8");

        url = developerServer + "?command=stopVirtualMachine&id=" + s_linuxVmId.get() + "&apikey=" + encodedApiKey + "&signature=" + encodedSignature;
        client = new HttpClient();
        method = new GetMethod(url);
        responseCode = client.executeMethod(method);
        s_logger.info("Stop VM response code: " + responseCode);
        if (responseCode == 200) {
            InputStream input = method.getResponseBodyAsStream();
            Element el = queryAsyncJobResult(server, input);
            Map<String, String> success = getSingleValueFromXML(el, new String[] {"success"});
            s_logger.info("VM was stopped with the status: " + success.get("success"));
        } else {
            s_logger.error("Stop VM test failed with error code: " + responseCode + ". Following URL was sent: " + url);
            return responseCode;
        }

        //Start centos VM
        requestToSign = "apikey=" + encodedApiKey + "&command=startVirtualMachine&id=" + s_linuxVmId.get();
        requestToSign = requestToSign.toLowerCase();
        signature = signRequest(requestToSign, s_secretKey.get());
        encodedSignature = URLEncoder.encode(signature, "UTF-8");

        url = developerServer + "?command=startVirtualMachine&id=" + s_linuxVmId.get() + "&apikey=" + encodedApiKey + "&signature=" + encodedSignature;
        client = new HttpClient();
        method = new GetMethod(url);
        responseCode = client.executeMethod(method);
        s_logger.info("Start VM response code: " + responseCode);

        if (responseCode == 200) {
            InputStream input = method.getResponseBodyAsStream();
            Element el = queryAsyncJobResult(server, input);
            Map<String, String> success = getSingleValueFromXML(el, new String[] {"id"});

            if (success.get("id") == null) {
                s_logger.info("Start linux vm response code: 401");
                return 401;
            } else {
                s_logger.info("Start vm response code: " + responseCode);
            }

            s_logger.info("VM was started with the status: " + success.get("success"));
        } else {
            s_logger.error("Start VM test failed with error code: " + responseCode + ". Following URL was sent: " + url);
            return responseCode;
        }

////        // -----------------------------
////        // DISABLE USER
////        // -----------------------------
//        {
//            url = server + "?command=disableUser&id=" + userId;
//            client = new HttpClient();
//            method = new GetMethod(url);
//            responseCode = client.executeMethod(method);
//            s_logger.info("disable user response code: " + responseCode);
//            if (responseCode == 200) {
//                InputStream input = method.getResponseBodyAsStream();
//                Element el = queryAsyncJobResult(server, input);
//                s_logger
//                        .info("Disabled user successfully");
//            } else  {
//                s_logger.error("disable user failed with error code: " + responseCode + ". Following URL was sent: " + url);
//                return responseCode;
//            }
//        }

        // -----------------------------
        // DELETE USER
        // -----------------------------
        {
            url = server + "?command=deleteUser&id=" + userId;
            client = new HttpClient();
            method = new GetMethod(url);
            responseCode = client.executeMethod(method);
            s_logger.info("delete user response code: " + responseCode);
            if (responseCode == 200) {
                InputStream input = method.getResponseBodyAsStream();
                Element el = queryAsyncJobResult(server, input);
                s_logger.info("Deleted user successfully");
            } else {
                s_logger.error("delete user failed with error code: " + responseCode + ". Following URL was sent: " + url);
                return responseCode;
            }
        }
        return responseCode;
    }

    private static int executeEventsAndBilling(String server, String developerServer) throws HttpException, IOException {
        // test steps:
        // - get all the events in the system for all users in the system
        // - generate all the usage records in the system
        // - get all the usage records in the system

        // -----------------------------
        // GET EVENTS
        // -----------------------------
        String url = server + "?command=listEvents&page=1&account=" + s_account.get();

        s_logger.info("Getting events for the account " + s_account.get());
        HttpClient client = new HttpClient();
        HttpMethod method = new GetMethod(url);
        int responseCode = client.executeMethod(method);
        s_logger.info("get events response code: " + responseCode);
        if (responseCode == 200) {
            InputStream is = method.getResponseBodyAsStream();
            Map<String, List<String>> eventDescriptions = getMultipleValuesFromXML(is, new String[] {"description"});
            List<String> descriptionText = eventDescriptions.get("description");
            if (descriptionText == null) {
                s_logger.info("no events retrieved...");
            } else {
                for (String text : descriptionText) {
                    s_logger.info("event: " + text);
                }
            }
        } else {
            s_logger.error("list events failed with error code: " + responseCode + ". Following URL was sent: " + url);

            return responseCode;
        }
        return responseCode;
    }

    private static int executeStop(String server, String developerServer, String username) throws HttpException, IOException {
        // test steps:
        // - get userId for the given username
        // - list virtual machines for the user
        // - stop all virtual machines
        // - get ip addresses for the user
        // - release ip addresses

        // -----------------------------
        // GET USER
        // -----------------------------
        String userId = s_userId.get().toString();
        String encodedUserId = URLEncoder.encode(userId, "UTF-8");

        String url = server + "?command=listUsers&id=" + encodedUserId;
        s_logger.info("Stopping resources for user: " + username);
        HttpClient client = new HttpClient();
        HttpMethod method = new GetMethod(url);
        int responseCode = client.executeMethod(method);
        s_logger.info("get user response code: " + responseCode);
        if (responseCode == 200) {
            InputStream is = method.getResponseBodyAsStream();
            Map<String, String> userIdValues = getSingleValueFromXML(is, new String[] {"id"});
            String userIdStr = userIdValues.get("id");
            if (userIdStr != null) {
                userId = userIdStr;
                if (userId == null) {
                    s_logger.error("get user failed to retrieve a valid user id, aborting depolyment test" + ". Following URL was sent: " + url);
                    return -1;
                }
            }
        } else {
            s_logger.error("get user failed with error code: " + responseCode + ". Following URL was sent: " + url);
            return responseCode;
        }

        {
            // ----------------------------------
            // LIST VIRTUAL MACHINES
            // ----------------------------------
            String encodedApiKey = URLEncoder.encode(s_apiKey.get(), "UTF-8");
            String requestToSign = "apikey=" + encodedApiKey + "&command=listVirtualMachines";
            requestToSign = requestToSign.toLowerCase();
            String signature = signRequest(requestToSign, s_secretKey.get());
            String encodedSignature = URLEncoder.encode(signature, "UTF-8");

            url = developerServer + "?command=listVirtualMachines&apikey=" + encodedApiKey + "&signature=" + encodedSignature;

            s_logger.info("Listing all virtual machines for the user with url " + url);
            String[] vmIds = null;
            client = new HttpClient();
            method = new GetMethod(url);
            responseCode = client.executeMethod(method);
            s_logger.info("list virtual machines response code: " + responseCode);
            if (responseCode == 200) {
                InputStream is = method.getResponseBodyAsStream();
                Map<String, List<String>> vmIdValues = getMultipleValuesFromXML(is, new String[] {"id"});
                if (vmIdValues.containsKey("id")) {
                    List<String> vmIdList = vmIdValues.get("id");
                    if (vmIdList != null) {
                        vmIds = new String[vmIdList.size()];
                        vmIdList.toArray(vmIds);
                        String vmIdLogStr = "";
                        if ((vmIds != null) && (vmIds.length > 0)) {
                            vmIdLogStr = vmIds[0];
                            for (int i = 1; i < vmIds.length; i++) {
                                vmIdLogStr = vmIdLogStr + "," + vmIds[i];
                            }
                        }
                        s_logger.info("got virtual machine ids: " + vmIdLogStr);
                    }
                }

            } else {
                s_logger.error("list virtual machines test failed with error code: " + responseCode + ". Following URL was sent: " + url);
                return responseCode;
            }

            // ----------------------------------
            // STOP/DESTROY VIRTUAL MACHINES
            // ----------------------------------
            if (vmIds != null) {
                for (String vmId : vmIds) {
                    requestToSign = "apikey=" + encodedApiKey + "&command=stopVirtualMachine&id=" + vmId;
                    requestToSign = requestToSign.toLowerCase();
                    signature = signRequest(requestToSign, s_secretKey.get());
                    encodedSignature = URLEncoder.encode(signature, "UTF-8");

                    url = developerServer + "?command=stopVirtualMachine&id=" + vmId + "&apikey=" + encodedApiKey + "&signature=" + encodedSignature;
                    client = new HttpClient();
                    method = new GetMethod(url);
                    responseCode = client.executeMethod(method);
                    s_logger.info("StopVirtualMachine" + " [" + vmId + "] response code: " + responseCode);
                    if (responseCode == 200) {
                        InputStream input = method.getResponseBodyAsStream();
                        Element el = queryAsyncJobResult(server, input);
                        Map<String, String> success = getSingleValueFromXML(el, new String[] {"success"});
                        s_logger.info("StopVirtualMachine..success? " + success.get("success"));
                    } else {
                        s_logger.error("Stop virtual machine test failed with error code: " + responseCode + ". Following URL was sent: " + url);
                        return responseCode;
                    }
                }
            }

//            {
//                url = server + "?command=deleteUser&id=" + userId;
//                client = new HttpClient();
//                method = new GetMethod(url);
//                responseCode = client.executeMethod(method);
//                s_logger.info("delete user response code: " + responseCode);
//                if (responseCode == 200) {
//                    InputStream input = method.getResponseBodyAsStream();
//                    Element el = queryAsyncJobResult(server, input);
//                    s_logger
//                            .info("Deleted user successfully");
//                } else  {
//                    s_logger.error("delete user failed with error code: " + responseCode + ". Following URL was sent: " + url);
//                    return responseCode;
//                }
//            }

        }

        s_linuxIP.set("");
        s_linuxVmId.set("");
        s_linuxPassword.set("");
        s_windowsIP.set("");
        s_secretKey.set("");
        s_apiKey.set("");
        s_userId.set(Long.parseLong("0"));
        s_account.set("");
        s_domainRouterId.set("");
        return responseCode;
    }

    public static String signRequest(String request, String key) {
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            SecretKeySpec keySpec = new SecretKeySpec(key.getBytes(), "HmacSHA1");
            mac.init(keySpec);
            mac.update(request.getBytes());
            byte[] encryptedBytes = mac.doFinal();
            return Base64.encodeBase64String(encryptedBytes);
        } catch (Exception ex) {
            s_logger.error("unable to sign request", ex);
        }
        return null;
    }

    private static String sshWinTest(String host) {
        if (host == null) {
            s_logger.info("Did not receive a host back from test, ignoring win ssh test");
            return null;
        }

        // We will retry 5 times before quitting
        int retry = 1;

        while (true) {
            try {
                if (retry > 0) {
                    s_logger.info("Retry attempt : " + retry + " ...sleeping 300 seconds before next attempt. Account is " + s_account.get());
                    Thread.sleep(300000);
                }

                s_logger.info("Attempting to SSH into windows host " + host + " with retry attempt: " + retry + " for account " + s_account.get());

                Connection conn = new Connection(host);
                conn.connect(null, 60000, 60000);

                s_logger.info("User " + s_account.get() + " ssHed successfully into windows host " + host);
                boolean success = false;
                boolean isAuthenticated = conn.authenticateWithPassword("Administrator", "password");
                if (isAuthenticated == false) {
                    return "Authentication failed";
                } else {
                    s_logger.info("Authentication is successfull");
                }

                try {
                    SCPClient scp = new SCPClient(conn);
                    scp.put("wget.exe", "wget.exe", "C:\\Users\\Administrator", "0777");
                    s_logger.info("Successfully put wget.exe file");
                } catch (Exception ex) {
                    s_logger.error("Unable to put wget.exe " + ex);
                }

                if (conn == null) {
                    s_logger.error("Connection is null");
                }
                Session sess = conn.openSession();

                s_logger.info("User + " + s_account.get() + " executing : wget http://192.168.1.250/dump.bin");
                sess.execCommand("wget http://192.168.1.250/dump.bin && dir dump.bin");

                InputStream stdout = sess.getStdout();
                InputStream stderr = sess.getStderr();

                byte[] buffer = new byte[8192];
                while (true) {
                    if ((stdout.available() == 0) && (stderr.available() == 0)) {
                        int conditions = sess.waitForCondition(ChannelCondition.STDOUT_DATA | ChannelCondition.STDERR_DATA | ChannelCondition.EOF, 120000);

                        if ((conditions & ChannelCondition.TIMEOUT) != 0) {
                            s_logger.info("Timeout while waiting for data from peer.");
                            return null;
                        }

                        if ((conditions & ChannelCondition.EOF) != 0) {
                            if ((conditions & (ChannelCondition.STDOUT_DATA | ChannelCondition.STDERR_DATA)) == 0) {
                                break;
                            }
                        }
                    }

                    while (stdout.available() > 0) {
                        success = true;
                        int len = stdout.read(buffer);
                        if (len > 0) // this check is somewhat paranoid
                            s_logger.info(new String(buffer, 0, len));
                    }

                    while (stderr.available() > 0) {
                        /* int len = */stderr.read(buffer);
                    }
                }
                sess.close();
                conn.close();

                if (success) {
                    Thread.sleep(120000);
                    return null;
                } else {
                    retry++;
                    if (retry == MAX_RETRY_WIN) {
                        return "SSH Windows Network test fail for account " + s_account.get();
                    }
                }
            } catch (Exception e) {
                s_logger.error(e);
                retry++;
                if (retry == MAX_RETRY_WIN) {
                    return "SSH Windows Network test fail with error " + e.getMessage();
                }
            }
        }
    }

    private static String sshTest(String host, String password) {
        int i = 0;
        if (host == null) {
            s_logger.info("Did not receive a host back from test, ignoring ssh test");
            return null;
        }

        if (password == null) {
            s_logger.info("Did not receive a password back from test, ignoring ssh test");
            return null;
        }

        // We will retry 5 times before quitting
        String result = null;
        int retry = 0;

        while (true) {
            try {
                if (retry > 0) {
                    s_logger.info("Retry attempt : " + retry + " ...sleeping 120 seconds before next attempt. Account is " + s_account.get());
                    Thread.sleep(120000);
                }

                s_logger.info("Attempting to SSH into linux host " + host + " with retry attempt: " + retry + ". Account is " + s_account.get());

                Connection conn = new Connection(host);
                conn.connect(null, 60000, 60000);

                s_logger.info("User + " + s_account.get() + " ssHed successfully into linux host " + host);

                boolean isAuthenticated = conn.authenticateWithPassword("root", password);

                if (isAuthenticated == false) {
                    s_logger.info("Authentication failed for root with password" + password);
                    return "Authentication failed";

                }

                boolean success = false;
                String linuxCommand = null;

                if (i % 10 == 0)
                    linuxCommand = "rm -rf *; wget http://192.168.1.250/dump.bin && ls -al dump.bin";
                else
                    linuxCommand = "wget http://192.168.1.250/dump.bin && ls -al dump.bin";

                Session sess = conn.openSession();
                s_logger.info("User " + s_account.get() + " executing : " + linuxCommand);
                sess.execCommand(linuxCommand);

                InputStream stdout = sess.getStdout();
                InputStream stderr = sess.getStderr();

                byte[] buffer = new byte[8192];
                while (true) {
                    if ((stdout.available() == 0) && (stderr.available() == 0)) {
                        int conditions = sess.waitForCondition(ChannelCondition.STDOUT_DATA | ChannelCondition.STDERR_DATA | ChannelCondition.EOF, 120000);

                        if ((conditions & ChannelCondition.TIMEOUT) != 0) {
                            s_logger.info("Timeout while waiting for data from peer.");
                            return null;
                        }

                        if ((conditions & ChannelCondition.EOF) != 0) {
                            if ((conditions & (ChannelCondition.STDOUT_DATA | ChannelCondition.STDERR_DATA)) == 0) {
                                break;
                            }
                        }
                    }

                    while (stdout.available() > 0) {
                        success = true;
                        int len = stdout.read(buffer);
                        if (len > 0) // this check is somewhat paranoid
                            s_logger.info(new String(buffer, 0, len));
                    }

                    while (stderr.available() > 0) {
                        /* int len = */stderr.read(buffer);
                    }
                }

                sess.close();
                conn.close();

                if (!success) {
                    retry++;
                    if (retry == MAX_RETRY_LINUX) {
                        result = "SSH Linux Network test fail";
                    }
                }

                return result;
            } catch (Exception e) {
                retry++;
                s_logger.error("SSH Linux Network test fail with error");
                if (retry == MAX_RETRY_LINUX) {
                    return "SSH Linux Network test fail with error " + e.getMessage();
                }
            }
            i++;
        }
    }

    public static String createMD5Password(String password) {
        MessageDigest md5;

        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new CloudRuntimeException("Error", e);
        }

        md5.reset();
        BigInteger pwInt = new BigInteger(1, md5.digest(password.getBytes()));

        // make sure our MD5 hash value is 32 digits long...
        StringBuffer sb = new StringBuffer();
        String pwStr = pwInt.toString(16);
        int padding = 32 - pwStr.length();
        for (int i = 0; i < padding; i++) {
            sb.append('0');
        }
        sb.append(pwStr);
        return sb.toString();
    }

    public static Element queryAsyncJobResult(String host, InputStream inputStream) {
        Element returnBody = null;

        Map<String, String> values = getSingleValueFromXML(inputStream, new String[] {"jobid"});
        String jobId = values.get("jobid");

        if (jobId == null) {
            s_logger.error("Unable to get a jobId");
            return null;
        }

        //s_logger.info("Job id is " + jobId);
        String resultUrl = host + "?command=queryAsyncJobResult&jobid=" + jobId;
        HttpClient client = new HttpClient();
        HttpMethod method = new GetMethod(resultUrl);
        while (true) {
            try {
                client.executeMethod(method);
                //s_logger.info("Method is executed successfully. Following url was sent " + resultUrl);
                InputStream is = method.getResponseBodyAsStream();
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                Document doc = builder.parse(is);
                returnBody = doc.getDocumentElement();
                doc.getDocumentElement().normalize();
                Element jobStatusTag = (Element)returnBody.getElementsByTagName("jobstatus").item(0);
                String jobStatus = jobStatusTag.getTextContent();
                if (jobStatus.equals("0")) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        s_logger.debug("[ignored] interupted while during async job result query.");
                    }
                } else {
                    break;
                }

            } catch (Exception ex) {
                s_logger.error(ex);
            }
        }
        return returnBody;
    }

}
