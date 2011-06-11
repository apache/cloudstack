/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */

package com.cloud.api;

import java.lang.reflect.Type;

import org.apache.log4j.Logger;

import com.cloud.configuration.Config;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.utils.component.ComponentLocator;
import com.cloud.utils.encoding.URLEncoder;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class EncodedStringTypeAdapter implements JsonSerializer<String> {
    public static final Logger s_logger = Logger.getLogger(EncodedStringTypeAdapter.class.getName());
    private static final boolean encodeApiResponse = configure();
    
    private static boolean configure() {
        ComponentLocator locator = ComponentLocator.getCurrentLocator();

        ConfigurationDao configDao = locator.getDao(ConfigurationDao.class);
        if (configDao != null) {
            return Boolean.valueOf(configDao.getValue(Config.EncodeApiResponse.key()));
        } else {
            return true;
        }
    }

    @Override
    public JsonElement serialize(String src, Type typeOfResponseObj, JsonSerializationContext ctx) {
        return new JsonPrimitive(encodeString(src));

    }

    private static String encodeString(String value) {
        if (!encodeApiResponse) {
            return value;
        }
        try {
            return new URLEncoder().encode(value).replaceAll("\\+", "%20");
        } catch (Exception e) {
            s_logger.warn("Unable to encode: " + value, e);
        }
        return value;
    }
    
}
