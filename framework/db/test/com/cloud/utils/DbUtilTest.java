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
