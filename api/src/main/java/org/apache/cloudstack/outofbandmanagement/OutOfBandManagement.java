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

package org.apache.cloudstack.outofbandmanagement;

import com.cloud.utils.fsm.NoTransitionException;
import com.cloud.utils.fsm.StateMachine2;
import com.cloud.utils.fsm.StateObject;
import org.apache.cloudstack.api.InternalIdentity;
import org.apache.cloudstack.utils.identity.ManagementServerNode;

import java.util.Set;

public interface OutOfBandManagement extends StateObject<OutOfBandManagement.PowerState>, InternalIdentity {

    PowerState getState();

    Long getHostId();

    boolean isEnabled();

    PowerState getPowerState();

    String getDriver();

    String getAddress();

    String getPort();

    String getUsername();

    String getPassword();

    Long getManagementServerId();

    void setEnabled(boolean enabled);

    void setDriver(String driver);

    void setAddress(String address);

    void setPort(String port);

    void setUsername(String username);

    void setPassword(String password);

    enum Option {
        DRIVER,
        ADDRESS,
        PORT,
        USERNAME,
        PASSWORD
    }

    /**
     * <ul>
     *  <li> <b>on:</b> Power up chassis.
     *  <li> <b>off:</b> Power down chassis into soft off (S4/S5 state). WARNING: This command does not initiate a clean shutdown of the operating system prior to powering down the system.
     *  <li> <b>cycle:</b> Provides a power off interval of at least 1 second. No action should occur if chassis power is in S4/S5 state, but it is recommended to check power state first and only issue a power cycle command if the system power is on or in lower sleep state than S4/S5.
     *  <li> <b>reset:</b> This command will perform a hard reset.
     *  <li> <b>soft:</b> Initiate a soft-shutdown of OS via ACPI. This can be done in a number of ways, commonly by simulating an overtemperture or by simulating a power button press. It is necessary for there to be Operating System support for ACPI and some sort of daemon watching for events for this soft power to work.
     *  <li> <b>status:</b> Show current chassis power status.
     *  </ul>
     */
    enum PowerOperation {
        ON,
        OFF,
        CYCLE,
        RESET,
        SOFT,
        STATUS
    }

    enum PowerState {
        On,
        Off,
        Unknown,
        Disabled;

        public enum Event {
            On("Chassis Power is On"),
            Off("Chassis Power is Off"),
            AuthError("Authentication error happened"),
            Unknown("An unknown error happened"),
            Enabled("Out-of-band management enabled"),
            Disabled("Out-of-band management disabled");

            private String description;
            Event(String description) {
                this.description = description;
            }
            public String toString() {
                return String.format("%s(%s)", super.toString(), this.getDescription());
            }
            public String getDescription() {
                return description;
            }
            public Long getServerId() {
                // TODO: change in future if we've better claim & ownership
                // Right now the first one to update the db wins
                // and mgmt server id would eventually become consistent
                return ManagementServerNode.getManagementServerId();
            }
        }

        public Event toEvent() {
            if (this.equals(On)) {
                return Event.On;
            } else if (this.equals(Off)) {
                return Event.Off;
            } else if (this.equals(Disabled)) {
                return Event.Disabled;
            }
            return Event.Unknown;
        }

        private static final StateMachine2<PowerState, Event, OutOfBandManagement> FSM = new StateMachine2<PowerState, Event, OutOfBandManagement>();
        static {
            FSM.addInitialTransition(Event.On, On);
            FSM.addInitialTransition(Event.Off, Off);
            FSM.addInitialTransition(Event.Unknown, Unknown);
            FSM.addInitialTransition(Event.AuthError, Unknown);
            FSM.addInitialTransition(Event.Disabled, Disabled);

            FSM.addTransitionFromStates(Event.On, On, On, Off, Unknown, Disabled);
            FSM.addTransitionFromStates(Event.Off, Off, On, Off, Unknown, Disabled);
            FSM.addTransitionFromStates(Event.Unknown, Unknown, On, Off, Unknown, Disabled);
            FSM.addTransitionFromStates(Event.AuthError, Unknown, On, Off, Disabled);
            FSM.addTransitionFromStates(Event.Disabled, Disabled, On, Off, Unknown);
        }

        public static StateMachine2<PowerState, Event, OutOfBandManagement> getStateMachine() {
            return FSM;
        }

        public PowerState getNextPowerState(Event e) throws NoTransitionException {
            return FSM.getNextState(this, e);
        }

        public Set<Event> getPossibleEvents() {
            return FSM.getPossibleEvents(this);
        }

    }
}
