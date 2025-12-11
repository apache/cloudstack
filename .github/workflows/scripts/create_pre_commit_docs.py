#!/usr/bin/env python3

# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.

import yaml


def generate_pre_commit_table(yaml_path):
    """
    Generates a Markdown table from a pre-commit-config.yaml file.
    """
    try:
        with open(yaml_path) as f:
            config = yaml.safe_load(f)
    except FileNotFoundError:
        return f"Error: The file '{yaml_path}' was not found."
    except yaml.YAMLError as e:
        return f"Error parsing YAML file: {e}"

    table_header = "| Hook ID | Language | Description | Version |\n"
    table_separator = "|---|---|---|---|\n"
    table_rows = []

    for repo in config.get("repos", []):
        version = repo.get("rev", "N/A")
        url = repo.get("repo", "N/A")
        for hook in repo.get("hooks", []):
            hook_id = hook.get("id", "N/A")
            language = hook.get("language", "N/A")
            description = hook.get("description", "N/A")
            if description == "N/A":
                description = hook.get("name", "N/A")
            # args = ", ".join(hook.get("args", [])) if hook.get("args") else "N/A"

            if url not in ["local", "meta"]:
                entry = f"[{hook_id}]({url})"
            else:
                entry = f"{hook_id}"

            table_rows.append(f"| {entry} | {language} | {description} | {version} |\n")

    return table_header + table_separator + "".join(table_rows)


def create_markdown_file(target_file_path, content_to_append):
    """
    Creates a Markdown file with the content.
    """
    try:
        with open(target_file_path, "w+") as f:
            f.seek(0)
            f.write(
                """<!--
 Licensed to the Apache Software Foundation (ASF) under one
 or more contributor license agreements.  See the NOTICE file
 distributed with this work for additional information
 regarding copyright ownership.  The ASF licenses this file
 to you under the Apache License, Version 2.0 (the
 "License"); you may not use this file except in compliance
 with the License.  You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing,
 software distributed under the License is distributed on an
 "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 KIND, either express or implied.  See the License for the
 specific language governing permissions and limitations
 under the License.
 -->

# pre-commit hook documentation

 """
            )
            f.write(content_to_append)
        return f"File content successfully created at '{target_file_path}'."
    except OSError as e:
        return f"Error creating file: {e}"


if __name__ == "__main__":

    pre_commit_yaml_path = (
        ".pre-commit-config.yaml"  # Assuming this file is in the same directory
    )
    output_markdown_path = "PRE_COMMIT_HOOK_DOCS.md"

    # Generate the Markdown table
    markdown_table = generate_pre_commit_table(pre_commit_yaml_path)

    # Add the table to the target Markdown file
    if "Error" not in markdown_table:
        result = create_markdown_file(output_markdown_path, markdown_table)
        print(result)
    else:
        print(markdown_table)
