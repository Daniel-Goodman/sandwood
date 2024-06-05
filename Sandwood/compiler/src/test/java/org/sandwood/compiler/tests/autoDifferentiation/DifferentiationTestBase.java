/*
 * Sandwood
 *
 * Copyright (c) 2019-2026, Oracle and/or its affiliates
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl/
 */

package org.sandwood.compiler.tests.autoDifferentiation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;

import org.sandwood.common.execution.ExecutionType;
import org.sandwood.compiler.CompilationOptions;
import org.sandwood.compiler.compilation.CompilationContext;
import org.sandwood.compiler.compilation.CompilationContext.CompilationPhase;
import org.sandwood.compiler.compilation.ForwardExecutionBuilder;
import org.sandwood.compiler.compilation.ForwardExecutionBuilder.Distributions;
import org.sandwood.compiler.compilation.ForwardExecutionBuilder.GuardStatus;
import org.sandwood.compiler.compilation.ForwardExecutionBuilder.PrimingStatus;
import org.sandwood.compiler.compilation.util.CompilationDesc;
import org.sandwood.compiler.dataflowGraph.scopes.GlobalScope;
import org.sandwood.compiler.dataflowGraph.tasks.DataflowTask;
import org.sandwood.compiler.dataflowGraph.variables.Variable;
import org.sandwood.compiler.dataflowGraph.variables.arrayVariable.ArrayVariable;
import org.sandwood.compiler.dataflowGraph.variables.randomVariables.Bernoulli;
import org.sandwood.compiler.dataflowGraph.variables.randomVariables.Uniform;
import org.sandwood.compiler.dataflowGraph.variables.scalarVariables.BooleanVariable;
import org.sandwood.compiler.dataflowGraph.variables.scalarVariables.DoubleVariable;
import org.sandwood.compiler.dataflowGraph.variables.scalarVariables.IntVariable;
import org.sandwood.compiler.dataflowGraph.variables.scalarVariables.NumberVariable;
import org.sandwood.compiler.exceptions.SandwoodModelException;
import org.sandwood.compiler.tests.util.CompilerState;
import org.sandwood.compiler.tests.util.TestStringGenerator;
import org.sandwood.compiler.traces.Traces;
import org.sandwood.compiler.traces.TracesImplementation;
import org.sandwood.compiler.trees.ArgDesc;
import org.sandwood.compiler.trees.Tree;
import org.sandwood.compiler.trees.irTree.IRTree;
import org.sandwood.compiler.trees.irTree.IRTreeReturn;
import org.sandwood.compiler.trees.irTree.IRTreeVoid;
import org.sandwood.compiler.trees.transformationTree.util.KnownValuesTrans;
import org.sandwood.compiler.util.StringUtil;

public abstract class DifferentiationTestBase implements InputGenerator {

    // Record to hold all the model DAG to test and the sequences of variables to differentiate against for a single
    // test.
    public record DagTest(List<NumberVariable<?>> baseVars, List<DoubleVariable> diffVars, String description) {

        public Variable<?>[] getAllVars() {
            Variable<?>[] vars = new Variable[baseVars.size() + diffVars.size()];
            int i=0;
            for(Variable<?> v:baseVars)
                vars[i++] = v;
            for(Variable<?> v:diffVars)
                vars[i++] = v;
            return vars;
        }
    };

    // Record holding the DAG model and the differentiation variables for all the possible tests.
    protected record DagVars(List<NumberVariable<?>> baseVars, List<DoubleVariable>... diffVars) {
        @SafeVarargs
        public DagVars(List<NumberVariable<?>> baseVars, List<DoubleVariable>... diffVars) {
            this.baseVars = new ArrayList<>(baseVars);
            Collections.sort(this.baseVars);
            this.diffVars = diffVars;
        }
    };

    private final String sourceDir = "src" + File.separator + "test" + File.separator + "resources" + File.separator
            + "expectedOutputs";

    private final boolean constructingResults;
    private final TestStringGenerator tsg = new TestStringGenerator();

    private Set<Variable<?>> inputs = new HashSet<>();

    protected DifferentiationTestBase(boolean constructingResults) {
        this.constructingResults = constructingResults;
    }

    protected static abstract class DagTestGenerator implements Comparable<DagTestGenerator> {

        private final String desc;
        /*
         * A flag to track how many tests there are for this generator. There must always be at least one test. -1
         * reports that no test have yet been generated.
         */
        private int testCount = -1;

        protected DagTestGenerator(String desc) {
            this.desc = desc;
        }

        protected abstract DagVars generateDag(InputGenerator g);

        /*
         * An iterator used to delay the construction of the DAG so that the variables ID counter can be reset each time
         * without risk of collisions. THis ensures that the output does not change if additional tests are added.
         */
        public final Iterable<DagTest> generate(InputGenerator g) {
            return new Iterable<DagTest>() {
                @Override
                public Iterator<DagTest> iterator() {
                    return new Iterator<>() {
                        int i = 0;

                        @Override
                        public boolean hasNext() {
                            return i == -1 || i < testCount;
                        }

                        @Override
                        public DagTest next() {
                            g.cleanState();
                            DagVars vars = generateDag(g);
                            testCount = vars.diffVars.length;
                            assertTrue(testCount > 0);
                            return new DagTest(vars.baseVars, vars.diffVars[i++], desc);
                        }
                    };
                }
            };
        }

        @Override
        public String toString() {
            return desc;
        }

        @Override
        public int compareTo(DagTestGenerator o) {
            return desc.compareTo(o.desc);
        }
    }

    // Methods to generate inputs used by the DAGs.
    public DoubleVariable getDouble(String name) {
        DoubleVariable d = Uniform.uniform(0, 1).sample();
        d.setAlias(name);
        inputs.add(d);
        return d;
    }

    public DoubleVariable getDouble(String name, double value) {
        DoubleVariable d = DoubleVariable.doubleVariable(value);
        d.setAlias(name);
        inputs.add(d);
        return d;
    }

    public IntVariable getInt(String name) {
        IntVariable i = Uniform.uniform(0, 1).sample().castToInteger();
        i.setAlias(name);
        inputs.add(i);
        return i;
    }

    public IntVariable getInt(String name, int value) {
        IntVariable i = IntVariable.intVariable(value);
        i.setAlias(name);
        inputs.add(i);
        return i;
    }

    public BooleanVariable getBoolean(String name) {
        BooleanVariable b = Bernoulli.bernoulli(0.5).sample();
        b.setAlias(name);
        inputs.add(b);
        return b;
    }

    @Override
    public void cleanState() {
        try {
            CompilerState.reset();
        } catch(Exception e) {
            assertTrue(false);
        }

        inputs.clear();
    }

    protected void test(DagTest t) throws IOException, NoSuchMethodException, SecurityException, IllegalAccessException,
            IllegalArgumentException, InvocationTargetException {
        String diffCode = getCode(t);

        // Tool to make changes to the expected result easier to incorporate.
        StringBuilder sb = new StringBuilder();
        for(NumberVariable<?> v:t.baseVars)
            sb.append(v.getAlias() + "-");

        sb.append("differentiated-by");

        for(DoubleVariable d:t.diffVars)
            sb.append("-" + d.getAlias());

        String expectedFilename = tsg.processString(this, sourceDir, t.description + "-" + sb.toString(), "", diffCode,
                constructingResults, "txt");
        String expectedApiCode = new String(Files.readAllBytes(Paths.get(expectedFilename)));

        // Normalize new lines
        diffCode = StringUtil.normalizeNewLines(diffCode);
        // This second use is required because GitHub rewrites the data files.
        expectedApiCode = StringUtil.normalizeNewLines(expectedApiCode);

        assertEquals(expectedApiCode, diffCode);
    }

    private String getCode(DagTest test) {
        CompilationContext compilationCtx = getCompilationCtx(test.getAllVars());

        // Check the graph is valid as a sanity check
        // Test the graph for errors.
        checkModelValidity(compilationCtx);

        StringBuilder sb = new StringBuilder();

        sb.append("Initial Graph\n");

        getInitialModel(test, sb, compilationCtx);

        sb.append("\n\nConstants\n");
        // Clear function dependent values to prevent pollution from one context to
        // another.
        compilationCtx.initialize();
        compilationCtx.pushIsSerial(true);
        // Set the phase
        compilationCtx.phase = CompilationPhase.INITIALIZATION_OF_MODEL;

        // Order the required variables and construct the trees to generate them.
        Set<Variable<?>> deterministicVars = new HashSet<>(compilationCtx.traces.deterministicVariables());
        PriorityQueue<Variable<?>> p = new PriorityQueue<>(deterministicVars);
        while(!p.isEmpty()) {
            ForwardExecutionBuilder.constructForwardMethod(p.poll(), GuardStatus.NONE, Distributions.NO_DISTRIBUTIONS,
                    PrimingStatus.PRIMING, compilationCtx);
        }

        List<IRTreeVoid> arrayInitializations = compilationCtx.getArrayInitializations();
        for(IRTreeVoid tree:arrayInitializations)
            compilationCtx.addTreeToScope(GlobalScope.scope, tree);

        sb.append(compilationCtx.getOutermostScopeTree());

        compilationCtx.phase = CompilationPhase.MAIN_METHODS;
        compilationCtx.popIsSerial();

        sb.append("\n\nAllocator\n");
        compilationCtx.initialize();
        sb.append(compilationCtx.getVarAllocator());

        compilationCtx.initialize();
        for(NumberVariable<?> dag:test.baseVars) {
            StringBuilder title = new StringBuilder();
            title.append("\n\nDifferential Graph: \'" + dag.getAlias());

            NumberVariable<?> d = dag;
            Iterator<DoubleVariable> i = test.diffVars.iterator();
            while(i.hasNext()) {
                DoubleVariable diffVar = i.next();
                title.append("\' differentiated by \'" + diffVar.getAlias());

                assertTrue(diffVar != null);
                assertTrue(d.isDifferentiable(diffVar),
                        "Graph is not diferentiable with variable " + diffVar + " from sequence " + test.diffVars);
                d = d.getDifferential(diffVar, compilationCtx);
                assertTrue(d.scope() == dag.scope());

                sb.append(title);
                sb.append("\'\n");

                for(Variable<?> v:getVaraibles(List.of(d))) {
                    if(v != dag)
                        constructTree(v, compilationCtx);
                }
                IRTreeVoid tree = compilationCtx.getOutermostScopeTree();
                sb.append(tree);

                sb.append("\n\nOptimised\n");
                sb.append(tree.toTransformationTree().applyOptimisations(new ArgDesc[0], Collections.emptyMap(),
                        KnownValuesTrans.constructKnownValues()));

                sb.append("\n\nAdditional Constants\n");
                getAdditiionalConstants(deterministicVars, arrayInitializations, sb, compilationCtx);

                sb.append("\n\nUpdated Allocator\n");
                sb.append(compilationCtx.getVarAllocator());
            }
        }

        return sb.toString();
    }

    private void checkModelValidity(CompilationContext compilationCtx) {
        List<SandwoodModelException> l = new ArrayList<>();
        for(DataflowTask<?> d:compilationCtx.traces.getAllTasks())
            d.testTask(l);
        assertTrue(l.isEmpty(), "Test models should be valid: " + l);
    }

    private void getInitialModel(DagTest test, StringBuilder sb, CompilationContext compilationCtx) {
        for(Variable<?> v:getVaraibles(test.baseVars))
            constructTree(v, compilationCtx);
        sb.append(compilationCtx.getOutermostScopeTree());
    }

    private void getAdditiionalConstants(Set<Variable<?>> deterministicVars, List<IRTreeVoid> arrayInitializations,
            StringBuilder sb, CompilationContext compilationCtx) {
        PriorityQueue<Variable<?>> p;
        compilationCtx.initialize();
        compilationCtx.pushIsSerial(true);
        // Set the phase
        compilationCtx.phase = CompilationPhase.INITIALIZATION_OF_MODEL;

        // Order the required variables and construct the trees to generate them.
        p = new PriorityQueue<>(compilationCtx.traces.deterministicVariables());
        while(!p.isEmpty()) {
            Variable<?> v = p.poll();
            if(!deterministicVars.contains(v))
                ForwardExecutionBuilder.constructForwardMethod(v, GuardStatus.NONE, Distributions.NO_DISTRIBUTIONS,
                        PrimingStatus.PRIMING, compilationCtx);
        }

        for(IRTreeVoid initTree:compilationCtx.getArrayInitializations()) {
            if(!arrayInitializations.contains(initTree))
                compilationCtx.addTreeToScope(GlobalScope.scope, initTree);
        }

        sb.append(compilationCtx.getOutermostScopeTree());

        compilationCtx.phase = CompilationPhase.MAIN_METHODS;
        compilationCtx.popIsSerial();
    }

    private List<Variable<?>> getVaraibles(List<NumberVariable<?>> baseVars) {
        List<Variable<?>> intermediates = new ArrayList<>();
        for(Variable<?> v:Variable.collectInputVariable(new HashSet<>(baseVars))) {
            if(!inputs.contains(v) && v.isIntermediate() && !v.isDeterministic())
                intermediates.add(v);
        }
        intermediates.addAll(baseVars);
        Collections.sort(intermediates);
        return intermediates;
    }

    private <A extends Variable<A>> void constructTree(Variable<A> v, CompilationContext compilationCtx) {
        IRTreeReturn<A> tree = v.getParent().getForwardIR(compilationCtx);
        IRTreeVoid store = IRTree.store(v.getVarDesc(), tree, Tree.NoComment);
        compilationCtx.addTreeToScope(v.scope(), store);
    }

    private CompilationContext getCompilationCtx(Variable<?>... vars) {
        CompilationOptions opts = new CompilationOptions();
        CompilationDesc compDesc = new CompilationDesc();
        Traces traces = TracesImplementation.getTraces(compDesc, vars);

        CompilationContext compilationCtx = new CompilationContext(opts, traces, ExecutionType.SingleThreadCPU);

        for(Variable<?> v:compilationCtx.traces.getAllVariables()) {
            boolean isSubArray = v.getType().isArray() && ((ArrayVariable<?>) v).isSubArray();
            boolean isModelInput = inputs.contains(v);
            if((v.isIntermediate() || (v.isSample() && !isSubArray)) && !isModelInput) {
                compilationCtx.addConstructedClassField(v);
            }
        }

        return compilationCtx;
    }
}
