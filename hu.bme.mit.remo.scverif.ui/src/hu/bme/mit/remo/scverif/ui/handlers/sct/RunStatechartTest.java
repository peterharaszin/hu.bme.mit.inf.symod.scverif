//package hu.bme.mit.remo.scverif.ui.handlers.sct;
//
//import static org.junit.Assert.*;
//
//import org.junit.After;
//import org.junit.AfterClass;
//import org.junit.Before;
//import org.junit.BeforeClass;
//import org.junit.Test;
//import org.yakindu.scr.TimerService;
//import org.yakindu.scr.callhandling.CallHandlingStatemachine;
//import org.yakindu.scr.callhandling.ICallHandlingStatemachine.SCIUserOperationCallback;
//
//public class RunStatechartTest {
//
//	@BeforeClass
//	public static void setUpBeforeClass() throws Exception {
//	}
//
//	@AfterClass
//	public static void tearDownAfterClass() throws Exception {
//	}
//
//	@Before
//	public void setUp() throws Exception {
//		CallHandlingStatemachine sm = new CallHandlingStatemachine();
//
////		sm.getSCIUser().setSCIUserOperationCallback(new SCIUserOperationCallback() {
////			@Override
////			public void justATestOperation() {
////				System.out.println("cucc");
////			}
////		});
////		// sm.setTimerService(new TimerService());
////		sm.setTimer(new TimerService());
////		// enter the sm and active the Idle state
////		sm.enter();
////		// Raise an incoming call
////		sm.getSCIPhone().raiseIncoming_call();
////		sm.runCycle();
////		// Accept the call
////		sm.getSCIUser().raiseAccept_call();
////		sm.runCycle();
////		for (int i = 0; i < 50; i++) {
////			Thread.sleep(200);
////			sm.runCycle();
////		}
////		System.out.println(String.format("The phone call took %d s", +sm.getSCIPhone().getDuration()));
////		sm.getSCIUser().raiseDismiss_call();
////		sm.runCycle();		
//	}
//
//	@After
//	public void tearDown() throws Exception {
//	}
//
//	@Test
//	public void test() {
//		fail("Not yet implemented");
//	}
//
//}
