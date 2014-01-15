package org.apache.cloudstack.storage.datastore.provider;

import org.apache.log4j.Logger;

import org.apache.cloudstack.engine.subsystem.api.storage.HypervisorHostListener;

public class NexentaHostListener implements HypervisorHostListener {
    private static final Logger logger = Logger.getLogger(NexentaHostListener.class);

    public boolean hostConnect(long hostId, long poolId) {
        return true;
    }

    public boolean hostDisconnected(long hostId, long poolId) {
        return true;
    }
}
