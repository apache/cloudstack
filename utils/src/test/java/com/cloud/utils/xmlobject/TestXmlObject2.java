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

public class TestXmlObject2 {
    void p(String str) {
        System.out.println(str);
    }

    XmlObject xo(String name) {
        return new XmlObject(name);
    }

    @Test
    public void test() {
        XmlObject root = new XmlObject("test");
        root.putElement("key1", "value1").putElement("key2", "value2");
        p(root.dump());

        XmlObject c1 = new XmlObject("child1");
        XmlObject c2 = new XmlObject("child2");
        c2.putElement("ckey1", "value1");
        c1.putElement(c2.getTag(), c2);
        root.putElement(c1.getTag(), c1);
        p(root.dump());

        root =
            xo("test2").putElement("key1", "value1")
                .putElement("child1", xo("child1").setText("yyy"))
                .putElement("child1", xo("child1").putElement("child2", xo("child2").putElement("child3", xo("child3").putElement("key3", "value3").setText("xxxxx"))));

        p(root.dump());
    }

}
