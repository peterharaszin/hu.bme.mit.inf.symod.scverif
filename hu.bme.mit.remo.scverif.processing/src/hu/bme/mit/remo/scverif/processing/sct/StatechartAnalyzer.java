/**
 * 
 */
package hu.bme.mit.remo.scverif.processing.sct;

import java.util.regex.Pattern;

import org.eclipse.core.resources.IFile;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.yakindu.sct.model.sgraph.Declaration;
import org.yakindu.sct.model.sgraph.Effect;
import org.yakindu.sct.model.sgraph.Event;
import org.yakindu.sct.model.sgraph.Reaction;
import org.yakindu.sct.model.sgraph.ReactionProperty;
import org.yakindu.sct.model.sgraph.Region;
import org.yakindu.sct.model.sgraph.Scope;
import org.yakindu.sct.model.sgraph.Statechart;
import org.yakindu.sct.model.sgraph.Transition;
import org.yakindu.sct.model.sgraph.Trigger;
import org.yakindu.sct.model.sgraph.Variable;
import org.yakindu.sct.model.sgraph.Vertex;

/**
 * TODO: fix ugly model processing...
 * 
 * @author Pete
 *
 */
public class StatechartAnalyzer {
	// temporary, REALLY ugly solution for writing out results
	StringBuilder temporaryStringBuilder = new StringBuilder();
	Statechart statechart;

	public StatechartAnalyzer() {
		this.statechart = null;
	}

	public StatechartAnalyzer(Statechart statechart) {
		this.statechart = statechart;
	}

	public static Statechart getStatechartFromIFile(IFile sctFile) {
		// Loads the resource
		ResourceSet resourceSet = new ResourceSetImpl();
		URI fileURI = URI.createPlatformResourceURI(sctFile.getFullPath()
				.toString(), false);
		Resource res = resourceSet.getResource(fileURI, true);

		// Process SCT model
		for (EObject content : res.getContents()) {
			// EObject content = res.getContents().get(0);
			// if it's an implementation of the model object 'Statechart'.
			if (content instanceof org.yakindu.sct.model.sgraph.impl.StatechartImpl) {
				return (Statechart) content;
			}
			// if (content instanceof
			// org.eclipse.gmf.runtime.notation.impl.DiagramImpl) {
			// // ez nem fog kelleni...
			// }
		}
		return null;
	}

	/**
	 * Process Yakindu statechart
	 * 
	 * @param statechart
	 */
	public void processStatechart() {
		temporaryStringBuilder.setLength(0);

		String nameOfStatechart = statechart.getName();
				
		String specification = statechart.getSpecification();
		temporaryStringBuilder.append("statechart.getName(): \n"
				+ nameOfStatechart + "\n");
		temporaryStringBuilder.append("statechart.getSpecification(): \n"
				+ specification + "\n");
		temporaryStringBuilder.append("iterating through '" + nameOfStatechart
				+ "' statechart.getRegions():\n");
		
		
		EList<Region> regions = statechart.getRegions();		
		for (Region region : regions) {
			String regionName = region.getName();
			temporaryStringBuilder.append("current region's name: '"
					+ regionName + "'\n");
			EList<Vertex> vertices = region.getVertices();
			
			temporaryStringBuilder
			.append("iterating through region.getVertices() in: '"
					+ regionName + "'\n");
			for (Vertex vertex : vertices) {
				processState(vertex);
			}			
		}
		
		temporaryStringBuilder.append("iterating through '" + nameOfStatechart
				+ "' statechart's reactions: \n");
		EList<Reaction> reactions = statechart.getReactions();
		for (Reaction reaction : reactions) {
			processReaction(reaction);			
		}
		
		temporaryStringBuilder.append("iterating through '" + nameOfStatechart
				+ "' statechart's local reactions: \n");
		EList<Reaction> localReactions = statechart.getLocalReactions();
		for (Reaction reaction : localReactions) {
			processReaction(reaction);
		}
		
		EList<Scope> scopes = statechart.getScopes();
		for (Scope scope : scopes) {
			processScope(scope);
		}

		// temporary, ugly solution
		System.out.println(temporaryStringBuilder.toString());

	}

	private void processReaction(Reaction reaction) {
		System.out.println("Processing reaction...");
		
		temporaryStringBuilder.append("reaction's properties: \n");
		EList<ReactionProperty> reactionProperties = reaction.getProperties();		
		for (ReactionProperty reactionProperty : reactionProperties) {
			processReactionProperty(reactionProperty);			
		}
		
		Effect effect = reaction.getEffect();
		Trigger trigger = reaction.getTrigger();


		processEffect(effect);
		processTrigger(trigger);
	}

	private void processTrigger(Trigger trigger) {
		temporaryStringBuilder.append("		trigger.getClass().getName();: '"
				+ trigger.getClass().getName() + "' \n");
		EList<EObject> eContents = trigger.eContents();
		for (EObject eObject : eContents) {
			System.out.println("eObject.toString(): " + eObject.toString());
			System.out.println("eObject.eClass().getClass().getName(): "
					+ eObject.eClass().getClass().getName());
		}
		// org.yakindu.sct.model.stext.stext.impl.TimeEventSpecImpl
		TreeIterator<EObject> eAllContents = trigger.eAllContents();
		while (eAllContents.hasNext()) {
			EObject eObject = eAllContents.next();
			System.out.println("eObject.toString(): " + eObject.toString());
			System.out.println("eObject.getClass().getName(): "
					+ eObject.getClass().getName());
			boolean isContainingTimeEvent = (eObject.getClass().getName() == "org.yakindu.sct.model.stext.stext.impl.TimeEventSpecImpl");
			System.out
					.println("eObject.getClass().getName() == 'org.yakindu.sct.model.stext.stext.impl.TimeEventSpecImpl': "
							+ isContainingTimeEvent);
		}
	}

	private void processEffect(Effect effect) {
		temporaryStringBuilder.append("		effect.getClass().getName(): '"
				+ effect.getClass().getName() + "' \n");
	}

	private void processReactionProperty(ReactionProperty reactionProperty) {
		temporaryStringBuilder
				.append("		reactionProperty.getClass().getName(): \n"
						+ reactionProperty.getClass().getName());
	}

	public void processScope(Scope scope) {
		temporaryStringBuilder.append("scope.eClass().getName(): \n"
				+ scope.eClass().getName() + "\n");
		EList<Variable> interfaceVariables = scope.getVariables();
		
		for (Variable variable : interfaceVariables) {
			processVariable(variable);
		}
		
		EList<Event> interfaceEvents = scope.getEvents();
		for (Event event : interfaceEvents) {
			processEvent(event);
		}
	
		EList<Declaration> interfaceDeclarations = scope.getDeclarations();
		for (Declaration declaration : interfaceDeclarations) {
			processDeclaration(declaration);
		}
	}

	public void processEvent(Event event) {
		temporaryStringBuilder.append("      event's name: " + event.getName()
				+ "\n");
	}

	public void processVariable(Variable variable) {
		temporaryStringBuilder.append("      variable's name: "
				+ variable.getName() + "\n");
	}

	public void processDeclaration(Declaration declaration) {
		temporaryStringBuilder.append("      declaration's name: "
				+ declaration.getName() + "\n");
	}

	/**
	 * @todo NEM túl értelmes megoldás...
	 * @param vertex
	 * @return
	 */
	public boolean isEntryState(Vertex vertex) {
		return (vertex.getName().length() == 0);
	}

	/**
	 * Process Statechart state (Vertex)
	 * 
	 * @param vertex
	 */
	public void processState(Vertex vertex) {
		String vertexName = vertex.getName();
		EList<Transition> vertexIncomingTransitions = vertex
				.getIncomingTransitions();
		
		EList<Transition> vertexOutgoingTransitions = vertex
				.getOutgoingTransitions();
		
		temporaryStringBuilder.append("\n<-- Outgoing transitions:\n");
		for (Transition currentOutgoingTransition : vertexOutgoingTransitions) {
			processTransition(currentOutgoingTransition);
		}
		
		Region parentRegion = vertex.getParentRegion();
		temporaryStringBuilder.append("»» current vertexName: '" + vertexName
				+ "'" + (vertexName.length() == 0 ? " (entry state!)" : "")
				+ "\n");
		temporaryStringBuilder.append(parentRegion);

		temporaryStringBuilder.append("\n--> Incoming transitions:\n");
		for (Transition currentIncomingTransition : vertexIncomingTransitions) {
			processTransition(currentIncomingTransition);
		}
	}

	public void processTransition(Transition transition) {
		Vertex transitionTarget = transition.getTarget();
		String transitionTargetName = transitionTarget.getName();
		String transitionSpecification = transition.getSpecification();
		Trigger transitionTrigger = transition.getTrigger();

		temporaryStringBuilder.append("transition's target: '"
				+ transitionTargetName + "',\n"
				+ "transition's specification: '" + transitionSpecification
				+ "', " + "\n");

		if (transitionTrigger != null) {
			temporaryStringBuilder.append("\t\ttransition's trigger: ");
			temporaryStringBuilder.append(transitionTrigger.toString() + "\n");
			processTrigger(transitionTrigger);
		}
	}

	public Statechart getStatechart() {
		return statechart;
	}

	public void setStatechart(Statechart statechart) {
		this.statechart = statechart;
		temporaryStringBuilder.setLength(0);
	}

	public boolean doesContainTimeEventReactionTrigger() {		
		TreeIterator<EObject> eAllContents = statechart.eAllContents();			
		while (eAllContents.hasNext()) {
			EObject nextEObject = eAllContents.next();
			boolean isContainingTimeEvent = (nextEObject.getClass().getName() == "org.yakindu.sct.model.stext.stext.impl.TimeEventSpecImpl");
			if (isContainingTimeEvent == true) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Chcek whether the provided interface equals to the statechart's original interface
	 * 
	 * @param anotherSpecification
	 * @return
	 */
	public boolean interfaceSpecificationEqualsTo(String anotherSpecification) {	
		// using Pattern.split() may perform better than the StringTokenizer:
		// http://stackoverflow.com/questions/691184/scanner-vs-stringtokenizer-vs-string-split/691224#691224
		// "Using String.split() is convenient as you can tokenise and get the result in a single line. But it is sub-optimal in that it must recompile the regular expression each time. A possible gain is to compile the pattern once, then call Pattern.split():"
		Pattern p = Pattern.compile("\\s+");
		
		String specification = statechart.getSpecification();
		String[] originalSpecificationTokens = p.split(specification);
		String[] anotherSpecificationTokens = p.split(anotherSpecification);
		
		if(originalSpecificationTokens.length != anotherSpecificationTokens.length){
			System.err.println("The number of tokens don't match: original interface's tokens: "+originalSpecificationTokens.length+", the other interface's tokens: "+anotherSpecificationTokens.length);
			return false;
		}

		for (int i = 0; i < anotherSpecificationTokens.length; i++) {
			if(!originalSpecificationTokens[i].equals(anotherSpecificationTokens[i])){
				System.err.println("The current token doesn't match: original token: "+originalSpecificationTokens[i]+", the other interface's current token: "+anotherSpecificationTokens[i]);
				return false;
			}
		}
		return true;
	}
}
