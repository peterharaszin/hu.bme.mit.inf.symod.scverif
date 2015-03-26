package hu.bme.mit.remo.scverif.ui.handlers.sct;

import static org.junit.Assert.*;
import hu.bme.mit.remo.scverif.processing.sct.StatechartAnalyzer;
//import hu.bme.mit.remo.scverif.ui.jobs.DoRemoJobs;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
//import org.yakindu.sct.model.sgraph.Statechart;

public class SCTTest {

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
//		DoRemoJobs doRemoJobs = new DoRemoJobs(null);
//		statechartFromBundle = doRemoJobs.getStatechartFromBundle();
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
