/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cloudstack.storage.command.browser;

import com.cloud.agent.api.Answer;

import java.util.Collections;
import java.util.List;

public class ListDataStoreObjectsAnswer extends Answer {

    private boolean pathExists;

    private int count;

    private List<String> names;

    private List<String> paths;

    private List<String> absPaths;

    private List<Boolean> isDirs;

    private List<Long> sizes;

    private List<Long> lastModified;

    public ListDataStoreObjectsAnswer() {
        super();
    }

    public ListDataStoreObjectsAnswer(boolean pathExists, int count, List<String> names, List<String> paths,
            List<String> absPaths, List<Boolean> isDirs,
            List<Long> sizes,
            List<Long> lastModified) {
        super();
        this.pathExists = pathExists;
        this.count = count;
        this.names = names;
        this.paths = paths;
        this.absPaths = absPaths;
        this.isDirs = isDirs;
        this.sizes = sizes;
        this.lastModified = lastModified;
    }

    public boolean isPathExists() {
        return pathExists;
    }

    public int getCount() {
        return count;
    }

    public List<String> getNames() {
        if (names == null) {
            return Collections.emptyList();
        }
        return names;
    }

    public List<String> getPaths() {
        if (paths == null) {
            return Collections.emptyList();
        }
        return paths;
    }

    public List<String> getAbsPaths() {
        if (absPaths == null) {
            return Collections.emptyList();
        }
        return absPaths;
    }

    public List<Boolean> getIsDirs() {
        if (isDirs == null) {
            return Collections.emptyList();
        }
        return isDirs;
    }

    public List<Long> getSizes() {
        if (sizes == null) {
            return Collections.emptyList();
        }
        return sizes;
    }

    public List<Long> getLastModified() {
        if (lastModified == null) {
            return Collections.emptyList();
        }
        return lastModified;
    }
}
