package com.cloud.agent.manager.allocator.impl;

import com.cloud.agent.manager.allocator.HostAllocator;
import com.cloud.capacity.CapacityManager;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.offering.ServiceOffering;
import com.cloud.utils.Pair;
import com.cloud.utils.component.AdapterBase;
import org.apache.commons.collections.CollectionUtils;

import javax.inject.Inject;
import java.util.List;

public abstract class BaseAllocator extends AdapterBase implements HostAllocator {

    @Inject
    protected HostDao hostDao;

    @Inject
    protected CapacityManager capacityManager;

    protected void retainHostsMatchingServiceOfferingAndTemplateTags(List<HostVO> availableHosts, Host.Type type, long dcId, Long podId, Long clusterId, String offeringHostTag, String templateTag) {
        logger.debug("Hosts {} will be checked for template and host tags compatibility.", availableHosts);

        if (offeringHostTag != null) {
            logger.debug("Looking for hosts having the tag [{}] specified in the Service Offering.", offeringHostTag);
            List<HostVO> hostsWithHostTag = hostDao.listByHostTag(type, clusterId, podId, dcId, offeringHostTag);
            logger.debug("Retaining hosts {} because they match the offering host tag {}.", hostsWithHostTag, offeringHostTag);
            availableHosts.retainAll(hostsWithHostTag);
        }

        if (templateTag != null) {
            logger.debug("Looking for hosts having the tag [{}] specified in the Template.", templateTag);
            List<HostVO> hostsWithTemplateTag = hostDao.listByHostTag(type, clusterId, podId, dcId, templateTag);
            logger.debug("Retaining hosts {} because they match the template tag {}.", hostsWithTemplateTag, templateTag);
            availableHosts.retainAll(hostsWithTemplateTag);
        }

        logger.debug("Remaining hosts after template tag and host tags validations are {}.", availableHosts);
    }

    protected void addHostsBasedOnTagRules(String hostTagOnOffering, List<HostVO> clusterHosts) {
        List<HostVO> hostsWithTagRules = hostDao.findHostsWithTagRuleThatMatchComputeOfferingTags(hostTagOnOffering);

        if (CollectionUtils.isEmpty(hostsWithTagRules)) {
            logger.info("No hosts found with tag rules matching the compute offering tag [{}].", hostTagOnOffering);
            return;
        }

        logger.info("Found hosts %s with tag rules matching the compute offering tag [{}].", hostsWithTagRules, hostTagOnOffering);
        clusterHosts.addAll(hostsWithTagRules);
    }

    /**
     * Adds hosts with enough CPU capability and enough CPU capacity to the suitable hosts list.
     */
    protected boolean hostHasCpuCapabilityAndCapacity(boolean considerReservedCapacity, ServiceOffering offering, Host host) {
        logger.debug("Looking for CPU frequency {} MHz and RAM {} MB.", () -> offering.getCpu() * offering.getSpeed(), offering::getRamSize);
        Pair<Boolean, Boolean> cpuCapabilityAndCapacity = capacityManager.checkIfHostHasCpuCapabilityAndCapacity(host, offering, considerReservedCapacity);
        Boolean hasCpuCapability = cpuCapabilityAndCapacity.first();
        Boolean hasCpuCapacity = cpuCapabilityAndCapacity.second();

        if (hasCpuCapability && hasCpuCapacity) {
            logger.debug("Host {} is a suitable host as it has enough CPU capability and CPU capacity.", () -> host);
            return true;
        }

        logger.debug("Cannot use host {}. Does the host have CPU capability? {}. Does the host have CPU capacity? {}..", () -> host, () -> hasCpuCapability, () -> hasCpuCapacity);
        return false;
    }

}
