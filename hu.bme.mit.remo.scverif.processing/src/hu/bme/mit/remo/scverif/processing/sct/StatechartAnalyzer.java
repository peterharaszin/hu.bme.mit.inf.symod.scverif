/**
 * 
 */
package hu.bme.mit.remo.scverif.processing.sct;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IFile;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.yakindu.base.expressions.expressions.impl.ElementReferenceExpressionImpl;
import org.yakindu.base.expressions.expressions.impl.FeatureCallImpl;
import org.yakindu.sct.model.sgraph.Declaration;
import org.yakindu.sct.model.sgraph.Effect;
import org.yakindu.sct.model.sgraph.Event;
import org.yakindu.sct.model.sgraph.Reaction;
import org.yakindu.sct.model.sgraph.ReactionProperty;
import org.yakindu.sct.model.sgraph.Region;
import org.yakindu.sct.model.sgraph.Scope;
import org.yakindu.sct.model.sgraph.State;
import org.yakindu.sct.model.sgraph.Statechart;
import org.yakindu.sct.model.sgraph.Transition;
import org.yakindu.sct.model.sgraph.Trigger;
import org.yakindu.sct.model.sgraph.Variable;
import org.yakindu.sct.model.sgraph.Vertex;
import org.yakindu.sct.model.sgraph.impl.EntryImpl;
import org.yakindu.sct.model.sgraph.impl.RegionImpl;
import org.yakindu.sct.model.sgraph.impl.StateImpl;
import org.yakindu.sct.model.sgraph.impl.TransitionImpl;
import org.yakindu.sct.model.stext.stext.AlwaysEvent;
import org.yakindu.sct.model.stext.stext.EventSpec;
import org.yakindu.sct.model.stext.stext.InterfaceScope;
import org.yakindu.sct.model.stext.stext.ReactionTrigger;
import org.yakindu.sct.model.stext.stext.TimeEventSpec;
import org.yakindu.sct.model.stext.stext.impl.EventDefinitionImpl;
import org.yakindu.sct.model.stext.stext.impl.InterfaceScopeImpl;
import org.yakindu.sct.model.stext.stext.impl.OperationDefinitionImpl;
import org.yakindu.sct.model.stext.stext.impl.ReactionTriggerImpl;
import org.yakindu.sct.model.stext.stext.impl.RegularEventSpecImpl;
import org.yakindu.sct.model.stext.stext.impl.SimpleScopeImpl;
import org.yakindu.sct.model.stext.stext.impl.TimeEventSpecImpl;
import org.yakindu.sct.model.stext.stext.impl.VariableDefinitionImpl;

/**
 * Class for the statecharts' static checkings
 * 
 * @author Peter Haraszin
 *
 */
public class StatechartAnalyzer {
    public static final Logger logger = Logger.getLogger("System Modeling Log");
    
    private Statechart statechart;
    private HashMap<Class<? extends EObject>, ArrayList<EObject>> modelElementsCollector;

    public StatechartAnalyzer() {
        this.statechart = null;
    }

    public StatechartAnalyzer(Statechart statechart) {
        setStatechart(statechart);
    }

    public static Statechart getStatechartFromIFile(IFile sctFile) {
        // Loads the resource
        ResourceSet resourceSet = new ResourceSetImpl();
        URI fileURI = URI.createPlatformResourceURI(sctFile.getFullPath().toString(), false);
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

    public boolean doesContainTimeEventReactionTrigger() {
        logger.info("Does it contain a ReactionTrigger which is an instance of a time event?\n");

        TreeIterator<EObject> eAllContents = statechart.eAllContents();
        while (eAllContents.hasNext()) {
            EObject nextEObject = eAllContents.next();

            //          boolean isContainingTimeEvent = (nextEObject.getClass().getName() == "org.yakindu.sct.model.stext.stext.impl.TimeEventSpecImpl");
            boolean isContainingTimeEvent = (nextEObject instanceof org.yakindu.sct.model.stext.stext.impl.TimeEventSpecImpl);
            if (isContainingTimeEvent == true) {
                return true;
            }
        }

        return false;
    }

    public void collectModelElementsIntoMap() {
        logger.info("collecting and inspecting model elements...\n\n");

        modelElementsCollector = new HashMap<Class<? extends EObject>, ArrayList<EObject>>();

        TreeIterator<EObject> eAllContents = statechart.eAllContents();

        while (eAllContents.hasNext()) {
            EObject nextEObject = eAllContents.next();

            Class<? extends EObject> nextEObjectClass = nextEObject.getClass();
            ArrayList<EObject> elementList = modelElementsCollector.get(nextEObjectClass);
            if (elementList == null) {
                elementList = new ArrayList<EObject>();
                modelElementsCollector.put(nextEObjectClass, elementList);
            }

            // logger.info("adding: "+nextEObject.toString() + "\n");

            elementList.add(nextEObject);
        }

        logger.info("\n\n");

        //        ArrayList<? extends EObject> interfaceList = modelElementsCollector.get(org.yakindu.sct.model.stext.stext.impl.InterfaceScopeImpl.class);
        //        for (EObject currentInterfaceEObject : interfaceList) {
        //            org.yakindu.sct.model.stext.stext.impl.InterfaceScopeImpl currentInterface = (org.yakindu.sct.model.stext.stext.impl.InterfaceScopeImpl) currentInterfaceEObject;
        //            logger.info("currentInterface.getName(): " + currentInterface.getName()+"\n");
        //        }
    }

    public LinkedList<ForbiddenElement> getForbiddenElements() {
        LinkedList<ForbiddenElement> forbiddenElementList = new LinkedList<ForbiddenElement>();

        // always/oncycle keywords are forbidden
        ArrayList<ReactionTriggerImpl> reactionTriggers = getReactionTriggers();
        if (reactionTriggers != null) {
            for (ReactionTriggerImpl reactionTriggerImpl : reactionTriggers) {
                forbiddenElementList.addAll(checkReactionTrigger(reactionTriggerImpl));
            }
        }

        return forbiddenElementList;
    }

    /**
     * Check if the current statechart has an EventDefinition with the name passed as a parameter.
     * 
     * @param nameToLookFor
     * @return true if the statechart contains the event with the given name.
     * 
     * @see org.yakindu.sct.model.stext.stext.impl.EventDefinitionImpl
     */
    public boolean hasEventDefinition(String nameToLookFor) {
        ArrayList<EObject> eventList = modelElementsCollector.get(EventDefinitionImpl.class);
        for (EObject currentEventDefEObject : eventList) {
            EventDefinitionImpl currentEvent = (EventDefinitionImpl) currentEventDefEObject;
            if (currentEvent.getName().equals(nameToLookFor)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Check if the current statechart has an InterfaceScope with the name passed as a parameter.
     * 
     * @param nameToLookFor
     * @return true if the statechart contains the interface with the given name.
     * 
     * @see org.yakindu.sct.model.stext.stext.impl.InterfaceScopeImpl
     */
    public boolean hasInterfaceScope(String nameToLookFor) {
        ArrayList<EObject> interfaceList = modelElementsCollector.get(InterfaceScopeImpl.class);
        for (EObject currentEventDefEObject : interfaceList) {
            InterfaceScopeImpl currentEvent = (InterfaceScopeImpl) currentEventDefEObject;
            if (currentEvent.getName().equals(nameToLookFor)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Check if the current statechart has an OperationDefinition with the name passed as a parameter.
     * 
     * @param nameToLookFor
     * @return true if the statechart contains the operation with the given name.
     * 
     * @see org.yakindu.sct.model.stext.stext.impl.OperationDefinitionImpl
     */
    public boolean hasOperationDefinition(String nameToLookFor) {
        ArrayList<EObject> operationList = modelElementsCollector.get(OperationDefinitionImpl.class);
        for (EObject currentEventDefEObject : operationList) {
            OperationDefinitionImpl currentEvent = (OperationDefinitionImpl) currentEventDefEObject;
            if (currentEvent.getName().equals(nameToLookFor)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Check if the current statechart has a VariableDefinition with the name passed as a parameter.
     * 
     * @param nameToLookFor
     * @return true if the statechart contains the variable with the given name.
     * 
     * @see org.yakindu.sct.model.stext.stext.impl.VariableDefinitionImpl
     */
    public boolean hasVariableDefinition(String nameToLookFor) {
        ArrayList<EObject> variableList = modelElementsCollector.get(VariableDefinitionImpl.class);
        for (EObject currentEventDefEObject : variableList) {
            VariableDefinitionImpl currentEvent = (VariableDefinitionImpl) currentEventDefEObject;
            if (currentEvent.getName().equals(nameToLookFor)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Check if the current statechart has a Region with the name passed as a parameter.
     * 
     * @param nameToLookFor
     * @return true if the statechart contains the region with the given name.
     * 
     * @see org.yakindu.sct.model.sgraph.impl.RegionImpl
     */
    public boolean hasRegion(String nameToLookFor) {
        ArrayList<EObject> regionList = modelElementsCollector.get(RegionImpl.class);
        for (EObject currentEventDefEObject : regionList) {
            RegionImpl currentEvent = (RegionImpl) currentEventDefEObject;
            if (currentEvent.getName().equals(nameToLookFor)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Check if the current statechart has an Entry with the name passed as a parameter.
     * 
     * @param nameToLookFor
     * @return true if the statechart contains the entry with the given name.
     * 
     * @see org.yakindu.sct.model.sgraph.impl.EntryImpl
     */
    public boolean hasEntry(String nameToLookFor) {
        ArrayList<EObject> entryList = modelElementsCollector.get(EntryImpl.class);
        for (EObject currentEventDefEObject : entryList) {
            EntryImpl currentEvent = (EntryImpl) currentEventDefEObject;
            if (currentEvent.getName().equals(nameToLookFor)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Get interfaces in the model
     * 
     * @see org.yakindu.sct.model.stext.stext.impl.InterfaceScopeImpl
     * @see http://stackoverflow.com/questions/4581407/how-can-i-convert-arraylistobject-to-arrayliststring/23777137#23777137
     * @return
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public ArrayList<InterfaceScope> getInterfaces() {
        // Doing it the dirty way...
        return (ArrayList) modelElementsCollector.get(InterfaceScopeImpl.class);
    }

    /**
     * Get events in the model
     * 
     * @see org.yakindu.sct.model.stext.stext.impl.EventDefinitionImpl
     * @see http://stackoverflow.com/questions/4581407/how-can-i-convert-arraylistobject-to-arrayliststring/23777137#23777137
     * @return
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public ArrayList<EventDefinitionImpl> getEvents() {
        // Doing it the dirty way...
        return (ArrayList) modelElementsCollector.get(EventDefinitionImpl.class);
    }

    /**
     * Get operations in the model
     * 
     * @see org.yakindu.sct.model.stext.stext.impl.OperationDefinitionImpl
     * @see http://stackoverflow.com/questions/4581407/how-can-i-convert-arraylistobject-to-arrayliststring/23777137#23777137
     * @return
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public ArrayList<OperationDefinitionImpl> getOperations() {
        // Doing it the dirty way...
        return (ArrayList) modelElementsCollector.get(OperationDefinitionImpl.class);
    }

    /**
     * Get variables in the model
     * 
     * @see org.yakindu.sct.model.stext.stext.impl.VariableDefinitionImpl
     * @see http://stackoverflow.com/questions/4581407/how-can-i-convert-arraylistobject-to-arrayliststring/23777137#23777137
     * @return
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public ArrayList<VariableDefinitionImpl> getVariables() {
        // Doing it the dirty way...
        return (ArrayList) modelElementsCollector.get(VariableDefinitionImpl.class);
    }

    /**
     * Get SimpleScopes in the model
     * 
     * @see org.yakindu.sct.model.stext.stext.impl.SimpleScopeImpl
     * @see http://stackoverflow.com/questions/4581407/how-can-i-convert-arraylistobject-to-arrayliststring/23777137#23777137
     * @return
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public ArrayList<SimpleScopeImpl> getSimpleScopes() {
        // Doing it the dirty way...
        return (ArrayList) modelElementsCollector.get(SimpleScopeImpl.class);
    }

    /**
     * Get ReactionTriggers in the model
     * 
     * @see org.yakindu.sct.model.stext.stext.impl.ReactionTriggerImpl
     * @see http://stackoverflow.com/questions/4581407/how-can-i-convert-arraylistobject-to-arrayliststring/23777137#23777137
     * @return
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public ArrayList<ReactionTriggerImpl> getReactionTriggers() {
        // Doing it the dirty way...
        return (ArrayList) modelElementsCollector.get(ReactionTriggerImpl.class);
    }

    /**
     * Get RegularEvents in the model
     * 
     * @see org.yakindu.sct.model.stext.stext.impl.RegularEventSpecImpl
     * @see http://stackoverflow.com/questions/4581407/how-can-i-convert-arraylistobject-to-arrayliststring/23777137#23777137
     * @return
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public ArrayList<RegularEventSpecImpl> getRegularEventSpecs() {
        // Doing it the dirty way...
        return (ArrayList) modelElementsCollector.get(RegularEventSpecImpl.class);
    }

    /**
     * Get TimeEventSpecs in the model
     * 
     * @see org.yakindu.sct.model.stext.stext.impl.TimeEventSpecImpl
     * @see http://stackoverflow.com/questions/4581407/how-can-i-convert-arraylistobject-to-arrayliststring/23777137#23777137
     * @return
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public ArrayList<TimeEventSpecImpl> getTimeEventSpecs() {
        // Doing it the dirty way...
        return (ArrayList) modelElementsCollector.get(TimeEventSpecImpl.class);
    }

    /**
     * Get entries in the model
     * 
     * @see org.yakindu.sct.model.sgraph.impl.EntryImpl
     * @see http://stackoverflow.com/questions/4581407/how-can-i-convert-arraylistobject-to-arrayliststring/23777137#23777137
     * @return
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public ArrayList<EntryImpl> getEntries() {
        // Doing it the dirty way...
        return (ArrayList) modelElementsCollector.get(EntryImpl.class);
    }

    /**
     * Get regions in the model
     * 
     * @see org.yakindu.sct.model.sgraph.impl.RegionImpl
     * @see http://stackoverflow.com/questions/4581407/how-can-i-convert-arraylistobject-to-arrayliststring/23777137#23777137
     * @return
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public ArrayList<RegionImpl> getRegions() {
        // Doing it the dirty way...
        return (ArrayList) modelElementsCollector.get(RegionImpl.class);
    }

    /**
     * Get transitions in the model
     * 
     * @see org.yakindu.sct.model.sgraph.impl.TransitionImpl
     * @see http://stackoverflow.com/questions/4581407/how-can-i-convert-arraylistobject-to-arrayliststring/23777137#23777137
     * @return
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public ArrayList<TransitionImpl> getTransitions() {
        // Doing it the dirty way...
        return (ArrayList) modelElementsCollector.get(TransitionImpl.class);
    }

    /**
     * Get transitions in the model
     * 
     * @see org.yakindu.sct.model.sgraph.impl.StateImpl
     * @see http://stackoverflow.com/questions/4581407/how-can-i-convert-arraylistobject-to-arrayliststring/23777137#23777137
     * @return
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public ArrayList<StateImpl> getStates() {
        // Doing it the dirty way...
        return (ArrayList) modelElementsCollector.get(StateImpl.class);
    }

    /**
     * Get FeatureCalls in the model
     * 
     * @see org.yakindu.sct.model.sgraph.impl.FeatureCallImpl
     * @see http://stackoverflow.com/questions/4581407/how-can-i-convert-arraylistobject-to-arrayliststring/23777137#23777137
     * @return
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public ArrayList<FeatureCallImpl> getFeatureCalls() {
        // Doing it the dirty way...
        return (ArrayList) modelElementsCollector.get(FeatureCallImpl.class);
    }

    /**
     * Get ElementReferenceExpressions in the model
     * 
     * @see org.yakindu.sct.model.sgraph.impl.ElementReferenceExpressionImpl
     * @see http://stackoverflow.com/questions/4581407/how-can-i-convert-arraylistobject-to-arrayliststring/23777137#23777137
     * @return
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public ArrayList<ElementReferenceExpressionImpl> getElementReferenceExpressions() {
        // Doing it the dirty way...
        return (ArrayList) modelElementsCollector.get(ElementReferenceExpressionImpl.class);
    }

    /**
     * Get the map containing all the elements in the model as Class-EObject pairs
     * @return
     */
    public HashMap<Class<? extends EObject>, ArrayList<EObject>> getModelElementsCollector() {
        return modelElementsCollector;
    }

    /**
     * Check if the ReactionTrigger contains forbidden elements. For example, the usage of oncycle and always keywords is forbidden.
     * Both "always" and "oncycle" is mapped to an AlwaysEvent.
     * 
     * @see https://code.google.com/a/eclipselabs.org/p/yakindu/source/browse/SCT2/trunk/plugins/org.yakindu.sct.model.stext/src/org/yakindu/sct/model/stext/SText.xtext#156
     * 
     * @throws ForbiddenElement 
     * @see http://yakindu.eclipselabs.org.codespot.com/svn-history/r2842/SCT2/trunk/plugins/org.yakindu.sct.model.stext/src/org/yakindu/sct/model/stext/validation/STextJavaValidator.java
     * @see org.yakindu.sct.model.stext.stext.StextPackage#getAlwaysEvent()
     */
    public LinkedList<ForbiddenElement> checkReactionTrigger(ReactionTrigger reactionTrigger) {
        EList<EventSpec> triggers = reactionTrigger.getTriggers();

        LinkedList<ForbiddenElement> forbiddenElementList = new LinkedList<ForbiddenElement>();

        if (triggers == null || triggers.isEmpty()) {
            EObject eContainer = reactionTrigger.eContainer();
            
            if(eContainer instanceof Transition){
                Transition containerTransition = (Transition) reactionTrigger.eContainer();
                Vertex sourceVertex = containerTransition.getSource();

                // it's only forbidden if its parent is a State (there are cases where Pseudostates are allowed)
                if (sourceVertex instanceof State) {
                    String sourceVertexName = sourceVertex.getName();
                    forbiddenElementList.add(new ForbiddenElement(
                            "Trigger can not be empty! (source vertex name: " + sourceVertexName + ")"));
                }                
            }
            else if(eContainer instanceof State){ // akkor egy csúcsban van benne a belső állapotátmenet (beleírta a dobozba)
                Vertex sourceVertex = (Vertex) eContainer;

                // it's only forbidden if its parent is a State (there are cases where Pseudostates are allowed)
                if (sourceVertex instanceof State) {
                    String sourceVertexName = sourceVertex.getName();
                    forbiddenElementList.add(new ForbiddenElement(
                            "Trigger can not be empty! (source vertex name: " + sourceVertexName + ")"));
                }                
                
            }
        }

        for (EventSpec eventSpec : triggers) {
            // Do not allow oncycle and always as event for reactions.
            if (eventSpec instanceof AlwaysEvent) {
                forbiddenElementList.add(new ForbiddenElement(
                        "The usage of always/oncycle keyword (or triggerless transitions) is forbidden!"));
            }
        }

        return forbiddenElementList;
    }

    /**
     * Process Yakindu statechart
     * 
     * @param statechart
     */
    public void processStatechart() {
        String nameOfStatechart = statechart.getName();

        LinkedList<ForbiddenElement> checkForbiddenElements = getForbiddenElements();

        logger.info("=========================\n");

        logger.info("Checking forbidden elements...\n");
        if (checkForbiddenElements == null) {
            logger.info("There were no forbidden elements\n");
        } else {
            for (ForbiddenElement forbiddenElement : checkForbiddenElements) {
                logger.info(forbiddenElement.toString() + "\n");
            }
        }

        logger.info("=========================\n");

        String specification = statechart.getSpecification();
        logger.info("statechart.getName(): \n" + nameOfStatechart + "\n");
        logger.info("statechart.getSpecification(): \n" + specification + "\n");
        logger.info("iterating through '" + nameOfStatechart + "' statechart.getRegions():\n");

        logger.info("checkScopes(): ");
        checkScopes();

        EList<Region> regions = statechart.getRegions();
        for (Region region : regions) {

            String regionName = region.getName();
            logger.info("current region's name: '" + regionName + "'\n");
            EList<Vertex> vertices = region.getVertices();

            logger.info("iterating through region.getVertices() in: '" + regionName + "'\n");
            for (Vertex vertex : vertices) {
                processState(vertex);
            }
        }

        logger.info("iterating through '" + nameOfStatechart + "' statechart's reactions: \n");
        EList<Reaction> reactions = statechart.getReactions();
        for (Reaction reaction : reactions) {
            processReaction(reaction);
        }

        logger.info("iterating through '" + nameOfStatechart + "' statechart's local reactions: \n");
        EList<Reaction> localReactions = statechart.getLocalReactions();
        for (Reaction reaction : localReactions) {
            processReaction(reaction);
        }

        EList<Scope> scopes = statechart.getScopes();
        for (Scope scope : scopes) {
            processScope(scope);
        }

        logger.info(
                "does it contain time event reaction trigger? --> " + doesContainTimeEventReactionTrigger() + "\n");

        logger.info("collect and inspect model elements");
        collectModelElementsIntoMap();
    }

    private void processReaction(Reaction reaction) {
        System.out.println("Processing reaction...");

        logger.info("reaction's properties: \n");
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
        LinkedList<ForbiddenElement> forbiddenElementList = checkReactionTrigger((ReactionTrigger) trigger);

        for (ForbiddenElement forbiddenElement : forbiddenElementList) {
            logger.info("forbidden element has been found: " + forbiddenElement.getMessage() + "\n");
        }

        logger.info("		trigger.getClass().getName();: '" + trigger.getClass().getName() + "' \n");
        // EList<EObject> eContents = trigger.eContents();
        //        for (EObject eObject : eContents) {
        //            System.out.println("eObject.toString(): " + eObject.toString());
        //            System.out.println("eObject.eClass().getClass().getName(): " + eObject.eClass().getClass().getName());
        //        }
        // org.yakindu.sct.model.stext.stext.impl.TimeEventSpecImpl
        TreeIterator<EObject> eAllContents = trigger.eAllContents();
        while (eAllContents.hasNext()) {
            EObject eObject = eAllContents.next();
            boolean isContainingTimeEvent = (eObject instanceof TimeEventSpec);
            if (isContainingTimeEvent) {
                System.out.println("eObject instanceof TimeEventSpec: " + isContainingTimeEvent);
            }
        }
    }

    private void processEffect(Effect effect) {
        logger.info("		effect.getClass().getName(): '" + effect.getClass().getName() + "' \n");
    }

    private void processReactionProperty(ReactionProperty reactionProperty) {
        logger.info("		reactionProperty.getClass().getName(): \n" + reactionProperty.getClass().getName());
    }

    public void processScope(Scope scope) {
        logger.info("scope.eClass().getName(): '" + scope.eClass().getName() + "'\n");
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
        logger.info("      event's name: " + event.getName() + "\n");
    }

    public void processVariable(Variable variable) {
        logger.info("      variable's name: " + variable.getName() + "\n");
    }

    public void processDeclaration(Declaration declaration) {
        logger.info("      declaration's name: " + declaration.getName() + "\n");
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
        EList<Transition> vertexIncomingTransitions = vertex.getIncomingTransitions();

        EList<Transition> vertexOutgoingTransitions = vertex.getOutgoingTransitions();

        logger.info("\n<-- Outgoing transitions:\n");
        for (Transition currentOutgoingTransition : vertexOutgoingTransitions) {
            processTransition(currentOutgoingTransition);
        }

        Region parentRegion = vertex.getParentRegion();
        logger.info("»» current vertexName: '" + vertexName + "'"
                + (vertexName.length() == 0 ? " (entry state!)" : "") + "\n");
        logger.info(parentRegion.toString());

        logger.info("\n--> Incoming transitions:\n");
        for (Transition currentIncomingTransition : vertexIncomingTransitions) {
            processTransition(currentIncomingTransition);
        }
    }

    public void processTransition(Transition transition) {
        Vertex transitionTarget = transition.getTarget();
        String transitionTargetName = transitionTarget.getName();
        String transitionSpecification = transition.getSpecification();
        Trigger transitionTrigger = transition.getTrigger();

        logger.info("transition's target: '" + transitionTargetName + "',\n"
                + "transition's specification: '" + transitionSpecification + "', " + "\n");

        if (transitionTrigger != null) {
            logger.info("\t\ttransition's trigger: ");
            logger.info(transitionTrigger.toString() + "\n");
            processTrigger(transitionTrigger);
        }
    }

    public Statechart getStatechart() {
        return statechart;
    }

    public void setStatechart(Statechart statechart) {
        this.statechart = statechart;
        collectModelElementsIntoMap();
    }

    public void checkScopes() {
        logger.info("checkScopes:\n");
        EList<Scope> scopes = statechart.getScopes();

        //        EList<Reaction> reactions = statechart.getReactions();
        //        for (Reaction reaction : reactions) {
        //            checkReactionTrigger(reactionTrigger);
        //        }

        for (Scope scope : scopes) {
            if (scope instanceof InterfaceScope) {
                InterfaceScope iScope = (InterfaceScope) scope;
                logger.info("iScope.getName(): " + iScope.getName() + "\n");
                logger.info("iScope.getEvents(): " + iScope.getEvents() + "\n");
                logger.info("iScope.getVariables(): " + iScope.getVariables() + "\n");
            }
        }
    }
}
