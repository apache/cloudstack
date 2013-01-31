package com.cloud.ucs.structure;

import java.util.ArrayList;
import java.util.List;

import com.cloud.utils.xmlobject.XmlObject;
import com.cloud.utils.xmlobject.XmlObjectParser;

public class ComputeBlade {
    String adminPower;
    String adminState;
    String assignedToDn;
    String association;
    String availability;
    String availableMemory;
    String chassisId;
    String dn;
    String name;
    String numOfAdaptors;
    String numOfCores;
    String numOfCoresEnabled;
    String numOfCpus;
    String numOfEthHostIfs;
    String numOfFcHostIfs;
    String numOfThreads;
    String operPower;
    String totalMemory;
    String uuid;
    
    public static List<ComputeBlade> fromXmString(String xmlstr) {
        XmlObject root = XmlObjectParser.parseFromString(xmlstr);
        List<XmlObject> lst = root.getAsList("configResolveClass.outConfigs.computeBlade");
        List<ComputeBlade> blades = new ArrayList<ComputeBlade>();
        if (lst == null) {
            return blades;
        }
        for (XmlObject xo : lst) {
            blades.add(fromXmlObject(xo));
        }
        return blades;
    }
    
    public static ComputeBlade fromXmlObject(XmlObject obj) {
        ComputeBlade ret = new ComputeBlade();
        return obj.evaluateObject(ret);
    }
    
    public String getAdminPower() {
        return adminPower;
    }
    public void setAdminPower(String adminPower) {
        this.adminPower = adminPower;
    }
    public String getAdminState() {
        return adminState;
    }
    public void setAdminState(String adminState) {
        this.adminState = adminState;
    }
    public String getAssignedToDn() {
        return assignedToDn;
    }
    public void setAssignedToDn(String assignedToDn) {
        this.assignedToDn = assignedToDn;
    }
    public String getAssociation() {
        return association;
    }
    public void setAssociation(String association) {
        this.association = association;
    }
    public String getAvailability() {
        return availability;
    }
    public void setAvailability(String availability) {
        this.availability = availability;
    }
    public String getAvailableMemory() {
        return availableMemory;
    }
    public void setAvailableMemory(String availableMemory) {
        this.availableMemory = availableMemory;
    }
    public String getChassisId() {
        return chassisId;
    }
    public void setChassisId(String chassisId) {
        this.chassisId = chassisId;
    }
    public String getDn() {
        return dn;
    }
    public void setDn(String dn) {
        this.dn = dn;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getNumOfAdaptors() {
        return numOfAdaptors;
    }
    public void setNumOfAdaptors(String numOfAdaptors) {
        this.numOfAdaptors = numOfAdaptors;
    }
    public String getNumOfCores() {
        return numOfCores;
    }
    public void setNumOfCores(String numOfCores) {
        this.numOfCores = numOfCores;
    }
    public String getNumOfCoresEnabled() {
        return numOfCoresEnabled;
    }
    public void setNumOfCoresEnabled(String numOfCoresEnabled) {
        this.numOfCoresEnabled = numOfCoresEnabled;
    }
    public String getNumOfCpus() {
        return numOfCpus;
    }
    public void setNumOfCpus(String numOfCpus) {
        this.numOfCpus = numOfCpus;
    }
    public String getNumOfEthHostIfs() {
        return numOfEthHostIfs;
    }
    public void setNumOfEthHostIfs(String numOfEthHostIfs) {
        this.numOfEthHostIfs = numOfEthHostIfs;
    }
    public String getNumOfFcHostIfs() {
        return numOfFcHostIfs;
    }
    public void setNumOfFcHostIfs(String numOfFcHostIfs) {
        this.numOfFcHostIfs = numOfFcHostIfs;
    }
    public String getNumOfThreads() {
        return numOfThreads;
    }
    public void setNumOfThreads(String numOfThreads) {
        this.numOfThreads = numOfThreads;
    }
    public String getOperPower() {
        return operPower;
    }
    public void setOperPower(String operPower) {
        this.operPower = operPower;
    }
    public String getTotalMemory() {
        return totalMemory;
    }
    public void setTotalMemory(String totalMemory) {
        this.totalMemory = totalMemory;
    }
    public String getUuid() {
        return uuid;
    }
    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
    public boolean isAssociated() {
        return this.assignedToDn.equals("");
    }
}
