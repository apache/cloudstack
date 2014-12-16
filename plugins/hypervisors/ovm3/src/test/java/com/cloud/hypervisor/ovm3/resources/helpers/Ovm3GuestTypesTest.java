package com.cloud.hypervisor.ovm3.resources.helpers;

import org.junit.Test;

import com.cloud.hypervisor.ovm3.objects.XmlTestResultTest;

public class Ovm3GuestTypesTest {
    XmlTestResultTest results = new XmlTestResultTest();
    String ora = "Oracle Enterprise Linux 6.0 (64-bit)";
    Ovm3VmGuestTypes ovm3gt = new Ovm3VmGuestTypes();

    @Test
    public void testGetPvByOs() {
        results.basicStringTest(ovm3gt.getOvm3GuestType(ora), "xen_pvm");
    }
}
