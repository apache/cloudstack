package com.cloud.hypervisor.xenserver.resource;

public class Xenserver84Resource extends CitrixResourceBase {
    @Override
    protected String getPatchFilePath() {
        return "scripts/vm/hypervisor/xenserver/xenserver84/patch";
    }
}
