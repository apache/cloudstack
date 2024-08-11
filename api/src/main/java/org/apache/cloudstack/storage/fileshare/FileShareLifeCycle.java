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

package org.apache.cloudstack.storage.fileshare;

import com.cloud.dc.DataCenter;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.utils.Pair;

public interface FileShareLifeCycle {
    void checkPrerequisites(DataCenter zone, Long serviceOfferingId);

    Pair<Long, Long> deployFileShare(FileShare fileShare, Long networkId, Long diskOfferingId, Long size, Long minIops, Long maxIops) throws ResourceUnavailableException, InsufficientCapacityException, ResourceAllocationException, OperationTimedoutException;

    void startFileShare(FileShare fileShare) throws OperationTimedoutException, ResourceUnavailableException, InsufficientCapacityException;

    boolean stopFileShare(FileShare fileShare, Boolean forced);

    boolean deleteFileShare(FileShare fileShare);

    Pair<Boolean, Long> reDeployFileShare(FileShare fileShare) throws ResourceUnavailableException, InsufficientCapacityException, ResourceAllocationException, OperationTimedoutException;
}
