/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.jena.reasoner.rulesys;

import java.util.*;

import org.apache.jena.graph.* ;
import org.apache.jena.rdf.model.* ;
import org.apache.jena.reasoner.* ;
import org.apache.jena.shared.* ;
import org.apache.jena.vocabulary.ReasonerVocabulary ;

/**
 * Rule-based reasoner interface. This is the default rule reasoner to use.
 * It supports both forward reasoning and backward reasoning, including use
 * of forward rules to generate and instantiate backward rules.
 */
public class FBRuleReasoner implements RuleReasoner {

    /** The parent reasoner factory which is consulted to answer capability questions */
    protected ReasonerFactory factory;

    /** The rules to be used by this instance of the forward engine */
    protected List<Rule> rules = new ArrayList<>();

    /** A precomputed set of schema deductions */
    protected Graph schemaGraph;

    /** Flag to set whether the inference class should record derivations */
    protected boolean recordDerivations = false;

    /** Flag which, if true, enables tracing of rule actions to logger.info */
    boolean traceOn = false;
//    boolean traceOn = true;

    /** Flag, if true we cache the closure of the pure rule set with its axioms */
    protected static final boolean cachePreload = true;

    /** The cached empty closure, if wanted */
    protected InfGraph preload;

    /** The original configuration properties, if any */
    protected Resource configuration;

    /**
     * Constructor. This is the raw version that does not reference a ReasonerFactory
     * and so has no capabilities description.
     * @param rules a list of Rule instances which defines the ruleset to process
     */
    public FBRuleReasoner(List<Rule> rules) {
        if (rules == null) throw new NullPointerException( "null rules" );
        this.rules = rules;
    }

    /**
     * Constructor
     * @param factory the parent reasoner factory which is consulted to answer capability questions
     */
    public FBRuleReasoner(ReasonerFactory factory) {
        this( new ArrayList<Rule>(), factory);
    }

    /**
     * Constructor
     * @param factory the parent reasoner factory which is consulted to answer capability questions
     * @param configuration RDF node to configure the rule set and mode, can be null
     */
    public FBRuleReasoner(ReasonerFactory factory, Resource configuration) {
        this( new ArrayList<Rule>(), factory);
        this.configuration = configuration;
        if (configuration != null) loadConfiguration( configuration );
    }

    /**
         load the configuration from the configuring Resource (in its Model).
    */
    protected void loadConfiguration( Resource configuration )
        {
        StmtIterator i = configuration.listProperties();
        while (i.hasNext()) {
            Statement st = i.nextStatement();
            doSetRDFNodeParameter( st.getPredicate(), st.getObject() );
        }
        }

    /**
     * Constructor
     * @param rules a list of Rule instances which defines the ruleset to process
     * @param factory the parent reasoner factory which is consulted to answer capability questions
     */
    public FBRuleReasoner(List<Rule> rules, ReasonerFactory factory) {
        this(rules);
        this.factory = factory;
    }

    /**
     * Internal constructor, used to generated a partial binding of a schema
     * to a rule reasoner instance.
     */
    protected FBRuleReasoner(List<Rule> rules, Graph schemaGraph, ReasonerFactory factory) {
        this(rules, factory);
        this.schemaGraph = schemaGraph;
    }

    /**
         Add the given rules to the current set and answer this Reasoner. Provided
         so that the Factory can deal out reasoners with specified rulesets.
         There may well be a better way to arrange this.
    */
    public FBRuleReasoner addRules(List<Rule> rules) {
        List<Rule> combined = new ArrayList<>( this.rules );
        combined.addAll( rules );
        setRules( combined );
        return this;
        }

    /**
     * Return a description of the capabilities of this reasoner encoded in
     * RDF. These capabilities may be static or may depend on configuration
     * information supplied at construction time. May be null if there are
     * no useful capabilities registered.
     */
    @Override
    public Model getReasonerCapabilities() {
        if (factory != null) {
            return factory.getCapabilities();
        } else {
            return null;
        }
    }

    /**
        Answer the schema graph bound into this reasoner, or null if there
        isn't one. (Added mainly for testing purposes, which is why it isn't
        part of the Reasoner API.)
    */
    public Graph getBoundSchema()
        { return schemaGraph; }

    /**
     * Add a configuration description for this reasoner into a partial
     * configuration specification model.
     * @param configSpec a Model into which the configuration information should be placed
     * @param base the Resource to which the configuration parameters should be added.
     */
    @Override
    public void addDescription(Model configSpec, Resource base) {
        if (configuration != null) {
            StmtIterator i = configuration.listProperties();
            while (i.hasNext()) {
                Statement st = i.nextStatement();
                configSpec.add(base, st.getPredicate(), st.getObject());
            }
        }
    }

    /**
     * Determine whether the given property is recognized and treated specially
     * by this reasoner. This is a convenience packaging of a special case of getCapabilities.
     * @param property the property which we want to ask the reasoner about, given as a Node since
     * this is part of the SPI rather than API
     * @return true if the given property is handled specially by the reasoner.
     */
    @Override
    public boolean supportsProperty(Property property) {
        if (factory == null) return false;
        Model caps = factory.getCapabilities();
        Resource root = caps.getResource(factory.getURI());
        return caps.contains(root, ReasonerVocabulary.supportsP, property);
    }

    /**
     * Precompute the implications of a schema graph. The statements in the graph
     * will be combined with the data when the final InfGraph is created.
     */
    @Override
    public Reasoner bindSchema(Graph tbox) throws ReasonerException {
        if (schemaGraph != null) {
            throw new ReasonerException("Can only bind one schema at a time to an OWLRuleReasoner");
        }
        FBRuleInfGraph graph = new FBRuleInfGraph(this, rules, getPreload(), tbox);
        graph.prepare();
        FBRuleReasoner fbr  = new FBRuleReasoner(rules, graph, factory);
        fbr.setDerivationLogging(recordDerivations);
        fbr.setTraceOn(traceOn);
        return fbr;
    }

    /**
     * Precompute the implications of a schema Model. The statements in the graph
     * will be combined with the data when the final InfGraph is created.
     */
    @Override
    public Reasoner bindSchema(Model tbox) throws ReasonerException {
        return bindSchema(tbox.getGraph());
    }

    /**
     * Attach the reasoner to a set of RDF data to process.
     * The reasoner may already have been bound to specific rules or ontology
     * axioms (encoded in RDF) through earlier bindRuleset calls.
     *
     * @param data the RDF data to be processed, some reasoners may restrict
     * the range of RDF which is legal here (e.g. syntactic restrictions in OWL).
     * @return an inference graph through which the data+reasoner can be queried.
     * @throws ReasonerException if the data is ill-formed according to the
     * constraints imposed by this reasoner.
     */
    @Override
    public InfGraph bind( Graph data ) throws ReasonerException {
        Graph schemaArg = schemaGraph == null ? getPreload() : (FBRuleInfGraph) schemaGraph;
        FBRuleInfGraph graph = new FBRuleInfGraph( this, rules, schemaArg );
        graph.setDerivationLogging( recordDerivations );
        graph.setTraceOn( traceOn );
        graph.rebind( data );
        return graph;
    }

    /**
     * Set (or change) the rule set that this reasoner should execute.
     * @param rules a list of Rule objects
     */
    @Override
    public void setRules(List<Rule> rules) {
        this.rules = rules;
        preload = null;
        if (schemaGraph != null) {
            // The change of rules invalidates the existing precomputed schema graph
            // This might be recoverable but for now simply flag the error and let the
            // user reorder their code to set the rules before doing a bind!
            throw new ReasonerException("Cannot change the rule set for a bound rule reasoner.\nSet the rules before calling bindSchema");
        }
    }

    /**
     * Return the list of Rules used by this reasoner
     * @return a List of Rule objects
     */
    @Override
    public List<Rule> getRules() {
        return rules;
    }

    /**
         Answer the list of rules loaded from the given filename. May throw a
         ReasonerException wrapping an IOException.
    */
    public static List<Rule> loadRules( String fileName ) {
        try
            { return Rule.parseRules(Util.loadRuleParserFromResourceFile( fileName ) ); }
        catch (WrappedIOException e)
            { throw new ReasonerException("Can't load rules file: " + fileName, e.getCause() ); }
    }

    /**
     * Register an RDF predicate as one whose presence in a goal should force
     * the goal to be tabled. This is better done directly in the rule set.
     */
    public synchronized void tablePredicate(Node predicate) {
        // Create a dummy rule which tables the predicate ...
        Rule tablePredicateRule = new Rule("",
                new ClauseEntry[]{
                    new Functor("table", new Node[] { predicate },BuiltinRegistry.theRegistry)
                },
                new ClauseEntry[]{});
        // ... end append the rule to the ruleset
        rules.add(tablePredicateRule);
    }

    /**
     * Get the single static precomputed rule closure.
     */
    protected synchronized InfGraph getPreload() {
        if (cachePreload && preload == null) {
            preload = (new FBRuleInfGraph(this, rules, null));
            preload.prepare();
        }
        return preload;
    }

    /**
     * Switch on/off derivation logging.
     * If set to true then the InfGraph created from the bind operation will start
     * life with recording of derivations switched on. This is currently only of relevance
     * to rule-based reasoners.
     * <p>
     * Default - false.
     */
    @Override
    public void setDerivationLogging(boolean logOn) {
        recordDerivations = logOn;
    }

    /**
     * Set the state of the trace flag. If set to true then rule firings
     * are logged out to the Logger at "INFO" level.
     */
    public void setTraceOn(boolean state) {
        traceOn = state;
    }

    /**
     * Return the state of the trace flag.If set to true then rule firings
     * are logged out to the Log at "INFO" level.
     */
    public boolean isTraceOn() {
        return traceOn;
    }

    /**
     * Set a configuration parameter for the reasoner. The supported parameters
     * are:
     * <ul>
     * <li>PROPderivationLogging - set to true to enable recording all rule derivations</li>
     * <li>PROPtraceOn - set to true to enable verbose trace information to be sent to the logger INFO channel</li>
     * </ul>
     *
     * @param parameter the property identifying the parameter to be changed
     * @param value the new value for the parameter, typically this is a wrapped
     * java object like Boolean or Integer.
     * @throws IllegalParameterException if the parameter is unknown
     */
    @Override
    public void setParameter(Property parameter, Object value) {
        if (!doSetParameter(parameter, value)) {
            throw new IllegalParameterException("RuleReasoner does not recognize configuration parameter " + parameter);
        } else {
            // Record the configuration change
            if (configuration == null) {
                Model configModel = ModelFactory.createDefaultModel();
                configuration = configModel.createResource();
            }
            Util.updateParameter(configuration, parameter, value);
        }
    }

    /**
         Set a parameter from a statement, given the property and its RDFNode value.
         Most parameters are, historically, set from the string value of the RDFNode,
         but newer parameters may have Resource values with embedded models,
         for which their toString() is not just suspect, but definitively wrong. Hence the
         introduction of this relay station.

         @param parameter the propoerty naming the value to set
         @param value the RDFNode with the value of that property
         @return true if the property was understood, false otherwise
    */
    protected boolean doSetRDFNodeParameter( Property parameter, RDFNode value )
        {
        return
            (value instanceof Resource && doSetResourceParameter( parameter, (Resource) value ))
            || (value instanceof Literal && doSetParameter( parameter, ((Literal)value).getValue() ))
            || doSetParameter(parameter, value.toString())
            ;
        }

    /**
         Set a parameter with a Resource value. Answer false if the parameter is not
         understood. Default understands no parameters; subclasses may override.
    */
    protected boolean doSetResourceParameter( Property parameter, Resource value )
        { return false; }

    /**
     * Set a configuration parameter for the reasoner. The supported parameters
     * are:
     * <ul>
     * <li>PROPderivationLogging - set to true to enable recording all rule derivations</li>
     * <li>PROPtraceOn - set to true to enable verbose trace information to be sent to the logger INFO channel</li>
     * </ul>
     * @param parameter the property identifying the parameter to be changed
     * @param value the new value for the parameter, typically this is a wrapped
     * java object like Boolean or Integer.
     * @return false if the parameter was not known
     */
    protected boolean doSetParameter(Property parameter, Object value) {
        if (parameter.equals(ReasonerVocabulary.PROPderivationLogging)) {
            recordDerivations = Util.convertBooleanPredicateArg(parameter, value);
            return true;
        } else if (parameter.equals(ReasonerVocabulary.PROPtraceOn)) {
            traceOn =  Util.convertBooleanPredicateArg(parameter, value);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Return the Jena Graph Capabilities that the inference graphs generated
     * by this reasoner are expected to conform to.
     */
    @Deprecated
    @Override
    public Capabilities getGraphCapabilities() {
        return BaseInfGraph.reasonerInfCapabilities;
    }
}
