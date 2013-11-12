// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// the License.  You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package com.cloud.utils;

import javax.persistence.Column;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import com.cloud.utils.db.DbUtil;

public class DbUtilTest {

    static class Testbean {
        String noAnnotation;
        @Column()
        String withAnnotation;
        @Column(name = "surprise")
        String withAnnotationAndName;
    }

    @Test
    public void getColumnName() throws SecurityException, NoSuchFieldException {
        // if no annotation, then the field name
        Assert.assertEquals("noAnnotation", DbUtil.getColumnName(Testbean.class
                .getDeclaredField("noAnnotation")));
        // there is annotation with name, take the name
        Assert.assertEquals("surprise", DbUtil.getColumnName(Testbean.class
                .getDeclaredField("withAnnotationAndName")));
    }

    @Test
    @Ignore
    public void getColumnNameWithAnnotationButWithoutNameAttribute()
            throws SecurityException, NoSuchFieldException {
        // there is annotation, but no name defined, fallback to field name
        // this does not work this way, it probably should
        Assert.assertEquals("withAnnotation", DbUtil
                .getColumnName(Testbean.class
                        .getDeclaredField("withAnnotation")));

    }

    static class IsPersistableTestBean {
        static final String staticFinal = "no";
        final String justFinal = "no";
        transient String transientField;
        transient static String strange = "";
        String instanceField;
    }

    @Test
    public void isPersistable() throws SecurityException, NoSuchFieldException {
        Assert.assertFalse(DbUtil.isPersistable(IsPersistableTestBean.class
                .getDeclaredField("staticFinal")));
        Assert.assertFalse(DbUtil.isPersistable(IsPersistableTestBean.class
                .getDeclaredField("justFinal")));
        Assert.assertFalse(DbUtil.isPersistable(IsPersistableTestBean.class
                .getDeclaredField("transientField")));
        Assert.assertFalse(DbUtil.isPersistable(IsPersistableTestBean.class
                .getDeclaredField("strange")));
        Assert.assertTrue(DbUtil.isPersistable(IsPersistableTestBean.class
                .getDeclaredField("instanceField")));
    }

}
