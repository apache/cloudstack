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
package org.apache.cloudstack.storage.utils;

import com.cloud.exception.InvalidParameterValueException;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.cloudstack.framework.config.ValidatedConfigKey;
import org.apache.commons.lang3.StringUtils;

import java.util.function.Consumer;

/**
 * Single registration point for ONTAP plugin {@link ConfigKey}s (same pattern as
 * Linstor/StorPool). Callers read live values via {@code Key.value()}; they must not
 * construct new {@code ConfigKey} instances.
 */
public class OntapConfigurationManager implements Configurable {
    public static final ValidatedConfigKey<Integer> AsupIntervalHours = new ValidatedConfigKey<>(
            OntapStorageConstants.ADVANCED_CONFIG_KEY_CATEGORY, Integer.class,
            OntapStorageConstants.ASUP_INTERVAL_CONFIG_KEY,
            String.valueOf(OntapStorageConstants.ASUP_DEFAULT_INTERVAL_HOURS),
            OntapStorageConstants.ASUP_INTERVAL_DESCRIPTION,
            true, ConfigKey.Scope.Global, null, asupIntervalValidator());

    public static final ConfigKey<?>[] CONFIG_KEYS = new ConfigKey<?>[] {
            AsupIntervalHours
    };

    /**
     * {@link ValidatedConfigKey#validateValue(String)} always passes the raw config string,
     * even when the key type is {@link Integer}. Adapt that to {@code Consumer<Integer>}.
     */
    @SuppressWarnings("unchecked")
    private static Consumer<Integer> asupIntervalValidator() {
        Consumer<Object> validator = OntapConfigurationManager::validateAsupInterval;
        return (Consumer<Integer>) (Consumer<?>) validator;
    }

    /**
     * Rejects {@code ontap.autosupport.interval} values that are not {@link
     * OntapStorageConstants#ASUP_DISABLED_INTERVAL_HOURS} (disabled) or integers in
     * [{@link OntapStorageConstants#ASUP_MIN_INTERVAL_HOURS},
     * {@link OntapStorageConstants#ASUP_MAX_INTERVAL_HOURS}] hours.
     * Invoked by {@link ValidatedConfigKey} when the setting is saved in Global Settings.
     * {@code raw} is the saved string (or null).
     */
    private static void validateAsupInterval(Object raw) {
        String value = raw == null ? null : String.valueOf(raw).trim();
        if (StringUtils.isBlank(value)) {
            throw new InvalidParameterValueException(asupIntervalRangeMessage());
        }
        final int parsed;
        try {
            parsed = Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new InvalidParameterValueException(
                    OntapStorageConstants.ASUP_INTERVAL_CONFIG_KEY + " must be an integer. "
                            + asupIntervalRangeMessage());
        }
        if (parsed == OntapStorageConstants.ASUP_DISABLED_INTERVAL_HOURS) {
            return;
        }
        if (parsed < OntapStorageConstants.ASUP_MIN_INTERVAL_HOURS
                || parsed > OntapStorageConstants.ASUP_MAX_INTERVAL_HOURS) {
            throw new InvalidParameterValueException(asupIntervalRangeMessage());
        }
    }

    private static String asupIntervalRangeMessage() {
        return String.format(
                "%s must be %d to disable, or between %d hours and %d hours (1 week).",
                OntapStorageConstants.ASUP_INTERVAL_CONFIG_KEY,
                OntapStorageConstants.ASUP_DISABLED_INTERVAL_HOURS,
                OntapStorageConstants.ASUP_MIN_INTERVAL_HOURS,
                OntapStorageConstants.ASUP_MAX_INTERVAL_HOURS);
    }

    @Override
    public String getConfigComponentName() {
        return OntapConfigurationManager.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return CONFIG_KEYS;
    }
}
