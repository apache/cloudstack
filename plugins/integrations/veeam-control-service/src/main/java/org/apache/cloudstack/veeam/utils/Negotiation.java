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

package org.apache.cloudstack.veeam.utils;


import javax.servlet.http.HttpServletRequest;

public final class Negotiation {

    public enum OutFormat { XML, JSON }

    public static OutFormat responseFormat(HttpServletRequest req) {
        String accept = req.getHeader("Accept");
        if (accept == null || accept.isBlank() || accept.contains("*/*")) {
            return OutFormat.XML;
        }
        accept = accept.toLowerCase();
        if (accept.contains("application/json")) return OutFormat.JSON;
        if (accept.contains("application/xml") || accept.contains("text/xml")) {
            return OutFormat.XML;
        }
        return OutFormat.XML;
    }

    public static String contentType(OutFormat fmt) {
        return fmt == OutFormat.JSON
                ? "application/json"
                : "application/xml";
    }
}
