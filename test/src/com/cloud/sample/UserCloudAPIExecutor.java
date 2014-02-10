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

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

/**
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */

/**
 * Sample CloudStack Management User API Executor.
 *
 * Prerequisites: - Edit usercloud.properties to include your host, apiUrl, apiKey, and secretKey - Use ./executeUserAPI.sh to
 * execute this test class
 *
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

            System.out.println("Constructing API call to host = '" + host + "' with API command = '" + apiUrl + "' using apiKey = '" + apiKey + "' and secretKey = '" +
                secretKey + "'");

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
            return URLEncoder.encode(Base64.encodeBase64String(encryptedBytes), "UTF-8");
        } catch (Exception ex) {
            System.out.println(ex);
        }
        return null;
    }
}
