package com.cloud.utils.log;

import junit.framework.TestCase;

import org.apache.log4j.Logger;

import com.cloud.utils.component.ComponentLocator;
import com.cloud.utils.db.DB;
import com.cloud.utils.exception.CloudRuntimeException;


public class CglibThrowableRendererTest extends TestCase {
    private final static Logger s_logger = Logger.getLogger(CglibThrowableRendererTest.class);
    public static class Test {
        @DB
        public void exception() {
            throw new CloudRuntimeException("exception");
        }
    }
    
    public void testException() {
        Test test = ComponentLocator.inject(Test.class);
        try {
            test.exception();
        } catch (Exception e) {
            s_logger.warn("exception caught", e);
        }
    }

}
