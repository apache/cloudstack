// Licensed to the Apache Software Foundation (ASF) under one or more
// contributor license agreements. See the NOTICE file distributed with this
// work for additional information regarding copyright ownership. The ASF
// licenses this file to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance with the
// License. You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
// WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
// License for the specific language governing permissions and limitations
// under the License.
package org.apache.cloudstack.ai;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.cloud.utils.db.TransactionLegacy;

/**
 * Creates the plugin's one table (SPEC.md section 9).
 *
 * <p><b>Why the plugin does this itself.</b> CloudStack applies schema
 * through {@code DbUpgrade} classes registered in its core upgrade
 * chain, and the precedent for a plugin's own tables — Quota — is that
 * they live in CloudStack's {@code engine/schema} upgrade scripts.
 * Neither is open to a plugin JAR dropped into an existing management
 * server, so {@code META-INF/db/schema-ai-services.sql} shipped with
 * nothing to run it: the table was never created, and the first
 * {@code deployAiModel} would have failed on a missing table.
 *
 * <p>Applying it at startup is the smaller of two evils. It is safe
 * because the script is {@code CREATE TABLE IF NOT EXISTS}: a server
 * that already has the table does nothing. If this plugin is ever
 * merged upstream, the right home for the table is CloudStack's own
 * upgrade path and this class should go.
 *
 * <p>The parser is deliberate rather than a split on {@code ';'}: a
 * semicolon inside a {@code COMMENT} string would otherwise be cut in
 * half and executed as two broken statements.
 */
public final class AiServicesSchema {

    private static final Logger LOG = LogManager.getLogger(AiServicesSchema.class);

    static final String RESOURCE = "META-INF/db/schema-ai-services.sql";

    /** One statement, already stripped of comments. */
    public interface StatementRunner {
        void run(String sql) throws SQLException;
    }

    private AiServicesSchema() {
    }

    /** The bundled script, or "" if it is not on the classpath. */
    public static String script() {
        ClassLoader loader = AiServicesSchema.class.getClassLoader();
        try (InputStream in = loader.getResourceAsStream(RESOURCE)) {
            if (in == null) {
                LOG.error("{} is not on the classpath; ai_deployment cannot be created", RESOURCE);
                return "";
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exc) {
            LOG.error("could not read {}", RESOURCE, exc);
            return "";
        }
    }

    /**
     * Split a script into executable statements.
     *
     * <p>Drops {@code --} comments and blank lines, and treats a
     * semicolon inside a single-quoted string as data rather than a
     * terminator. SQL escapes a quote by doubling it, which is why the
     * real script's {@code the engine''s identifier} does not end its
     * string.
     */
    public static List<String> statements(String script) {
        List<String> found = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inString = false;

        for (int i = 0; i < script.length(); i++) {
            char c = script.charAt(i);

            if (!inString && c == '-' && i + 1 < script.length() && script.charAt(i + 1) == '-') {
                // A line comment: skip to the newline, which is kept so
                // the statement either side stays separated.
                while (i < script.length() && script.charAt(i) != '\n') {
                    i++;
                }
                current.append('\n');
                continue;
            }

            if (c == '\'') {
                // A doubled quote is an escaped one, not the end.
                if (inString && i + 1 < script.length() && script.charAt(i + 1) == '\'') {
                    current.append(c).append(script.charAt(i + 1));
                    i++;
                    continue;
                }
                inString = !inString;
            }

            if (c == ';' && !inString) {
                add(found, current);
                current.setLength(0);
                continue;
            }
            current.append(c);
        }
        add(found, current);
        return found;
    }

    private static void add(List<String> found, CharSequence candidate) {
        String trimmed = candidate.toString().trim();
        if (!trimmed.isEmpty()) {
            found.add(trimmed);
        }
    }

    /**
     * Run every statement in the bundled script.
     *
     * <p>Throws on the first failure rather than carrying on: a
     * half-created schema that reports success is worse than one that
     * says what broke.
     */
    public static void apply(StatementRunner runner) throws SQLException {
        for (String statement : statements(script())) {
            runner.run(statement);
        }
    }

    /**
     * Apply the schema to CloudStack's own database.
     *
     * <p>Returns whether it worked. The caller logs and carries on: a
     * management server should not fail to start because an optional
     * plugin could not create its table. Every command that needs the
     * table will fail loudly instead, and this logs the reason once at
     * startup where an operator will see it.
     */
    public static boolean applyToCloudDb() {
        TransactionLegacy transaction = TransactionLegacy.open(TransactionLegacy.CLOUD_DB);
        try {
            Connection connection = transaction.getConnection();
            apply(sql -> {
                try (Statement statement = connection.createStatement()) {
                    statement.execute(sql);
                }
            });
            LOG.info("ai_deployment is present");
            return true;
        } catch (SQLException | RuntimeException exc) {
            LOG.error(
                    "could not create the ai_deployment table from {}. AI Services commands that "
                            + "own a deployment will fail until it exists; apply the script by hand "
                            + "against the cloud database.",
                    RESOURCE,
                    exc);
            return false;
        } finally {
            transaction.close();
        }
    }
}
