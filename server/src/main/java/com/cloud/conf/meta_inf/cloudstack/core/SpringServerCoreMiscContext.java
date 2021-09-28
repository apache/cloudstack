package com.cloud.conf.meta_inf.cloudstack.core;

import com.cloud.agent.manager.authn.impl.BasicAgentAuthManager;
import com.cloud.api.ApiDBUtils;
import com.cloud.api.ApiDispatcher;
import com.cloud.api.ApiResponseHelper;
import com.cloud.api.dispatch.CommandCreationWorker;
import com.cloud.api.dispatch.DispatchChainFactory;
import com.cloud.api.dispatch.ParamGenericValidationWorker;
import com.cloud.api.dispatch.ParamUnpackWorker;
import com.cloud.api.dispatch.SpecificCmdValidationWorker;
import com.cloud.event.ActionEventUtils;
import com.cloud.event.AlertGenerator;
import com.cloud.hypervisor.CloudZonesStartupProcessor;
import com.cloud.hypervisor.kvm.dpdk.DpdkHelperImpl;
import com.cloud.network.ExternalIpAddressAllocator;
import com.cloud.network.element.VpcVirtualRouterElement;
import org.apache.cloudstack.utils.identity.ManagementServerNode;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 * Generated Java based configuration
 */
@Configuration
public class SpringServerCoreMiscContext {


    @Bean("ExternalIpAddressAllocator")
    public ExternalIpAddressAllocator ExternalIpAddressAllocator() {
        ExternalIpAddressAllocator bean = new ExternalIpAddressAllocator();
        bean.setName("Basic");
        return bean;
    }

    @Bean("apiResponseHelper")
    public ApiResponseHelper apiResponseHelper() {
        return new ApiResponseHelper();
    }

    @Bean("actionEventUtils")
    public ActionEventUtils actionEventUtils() {
        return new ActionEventUtils();
    }

    @Bean("paramUnpackWorker")
    public ParamUnpackWorker paramUnpackWorker() {
        return new ParamUnpackWorker();
    }

    @Bean("DPDKHelper")
    public DpdkHelperImpl DPDKHelper() {
        return new DpdkHelperImpl();
    }

    @Bean("paramGenericValidationWorker")
    public ParamGenericValidationWorker paramGenericValidationWorker() {
        return new ParamGenericValidationWorker();
    }

    @Bean("apiDispatcher")
    public ApiDispatcher apiDispatcher() {
        return new ApiDispatcher();
    }

    @Bean("dispatchChainFactory")
    public DispatchChainFactory dispatchChainFactory() {
        return new DispatchChainFactory();
    }

    @Bean("specificCmdValidationWorker")
    public SpecificCmdValidationWorker specificCmdValidationWorker() {
        return new SpecificCmdValidationWorker();
    }

    @Bean("apiDBUtils")
    public ApiDBUtils apiDBUtils() {
        return new ApiDBUtils();
    }

    @Bean("basicAgentAuthManager")
    public BasicAgentAuthManager basicAgentAuthManager() {
        BasicAgentAuthManager bean = new BasicAgentAuthManager();
        bean.setName("BASIC");
        return bean;
    }

    @Bean("managementServerNode")
    public ManagementServerNode managementServerNode() {
        return new ManagementServerNode();
    }

    @Bean("commandCreationWorker")
    public CommandCreationWorker commandCreationWorker() {
        return new CommandCreationWorker();
    }

    @Bean("VpcVirtualRouter")
    public VpcVirtualRouterElement VpcVirtualRouter() {
        VpcVirtualRouterElement bean = new VpcVirtualRouterElement();
        bean.setName("VpcVirtualRouter");
        return bean;
    }

    @Bean("alertGenerator")
    public AlertGenerator alertGenerator() {
        return new AlertGenerator();
    }

    @Bean("cloudZonesStartupProcessor")
    public CloudZonesStartupProcessor cloudZonesStartupProcessor() {
        return new CloudZonesStartupProcessor();
    }

}
