package com.cloud.hypervisor.kvm.resource;

import java.util.concurrent.Callable;

import org.libvirt.Connect;
import org.libvirt.Domain;
import org.libvirt.LibvirtException;

public class MigrateKVMAsync implements Callable<Domain> {

    private final LibvirtComputingResource libvirtComputingResource;

    private Domain dm = null;
    private Connect dconn = null;
    private String dxml = "";
    private String vmName = "";
    private String destIp = "";

    public MigrateKVMAsync(final LibvirtComputingResource libvirtComputingResource, final Domain dm, final Connect dconn, final String dxml, final String vmName, final String destIp) {
        this.libvirtComputingResource = libvirtComputingResource;

        this.dm = dm;
        this.dconn = dconn;
        this.dxml = dxml;
        this.vmName = vmName;
        this.destIp = destIp;
    }

    @Override
    public Domain call() throws LibvirtException {
        // set compression flag for migration if libvirt version supports it
        if (dconn.getLibVirVersion() < 1003000) {
            return dm.migrate(dconn, 1 << 0, dxml, vmName, "tcp:" + destIp, libvirtComputingResource.getMigrateSpeed());
        } else {
            return dm.migrate(dconn, 1 << 0|1 << 11, dxml, vmName, "tcp:" + destIp, libvirtComputingResource.getMigrateSpeed());
        }
    }
}