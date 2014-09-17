//
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

package com.cloud.utils.xmlobject;

import org.junit.Test;

public class TestXmlObject {

    void p(String str) {
        System.out.println(str);
    }

    @Test
    public void test() {

        // deprecated, since we no longer use component.xml.in any more
        /*
            XmlObject xo = XmlObjectParser.parseFromFile("z:/components.xml.in");
            p(xo.getTag());
            p((String) xo.get("system-integrity-checker.checker").toString());
            List<XmlObject> lst = xo.get("management-server.adapters");
            for (XmlObject x : lst) {
                List<XmlObject> lst1 = x.getAsList("adapter");
                for (XmlObject y : lst1) {
                    p(y.toString());
                }
            }
            */

        XmlObject xml = new XmlObject("vlan").putElement("vlan-id", String.valueOf(19)).putElement("tagged",
                new XmlObject("teng").putElement("name", "0/0")
        ).putElement("shutdown", "false");
        System.out.println(xml.toString());
    }

}
