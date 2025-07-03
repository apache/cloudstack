//Licensed to the Apache Software Foundation (ASF) under one
//or more contributor license agreements.  See the NOTICE file
//distributed with this work for additional information
//regarding copyright ownership.  The ASF licenses this file
//to you under the Apache License, Version 2.0 (the
//"License"); you may not use this file except in compliance
//with the License.  You may obtain a copy of the License at
//
//http://www.apache.org/licenses/LICENSE-2.0
//
//Unless required by applicable law or agreed to in writing,
//software distributed under the License is distributed on an
//"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
//KIND, either express or implied.  See the License for the
//specific language governing permissions and limitations
//under the License.
package org.apache.cloudstack.utils.jsinterpreter;

import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

public class GenericRuleHelper {

    protected static Logger LOGGER = LogManager.getLogger(GenericRuleHelper.class);

    private static final String PARSE_TAGS = "tags = tags ? tags.split(',') : [];";


    public static boolean interpretTagAsRule(String rule, String tags, long timeout, String configName) {
        String script = PARSE_TAGS + rule;
        Boolean scriptReturn = interpretRule(tags, timeout, "tags", script, configName);

        if (scriptReturn != null) {
            return scriptReturn;
        }

        LOGGER.debug(String.format("Result of tag rule [%s] was not a boolean, returning false.", script));
        return false;
    }

    public static boolean interpretGuestOsRule(String rule, String vmGuestOs, long timeout, String configName) {
        Boolean scriptReturn = interpretRule(vmGuestOs, timeout, "vmGuestOs", rule, configName);

        if (scriptReturn != null) {
            return scriptReturn;
        }

        LOGGER.debug(String.format("Result of guest OS rule [%s] was not a boolean, returning false.", rule));
        return false;
    }

    private static Boolean interpretRule(String rule, long timeout, String variable, String script, String configName) {
        rule = String.format("'%s'", StringEscapeUtils.escapeEcmaScript(rule));
        try (JsInterpreter jsInterpreter = new JsInterpreter(timeout, configName)) {
            jsInterpreter.injectVariable(variable, rule);
            Object scriptReturn = jsInterpreter.executeScript(script);
            if (scriptReturn instanceof Boolean) {
                return (Boolean) scriptReturn;
            }
        } catch (IOException ex) {
            String message = String.format("Error while executing script [%s].", script);
            LOGGER.error(message, ex);
            throw new CloudRuntimeException(message, ex);
        }
        return null;
    }

}
