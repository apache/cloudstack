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

public class StringEnumerationOption extends Option {
    public String value = "";
    public String choices[] = new String[] {};

    @Override
    public int parse(int position, String[] args) {
        if (position + 1 >= args.length)
            throw new NoArgumentForOptionException("Cannot find required argument for option \"" + args[position] + "\".");

        value = args[position + 1];

        for (String s : choices) {
            if (value.equals(s))
                return super.parse(position, args) + 1;
        }

        throw new NoArgumentForOptionException("Unexpected argument for option \"" + args[position] + "\": \"" + value + "\". Expected argument: "
                + join("|", choices) + ".");
    }

    @Override
    public String help() {
        StringBuilder help = new StringBuilder();
        help.append(join("|", name, alias, aliases)).append(" ").append(join("|", choices)).append("\t").append(description);
        if (required)
            help.append(" Required.");
        else if (value != null && value.length() > 0)
            help.append(" Default value is \"").append(value).append("\".");
        return help.toString();
    }

    /**
     * Join strings in array into one large string.
     */
    protected String join(String delim, String values[]) {
        StringBuilder sb = new StringBuilder();
        if (values != null) {
            boolean first = true;
            for (String s : values) {
                if (s != null && s.length() > 0) {
                    if (first)
                        first = false;
                    else
                        sb.append(delim);

                    sb.append(s);
                }
            }
        }

        return sb.toString();
    }

}
