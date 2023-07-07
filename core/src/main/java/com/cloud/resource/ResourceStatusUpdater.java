package com.cloud.resource;

/**
 * ResourceStatusUpdater is a resource that can trigger out of band status updates
 */
public interface ResourceStatusUpdater {
    /**
     * @param updater The object to call triggerUpdate() on
     */
    void registerStatusUpdater(AgentStatusUpdater updater);
}
