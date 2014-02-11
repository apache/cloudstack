package com.cloud.template;

import org.junit.Test;

import com.cloud.exception.InvalidParameterValueException;

public class TemplateManagerImplTest {

    TemplateManagerImpl tmgr = new TemplateManagerImpl();

    @Test(expected = InvalidParameterValueException.class)
    public void testVerifyTemplateIdOfSystemTemplate() {
        tmgr.verifyTemplateId(1L);
    }

    public void testVerifyTemplateIdOfNonSystemTemplate() {
        tmgr.verifyTemplateId(1L);
    }
}
