package com.cloud.hypervisor.xenserver.resource;

public class XcpServer83Resource extends XenServer650Resource {

    @Override
    protected String getPatchFilePath() {
        return "scripts/vm/hypervisor/xenserver/xcpserver83/patch";
    }
}
