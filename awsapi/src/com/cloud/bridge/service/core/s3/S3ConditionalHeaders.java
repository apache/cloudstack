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
package com.cloud.bridge.service.core.s3;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class S3ConditionalHeaders {
    protected Date modifiedSince;
    protected Date unmodifiedSince;
    protected String[] ifMatch;
    protected String[] ifNoneMatch;

    public S3ConditionalHeaders() {
        modifiedSince = null;
        unmodifiedSince = null;
        ifMatch = null;
        ifNoneMatch = null;
    }

    public void setModifiedSince(Calendar ifModifiedSince) {
        if (null != ifModifiedSince)
            modifiedSince = ifModifiedSince.getTime();
    }

    public void setUnModifiedSince(Calendar ifUnmodifiedSince) {
        if (null != ifUnmodifiedSince)
            unmodifiedSince = ifUnmodifiedSince.getTime();
    }

    public void setModifiedSince(String ifModifiedSince) {
        DateFormat formatter = null;
        Calendar cal = Calendar.getInstance();

        try {
            formatter = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z");
            cal.setTime(formatter.parse(ifModifiedSince));
            modifiedSince = cal.getTime();
        } catch (Exception e) {
        }
    }

    public void setUnModifiedSince(String ifUnmodifiedSince) {
        DateFormat formatter = null;
        Calendar cal = Calendar.getInstance();

        try {
            formatter = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z");
            cal.setTime(formatter.parse(ifUnmodifiedSince));
            unmodifiedSince = cal.getTime();
        } catch (Exception e) {
        }
    }

    public void setMatch(String[] ifMatch) {
        this.ifMatch = ifMatch;
    }

    /**
     * Takes the header value from HTTP "If-Match", for example is:
     * If-Match: "xyzzy", "r2d2xxxx", "c3piozzzz"
     *
     * @param ifMatch
     */
    public void setMatch(String ifMatch) {
        if (null == ifMatch) {
            this.ifMatch = null;
        } else {
            String[] parts = ifMatch.split(",");
            this.ifMatch = new String[parts.length];
            for (int i = 0; i < parts.length; i++) {
                String temp = new String(parts[i].trim());
                if (temp.startsWith("\""))
                    temp = temp.substring(1);
                if (temp.endsWith("\""))
                    temp = temp.substring(0, temp.length() - 1);
                this.ifMatch[i] = temp;
            }
        }
    }

    public void setNoneMatch(String[] ifNoneMatch) {
        this.ifNoneMatch = ifNoneMatch;
    }

    public void setNoneMatch(String ifNoneMatch) {
        if (null == ifNoneMatch) {
            this.ifNoneMatch = null;
        } else {
            String[] parts = ifNoneMatch.split(",");
            this.ifNoneMatch = new String[parts.length];
            for (int i = 0; i < parts.length; i++) {
                String temp = new String(parts[i].trim());
                if (temp.startsWith("\""))
                    temp = temp.substring(1);
                if (temp.endsWith("\""))
                    temp = temp.substring(0, temp.length() - 1);
                this.ifNoneMatch[i] = temp;
                System.out.println(i + "> " + temp);
            }
        }
    }

    /**
     * Has the object been modified since the client has last checked?
     *
     * @param lastModified
     * @return a negative value means that the object has not been modified since
     *         a postive  value means that this test should be ignored.
     */
    public int ifModifiedSince(Date lastModified) {
        if (null == modifiedSince)
            return 1;

        if (0 >= modifiedSince.compareTo(lastModified))
            return 2;
        else
            return -1;
    }

    /**
     * Has the object been modified since the unmodified date?
     *
     * @param lastModified
     * @return a negative value means that the object has been modified since
     *         a postive  value means that this test should be ignored.
     */
    public int ifUnmodifiedSince(Date lastModified) {
        if (null == unmodifiedSince)
            return 1;

        if (0 < unmodifiedSince.compareTo(lastModified))
            return 2;
        else
            return -1;
    }

    /**
     * Does the object's contents (its MD5 signature) match what the client thinks
     * it is?
     *
     * @param ETag - an MD5 signature of the content of the data being stored in S3
     * @return a negative value means that the test has failed,
     *         a positive value means that the test succeeded or could not be done (so ignore it)
     */
    public int ifMatchEtag(String ETag) {
        if (null == ifMatch || null == ETag)
            return 1;

        for (int i = 0; i < ifMatch.length; i++) {
            if (ifMatch[i].equals(ETag))
                return 2;
        }
        return -1;
    }

    /**
     * None of the given ETags in the "If-None-Match" can match the ETag parameter for this
     * function to pass.
     *
     * @param ETag - an MD5 signature of the content of the data being stored in S3
     * @return a negative value means that the test has failed,
     *         a positive value means that the test succeeded or could not be done (so ignore it)
     */
    public int ifNoneMatchEtag(String ETag) {
        if (null == ifNoneMatch || null == ETag)
            return 1;

        for (int i = 0; i < ifNoneMatch.length; i++) {
            if (ifNoneMatch[i].equals(ETag))
                return -1;
        }
        return 2;
    }
}
