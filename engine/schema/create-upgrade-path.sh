#!/usr/bin/env bash
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
#
# Scaffolds a new database upgrade path, following the pattern used e.g. in
# https://github.com/apache/cloudstack/pull/12048/files
#
# Creates:
#   - engine/schema/src/main/java/com/cloud/upgrade/dao/Upgrade<from>to<to>.java
#   - engine/schema/src/main/resources/META-INF/db/schema-<from>to<to>.sql
#   - engine/schema/src/main/resources/META-INF/db/schema-<from>to<to>-cleanup.sql
# and wires the new class into DatabaseUpgradeChecker.java (import + .next() entry).
#
# Usage: engine/schema/create-upgrade-path.sh <fromVersion> <toVersion>
# Example: engine/schema/create-upgrade-path.sh 4.23.0.0 4.24.0.0

set -euo pipefail

usage() {
    echo "Usage: $0 <fromVersion> <toVersion>"
    echo "Example: $0 4.23.0.0 4.24.0.0"
    exit 1
}

[[ $# -eq 2 ]] || usage

FROM_VERSION="$1"
TO_VERSION="$2"

VERSION_REGEX='^[0-9]+\.[0-9]+\.[0-9]+\.[0-9]+$'
[[ "$FROM_VERSION" =~ $VERSION_REGEX ]] || { echo "Invalid fromVersion '$FROM_VERSION' (expected x.y.z.w)"; exit 1; }
[[ "$TO_VERSION" =~ $VERSION_REGEX ]] || { echo "Invalid toVersion '$TO_VERSION' (expected x.y.z.w)"; exit 1; }

FROM_COMPACT="${FROM_VERSION//./}"
TO_COMPACT="${TO_VERSION//./}"
CLASS_NAME="Upgrade${FROM_COMPACT}to${TO_COMPACT}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
DAO_DIR="$SCRIPT_DIR/src/main/java/com/cloud/upgrade/dao"
SQL_DIR="$SCRIPT_DIR/src/main/resources/META-INF/db"
CHECKER_FILE="$SCRIPT_DIR/src/main/java/com/cloud/upgrade/DatabaseUpgradeChecker.java"
LICENSE_TEMPLATE="$REPO_ROOT/.github/workflows/license-templates/LICENSE.txt"

[[ -f "$LICENSE_TEMPLATE" ]] || { echo "License template not found: $LICENSE_TEMPLATE"; exit 1; }

JAVA_FILE="$DAO_DIR/${CLASS_NAME}.java"
SQL_FILE="$SQL_DIR/schema-${FROM_COMPACT}to${TO_COMPACT}.sql"
CLEANUP_SQL_FILE="$SQL_DIR/schema-${FROM_COMPACT}to${TO_COMPACT}-cleanup.sql"

for f in "$JAVA_FILE" "$SQL_FILE" "$CLEANUP_SQL_FILE"; do
    if [[ -e "$f" ]]; then
        echo "Refusing to overwrite existing file: $f"
        exit 1
    fi
done

if grep -q "new ${CLASS_NAME}()" "$CHECKER_FILE"; then
    echo "DatabaseUpgradeChecker.java already references ${CLASS_NAME}"
    exit 1
fi

# Render the shared license template as line comments for the given comment prefix.
render_license() {
    local prefix="$1"
    awk -v prefix="$prefix" '{ if (length($0) == 0) print prefix; else print prefix " " $0 }' "$LICENSE_TEMPLATE"
}

LICENSE_JAVA="$(render_license "//")"
LICENSE_SQL="$(render_license "--")"

mkdir -p "$DAO_DIR" "$SQL_DIR"

# 1. Upgrade class
cat > "$JAVA_FILE" <<EOF
${LICENSE_JAVA}
package com.cloud.upgrade.dao;

public class ${CLASS_NAME} extends DbUpgradeAbstractImpl implements DbUpgrade, DbUpgradeSystemVmTemplate {

    @Override
    public String[] getUpgradableVersionRange() {
        return new String[]{"${FROM_VERSION}", "${TO_VERSION}"};
    }

    @Override
    public String getUpgradedVersion() {
        return "${TO_VERSION}";
    }
}
EOF

# 2. Schema upgrade + cleanup SQL scripts
cat > "$SQL_FILE" <<EOF
${LICENSE_SQL}

--;
-- Schema upgrade from ${FROM_VERSION} to ${TO_VERSION}
--;
EOF

cat > "$CLEANUP_SQL_FILE" <<EOF
${LICENSE_SQL}

--;
-- Schema upgrade cleanup from ${FROM_VERSION} to ${TO_VERSION}
--;
EOF

# 3. Wire the new class into DatabaseUpgradeChecker.java: add the import in its
#    existing sorted block, and append a .next() entry right before .build().
IMPORT_LINE="import com.cloud.upgrade.dao.${CLASS_NAME};"
FIRST_IMPORT_LINE=$(grep -n '^import com\.cloud\.upgrade\.dao\.Upgrade' "$CHECKER_FILE" | head -1 | cut -d: -f1)
LAST_IMPORT_LINE=$(grep -n '^import com\.cloud\.upgrade\.dao\.Upgrade' "$CHECKER_FILE" | tail -1 | cut -d: -f1)

SORTED_IMPORTS_FILE="$(mktemp)"
trap 'rm -f "$SORTED_IMPORTS_FILE"' EXIT
{
    grep '^import com\.cloud\.upgrade\.dao\.Upgrade' "$CHECKER_FILE"
    echo "$IMPORT_LINE"
} | sed 's/;$//' | LC_ALL=C sort -u | sed 's/$/;/' > "$SORTED_IMPORTS_FILE"

TMP_CHECKER="$(mktemp)"
awk -v first="$FIRST_IMPORT_LINE" -v last="$LAST_IMPORT_LINE" -v importfile="$SORTED_IMPORTS_FILE" '
    NR == first {
        while ((getline line < importfile) > 0) print line
    }
    NR >= first && NR <= last { next }
    { print }
' "$CHECKER_FILE" > "$TMP_CHECKER"

NEXT_LINE="                .next(\"${FROM_VERSION}\", new ${CLASS_NAME}())"
awk -v nextline="$NEXT_LINE" '
    /^[ \t]*\.build\(\);/ && !inserted {
        print nextline
        inserted = 1
    }
    { print }
' "$TMP_CHECKER" > "$CHECKER_FILE"
rm -f "$TMP_CHECKER"

echo "Created:"
echo "  $JAVA_FILE"
echo "  $SQL_FILE"
echo "  $CLEANUP_SQL_FILE"
echo "Updated:"
echo "  $CHECKER_FILE"
