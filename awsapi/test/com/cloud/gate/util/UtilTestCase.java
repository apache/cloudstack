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
package com.cloud.gate.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import junit.framework.Assert;

import org.apache.log4j.Logger;

import com.cloud.bridge.util.DateHelper;
import com.cloud.bridge.util.StringHelper;
import com.cloud.bridge.util.XElement;
import com.cloud.bridge.util.XSerializer;
import com.cloud.bridge.util.XSerializerJsonAdapter;
import com.cloud.bridge.util.XSerializerXmlAdapter;
import com.cloud.gate.testcase.BaseTestCase;

class SubFoo {
    @XElement(name = "Name")
    private String name;

    @XElement(name = "Value")
    private String value;

    public SubFoo() {
    }

    public SubFoo(String n, String v) {
        name = n;
        value = v;
    }
}

class BaseFoo {
    @XElement(name = "BaseName")
    private String baseName;

    @XElement(name = "BaseValue")
    private String baseValue;

    public BaseFoo() {
        baseName = "baseName";
        baseValue = "baseValue";
    }
}

class Foo extends BaseFoo {
    @XElement(name = "Name")
    private String name;

    @XElement(name = "Value")
    private String value;

    @XElement(name = "ByteValue")
    private byte bValue;

    @XElement(name = "ShortValue")
    private short sValue;

    @XElement(name = "LongValue")
    private long lValue;

    @XElement(name = "NullValue")
    private String nullValue;

    @XElement(name = "TimeValue")
    private Date dt = new Date();

    @XElement(name = "CalendarValue")
    private Calendar cal = Calendar.getInstance();

    @XElement(name = "SubObject")
    public SubFoo sub;

    @XElement(name = "SubObjects", item = "ListItem", itemClass = "com.cloud.gate.util.SubFoo")
    public List<SubFoo> subs;

    @XElement(name = "ArrayObjects", item = "ArrayItem", itemClass = "com.cloud.gate.util.SubFoo")
    public SubFoo[] subArray;

    public Foo() {
        subs = new ArrayList<SubFoo>();
    }

    public Foo(String name, String value) {
        this.name = name;
        this.value = value;

        subs = new ArrayList<SubFoo>();
    }
}

public class UtilTestCase extends BaseTestCase {
    protected final static Logger logger = Logger.getLogger(UtilTestCase.class);

    public void testStringHelper() {
        String value = StringHelper.substringInBetween("archive/doc1.doc", "archive", "/");
        Assert.assertTrue(value == null);

        value = StringHelper.substringInBetween("archive/sub1/doc1.doc", "archive", "/");
        Assert.assertTrue(value.equals("sub1"));

        value = StringHelper.substringInBetween("archive/sub2/doc1.doc", "archive", "/");
        Assert.assertTrue(value.equals("sub2"));

        value = StringHelper.substringInBetween("archive/sub3/subb/doc1.doc", "archive", "/");
        Assert.assertTrue(value.equals("sub3"));

        value = StringHelper.substringInBetween("archive/sub3/subb/doc1.doc", "archive/sub3", "/");
        Assert.assertTrue(value.equals("subb"));

        value = StringHelper.substringInBetween("archive/sub3/subb/doc1.doc", null, "/");
        Assert.assertTrue(value.equals("archive"));
    }

    public void testJava2XmlJson() {
        XSerializer serializer = new XSerializer(new XSerializerXmlAdapter());
        serializer.setFlattenCollection(true);
        serializer.setOmitNull(true);
        Foo foo = new Foo("dummyName", "dummyValue");
        foo.sub = new SubFoo("subName", "subValue");
        foo.subs.add(new SubFoo("Sub1", "Sub1-value"));
        foo.subs.add(new SubFoo("Sub2", "Sub2-value"));

        foo.subArray = new SubFoo[3];
        foo.subArray[0] = new SubFoo("Array-sub1", "Sub1-value");
        foo.subArray[1] = new SubFoo("Array-sub2", "Sub1-value");
        foo.subArray[2] = new SubFoo("Array-sub3", "Sub1-value");

        String output = serializer.serializeTo(foo, "Foo", "http://www.cloud.com/S3", 0);
        logger.info(output);

        serializer = new XSerializer(new XSerializerJsonAdapter());
        output = serializer.serializeTo(foo, "Foo", "http://www.cloud.com/S3", 0);
        logger.info(output);
    }

    public void testXml2Java() {
        XSerializer serializer = new XSerializer(new XSerializerXmlAdapter());
        serializer.setFlattenCollection(true);
        XSerializer.registerRootType("Foo", Foo.class);

        try {
            InputStream is = this.getClass().getResourceAsStream("/com/cloud/gate/util/Xml2JavaTestData.xml");
            String xml = StringHelper.stringFromStream(is);
            Object object = serializer.serializeFrom(xml);
            if (object != null) {
                String output = serializer.serializeTo(object, "Foo", "http://www.cloud.com/S3", 0);
                logger.info("Redump parsed java object");
                logger.info(output);
            }
            is.close();
        } catch (IOException e) {
            logger.error("Unexpected exception " + e.getMessage(), e);
        }
    }

    public void testMisc() {
        String[] tokens = "/".split("/");
        logger.info("length : " + tokens.length);
        for (int i = 0; i < tokens.length; i++) {
            logger.info("token " + i + ": " + tokens[i]);
        }

        logger.info(DateHelper.getDateDisplayString(DateHelper.GMT_TIMEZONE, new Date(), "E, d MMM yyyy HH:mm:ss z"));
    }
}
