import org.junit.Assert;
import org.junit.Test;
import org.yakindu.scr.TimerService;
import org.yakindu.scr.callhandling.CallHandlingStatemachine;
import org.yakindu.scr.callhandling.ICallHandlingStatemachine.SCIUserOperationCallback;


public class MyTest {

	@Test
	public void test1() {
		// ..
		Assert.assertTrue(true);
	}

	@Test
	public void test2() {
		Assert.fail("simple test failure");
	}
	

	@Test
	public void test3() {
		CallHandlingStatemachine sm = new CallHandlingStatemachine();
		sm.getSCIUser().setSCIUserOperationCallback(new SCIUserOperationCallback() {
			@Override
			public void justATestOperation() {
				Assert.fail("callback test failure");
			}
		});
		// sm.setTimerService(new TimerService());
		sm.setTimer(new TimerService());
		// enter the sm and active the Idle state
		sm.enter();
	}	
	
}
