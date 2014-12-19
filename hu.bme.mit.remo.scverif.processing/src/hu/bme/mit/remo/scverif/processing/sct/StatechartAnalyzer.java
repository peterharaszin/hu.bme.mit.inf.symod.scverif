/**
 * 
 */
package hu.bme.mit.remo.scverif.processing.sct;

import java.util.Iterator;

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
		EList<Region> regions = statechart.getRegions();
		EList<Reaction> reactions = statechart.getReactions();
		String specification = statechart.getSpecification();
		EList<Scope> scopes = statechart.getScopes();
		temporaryStringBuilder.append("statechart.getName(): \n"
				+ nameOfStatechart + "\n");
		temporaryStringBuilder.append("statechart.getSpecification(): \n"
				+ specification + "\n");
		temporaryStringBuilder.append("iterating through '" + nameOfStatechart
				+ "' statechart.getRegions():\n");
		for (Iterator<Region> regionsIterator = regions.iterator(); regionsIterator
				.hasNext();) {
			Region region = (Region) regionsIterator.next();
			String regionName = region.getName();
			temporaryStringBuilder.append("current region's name: '"
					+ regionName + "'\n");
			EList<Vertex> vertices = region.getVertices();
			temporaryStringBuilder
					.append("iterating through region.getVertices() in: '"
							+ regionName + "'\n");
			for (Iterator<Vertex> verticesIterator = vertices.iterator(); verticesIterator
					.hasNext();) {
				Vertex vertex = (Vertex) verticesIterator.next();
				processState(vertex);
			}
		}
		temporaryStringBuilder.append("iterating through '" + nameOfStatechart
				+ "' statechart's reactions: \n");
		for (Iterator<Reaction> reactionsIterator = reactions.iterator(); reactionsIterator
				.hasNext();) {
			Reaction reaction = (Reaction) reactionsIterator.next();
			processReaction(reaction);
		}
		EList<Reaction> localReactions = statechart.getLocalReactions();
		temporaryStringBuilder.append("iterating through '" + nameOfStatechart
				+ "' statechart's local reactions: \n");
		for (Iterator<Reaction> localReactionsIterator = localReactions
				.iterator(); localReactionsIterator.hasNext();) {
			Reaction reaction = (Reaction) localReactionsIterator.next();
			processReaction(reaction);
		}

		for (Scope scope : scopes) {
			processScope(scope);
		}

		// temporary, ugly solution
		System.out.println(temporaryStringBuilder.toString());

	}

	private void processReaction(Reaction reaction) {
		System.out.println("Processing reaction...");
		EList<ReactionProperty> reactionProperties = reaction.getProperties();
		Effect effect = reaction.getEffect();
		Trigger trigger = reaction.getTrigger();

		temporaryStringBuilder.append("reaction's properties: \n");
		for (Iterator<ReactionProperty> reactionPropertiesIterator = reactionProperties
				.iterator(); reactionPropertiesIterator.hasNext();) {
			ReactionProperty reactionProperty = (ReactionProperty) reactionPropertiesIterator
					.next();
			processReactionProperty(reactionProperty);
		}

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
		for (Iterator<Variable> variableIterator = interfaceVariables
				.iterator(); variableIterator.hasNext();) {
			Variable variable = (Variable) variableIterator.next();
			processVariable(variable);
		}
		EList<Event> interfaceEvents = scope.getEvents();
		for (Iterator<Event> eventsIterator = interfaceEvents.iterator(); eventsIterator
				.hasNext();) {
			Event event = (Event) eventsIterator.next();
			processEvent(event);
		}
		EList<Declaration> interfaceDeclarations = scope.getDeclarations();
		for (Iterator<Declaration> declarationIterator = interfaceDeclarations
				.iterator(); declarationIterator.hasNext();) {
			Declaration declaration = (Declaration) declarationIterator.next();
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
		Region parentRegion = vertex.getParentRegion();
		temporaryStringBuilder.append("»» current vertexName: '" + vertexName
				+ "'" + (vertexName.length() == 0 ? " (entry state!)" : "")
				+ "\n");

		temporaryStringBuilder.append("\n<-- Outgoing transitions:\n");
		for (Iterator<Transition> vertexOutgoingTransitionsIterator = vertexOutgoingTransitions
				.iterator(); vertexOutgoingTransitionsIterator.hasNext();) {
			Transition currentOutgoingTransition = (Transition) vertexOutgoingTransitionsIterator
					.next();
			processTransition(currentOutgoingTransition);
		}

		temporaryStringBuilder.append("\n--> Incoming transitions:\n");
		for (Iterator<Transition> vertexIncomingTransitionsIterator = vertexIncomingTransitions
				.iterator(); vertexIncomingTransitionsIterator.hasNext();) {
			Transition currentIncomingTransition = (Transition) vertexIncomingTransitionsIterator
					.next();
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

	public boolean interfaceSpecificationEqualsTo(String anotherSpecification) {
		// ...
		return false;
	}
}
