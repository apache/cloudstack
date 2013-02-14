package org.apache.cloudstack.storage.snapshot;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.cloud.storage.Snapshot;
import com.cloud.storage.Snapshot.Event;
import com.cloud.storage.Snapshot.State;
import com.cloud.storage.SnapshotVO;
import com.cloud.storage.dao.SnapshotDao;
import com.cloud.storage.listener.SnapshotStateListener;
import com.cloud.utils.fsm.NoTransitionException;
import com.cloud.utils.fsm.StateMachine2;

@Component
public class SnapshotStateMachineManagerImpl implements
SnapshotStateMachineManager {
	private StateMachine2<State, Event, SnapshotVO> stateMachine = new StateMachine2<State, Event, SnapshotVO>();
    @Inject
    protected SnapshotDao snapshotDao; 
	public SnapshotStateMachineManagerImpl() {
		stateMachine.addTransition(Snapshot.State.Allocated, Event.CreateRequested, Snapshot.State.Creating);
		stateMachine.addTransition(Snapshot.State.Creating, Event.OperationSucceeded, Snapshot.State.CreatedOnPrimary);
		stateMachine.addTransition(Snapshot.State.Creating, Event.OperationNotPerformed, Snapshot.State.BackedUp);
		stateMachine.addTransition(Snapshot.State.Creating, Event.OperationFailed, Snapshot.State.Error);
		stateMachine.addTransition(Snapshot.State.CreatedOnPrimary, Event.BackupToSecondary, Snapshot.State.BackingUp);
		stateMachine.addTransition(Snapshot.State.BackingUp, Event.OperationSucceeded, Snapshot.State.BackedUp);
		stateMachine.addTransition(Snapshot.State.BackingUp, Event.OperationFailed, Snapshot.State.Error);
		
		stateMachine.registerListener(new SnapshotStateListener());
	}
	
	public void processEvent(SnapshotVO snapshot, Event event) throws NoTransitionException {
		stateMachine.transitTo(snapshot, event, null, snapshotDao);
	}
} 
