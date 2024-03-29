package hu.bme.mit.inf.symod.scverif.processing.sct;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
//import org.yakindu.sct.model.sgraph.Statechart;

public class SctTest {

	private StatechartAnalyzer stateChartAnalyzer = new StatechartAnalyzer();
//	private Statechart statechartFromBundle;
		
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
//		DoStatechartVerification doStatechartVerification = new DoStatechartVerification(null);
//		statechartFromBundle = doStatechartVerification.getStatechartFromBundle();
//		stateChartAnalyzer.setStatechart(statechartFromBundle);
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void doesContainTimeEventTest() {
		boolean doesContainTimeEventReactionTrigger = stateChartAnalyzer.doesContainTimeEventReactionTrigger();
		assertFalse("Checking whether the statechart contains a Time Event ReactionTrigger", doesContainTimeEventReactionTrigger);
	}

}
