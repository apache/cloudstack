/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cloudstack.spring.module.web;

import javax.servlet.Filter;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;

import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;

import org.apache.cloudstack.spring.module.factory.CloudStackSpringContext;

public abstract class ModuleBasedFilter implements Filter {

    boolean enabled = false;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        String module = filterConfig.getInitParameter("module");
        CloudStackSpringContext context = (CloudStackSpringContext)filterConfig.getServletContext().getAttribute(CloudStackSpringContext.CLOUDSTACK_CONTEXT_SERVLET_KEY);

        if (context == null)
            return;

        ApplicationContext applicationContext = context.getApplicationContextForWeb(module);
        if (applicationContext != null) {
            AutowireCapableBeanFactory factory = applicationContext.getAutowireCapableBeanFactory();
            if (factory != null) {
                factory.autowireBean(this);
                enabled = true;
            }
        }
    }

    @Override
    public void destroy() {
        enabled = false;
    }

    public boolean isEnabled() {
        return enabled;
    }

}
