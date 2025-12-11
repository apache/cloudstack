<!--
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

 | Hook ID | Language | Description | Version |
|---|---|---|---|
| identity | N/A | a simple hook which prints all arguments passed to it, useful for debugging. | N/A |
| check-hooks-apply | N/A | check that all the hooks apply to the repository | N/A |
| [doctoc](https://github.com/thlorenz/doctoc.git) | N/A | automatically keeps your table of contents up to date | v2.2.0 |
| create-pre-commit-docs | python | creates a Markdown file with information on the pre-commit hooks | N/A |
| [oxipng](https://github.com/oxipng/oxipng) | N/A | optimize PNG images with lossless compression | v9.1.5 |
| [gitleaks](https://github.com/gitleaks/gitleaks) | N/A | detect hardcoded secrets | v8.27.2 |
| [chmod](https://github.com/Lucas-C/pre-commit-hooks) | N/A | manual hook to be run by macOS or Linux users for a full repository clean up | v1.5.5 |
| [insert-license](https://github.com/Lucas-C/pre-commit-hooks) | N/A | automatically adds a licence header to all Markdown files that don't have a license header | v1.5.5 |
| [insert-license](https://github.com/Lucas-C/pre-commit-hooks) | N/A | automatically adds a licence header to all Shell files that don't have a license header | v1.5.5 |
| [insert-license](https://github.com/Lucas-C/pre-commit-hooks) | N/A | automatically adds a licence header to all SQL files that don't have a license header | v1.5.5 |
| [insert-license](https://github.com/Lucas-C/pre-commit-hooks) | N/A | automatically adds a licence header to all YAML files that don't have a license header | v1.5.5 |
| [check-case-conflict](https://github.com/pre-commit/pre-commit-hooks) | N/A | check for case conflicts in file names | v6.0.0 |
| [check-illegal-windows-names](https://github.com/pre-commit/pre-commit-hooks) | N/A | check for Windows-illegal file names | v6.0.0 |
| [check-merge-conflict](https://github.com/pre-commit/pre-commit-hooks) | N/A | check for merge conflict markers | v6.0.0 |
| [check-shebang-scripts-are-executable](https://github.com/pre-commit/pre-commit-hooks) | N/A | check that scripts with shebangs are executable | v6.0.0 |
| [check-symlinks](https://github.com/pre-commit/pre-commit-hooks) | N/A | checks for symlinks which do not point to anything. | v6.0.0 |
| [check-vcs-permalinks](https://github.com/pre-commit/pre-commit-hooks) | N/A | ensures that links to vcs websites are permalinks | v6.0.0 |
| [destroyed-symlinks](https://github.com/pre-commit/pre-commit-hooks) | N/A | detects symlinks which are changed to regular files with a content of a path which that symlink was pointing to | v6.0.0 |
| [detect-aws-credentials](https://github.com/pre-commit/pre-commit-hooks) | N/A | checks for the existence of AWS secrets that you have set up with the AWS CLI | v6.0.0 |
| [detect-private-key](https://github.com/pre-commit/pre-commit-hooks) | N/A | checks for the existence of private keys | v6.0.0 |
| [end-of-file-fixer](https://github.com/pre-commit/pre-commit-hooks) | N/A | makes sure files end in a newline and only a newline | v6.0.0 |
| [file-contents-sorter](https://github.com/pre-commit/pre-commit-hooks) | N/A | sort the lines in specified files (defaults to alphabetical) | v6.0.0 |
| [fix-byte-order-marker](https://github.com/pre-commit/pre-commit-hooks) | N/A | removes UTF-8 byte order marker | v6.0.0 |
| [forbid-submodules](https://github.com/pre-commit/pre-commit-hooks) | N/A | forbids any submodules in the repository | v6.0.0 |
| [mixed-line-ending](https://github.com/pre-commit/pre-commit-hooks) | N/A | replaces or checks mixed line ending | v6.0.0 |
| [trailing-whitespace](https://github.com/pre-commit/pre-commit-hooks) | N/A | trims trailing whitespace | v6.0.0 |
| [codespell](https://github.com/codespell-project/codespell) | N/A | Check spelling with codespell | v2.4.1 |
| [flake8](https://github.com/pycqa/flake8) | N/A | flake8 is a Python tool that glues together pycodestyle, pyflakes, mccabe, and third-party plugins to check the style and quality of some python code | 7.0.0 |
| [markdownlint](https://github.com/igorshubovych/markdownlint-cli) | N/A | check Markdown files with markdownlint | v0.45.0 |
| [yamllint](https://github.com/adrienverge/yamllint) | N/A | check YAML files with yamllint | v1.37.1 |
