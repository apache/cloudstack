package com.cloud.hypervisor.ovm3.resources.helpers;

import org.junit.Test;

import com.cloud.hypervisor.ovm3.objects.XmlTestResultTest;

public class Ovm3GuestTypesTest {
    XmlTestResultTest results = new XmlTestResultTest();
    String ora = "Oracle Enterprise Linux 6.0 (64-bit)";

    @Test
    public void test() {
        results.basicStringTest(Ovm3VmGuestTypes.getOvm3GuestType(ora), "xen_pvm");
    }
}
