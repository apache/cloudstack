/*
 * Copyright 2021 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.cloud.agent.properties;

import com.cloud.utils.PropertiesUtil;
import java.io.File;
import java.io.IOException;
import org.apache.cloudstack.utils.security.KeyStoreUtils;
import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.beanutils.converters.IntegerConverter;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

/**
 * This class provides a facility to read the agent's properties file and get
 * its properties, according to the {@link AgentProperties} properties constants.
 *
 */
public class AgentPropertiesFileHandler {

    private static final Logger logger = Logger.getLogger(AgentPropertiesFileHandler.class);

    /**
     * This method reads the property in the agent.properties file.
     *
     * @param property the property to retrieve.
     * @return The value of the property. If the property is not available, the
     * default defined value will be used.
     */
    public static <T> T getProperty(AgentProperties<T> property) {
        T defaultValue = property.getDefaultValue();
        String name = property.getName();

        File agentPropertiesFile = PropertiesUtil.findConfigFile(KeyStoreUtils.AGENT_PROPSFILE);

        if (agentPropertiesFile != null) {
            try {
                String configValue = PropertiesUtil.loadFromFile(agentPropertiesFile).getProperty(name);
                if (StringUtils.isNotBlank(configValue)) {
                    if (defaultValue instanceof Integer) {
                        ConvertUtils.register(new IntegerConverter(defaultValue), Integer.class);
                    }

                    return (T)ConvertUtils.convert(configValue, defaultValue.getClass());
                } else {
                    logger.debug(String.format("Property [%s] has empty or null value. Using default value [%s].", name, defaultValue));
                }
            } catch (IOException ex) {
                logger.debug(String.format("Failed to get property [%s]. Using default value [%s].", name, defaultValue), ex);
            }
        } else {
            logger.debug(String.format("File [%s] was not found, we will use default defined values. Property [%s]: [%s].", KeyStoreUtils.AGENT_PROPSFILE, name, defaultValue));
        }

        return defaultValue;
    }

}