// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package com.cloud.utils.backoff;

import com.cloud.utils.backoff.impl.ConstantTimeBackoff;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.naming.ConfigurationException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * Backoff implementation factory.
 *
 * @author mprokopchuk
 */
public interface BackoffFactory {
    Logger logger = LogManager.getLogger(BackoffFactory.class);
    /**
     * Property name for the implementation class (that extends {@link BackoffAlgorithm}) to be used either
     * by {@code agent.properties} file or by configuration key.
     */
    String BACKOFF_IMPLEMENTATION_KEY = "backoff.implementation";

    /**
     * Default backoff implementation class name ({@link ConstantTimeBackoff}).
     */
    String DEFAULT_BACKOFF_IMPLEMENTATION = ConstantTimeBackoff.class.getName();

    /**
     * Creates default {@link BackoffAlgorithm} implementation object ({@link ConstantTimeBackoff}).
     *
     * @param properties configuration properties
     * @return {@link BackoffAlgorithm} implementation object
     */
    static BackoffAlgorithm createDefault(Properties properties) {
        Properties newProperties = new Properties(properties);
        newProperties.put(BACKOFF_IMPLEMENTATION_KEY, DEFAULT_BACKOFF_IMPLEMENTATION);
        return create(newProperties);
    }

    /**
     * Creates {@link BackoffAlgorithm} implementation object, falls back to
     *
     * @param properties configuration properties
     * @return {@link BackoffAlgorithm} implementation object
     */
    static BackoffAlgorithm create(Properties properties) {
        Map<String, String> params = properties.entrySet().stream()
                .collect(Collectors.toMap(e -> (String) e.getKey(), e -> (String) e.getValue()));
        return create(params);
    }

    /**
     * Creates {@link BackoffAlgorithm} implementation object.
     *
     * @param params configuration parameters map
     * @return {@link BackoffAlgorithm} implementation object
     */
    static BackoffAlgorithm create(Map<String, String> params) {
        String className = params.getOrDefault(BACKOFF_IMPLEMENTATION_KEY, DEFAULT_BACKOFF_IMPLEMENTATION);
        BackoffAlgorithm backoff;
        try {
            backoff = (BackoffAlgorithm) Class.forName(className).getDeclaredConstructor().newInstance();
            backoff.configure("Configuration", new HashMap<>(params));
        } catch (ReflectiveOperationException e) {
            String msg = String.format("Failed to create backoff implementation for %s", className);
            logger.warn(msg, e);
            throw new RuntimeException(msg, e);
        } catch (ConfigurationException e) {
            String msg = String.format("Failed to configure backoff implementation for %s with parameters %s",
                    className, params);
            logger.warn(msg, e);
            throw new RuntimeException(msg, e);
        }

        return backoff;
    }
}
