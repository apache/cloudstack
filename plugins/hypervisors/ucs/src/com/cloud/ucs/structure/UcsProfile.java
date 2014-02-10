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
//
package com.cloud.ucs.structure;

import java.util.ArrayList;
import java.util.List;

import com.cloud.utils.xmlobject.XmlObject;
import com.cloud.utils.xmlobject.XmlObjectParser;

public class UcsProfile {
    private String dn;

    public static UcsProfile fromXmlObject(XmlObject xo) {
        UcsProfile p = new UcsProfile();
        return xo.evaluateObject(p);
    }

    public static List<UcsProfile> fromXmlString(String xmlstr) {
        List<UcsProfile> ps = new ArrayList<UcsProfile>();
        XmlObject xo = XmlObjectParser.parseFromString(xmlstr);
        List<XmlObject> xos = xo.getAsList("outDns.dn");
        if (xos != null) {
            for (XmlObject x : xos) {
                //UcsProfile p = UcsProfile.fromXmlObject(x);
                UcsProfile p = new UcsProfile();
                p.setDn(x.get("value").toString());
                ps.add(p);
            }
        }
        return ps;
    }

    public String getDn() {
        return dn;
    }

    public void setDn(String dn) {
        this.dn = dn;
    }
}
