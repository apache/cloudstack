
package com.cloud.conf.meta_inf.cloudstack.server_allocator;

import com.cloud.agent.manager.allocator.impl.FirstFitAllocator;
import com.cloud.agent.manager.allocator.impl.FirstFitRoutingAllocator;
import com.cloud.agent.manager.allocator.impl.RecreateHostAllocator;
import com.cloud.agent.manager.allocator.impl.TestingAllocator;
import com.cloud.consoleproxy.ConsoleProxyBalanceAllocator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 * Generated Java based configuration
 * 
 */
@Configuration
public class SpringServerAllocatorContext {


    @Bean("FirstFitRouting")
    public FirstFitRoutingAllocator FirstFitRouting() {
        FirstFitRoutingAllocator bean = new FirstFitRoutingAllocator();
        bean.setName("FirstFitRouting");
        return bean;
    }

    @Bean("testingAllocator")
    public TestingAllocator testingAllocator() {
        return new TestingAllocator();
    }

    @Bean("ConsoleProxyAllocator")
    public ConsoleProxyBalanceAllocator ConsoleProxyAllocator() {
        ConsoleProxyBalanceAllocator bean = new ConsoleProxyBalanceAllocator();
        bean.setName("Balance");
        return bean;
    }

    @Bean("firstFitAllocator")
    public FirstFitAllocator firstFitAllocator() {
        return new FirstFitAllocator();
    }

    @Bean("recreateHostAllocator")
    public RecreateHostAllocator recreateHostAllocator() {
        return new RecreateHostAllocator();
    }

}
