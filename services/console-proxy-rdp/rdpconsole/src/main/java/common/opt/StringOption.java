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

public class StringOption extends Option {
    public String value = "";

    @Override
    public int parse(int position, String[] args) {
        if (position + 1 >= args.length)
            throw new NoArgumentForOptionException("Cannot find required argument for option \"" + args[position] + "\".");

        value = args[position + 1];
        return super.parse(position, args) + 1;
    }

    @Override
    public String help() {
        StringBuilder help = new StringBuilder();
        help.append(join("|", name, alias, aliases)).append(" VALUE\t").append(description);
        if (required)
            help.append(" Required.");
        else if (value != null && value.length() > 0)
            help.append(" Default value is \"").append(value).append("\".");
        return help.toString();
    }
}
