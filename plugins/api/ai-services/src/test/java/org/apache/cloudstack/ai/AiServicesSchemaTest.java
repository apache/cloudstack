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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Applying the plugin's own table (SPEC.md section 9).
 *
 * <p>`ai_deployment` was shipped as a .sql file on the classpath with
 * nothing to run it. CloudStack applies schema through `DbUpgrade`
 * classes registered in its core upgrade chain, and the precedent for a
 * plugin's tables — Quota — is that they live in CloudStack's own
 * `engine/schema` scripts. Neither is available to a plugin dropped
 * into an existing management server, so the table was never created
 * and the first deployAiModel would have failed on a missing table.
 *
 * <p>So the plugin applies it itself, at startup, idempotently. The
 * statements are `IF NOT EXISTS`, so a management server that already
 * has the table does nothing and one that does not gets it.
 */
class AiServicesSchemaTest {

    /** Records what would have been executed. */
    static class Recorder implements AiServicesSchema.StatementRunner {
        final List<String> executed = new ArrayList<>();
        String failOn;

        @Override
        public void run(String sql) throws SQLException {
            if (failOn != null && sql.contains(failOn)) {
                throw new SQLException("refused: " + failOn);
            }
            executed.add(sql);
        }
    }

    @Test
    void theBundledScriptIsOnTheClasspath() {
        assertFalse(AiServicesSchema.script().isEmpty());
    }

    @Test
    void itParsesToOneStatement() {
        assertEquals(1, AiServicesSchema.statements(AiServicesSchema.script()).size());
    }

    @Test
    void thatStatementCreatesTheOwnershipTable() {
        String only = AiServicesSchema.statements(AiServicesSchema.script()).get(0);

        assertTrue(only.contains("ai_deployment"), only);
        assertTrue(only.toUpperCase().startsWith("CREATE TABLE"), only);
    }

    @Test
    void theLicenceHeaderIsNotExecuted() {
        for (String statement : AiServicesSchema.statements(AiServicesSchema.script())) {
            assertFalse(statement.contains("Licensed to the Apache"), statement);
        }
    }

    @Test
    void everyCreateIsIdempotent() {
        // Startup runs this every time; a bare CREATE TABLE would turn
        // the second restart into an error.
        for (String statement : AiServicesSchema.statements(AiServicesSchema.script())) {
            String upper = statement.toUpperCase();
            if (upper.startsWith("CREATE TABLE")) {
                assertTrue(upper.contains("IF NOT EXISTS"), statement);
            }
        }
    }

    @Test
    void aSemicolonInsideAStringDoesNotSplitTheStatement() {
        // A COMMENT holding a semicolon would otherwise be cut in half
        // and executed as two broken statements.
        List<String> parsed =
                AiServicesSchema.statements("CREATE TABLE t (a int COMMENT 'one; two');");

        assertEquals(1, parsed.size(), parsed.toString());
    }

    @Test
    void anEscapedQuoteDoesNotEndTheString() {
        // The real script has `the engine''s identifier`.
        List<String> parsed =
                AiServicesSchema.statements("CREATE TABLE t (a int COMMENT 'it''s; fine');");

        assertEquals(1, parsed.size(), parsed.toString());
    }

    @Test
    void aSemicolonInACommentDoesNotSplitEither() {
        List<String> parsed = AiServicesSchema.statements("-- a; comment\nCREATE TABLE t (a int);");

        assertEquals(1, parsed.size(), parsed.toString());
    }

    @Test
    void severalStatementsAreKeptInOrder() {
        List<String> parsed = AiServicesSchema.statements("CREATE TABLE a (x int);\nCREATE TABLE b (y int);");

        assertEquals(2, parsed.size());
        assertTrue(parsed.get(0).contains("a"));
        assertTrue(parsed.get(1).contains("b"));
    }

    @Test
    void anEmptyScriptRunsNothing() {
        assertTrue(AiServicesSchema.statements("-- nothing here\n\n").isEmpty());
    }

    @Test
    void applyRunsEveryStatement() throws Exception {
        Recorder recorder = new Recorder();

        AiServicesSchema.apply(recorder);

        assertEquals(1, recorder.executed.size());
        assertTrue(recorder.executed.get(0).contains("ai_deployment"));
    }

    @Test
    void aFailureIsReportedRatherThanSwallowed() {
        // The caller decides whether to abort; this must not pretend
        // the table exists when it does not.
        Recorder recorder = new Recorder();
        recorder.failOn = "ai_deployment";

        assertThrows(SQLException.class, () -> AiServicesSchema.apply(recorder));
    }

    @Test
    void startupAppliesTheSchema() {
        // Otherwise the file is back to being shipped and never run.
        final boolean[] applied = {false};
        AiServicesManagerImpl manager = new AiServicesManagerImpl() {
            @Override
            boolean applySchema() {
                applied[0] = true;
                return true;
            }

            @Override
            org.apache.cloudstack.framework.events.EventBus resolveEventBus() {
                return null;  // no Spring context in a unit test
            }
        };

        manager.start();

        assertTrue(applied[0]);
    }

    @Test
    void aSchemaFailureDoesNotStopTheManagementServer() {
        // An optional plugin that cannot create its table must not
        // take the whole management server down with it; the commands
        // that need the table fail instead, and the reason is logged.
        AiServicesManagerImpl manager = new AiServicesManagerImpl() {
            @Override
            boolean applySchema() {
                return false;
            }

            @Override
            org.apache.cloudstack.framework.events.EventBus resolveEventBus() {
                return null;
            }
        };

        assertTrue(manager.start());
    }

    @Test
    void applyingTwiceIssuesTheSameStatements() {
        // Idempotence is the database's job via IF NOT EXISTS; this
        // only checks we do not skip on a second run and leave an
        // upgraded schema unapplied.
        Recorder first = new Recorder();
        Recorder second = new Recorder();

        try {
            AiServicesSchema.apply(first);
            AiServicesSchema.apply(second);
        } catch (SQLException exc) {
            throw new AssertionError(exc);
        }

        assertEquals(first.executed, second.executed);
    }
}
