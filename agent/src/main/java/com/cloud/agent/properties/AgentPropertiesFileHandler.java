/*
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
import java.util.Properties;
import org.apache.cloudstack.utils.security.KeyStoreUtils;
import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.beanutils.converters.IntegerConverter;
import org.apache.commons.beanutils.converters.LongConverter;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * This class provides a facility to read the agent's properties file and get
 * its properties, according to the {@link AgentProperties} properties constants.
 *
 * Properties are loaded lazily and cached until the cache is cleared (for example, after persisting updates).
 */
public class AgentPropertiesFileHandler {

    protected static Logger LOGGER = LogManager.getLogger(AgentPropertiesFileHandler.class);

    // Simple singleton caching - loaded once, cached forever
    private static volatile Properties cachedProperties;

    /**
     * This method reads the property in the agent.properties file.
     * Properties are loaded once and cached for the agent's lifetime.
     *
     * @param property the property to retrieve.
     * @return The value of the property. If the property is not available, the
     * default defined value will be used.
     */
    public static <T> T getPropertyValue(AgentProperties.Property<T> property) {
        T defaultValue = property.getDefaultValue();
        String name = property.getName();

        Properties properties = getCachedProperties();

        if (properties == null) {
            LOGGER.debug(String.format("Properties file was not found or could not be loaded, using default values. Property [%s]: [%s].", name, defaultValue));
            return defaultValue;
        }

        try {
            String configValue = properties.getProperty(name);
            if (StringUtils.isBlank(configValue)) {
                LOGGER.debug("Property [{}] has empty or null value. Using default value [{}].", name, defaultValue);
                return defaultValue;
            }

            if (defaultValue instanceof Integer) {
                ConvertUtils.register(new IntegerConverter(defaultValue), Integer.class);
            }

            if (defaultValue instanceof Long) {
                ConvertUtils.register(new LongConverter(defaultValue), Long.class);
            }

            LOGGER.debug("Property [{}] was altered. Now using the value [{}].", name, configValue);
            return (T)ConvertUtils.convert(configValue, property.getTypeClass());

        } catch (RuntimeException ex) {
            LOGGER.debug("Failed to get property [{}]. Using default value [{}].", name, defaultValue, ex);
        }

        return defaultValue;
    }

    /**
     * Gets the cached properties, loading them once if not already loaded.
     * Agent properties are static configuration that don't change during runtime.
     *
     * @return cached Properties object or null if file cannot be loaded
     */
    private static Properties getCachedProperties() {
        Properties properties = cachedProperties;
        if (properties == null) {
            synchronized (AgentPropertiesFileHandler.class) {
                properties = cachedProperties;
                if (properties == null) {
                    loadProperties();
                    properties = cachedProperties;
                }
            }
        }
        return properties;
    }

    /**
     * Loads properties from file and caches them for the agent's lifetime.
     */
    private static void loadProperties() {
        File agentPropertiesFile = PropertiesUtil.findConfigFile(KeyStoreUtils.AGENT_PROPSFILE);

        if (agentPropertiesFile == null) {
            LOGGER.debug("File [{}] was not found.", KeyStoreUtils.AGENT_PROPSFILE);
            return;
        }

        try {
            Properties newProperties = PropertiesUtil.loadFromFile(agentPropertiesFile);
            cachedProperties = newProperties;

            LOGGER.info("Loaded {} properties from [{}]", newProperties.size(), agentPropertiesFile.getAbsolutePath());

        } catch (IOException ex) {
            LOGGER.error("Failed to load properties from file [{}].", agentPropertiesFile.getAbsolutePath(), ex);
        }
    }

    /**
     * Clears the properties cache.
     */
    public static synchronized void clearCache() {
        LOGGER.info("Clearing agent properties cache");
        cachedProperties = null;
    }

    /**
     * Returns whether the properties cache is currently loaded.
     *
     * @return true if properties are cached, false otherwise.
     */
    public static boolean isCacheLoaded() {
        return cachedProperties != null;
    }
}
