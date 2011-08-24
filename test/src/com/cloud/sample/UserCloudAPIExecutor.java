package com.cloud.sample;

import java.io.FileInputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import com.cloud.utils.encoding.Base64;

/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see .
 * 
 */

/**
 * Sample CloudStack Management User API Executor.
 * 
 * Prerequisites: - Edit usercloud.properties to include your host, apiUrl, apiKey, and secretKey - Use ./executeUserAPI.sh to
 * execute this test class
 * 
 * @author will
 * 
 */
public class UserCloudAPIExecutor {
    public static void main(String[] args) {
        // Host
        String host = null;

        // Fully qualified URL with http(s)://host:port
        String apiUrl = null;

        // ApiKey and secretKey as given by your CloudStack vendor
        String apiKey = null;
        String secretKey = null;

        try {
            Properties prop = new Properties();
            prop.load(new FileInputStream("usercloud.properties"));

            // host
            host = prop.getProperty("host");
            if (host == null) {
                System.out.println("Please specify a valid host in the format of http(s)://:/client/api in your usercloud.properties file.");
            }

            // apiUrl
            apiUrl = prop.getProperty("apiUrl");
            if (apiUrl == null) {
                System.out.println("Please specify a valid API URL in the format of command=&param1=&param2=... in your usercloud.properties file.");
            }

            // apiKey
            apiKey = prop.getProperty("apiKey");
            if (apiKey == null) {
                System.out.println("Please specify your API Key as provided by your CloudStack vendor in your usercloud.properties file.");
            }

            // secretKey
            secretKey = prop.getProperty("secretKey");
            if (secretKey == null) {
                System.out.println("Please specify your secret Key as provided by your CloudStack vendor in your usercloud.properties file.");
            }

            if (apiUrl == null || apiKey == null || secretKey == null) {
                return;
            }

            System.out.println("Constructing API call to host = '" + host + "' with API command = '" + apiUrl + "' using apiKey = '" + apiKey + "' and secretKey = '" + secretKey + "'");

            // Step 1: Make sure your APIKey is URL encoded
            String encodedApiKey = URLEncoder.encode(apiKey, "UTF-8");

            // Step 2: URL encode each parameter value, then sort the parameters and apiKey in
            // alphabetical order, and then toLowerCase all the parameters, parameter values and apiKey.
            // Please note that if any parameters with a '&' as a value will cause this test client to fail since we are using
            // '&' to delimit
            // the string
            List<String> sortedParams = new ArrayList<String>();
            sortedParams.add("apikey=" + encodedApiKey.toLowerCase());
            StringTokenizer st = new StringTokenizer(apiUrl, "&");
            String url = null;
            boolean first = true;
            while (st.hasMoreTokens()) {
                String paramValue = st.nextToken();
                String param = paramValue.substring(0, paramValue.indexOf("="));
                String value = URLEncoder.encode(paramValue.substring(paramValue.indexOf("=") + 1, paramValue.length()), "UTF-8");
                if (first) {
                    url = param + "=" + value;
                    first = false;
                } else {
                    url = url + "&" + param + "=" + value;
                }
                sortedParams.add(param.toLowerCase() + "=" + value.toLowerCase());
            }
            Collections.sort(sortedParams);

            System.out.println("Sorted Parameters: " + sortedParams);

            // Step 3: Construct the sorted URL and sign and URL encode the sorted URL with your secret key
            String sortedUrl = null;
            first = true;
            for (String param : sortedParams) {
                if (first) {
                    sortedUrl = param;
                    first = false;
                } else {
                    sortedUrl = sortedUrl + "&" + param;
                }
            }
            System.out.println("sorted URL : " + sortedUrl);
            String encodedSignature = signRequest(sortedUrl, secretKey);

            // Step 4: Construct the final URL we want to send to the CloudStack Management Server
            // Final result should look like:
            // http(s)://://client/api?&apiKey=&signature=
            String finalUrl = host + "?" + url + "&apiKey=" + apiKey + "&signature=" + encodedSignature;
            System.out.println("final URL : " + finalUrl);

            // Step 5: Perform a HTTP GET on this URL to execute the command
            HttpClient client = new HttpClient();
            HttpMethod method = new GetMethod(finalUrl);
            int responseCode = client.executeMethod(method);
            if (responseCode == 200) {
                // SUCCESS!
                System.out.println("Successfully executed command");
            } else {
                // FAILED!
                System.out.println("Unable to execute command with response code: " + responseCode);
            }

        } catch (Throwable t) {
            System.out.println(t);
        }
    }

    /**
     * 1. Signs a string with a secret key using SHA-1 2. Base64 encode the result 3. URL encode the final result
     * 
     * @param request
     * @param key
     * @return
     */
    public static String signRequest(String request, String key) {
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            SecretKeySpec keySpec = new SecretKeySpec(key.getBytes(), "HmacSHA1");
            mac.init(keySpec);
            mac.update(request.getBytes());
            byte[] encryptedBytes = mac.doFinal();
            return URLEncoder.encode(Base64.encodeBytes(encryptedBytes), "UTF-8");
        } catch (Exception ex) {
            System.out.println(ex);
        }
        return null;
    }
}
