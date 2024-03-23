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

public class TagAsRuleHelper {

    protected static Logger LOGGER = LogManager.getLogger(TagAsRuleHelper.class);

    private static final String PARSE_TAGS = "tags = tags ? tags.split(',') : [];";


    public static boolean interpretTagAsRule(String rule, String tags, long timeout) {
        String script = PARSE_TAGS + rule;
        tags = String.format("'%s'", StringEscapeUtils.escapeEcmaScript(tags));
        try (JsInterpreter jsInterpreter = new JsInterpreter(timeout)) {
            jsInterpreter.injectVariable("tags", tags);
            Object scriptReturn = jsInterpreter.executeScript(script);
            if (scriptReturn instanceof Boolean) {
                return (Boolean)scriptReturn;
            }
        } catch (IOException ex) {
            String message = String.format("Error while executing script [%s].", script);
            LOGGER.error(message, ex);
            throw new CloudRuntimeException(message, ex);
        }

        LOGGER.debug(String.format("Result of tag rule [%s] was not a boolean, returning false.", script));
        return false;
    }

}
