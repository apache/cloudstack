package org.apache.cloudstack.storage.test;

import junit.framework.Assert;

import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.springframework.test.context.testng.AbstractTransactionalTestNGSpringContextTests;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.cloud.utils.db.DB;
@ContextConfiguration(locations="classpath:/storageContext.xml")
public class TestNG extends AbstractTestNGSpringContextTests {
    @Test
    @DB
    public void test1() {
        Assert.assertEquals("", "192.168.56.2");
    }
}
