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

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import com.cloud.bridge.util.HeaderParam;

/**
 * We need to be able to pass in specific values into the S3 REST authentication algorithm
 * where these values can be obtained from either HTTP headers directly or from the body
 * of a POST request.
 */
public class S3AuthParams {

    private List<HeaderParam> headerList = new ArrayList<HeaderParam>();

    public S3AuthParams() {
    }

    public HeaderParam[] getHeaders() {
        return headerList.toArray(new HeaderParam[0]);
    }

    public void addHeader(HeaderParam param) {
        headerList.add(param);
    }

    public String getHeader(String header) {
        // ToDO - make this look up faster
        ListIterator it = headerList.listIterator();
        while (it.hasNext()) {
            HeaderParam temp = (HeaderParam)it.next();
            if (header.equalsIgnoreCase(temp.getName())) {
                return temp.getValue();
            }
        }
        return null;
    }
}
