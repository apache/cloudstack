package com.cloud.ucs.structure;

import java.util.ArrayList;
import java.util.List;

import com.cloud.utils.xmlobject.XmlObject;
import com.cloud.utils.xmlobject.XmlObjectParser;

public class UcsProfile {
    private String dn;
    
    public static UcsProfile fromXmlObject(XmlObject xo) {
        UcsProfile p = new UcsProfile();
        return xo.evaluateObject(p);
    }
    
    public static List<UcsProfile> fromXmlString(String xmlstr) {
        List<UcsProfile> ps = new ArrayList<UcsProfile>();
        XmlObject xo = XmlObjectParser.parseFromString(xmlstr);
        List<XmlObject> xos = xo.getAsList("outDns.dn");
        if (xos != null) {
            for (XmlObject x : xos) {
                UcsProfile p = UcsProfile.fromXmlObject(x);
                ps.add(p);
            }
        }
        return ps;
    }

    public String getDn() {
        return dn;
    }

    public void setDn(String dn) {
        this.dn = dn;
    }
}
