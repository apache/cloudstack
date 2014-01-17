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
package com.cloud.bridge.util;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.security.SignatureException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Iterator;
import java.util.TreeMap;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;

public class EC2RestAuth {
    protected final static Logger logger = Logger.getLogger(RestAuth.class);

    // TreeMap: used to Sort the UTF-8 query string components by parameter name with natural byte ordering
    protected TreeMap<String, String> queryParts = null;   // used to generate a CanonicalizedQueryString
    protected String canonicalizedQueryString = null;
    protected String hostHeader = null;
    protected String httpRequestURI = null;

    public EC2RestAuth() {
        // these must be lexicographically sorted
        queryParts = new TreeMap<String, String>();
    }

    public static Calendar parseDateString(String created) {
        DateFormat formatter = null;
        Calendar cal = Calendar.getInstance();

        // -> for some unknown reason SimpleDateFormat does not properly handle the 'Z' timezone
        if (created.endsWith("Z"))
            created = created.replace("Z", "+0000");

        try {
            formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssz");
            cal.setTime(formatter.parse(created));
            return cal;
        } catch (Exception e) {
        }

        try {
            formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
            cal.setTime(formatter.parse(created));
            return cal;
        } catch (Exception e) {
        }

        // -> the time zone is GMT if not defined
        try {
            formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            cal.setTime(formatter.parse(created));

            created = created + "+0000";
            formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
            cal.setTime(formatter.parse(created));
            return cal;
        } catch (Exception e) {
        }

        // -> including millseconds?
        try {
            formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.Sz");
            cal.setTime(formatter.parse(created));
            return cal;
        } catch (Exception e) {
        }

        try {
            formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SZ");
            cal.setTime(formatter.parse(created));
            return cal;
        } catch (Exception e) {
        }

        // -> the CloudStack API used to return this format for some calls
        try {
            formatter = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy");
            cal.setTime(formatter.parse(created));
            return cal;
        } catch (Exception e) {
        }

        return null;
    }

    /**
     * Assuming that a port number is to be included.
     *
     * @param header - contents of the "Host:" header, skipping the 'Host:' preamble.
     */
    public void setHostHeader(String hostHeader) {
        if (null == hostHeader)
            this.hostHeader = null;
        else
            this.hostHeader = hostHeader.trim().toLowerCase();
    }

    public void setHTTPRequestURI(String uri) {
        if (null == uri || 0 == uri.length())
            this.httpRequestURI = new String("/");
        else
            this.httpRequestURI = uri.trim();
    }

    /**
     * The given query string needs to be pulled apart, sorted by paramter name, and reconstructed.
     * We sort the query string values via a TreeMap.
     *
     * @param query - this string still has all URL encoding in place.
     */
    public void setQueryString(String query) {
        String parameter = null;

        if (null == query) {
            this.canonicalizedQueryString = null;
            return;
        }

        // -> sort by paramter name
        String[] parts = query.split("&");
        if (null != parts) {
            for (int i = 0; i < parts.length; i++) {

                parameter = parts[i];

                if (parameter.startsWith("?"))
                    parameter = parameter.substring(1);

                // -> don't include a 'Signature=' parameter
                if (parameter.startsWith("Signature="))
                    continue;

                int offset = parameter.indexOf("=");
                if (-1 == offset)
                    queryParts.put(parameter, parameter + "=");
                else
                    queryParts.put(parameter.substring(0, offset), parameter);
            }
        }

        // -> reconstruct into a canonicalized format
        Collection<String> headers = queryParts.values();
        Iterator<String> itr = headers.iterator();
        StringBuffer reconstruct = new StringBuffer();
        int count = 0;

        while (itr.hasNext()) {
            if (0 < count)
                reconstruct.append("&");
            reconstruct.append(itr.next());
            count++;
        }
        canonicalizedQueryString = reconstruct.toString();
    }

    /**
     * The request is authenticated if we can regenerate the same signature given
     * on the request.  Before calling this function make sure to set the header values
     * defined by the public values above.
     *
     * @param httpVerb  - the type of HTTP request (e.g., GET, PUT)
     * @param secretKey - value obtained from the AWSAccessKeyId
     * @param signature - the signature we are trying to recreate, note can be URL-encoded
     * @param method    - { "HmacSHA1", "HmacSHA256" }
     *
     * @throws SignatureException
     *
     * @return true if request has been authenticated, false otherwise
     * @throws UnsupportedEncodingException
     */
    public boolean verifySignature(String httpVerb, String secretKey, String signature, String method) throws SignatureException, UnsupportedEncodingException {

        if (null == httpVerb || null == secretKey || null == signature)
            return false;

        httpVerb = httpVerb.trim();
        secretKey = secretKey.trim();
        signature = signature.trim();

        // -> first calculate the StringToSign after the caller has initialized all the header values
        String StringToSign = genStringToSign(httpVerb);
        String calSig = calculateRFC2104HMAC(StringToSign, secretKey, method.equalsIgnoreCase("HmacSHA1"));

        // -> the passed in signature is defined to be URL encoded? (and it must be base64 encoded)
        int offset = signature.indexOf("%");
        if (-1 != offset)
            signature = URLDecoder.decode(signature, "UTF-8");

        boolean match = signature.equals(calSig);
        if (!match)
            logger.error("Signature mismatch, [" + signature + "] [" + calSig + "] over [" + StringToSign + "]");
        return match;
    }

    /**
     * This function generates the single string that will be used to sign with a users
     * secret key.
     *
     * StringToSign = HTTP-Verb + "\n" +
     * ValueOfHostHeaderInLowercase + "\n" +
     * HTTPRequestURI + "\n" +
     * CanonicalizedQueryString
     *
     * @return The single StringToSign or null.
     */
    private String genStringToSign(String httpVerb) {
        StringBuffer stringToSign = new StringBuffer();

        stringToSign.append(httpVerb).append("\n");

        if (null != this.hostHeader)
            stringToSign.append(this.hostHeader);
        stringToSign.append("\n");

        if (null != this.httpRequestURI)
            stringToSign.append(this.httpRequestURI);
        stringToSign.append("\n");

        if (null != this.canonicalizedQueryString)
            stringToSign.append(this.canonicalizedQueryString);

        if (0 == stringToSign.length())
            return null;
        else
            return stringToSign.toString();
    }

    /**
     * Create a signature by the following method:
     *     new String( Base64( SHA1 or SHA256 ( key, byte array )))
     *
     * @param signIt    - the data to generate a keyed HMAC over
     * @param secretKey - the user's unique key for the HMAC operation
     * @param useSHA1   - if false use SHA256
     * @return String   - the recalculated string
     * @throws SignatureException
     */
    private String calculateRFC2104HMAC(String signIt, String secretKey, boolean useSHA1) throws SignatureException {
        SecretKeySpec key = null;
        Mac hmacShaAlg = null;
        String result = null;

        try {
            if (useSHA1) {
                key = new SecretKeySpec(secretKey.getBytes(), "HmacSHA1");
                hmacShaAlg = Mac.getInstance("HmacSHA1");
            } else {
                key = new SecretKeySpec(secretKey.getBytes(), "HmacSHA256");
                hmacShaAlg = Mac.getInstance("HmacSHA256");
            }

            hmacShaAlg.init(key);
            byte[] rawHmac = hmacShaAlg.doFinal(signIt.getBytes());
            result = new String(Base64.encodeBase64(rawHmac));

        } catch (Exception e) {
            throw new SignatureException("Failed to generate keyed HMAC on REST request: " + e.getMessage());
        }
        return result.trim();
    }
}
