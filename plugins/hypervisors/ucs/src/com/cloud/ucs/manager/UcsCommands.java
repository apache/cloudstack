package com.cloud.ucs.manager;

import com.cloud.utils.xmlobject.XmlObject;

public class UcsCommands {
    public static String loginCmd(String username, String password) {
        XmlObject cmd = new XmlObject("aaaLogin");
        cmd.putElement("inName", username);
        cmd.putElement("inPassword", password);
        return cmd.dump();
    }
    
    public static String listComputeBlades(String cookie) {
        XmlObject cmd = new XmlObject("configResolveClass");
        cmd.putElement("classId", "computeBlade");
        cmd.putElement("cookie", cookie);
        cmd.putElement("inHierarchical", "false");
        return cmd.dump();
    }
    
    public static String listProfiles(String cookie) {
        XmlObject cmd = new XmlObject("configFindDnsByClassId");
        cmd.putElement("classId", "lsServer");
        cmd.putElement("cookie", cookie);
        return cmd.dump();
    }
    
    public static String cloneProfile(String cookie, String srcDn, String newProfileName) {
        XmlObject cmd = new XmlObject("lsClone");
        cmd.putElement("cookie", cookie);
        cmd.putElement("dn", srcDn);
        cmd.putElement("inTargetOrg", "org-root");
        cmd.putElement("inServerName", newProfileName);
        cmd.putElement("inHierarchical", "false");
        return cmd.dump();
    }
    
    public static String configResolveDn(String cookie, String dn) {
        XmlObject cmd = new XmlObject("configResolveDn");
        cmd.putElement("cookie", cookie);
        cmd.putElement("dn", dn);
        return cmd.toString();
    }
}
