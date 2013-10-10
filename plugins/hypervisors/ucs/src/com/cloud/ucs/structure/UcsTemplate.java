package com.cloud.ucs.structure;

import com.cloud.utils.xmlobject.XmlObject;
import com.cloud.utils.xmlobject.XmlObjectParser;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Date: 10/8/13
 * Time: 3:01 PM
 * To change this template use File | Settings | File Templates.
 */
public class UcsTemplate {
    private String dn;

    public static List<UcsTemplate> fromXmlString(String xmlstr) {
        List<UcsTemplate> tmps = new ArrayList<UcsTemplate>();
        XmlObject xo = XmlObjectParser.parseFromString(xmlstr);
        List<XmlObject> xos = xo.getAsList("outConfigs.lsServer");
        if (xos != null) {
            for (XmlObject x : xos) {
                UcsTemplate t = new UcsTemplate();
                t.setDn(x.get("dn").toString());
                tmps.add(t);
            }
        }
        return tmps;
    }

    public String getDn() {
        return dn;
    }

    public void setDn(String dn) {
        this.dn = dn;
    }
}
