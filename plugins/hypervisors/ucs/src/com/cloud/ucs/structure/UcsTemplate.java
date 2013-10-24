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

import com.cloud.utils.xmlobject.XmlObject;
import com.cloud.utils.xmlobject.XmlObjectParser;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Date: 10/8/13
 * Time: 3:01 PM
 * To change this template use File | Settings | File Templates.
 */
public class UcsTemplate {
    private String dn;

    public static List<UcsTemplate> fromXmlString(String xmlstr) {
        List<UcsTemplate> tmps = new ArrayList<UcsTemplate>();
        XmlObject xo = XmlObjectParser.parseFromString(xmlstr);
        List<XmlObject> xos = xo.getAsList("outConfigs.lsServer");
        if (xos != null) {
            for (XmlObject x : xos) {
                UcsTemplate t = new UcsTemplate();
                t.setDn(x.get("dn").toString());
                tmps.add(t);
            }
        }
        return tmps;
    }

    public String getDn() {
        return dn;
    }

    public void setDn(String dn) {
        this.dn = dn;
    }
}
