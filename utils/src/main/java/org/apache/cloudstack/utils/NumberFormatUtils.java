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

package org.apache.cloudstack.utils;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Locale-independent number formatting helpers. API responses must not depend on the Management
 * Server's default locale, so decimal separators are always formatted as a dot.
 */
public final class NumberFormatUtils {

    private NumberFormatUtils() {
    }

    /**
     * Formats the given format string using {@link Locale#ROOT}.
     */
    public static String format(String format, Object... args) {
        return String.format(Locale.ROOT, format, args);
    }

    /**
     * Creates a {@link DecimalFormat} for the given pattern that always uses a dot as decimal separator.
     */
    public static DecimalFormat decimalFormat(String pattern) {
        return new DecimalFormat(pattern, DecimalFormatSymbols.getInstance(Locale.ROOT));
    }
}
