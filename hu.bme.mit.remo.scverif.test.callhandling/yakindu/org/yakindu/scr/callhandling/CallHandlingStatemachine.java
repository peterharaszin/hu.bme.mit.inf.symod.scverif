package org.yakindu.scr.callhandling;
import org.yakindu.scr.ITimer;

public class CallHandlingStatemachine implements ICallHandlingStatemachine {

	private final boolean[] timeEvents = new boolean[2];

	private final class SCIUserImpl implements SCIUser {

		private SCIUserOperationCallback operationCallback;

		public void setSCIUserOperationCallback(SCIUserOperationCallback operationCallback) {
			this.operationCallback = operationCallback;
		}

		private boolean accept_call;

		public void raiseAccept_call() {
			accept_call = true;
		}

		private boolean dismiss_call;

		public void raiseDismiss_call() {
			dismiss_call = true;
		}

		public void clearEvents() {
			accept_call = false;
			dismiss_call = false;
		}

	}

	private SCIUserImpl sCIUser;
	private final class SCIPhoneImpl implements SCIPhone {

		private boolean incoming_call;

		public void raiseIncoming_call() {
			incoming_call = true;
		}

		private long duration;
		public long getDuration() {
			return duration;
		}

		public void setDuration(long value) {
			this.duration = value;
		}

		public void clearEvents() {
			incoming_call = false;
		}

	}

	private SCIPhoneImpl sCIPhone;

	public enum State {
		main_region_Idle, main_region_Incoming_Call, main_region_Active_Call, main_region_Dismiss_Call, $NullState$
	};

	private final State[] stateVector = new State[1];

	private int nextStateIndex;

	private ITimer timer;

	static {
	}

	public CallHandlingStatemachine() {

		sCIUser = new SCIUserImpl();
		sCIPhone = new SCIPhoneImpl();
	}

	public void init() {
		if (timer == null) {
			throw new IllegalStateException("timer not set.");
		}
		for (int i = 0; i < 1; i++) {
			stateVector[i] = State.$NullState$;
		}

		clearEvents();
		clearOutEvents();

		sCIPhone.duration = 0;
	}

	public void enter() {
		if (timer == null) {
			throw new IllegalStateException("timer not set.");
		}
		entryAction();

		nextStateIndex = 0;
		stateVector[0] = State.main_region_Idle;
	}

	public void exit() {
		switch (stateVector[0]) {
			case main_region_Idle :
				nextStateIndex = 0;
				stateVector[0] = State.$NullState$;
				break;

			case main_region_Incoming_Call :
				nextStateIndex = 0;
				stateVector[0] = State.$NullState$;
				break;

			case main_region_Active_Call :
				nextStateIndex = 0;
				stateVector[0] = State.$NullState$;

				timer.unsetTimer(this, 0);
				break;

			case main_region_Dismiss_Call :
				nextStateIndex = 0;
				stateVector[0] = State.$NullState$;

				timer.unsetTimer(this, 1);

				sCIPhone.duration = 0;
				break;

			default :
				break;
		}

		exitAction();
	}

	/**
	* This method resets the incoming events (time events included).
	*/
	protected void clearEvents() {
		sCIUser.clearEvents();
		sCIPhone.clearEvents();

		for (int i = 0; i < timeEvents.length; i++) {
			timeEvents[i] = false;
		}
	}

	/**
	* This method resets the outgoing events.
	*/
	protected void clearOutEvents() {
	}

	/**
	* Returns true if the given state is currently active otherwise false.
	*/
	public boolean isStateActive(State state) {
		switch (state) {
			case main_region_Idle :
				return stateVector[0] == State.main_region_Idle;
			case main_region_Incoming_Call :
				return stateVector[0] == State.main_region_Incoming_Call;
			case main_region_Active_Call :
				return stateVector[0] == State.main_region_Active_Call;
			case main_region_Dismiss_Call :
				return stateVector[0] == State.main_region_Dismiss_Call;
			default :
				return false;
		}
	}

	/**
	* Set the {@link ITimer} for the state machine. It must be set
	* externally on a timed state machine before a run cycle can be correct
	* executed.
	* 
	* @param timer
	*/
	public void setTimer(ITimer timer) {
		this.timer = timer;
	}

	/**
	* Returns the currently used timer.
	* 
	* @return {@link ITimer}
	*/
	public ITimer getTimer() {
		return timer;
	}

	public void timeElapsed(int eventID) {
		timeEvents[eventID] = true;
	}

	public SCIUser getSCIUser() {
		return sCIUser;
	}
	public SCIPhone getSCIPhone() {
		return sCIPhone;
	}

	/* Entry action for statechart 'CallHandling'. */
	private void entryAction() {
	}

	/* Exit action for state 'CallHandling'. */
	private void exitAction() {
	}

	/* The reactions of state Idle. */
	private void reactMain_region_Idle() {
		if (sCIPhone.incoming_call) {
			nextStateIndex = 0;
			stateVector[0] = State.$NullState$;

			sCIUser.operationCallback.justATestOperation();

			nextStateIndex = 0;
			stateVector[0] = State.main_region_Incoming_Call;
		}
	}

	/* The reactions of state Incoming Call. */
	private void reactMain_region_Incoming_Call() {
		if (sCIUser.accept_call) {
			nextStateIndex = 0;
			stateVector[0] = State.$NullState$;

			timer.setTimer(this, 0, 1 * 1000, true);

			nextStateIndex = 0;
			stateVector[0] = State.main_region_Active_Call;
		} else {
			if (sCIUser.dismiss_call) {
				nextStateIndex = 0;
				stateVector[0] = State.$NullState$;

				timer.setTimer(this, 0, 1 * 1000, true);

				nextStateIndex = 0;
				stateVector[0] = State.main_region_Active_Call;
			}
		}
	}

	/* The reactions of state Active Call. */
	private void reactMain_region_Active_Call() {
		if (sCIUser.dismiss_call) {
			nextStateIndex = 0;
			stateVector[0] = State.$NullState$;

			timer.unsetTimer(this, 0);

			timer.setTimer(this, 1, 2 * 1000, false);

			nextStateIndex = 0;
			stateVector[0] = State.main_region_Dismiss_Call;
		} else {
			nextStateIndex = 0;
			stateVector[0] = State.$NullState$;

			timer.unsetTimer(this, 0);

			timer.setTimer(this, 0, 1 * 1000, true);

			nextStateIndex = 0;
			stateVector[0] = State.main_region_Active_Call;
		}
	}

	/* The reactions of state Dismiss Call. */
	private void reactMain_region_Dismiss_Call() {
		if (timeEvents[1]) {
			nextStateIndex = 0;
			stateVector[0] = State.$NullState$;

			timer.unsetTimer(this, 1);

			sCIPhone.duration = 0;

			nextStateIndex = 0;
			stateVector[0] = State.main_region_Idle;
		}
	}

	public void runCycle() {

		clearOutEvents();

		for (nextStateIndex = 0; nextStateIndex < stateVector.length; nextStateIndex++) {

			switch (stateVector[nextStateIndex]) {
				case main_region_Idle :
					reactMain_region_Idle();
					break;
				case main_region_Incoming_Call :
					reactMain_region_Incoming_Call();
					break;
				case main_region_Active_Call :
					reactMain_region_Active_Call();
					break;
				case main_region_Dismiss_Call :
					reactMain_region_Dismiss_Call();
					break;
				default :
					// $NullState$
			}
		}

		clearEvents();
	}
}
