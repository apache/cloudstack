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
package org.apache.cloudstack.storage.volume;

import org.apache.cloudstack.engine.subsystem.api.storage.VolumeProfile;
import org.apache.cloudstack.storage.volume.db.VolumeVO;

import com.cloud.storage.Volume;
import com.cloud.storage.Volume.Event;
import com.cloud.storage.Volume.State;
import com.cloud.utils.fsm.NoTransitionException;
import com.cloud.utils.fsm.StateMachine2;

public interface VolumeManager {
    VolumeVO allocateDuplicateVolume(VolumeVO oldVol);

    VolumeVO processEvent(Volume vol, Volume.Event event) throws NoTransitionException;

    VolumeProfile getProfile(long volumeId);

    VolumeVO getVolume(long volumeId);

    VolumeVO updateVolume(VolumeVO volume);

    /**
     * @return
     */
    StateMachine2<State, Event, VolumeVO> getStateMachine();
}
