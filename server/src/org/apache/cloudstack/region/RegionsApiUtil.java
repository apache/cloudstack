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
package org.apache.cloudstack.region;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.log4j.Logger;

import com.cloud.domain.DomainVO;
import com.cloud.user.UserAccount;
import com.cloud.user.UserAccountVO;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

/**
 * Utility class for making API calls between peer Regions
 *
 */
public class RegionsApiUtil {
    public static final Logger s_logger = Logger.getLogger(RegionsApiUtil.class);

    /**
     * Makes an api call using region service end_point, api command and params
     * @param region
     * @param command
     * @param params
     * @return True, if api is successful
     */
    protected static boolean makeAPICall(Region region, String command, List<NameValuePair> params){
        try {
            String apiParams = buildParams(command, params);
            String url = buildUrl(apiParams, region);
            HttpClient client = new HttpClient();
            HttpMethod method = new GetMethod(url);
            if( client.executeMethod(method) == 200){
                return true;
            } else {
                return false;
            }
        } catch (HttpException e) {
            s_logger.error(e.getMessage());
            return false;
        } catch (IOException e) {
            s_logger.error(e.getMessage());
            return false;
        }
    }

    /**
     * Makes an api call using region service end_point, api command and params
     * Returns Account object on success
     * @param region
     * @param command
     * @param params
     * @return
     */
    protected static RegionAccount makeAccountAPICall(Region region, String command, List<NameValuePair> params){
        try {
            String url = buildUrl(buildParams(command, params), region);
            HttpClient client = new HttpClient();
            HttpMethod method = new GetMethod(url);
            if( client.executeMethod(method) == 200){
                InputStream is = method.getResponseBodyAsStream();
                //Translate response to Account object
                XStream xstream = new XStream(new DomDriver());
                xstream.alias("account", RegionAccount.class);
                xstream.alias("user", RegionUser.class);
                xstream.aliasField("id", RegionAccount.class, "uuid");
                xstream.aliasField("name", RegionAccount.class, "accountName");
                xstream.aliasField("accounttype", RegionAccount.class, "type");
                xstream.aliasField("domainid", RegionAccount.class, "domainUuid");
                xstream.aliasField("networkdomain", RegionAccount.class, "networkDomain");
                xstream.aliasField("id", RegionUser.class, "uuid");
                xstream.aliasField("accountId", RegionUser.class, "accountUuid");
                ObjectInputStream in = xstream.createObjectInputStream(is);
                return (RegionAccount)in.readObject();
            } else {
                return null;
            }
        } catch (HttpException e) {
            s_logger.error(e.getMessage());
            return null;
        } catch (IOException e) {
            s_logger.error(e.getMessage());
            return null;
        } catch (ClassNotFoundException e) {
            s_logger.error(e.getMessage());
            return null;
        }
    }

    /**
     * Makes an api call using region service end_point, api command and params
     * Returns Domain object on success
     * @param region
     * @param command
     * @param params
     * @return
     */
    protected static RegionDomain makeDomainAPICall(Region region, String command, List<NameValuePair> params){
        try {
            String url = buildUrl(buildParams(command, params), region);
            HttpClient client = new HttpClient();
            HttpMethod method = new GetMethod(url);
            if( client.executeMethod(method) == 200){
                InputStream is = method.getResponseBodyAsStream();
                XStream xstream = new XStream(new DomDriver());
                //Translate response to Domain object
                xstream.alias("domain", RegionDomain.class);
                xstream.aliasField("id", RegionDomain.class, "uuid");
                xstream.aliasField("parentdomainid", RegionDomain.class, "parentUuid");
                xstream.aliasField("networkdomain", DomainVO.class, "networkDomain");
                ObjectInputStream in = xstream.createObjectInputStream(is);
                return (RegionDomain)in.readObject();
            } else {
                return null;
            }
        } catch (HttpException e) {
            s_logger.error(e.getMessage());
            return null;
        } catch (IOException e) {
            s_logger.error(e.getMessage());
            return null;
        } catch (ClassNotFoundException e) {
            s_logger.error(e.getMessage());
            return null;
        }
    }

    /**
     * Makes an api call using region service end_point, api command and params
     * Returns UserAccount object on success
     * @param region
     * @param command
     * @param params
     * @return
     */
    protected static UserAccount makeUserAccountAPICall(Region region, String command, List<NameValuePair> params){
        try {
            String url = buildUrl(buildParams(command, params), region);
            HttpClient client = new HttpClient();
            HttpMethod method = new GetMethod(url);
            if( client.executeMethod(method) == 200){
                InputStream is = method.getResponseBodyAsStream();
                XStream xstream = new XStream(new DomDriver());
                xstream.alias("useraccount", UserAccountVO.class);
                xstream.aliasField("id", UserAccountVO.class, "uuid");
                ObjectInputStream in = xstream.createObjectInputStream(is);
                return (UserAccountVO)in.readObject();
            } else {
                return null;
            }
        } catch (HttpException e) {
            s_logger.error(e.getMessage());
            return null;
        } catch (IOException e) {
            s_logger.error(e.getMessage());
            return null;
        } catch (ClassNotFoundException e) {
            s_logger.error(e.getMessage());
            return null;
        }
    }

    /**
     * Builds parameters string with command and encoded param values
     * @param command
     * @param params
     * @return
     */
    protected static String buildParams(String command, List<NameValuePair> params) {
        StringBuffer paramString = new StringBuffer("command="+command);
        Iterator<NameValuePair> iter = params.iterator();
        try {
            while(iter.hasNext()){
                NameValuePair param = iter.next();
                if(param.getValue() != null && !(param.getValue().isEmpty())){
                    paramString.append("&"+param.getName()+"="+URLEncoder.encode(param.getValue(), "UTF-8"));
                }
            }
        }
        catch (UnsupportedEncodingException e) {
            s_logger.error(e.getMessage());
            return null;
        }
        return paramString.toString();
    }

    /**
     * Build URL for api call using region end_point
     * Parameters are sorted and signed using secret_key
     * @param apiParams
     * @param region
     * @return
     */
    private static String buildUrl(String apiParams, Region region) {

        String apiKey = "";
        String secretKey = "";


        if (apiKey == null || secretKey == null) {
            return region.getEndPoint() +"?"+ apiParams;
        }

        String encodedApiKey;
        try {
            encodedApiKey = URLEncoder.encode(apiKey, "UTF-8");

            List<String> sortedParams = new ArrayList<String>();
            sortedParams.add("apikey=" + encodedApiKey.toLowerCase());
            StringTokenizer st = new StringTokenizer(apiParams, "&");
            String url = null;
            boolean first = true;
            while (st.hasMoreTokens()) {
                String paramValue = st.nextToken();
                String param = paramValue.substring(0, paramValue.indexOf("="));
                String value = paramValue.substring(paramValue.indexOf("=") + 1, paramValue.length());
                if (first) {
                    url = param + "=" + value;
                    first = false;
                } else {
                    url = url + "&" + param + "=" + value;
                }
                sortedParams.add(param.toLowerCase() + "=" + value.toLowerCase());
            }
            Collections.sort(sortedParams);


            //Construct the sorted URL and sign and URL encode the sorted URL with your secret key
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
            String encodedSignature = signRequest(sortedUrl, secretKey);

            String finalUrl = region.getEndPoint() +"?"+apiParams+ "&apiKey=" + apiKey + "&signature=" + encodedSignature;

            return finalUrl;

        } catch (UnsupportedEncodingException e) {
            s_logger.error(e.getMessage());
            return null;
        }
    }

    /**
     * 1. Signs a string with a secret key using SHA-1 2. Base64 encode the result 3. URL encode the final result
     *
     * @param request
     * @param key
     * @return
     */
    private static String signRequest(String request, String key) {
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            SecretKeySpec keySpec = new SecretKeySpec(key.getBytes(), "HmacSHA1");
            mac.init(keySpec);
            mac.update(request.getBytes());
            byte[] encryptedBytes = mac.doFinal();
            return URLEncoder.encode(Base64.encodeBase64String(encryptedBytes), "UTF-8");
        } catch (Exception ex) {
            s_logger.error(ex.getMessage());
            return null;
        }
    }

}