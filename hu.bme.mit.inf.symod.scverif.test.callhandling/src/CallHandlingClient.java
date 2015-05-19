import org.yakindu.scr.TimerService;
import org.yakindu.scr.callhandling.CallHandlingStatemachine;
import org.yakindu.scr.callhandling.ICallHandlingStatemachine.SCIUserOperationCallback;

public class CallHandlingClient {
	public static void main(String[] args) throws Exception {
		CallHandlingStatemachine sm = new CallHandlingStatemachine();
		sm.getSCIUser().setSCIUserOperationCallback(new SCIUserOperationCallback() {
			@Override
			public void justATestOperation() {
				System.out.println("teststuff");
			}
		});
		// sm.setTimerService(new TimerService());
		sm.setTimer(new TimerService());
		// enter the sm and active the Idle state
		sm.enter();
		// Raise an incoming call
		sm.getSCIPhone().raiseIncoming_call();
		sm.runCycle();
		// Accept the call
		sm.getSCIUser().raiseAccept_call();
		sm.runCycle();
		for (int i = 0; i < 50; i++) {
			Thread.sleep(200);
			sm.runCycle();
		}
		System.out.println(String.format("The phone call took %d s", +sm
				.getSCIPhone().getDuration()));
		sm.getSCIUser().raiseDismiss_call();
		sm.runCycle();
	}
}
