/**
 * 
 */
package hu.bme.mit.remo.scverif.processing.sct;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.logging.Logger;

import org.eclipse.core.resources.IFile;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.yakindu.base.base.NamedElement;
import org.yakindu.base.expressions.expressions.ElementReferenceExpression;
import org.yakindu.base.expressions.expressions.FeatureCall;
import org.yakindu.sct.model.sgraph.Declaration;
import org.yakindu.sct.model.sgraph.Effect;
import org.yakindu.sct.model.sgraph.Entry;
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
import org.yakindu.sct.model.stext.stext.AlwaysEvent;
import org.yakindu.sct.model.stext.stext.EventDefinition;
import org.yakindu.sct.model.stext.stext.EventSpec;
import org.yakindu.sct.model.stext.stext.InterfaceScope;
import org.yakindu.sct.model.stext.stext.OperationDefinition;
import org.yakindu.sct.model.stext.stext.ReactionTrigger;
import org.yakindu.sct.model.stext.stext.RegularEventSpec;
import org.yakindu.sct.model.stext.stext.SimpleScope;
import org.yakindu.sct.model.stext.stext.TimeEventSpec;
import org.yakindu.sct.model.stext.stext.VariableDefinition;

/**
 * Class for the statecharts' static checkings
 * 
 * @author Peter Haraszin
 *
 */
public class StatechartAnalyzer {
    public static final Logger logger = Logger.getLogger("System Modeling Log");

    private Statechart statechart;
    private HashMap<Class<? extends EObject>, ArrayList<EObject>> modelElementsInAMap;

    public StatechartAnalyzer() {
        this.statechart = null;
    }

    public StatechartAnalyzer(Statechart statechart) {
        setStatechart(statechart);
    }  
    
    public static Statechart getStatechartFromUri(URI sctFileURI) {
//        sctFileURI.
        
        // Loads the resource
        ResourceSetImpl resourceSet = new ResourceSetImpl();
        Resource res = resourceSet.getResource(sctFileURI, true);

        // Process SCT model
        for (EObject content : res.getContents()) {
            // check if it's an implementation of the model object 'Statechart'.
            if (content instanceof Statechart) {
                return (Statechart) content;
            }
            // if (content instanceof
            // org.eclipse.gmf.runtime.notation.Diagram) {
            // // ez nem fog kelleni...
            // }
        }
        return null;        
    }
    
    public static Statechart getStatechartFromPath(Path sctFilePath) {
        // now we need to create a file URI (not a platform resource URI)
        URI sctFileURI = URI.createFileURI(sctFilePath.toUri().getPath());
        return getStatechartFromUri(sctFileURI);
    }
    
    public static Statechart getStatechartFromIFile(IFile sctFile) {
        URI sctFileURI = URI.createPlatformResourceURI(sctFile.getFullPath().toString(), false);
        return getStatechartFromUri(sctFileURI);
    }

    public boolean doesContainTimeEventReactionTrigger() {
        logger.info("Does it contain a ReactionTrigger which is an instance of a time event?\n");

        TreeIterator<EObject> eAllContents = statechart.eAllContents();
        while (eAllContents.hasNext()) {
            EObject nextEObject = eAllContents.next();

            //          boolean isContainingTimeEvent = (nextEObject.getClass().getName() == "org.yakindu.sct.model.stext.stext.TimeEventSpec");
            boolean isContainingTimeEvent = (nextEObject instanceof org.yakindu.sct.model.stext.stext.TimeEventSpec);
            if (isContainingTimeEvent == true) {
                return true;
            }
        }

        return false;
    }

    public static HashMap<Class<? extends EObject>, ArrayList<EObject>> collectModelElementsIntoMap(Statechart statechart) {
        logger.info("collecting and inspecting model elements...\n\n");

        HashMap<Class<? extends EObject>, ArrayList<EObject>> modelElementsMap = new HashMap<Class<? extends EObject>, ArrayList<EObject>>();

        TreeIterator<EObject> eAllContents = statechart.eAllContents();

        while (eAllContents.hasNext()) {
            EObject nextEObject = eAllContents.next();

            Class<? extends EObject> nextEObjectClass = nextEObject.getClass();
            // get which model object it represents (we would like to get the interface,
            // not the concrete implementation)
            @SuppressWarnings("unchecked")
            Class<? extends EObject> implementedInterfaceClassObject = (Class<? extends EObject>) nextEObjectClass.getInterfaces()[0];
            // ArrayList<EObject> elementList = modelElementsMap.get(nextEObjectClass);
            ArrayList<EObject> elementList = modelElementsMap.get(implementedInterfaceClassObject);
            if (elementList == null) {
                elementList = new ArrayList<EObject>();
                modelElementsMap.put(implementedInterfaceClassObject, elementList);
            }

            // logger.info("adding: "+nextEObject.toString() + "\n");

            elementList.add(nextEObject);
        }

        logger.info("\n\n");

        //                ArrayList<? extends EObject> interfaceList = modelElementsInAMap.get(org.yakindu.sct.model.stext.stext.InterfaceScope.class);
        //                for (EObject currentInterfaceEObject : interfaceList) {
        //                    org.yakindu.sct.model.stext.stext.InterfaceScope currentInterface = (org.yakindu.sct.model.stext.stext.InterfaceScope) currentInterfaceEObject;
        //                    logger.info("currentInterface.getName(): " + currentInterface.getName()+"\n");
        //                }
        return modelElementsMap;
    }

    public LinkedList<ForbiddenElement> getForbiddenElements() {
        LinkedList<ForbiddenElement> forbiddenElementList = new LinkedList<ForbiddenElement>();
        
        // always/oncycle keywords are forbidden
        ArrayList<ReactionTrigger> reactionTriggers = getReactionTriggers();
        if (reactionTriggers != null) {
            for (ReactionTrigger reactionTrigger : reactionTriggers) {
                forbiddenElementList.addAll(getForbiddenElementsInReactionTrigger(reactionTrigger));
            }
        }
        
        return forbiddenElementList;
    }
    
    public static LinkedList<ForbiddenElement> getForbiddenElements(HashMap<Class<? extends EObject>, ArrayList<EObject>> modelElementsInAMap) {
        ArrayList<EObject> reactionTriggers = modelElementsInAMap.get(ReactionTrigger.class);
        
        LinkedList<ForbiddenElement> forbiddenElementList = new LinkedList<ForbiddenElement>();

        // always/oncycle keywords are forbidden
        if (reactionTriggers != null) {
            for (EObject reactionTriggerEObject : reactionTriggers) {
                ReactionTrigger reactionTrigger = (ReactionTrigger) reactionTriggerEObject;
                forbiddenElementList.addAll(getForbiddenElementsInReactionTrigger(reactionTrigger));
            }
        }

        return forbiddenElementList;        
    }
    
    public static LinkedList<ForbiddenElement> getForbiddenElements(Statechart statechart) {
        HashMap<Class<? extends EObject>, ArrayList<EObject>> modelElementsInAMap = collectModelElementsIntoMap(statechart);
        
        return getForbiddenElements(modelElementsInAMap);
    }

    /**
     * Check if the current statechart has an EventDefinition with the name passed as a parameter.
     * 
     * @param nameToLookFor
     * @return true if the statechart contains the event with the given name.
     * 
     * @see org.yakindu.sct.model.stext.stext.EventDefinition
     */
    public boolean hasEventDefinition(String nameToLookFor) {
        ArrayList<EObject> eventList = modelElementsInAMap.get(EventDefinition.class);
        for (EObject currentEventDefEObject : eventList) {
            EventDefinition currentEvent = (EventDefinition) currentEventDefEObject;
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
     * @see org.yakindu.sct.model.stext.stext.InterfaceScope
     */
    public boolean hasInterfaceScope(String nameToLookFor) {
        ArrayList<EObject> interfaceList = modelElementsInAMap.get(InterfaceScope.class);
        for (EObject currentEventDefEObject : interfaceList) {
            InterfaceScope currentEvent = (InterfaceScope) currentEventDefEObject;
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
     * @see org.yakindu.sct.model.stext.stext.OperationDefinition
     */
    public boolean hasOperationDefinition(String nameToLookFor) {
        ArrayList<EObject> operationList = modelElementsInAMap.get(OperationDefinition.class);
        for (EObject currentEventDefEObject : operationList) {
            OperationDefinition currentEvent = (OperationDefinition) currentEventDefEObject;
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
     * @see org.yakindu.sct.model.stext.stext.VariableDefinition
     */
    public boolean hasVariableDefinition(String nameToLookFor) {
        ArrayList<EObject> variableList = modelElementsInAMap.get(VariableDefinition.class);
        for (EObject currentEventDefEObject : variableList) {
            VariableDefinition currentEvent = (VariableDefinition) currentEventDefEObject;
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
     * @see org.yakindu.sct.model.sgraph.Region
     */
    public boolean hasRegion(String nameToLookFor) {
        ArrayList<EObject> regionList = modelElementsInAMap.get(Region.class);
        for (EObject currentEventDefEObject : regionList) {
            Region currentEvent = (Region) currentEventDefEObject;
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
     * @see org.yakindu.sct.model.sgraph.Entry
     */
    public boolean hasEntry(String nameToLookFor) {
        ArrayList<EObject> entryList = modelElementsInAMap.get(Entry.class);
        for (EObject currentEventDefEObject : entryList) {
            Entry currentEvent = (Entry) currentEventDefEObject;
            if (currentEvent.getName().equals(nameToLookFor)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Get interfaces in the model
     * 
     * @see org.yakindu.sct.model.stext.stext.InterfaceScope
     * @see http://stackoverflow.com/questions/4581407/how-can-i-convert-arraylistobject-to-arrayliststring/23777137#23777137
     * @return
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public ArrayList<InterfaceScope> getInterfaces() {
        // Doing it the dirty way...
        return (ArrayList) modelElementsInAMap.get(InterfaceScope.class);
    }

    /**
     * Get events in the model
     * 
     * @see org.yakindu.sct.model.stext.stext.EventDefinition
     * @see http://stackoverflow.com/questions/4581407/how-can-i-convert-arraylistobject-to-arrayliststring/23777137#23777137
     * @return
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public ArrayList<EventDefinition> getEvents() {
        // Doing it the dirty way...
        return (ArrayList) modelElementsInAMap.get(EventDefinition.class);
    }

    /**
     * Get operations in the model
     * 
     * @see org.yakindu.sct.model.stext.stext.OperationDefinition
     * @see http://stackoverflow.com/questions/4581407/how-can-i-convert-arraylistobject-to-arrayliststring/23777137#23777137
     * @return
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public ArrayList<OperationDefinition> getOperations() {
        // Doing it the dirty way...
        return (ArrayList) modelElementsInAMap.get(OperationDefinition.class);
    }

    /**
     * Get variables in the model
     * 
     * @see org.yakindu.sct.model.stext.stext.VariableDefinition
     * @see http://stackoverflow.com/questions/4581407/how-can-i-convert-arraylistobject-to-arrayliststring/23777137#23777137
     * @return
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public ArrayList<VariableDefinition> getVariables() {
        // Doing it the dirty way...
        return (ArrayList) modelElementsInAMap.get(VariableDefinition.class);
    }

    /**
     * Get SimpleScopes in the model
     * 
     * @see org.yakindu.sct.model.stext.stext.SimpleScope
     * @see http://stackoverflow.com/questions/4581407/how-can-i-convert-arraylistobject-to-arrayliststring/23777137#23777137
     * @return
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public ArrayList<SimpleScope> getSimpleScopes() {
        // Doing it the dirty way...
        return (ArrayList) modelElementsInAMap.get(SimpleScope.class);
    }

    /**
     * Get ReactionTriggers in the model
     * 
     * @see org.yakindu.sct.model.stext.stext.ReactionTrigger
     * @see http://stackoverflow.com/questions/4581407/how-can-i-convert-arraylistobject-to-arrayliststring/23777137#23777137
     * @return
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public ArrayList<ReactionTrigger> getReactionTriggers() {
        // Doing it the dirty way...
        return (ArrayList) modelElementsInAMap.get(ReactionTrigger.class);
    }

    /**
     * Get RegularEvents in the model
     * 
     * @see org.yakindu.sct.model.stext.stext.RegularEventSpec
     * @see http://stackoverflow.com/questions/4581407/how-can-i-convert-arraylistobject-to-arrayliststring/23777137#23777137
     * @return
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public ArrayList<RegularEventSpec> getRegularEventSpecs() {
        // Doing it the dirty way...
        return (ArrayList) modelElementsInAMap.get(RegularEventSpec.class);
    }

    /**
     * Get TimeEventSpecs in the model
     * 
     * @see org.yakindu.sct.model.stext.stext.TimeEventSpec
     * @see http://stackoverflow.com/questions/4581407/how-can-i-convert-arraylistobject-to-arrayliststring/23777137#23777137
     * @return
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public ArrayList<TimeEventSpec> getTimeEventSpecs() {
        // Doing it the dirty way...
        return (ArrayList) modelElementsInAMap.get(TimeEventSpec.class);
    }

    /**
     * Get entries in the model
     * 
     * @see org.yakindu.sct.model.sgraph.Entry
     * @see http://stackoverflow.com/questions/4581407/how-can-i-convert-arraylistobject-to-arrayliststring/23777137#23777137
     * @return
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public ArrayList<Entry> getEntries() {
        // Doing it the dirty way...
        return (ArrayList) modelElementsInAMap.get(Entry.class);
    }

    /**
     * Get regions in the model
     * 
     * @see org.yakindu.sct.model.sgraph.Region
     * @see http://stackoverflow.com/questions/4581407/how-can-i-convert-arraylistobject-to-arrayliststring/23777137#23777137
     * @return
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public ArrayList<Region> getRegions() {
        // Doing it the dirty way...
        return (ArrayList) modelElementsInAMap.get(Region.class);
    }

    /**
     * Get transitions in the model
     * 
     * @see org.yakindu.sct.model.sgraph.Transition
     * @see http://stackoverflow.com/questions/4581407/how-can-i-convert-arraylistobject-to-arrayliststring/23777137#23777137
     * @return
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public ArrayList<Transition> getTransitions() {
        // Doing it the dirty way...
        return (ArrayList) modelElementsInAMap.get(Transition.class);
    }

    /**
     * Get transitions in the model
     * 
     * @see org.yakindu.sct.model.sgraph.State
     * @see http://stackoverflow.com/questions/4581407/how-can-i-convert-arraylistobject-to-arrayliststring/23777137#23777137
     * @return
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public ArrayList<State> getStates() {
        // Doing it the dirty way...
        return (ArrayList) modelElementsInAMap.get(State.class);
    }

    /**
     * Get FeatureCalls in the model
     * 
     * @see org.yakindu.sct.model.sgraph.FeatureCall
     * @see http://stackoverflow.com/questions/4581407/how-can-i-convert-arraylistobject-to-arrayliststring/23777137#23777137
     * @return
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public ArrayList<FeatureCall> getFeatureCalls() {
        // Doing it the dirty way...
        return (ArrayList) modelElementsInAMap.get(FeatureCall.class);
    }

    /**
     * Get ElementReferenceExpressions in the model
     * 
     * @see org.yakindu.sct.model.sgraph.ElementReferenceExpression
     * @see http://stackoverflow.com/questions/4581407/how-can-i-convert-arraylistobject-to-arrayliststring/23777137#23777137
     * @return
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public ArrayList<ElementReferenceExpression> getElementReferenceExpressions() {
        // Doing it the dirty way...
        return (ArrayList) modelElementsInAMap.get(ElementReferenceExpression.class);
    }

    /**
     * Get the map containing all the elements in the model as Class-EObject pairs
     * (Class is the interface's class, not the implementation (which ends with *Impl)!) 
     * @return
     */
    public HashMap<Class<? extends EObject>, ArrayList<EObject>> getModelElementsInAMap() {
        return modelElementsInAMap;
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
    public static LinkedList<ForbiddenElement> getForbiddenElementsInReactionTrigger(ReactionTrigger reactionTrigger) {
        EList<EventSpec> triggers = reactionTrigger.getTriggers();

        LinkedList<ForbiddenElement> forbiddenElementList = new LinkedList<ForbiddenElement>();

        // check if no trigger has been attached to the edge
        if (triggers == null || triggers.isEmpty()) {
            EObject eContainer = reactionTrigger.eContainer();

            // we deny the usage of transitions without triggers (except for pseudostates like choices)
            // - these are also mapped to AlwaysEvent like the usage of "always" or "oncycle" keywords
            if (eContainer instanceof Transition) {
                Transition containerTransition = (Transition) reactionTrigger.eContainer();
                Vertex sourceVertex = containerTransition.getSource();

                // it's only forbidden if its parent is a State (there are cases where Pseudostates are allowed)
                if (sourceVertex instanceof State) {
                    String sourceVertexName = sourceVertex.getName();
                    forbiddenElementList.add(new ForbiddenElement(
                            "Trigger can not be empty! (source vertex name: " + sourceVertexName + ")"));
                }
            } else if (eContainer instanceof State) {// akkor egy csúcsban van benne a belső állapotátmenet (beleírta a dobozba)
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
        logger.info("collect and inspect model elements");
        modelElementsInAMap = collectModelElementsIntoMap(statechart);

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

        logger.info("does it contain time event reaction trigger? --> " + doesContainTimeEventReactionTrigger() + "\n");

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
        LinkedList<ForbiddenElement> forbiddenElementList = getForbiddenElementsInReactionTrigger((ReactionTrigger) trigger);

        for (ForbiddenElement forbiddenElement : forbiddenElementList) {
            logger.info("forbidden element has been found: " + forbiddenElement.getMessage() + "\n");
        }

        logger.info("		trigger.getClass().getName();: '" + trigger.getClass().getName() + "' \n");
        // EList<EObject> eContents = trigger.eContents();
        //        for (EObject eObject : eContents) {
        //            System.out.println("eObject.toString(): " + eObject.toString());
        //            System.out.println("eObject.eClass().getClass().getName(): " + eObject.eClass().getClass().getName());
        //        }
        // org.yakindu.sct.model.stext.stext.TimeEventSpec
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
        logger.info("»» current vertexName: '" + vertexName + "'" + (vertexName.length() == 0 ? " (entry state!)" : "")
                + "\n");
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

        logger.info("transition's target: '" + transitionTargetName + "',\n" + "transition's specification: '"
                + transitionSpecification + "', " + "\n");

        if (transitionTrigger != null) {
            logger.info("\t\ttransition's trigger: ");
            logger.info(transitionTrigger.toString() + "\n");
            processTrigger(transitionTrigger);
        }
    }

    /**
     * Get missing elements in an interface compared to a reference interface.
     * (The order of the parameters matter. :) )
     * 
     * @param referenceModelElementsInAMap
     * @param toBeCheckedModelElementsInAMap
     * @return
     */
    @SuppressWarnings("serial")
    public static ArrayList<MissingEObject> getMissingElementsInInterface(
            HashMap<Class<? extends EObject>, ArrayList<EObject>> referenceModelElementsInAMap,
            HashMap<Class<? extends EObject>, ArrayList<EObject>> toBeCheckedModelElementsInAMap){

        ArrayList<MissingEObject> missingElements = new ArrayList<>();
        
        // we would like to check the existence of the following NamedElements:
        // InterfaceScope, EventDefinition, OperationDefinition, VariableDefinition
        
        // these have to get checked
        ArrayList<Class<? extends NamedElement>> classesOfEObjectsToCheck = new ArrayList<Class<? extends NamedElement>>() {
            {
                add(InterfaceScope.class);
                add(EventDefinition.class);
                add(OperationDefinition.class);
                add(VariableDefinition.class);
            }
        };

        // user friendly names of these elements
        HashMap<Class<? extends NamedElement>, String> modelElementUserFriendlyNameDictionary = new HashMap<Class<? extends NamedElement>, String>() {
            {
                put(InterfaceScope.class, "interface");
                put(EventDefinition.class, "event");
                put(OperationDefinition.class, "operation");
                put(VariableDefinition.class, "variable");
            }
        };    
        
        modelElementUserFriendlyNameDictionary.forEach((k, v) -> {
            System.out.println();
        });
        
        for (Class<? extends NamedElement> currentEObjectClass : classesOfEObjectsToCheck) {
            ArrayList<EObject> refInterfaceElements = referenceModelElementsInAMap.get(currentEObjectClass);
            ArrayList<EObject> toBeCheckedInterfaceElements = toBeCheckedModelElementsInAMap.get(currentEObjectClass);

            for (EObject currentRequiredEObject : refInterfaceElements) {
                String currentRequiredEObjectName = ((NamedElement) currentRequiredEObject).getName();
                boolean found = false;
                for (EObject currentProvidedEObject : toBeCheckedInterfaceElements) {
                    if (currentRequiredEObjectName.equals(((NamedElement) currentProvidedEObject).getName())) {
                        found = true; // OK, found the element we were looking for
                        break;
                    }
                }
                if (!found) {
                    // e.g. "The 'User' interface is missing!"
                    String message = "The '" + currentRequiredEObjectName + "' "
                            + modelElementUserFriendlyNameDictionary.get(currentEObjectClass) + " is missing!";
                    missingElements.add(new MissingEObject(message, currentRequiredEObject));
                }
            }

            // get required elements
            ArrayList<String> requiredElementNames = new ArrayList<>();

            // get provided elements
            ArrayList<String> providedElementNames = new ArrayList<>();
            for (EObject eObject : toBeCheckedInterfaceElements) {
                // add the current name to a list of Strings
                providedElementNames.add(((NamedElement) eObject).getName());
            }

            // check if the provided model contains the required elements
            for (String currentElementName : requiredElementNames) {
                if (!providedElementNames.contains(currentElementName)) {
                    missingElements.add(new MissingEObject(currentElementName));
                }
            }
        }
        
        return missingElements;
    }
    
    /**
     * Get missing elements in an interface compared to a reference interface.
     * (The order of the parameters matter. :) )
     * 
     * @param referenceMinimalSct
     * @param toBeCheckedSct
     * @return
     */
    public ArrayList<MissingEObject> getMissingElementsInInterface(Statechart referenceMinimalSct,
            Statechart toBeCheckedSct) {

        HashMap<Class<? extends EObject>, ArrayList<EObject>> referenceModelElementsInAMap = collectModelElementsIntoMap(
                referenceMinimalSct);
        HashMap<Class<? extends EObject>, ArrayList<EObject>> toBeCheckedModelElementsInAMap = collectModelElementsIntoMap(
                toBeCheckedSct);

        return getMissingElementsInInterface(referenceModelElementsInAMap, toBeCheckedModelElementsInAMap);
    }

    /**
     * Get statechart model
     * @return
     */
    public Statechart getStatechart() {
        return statechart;
    }

    /**
     * Set statechart
     * @param statechart
     */
    public void setStatechart(Statechart statechart) {
        this.statechart = statechart;
        modelElementsInAMap = collectModelElementsIntoMap(statechart);
    }

    /**
     * 
     */
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
