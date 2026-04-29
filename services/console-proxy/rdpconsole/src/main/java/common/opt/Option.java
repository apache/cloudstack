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

public class Option {

    public String name = "";
    public String alias = null;
    public String aliases[] = null;

    public String description = "";
    public boolean used = false;
    public boolean required = false;

    /**
     * Parse option value, if any.
     *
     * @param position
     *          position of this option in list of arguments
     * @param args
     *          command line arguments
     * @return how many arguments are consumed, at least 1
     */
    public int parse(int position, String args[]) {
        used = true;
        return 1;
    }

    @Override
    public String toString() {
        return help();
    }

    /**
     * Return help string for this option. Example:
     *
     * <pre>
     *   --foo|-f    Foo option.
     * </pre>
     */
    public String help() {
        return join("|", name, alias, aliases) + "\t" + description + ((required) ? " Required." : "");
    }

    /**
     * Return string like "--foo|-f|--another-foo-alias".
     */
    protected String join(String delim, String name, String alias, String aliases[]) {

        // Option name is mandatory
        StringBuilder sb = new StringBuilder(name.length());
        sb.append(name);

        // Alias is optional
        if (alias != null && alias.length() > 0) {
            sb.append(delim).append(alias);
        }

        // Other aliases are optional too
        if (aliases != null) {
            for (String s : aliases) {
                if (s != null && s.length() > 0) {
                    sb.append(delim).append(s);
                }
            }
        }

        return sb.toString();
    }

    /**
     * Return description of options in format suitable for help and usage text.
     *
     * @param header
     *          header string to print before list of options
     * @param options
     *          list of options to print
     */
    public static String toHelp(String header, Option[] options) {
        StringBuffer sb = new StringBuffer();
        sb.append(header).append(":\n");
        for (Option option : options) {
            sb.append("  ").append(option.help()).append('\n');
        }
        return sb.toString();
    }

}
