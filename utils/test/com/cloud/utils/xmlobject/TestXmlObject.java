package com.cloud.utils.xmlobject;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;

public class TestXmlObject {

    void p(String str) {
        System.out.println(str);
    }
    
    @Test
    public void test() {
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
    }

}
