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
package common.opt;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Simple parser of GNU-like options.
 */
public class OptionParser {

    public static Option helpOption() {
        return new Option() {
            {
                name = "--help";
                alias = "-h";
            }
        };
    }

    /**
     * Parse options, capture values and return rest of arguments.
     *
     * @param args
     *          command line arguments to parse
     * @param startFrom
     *          number of first argument to start parsing from
     * @param options
     *          options to fill with values
     * @return rest of command line after first non-option or "--" separator
     */
    public static String[] parseOptions(String args[], int startFrom, Option options[]) {
        // Convert array of options into map, where key is option name or alias
        Map<String, Option> optionMap = new HashMap<String, Option>(options.length);
        for (Option option : options) {
            optionMap.put(option.name, option);

            if (option.alias != null)
                optionMap.put(option.alias, option);

            if (option.aliases != null) {
                for (String alias : option.aliases)
                    optionMap.put(alias, option);
            }
        }

        // Parse arguments
        int position = startFrom;
        while (position < args.length) {
            // Double dash means end of options
            String optionName = args[position];
            if (optionName.equals("--")) {
                position++;
                break;
            }

            Option option = optionMap.get(optionName);

            // If option is not found, then this is argument, unless is starts with
            // dash
            if (option == null)
                if (!optionName.startsWith("-"))
                    break;
                else
                    throw new UnknownOptionException("Option \"" + optionName
                            + "\" is unknown. If this is not an option, then use \"--\" to separate options and arguments. Known options: " + optionMap.keySet().toString());

            position += option.parse(position, args);
        }

        // Check is required options are used on command line
        for (Option option : options) {
            if (option.required && !option.used)
                throw new OptionRequiredException("Option \"" + option.name + "\" is required.");
        }

        // Return rest of arguments, which are left after options
        return (position < args.length) ? Arrays.copyOfRange(args, position, args.length) : new String[] {};
    }

    /* Example. */
    public static void main(String args[]) {
        if (args.length == 0)
            args = new String[] {"--help", "--foo", "fooval", "--bar", "123", "-v", "--verbose", "-v", "-a", "a1", "-aa", "a2", "-aaa", "a3", "rest", "of",
        "arguments"};

        StringOption foo = new StringOption() {
            {
                name = "--foo";
                alias = "-f";
                value = "fooDefault";
            }
        };

        IntOption bar = new IntOption() {
            {
                name = "--bar";
                alias = "-b";
                value = 123;
            }
        };

        IncrementalOption verbose = new IncrementalOption() {
            {
                name = "--verbose";
                alias = "-v";
            }
        };

        StringArrayOption array = new StringArrayOption() {
            {
                name = "--array";
                alias = "-a";
                aliases = new String[] {"-aa", "-aaa"};
            }
        };

        String arguments[] = OptionParser.parseOptions(args, 0, new Option[] {helpOption(), foo, bar, verbose, array});

        assertTrue(foo.value.equals("fooval"));
        assertTrue(bar.value == 123);
        assertTrue(verbose.value == 3);
        assertTrue(Arrays.equals(array.value, new String[] {"a1", "a2", "a3"}));
        assertTrue(Arrays.equals(arguments, new String[] {"rest", "of", "arguments"}));
    }

    public static void assertTrue(boolean result) {
        if (!result)
            throw new AssertionError();
    }
}
