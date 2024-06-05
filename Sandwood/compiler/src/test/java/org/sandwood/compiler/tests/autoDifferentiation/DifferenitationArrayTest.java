/*
 * Sandwood
 *
 * Copyright (c) 2019-2026, Oracle and/or its affiliates
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl/
 */

package org.sandwood.compiler.tests.autoDifferentiation;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.sandwood.compiler.dataflowGraph.Sandwood;
import org.sandwood.compiler.dataflowGraph.tasks.returnTasks.Sigmoid;
import org.sandwood.compiler.dataflowGraph.variables.Variable;
import org.sandwood.compiler.dataflowGraph.variables.VariableType;
import org.sandwood.compiler.dataflowGraph.variables.arrayVariable.ArrayVariable;
import org.sandwood.compiler.dataflowGraph.variables.scalarVariables.BooleanVariable;
import org.sandwood.compiler.dataflowGraph.variables.scalarVariables.DoubleVariable;
import org.sandwood.compiler.dataflowGraph.variables.scalarVariables.IntVariable;

public class DifferenitationArrayTest extends DifferentiationTestBase {

    private static final boolean constructingResults = false;

    private static class Wrapper {
        DoubleVariable value;
    }

    public DifferenitationArrayTest() {
        super(constructingResults);
    }

    private static List<DagTestGenerator> testGenerators() {
        List<DagTestGenerator> l = new ArrayList<>();

        l.add(array1dTest1());
        l.add(array1dTest2());
        l.add(array1dTest3());
        l.add(array2dTest1());
        l.add(array2dSecondDifferentialTest1());
        l.add(array3dTest1());
        l.add(array3dTest2());
        l.add(array1dForLoopTest1());
        l.add(array2dForLoopTest1());
        l.add(array3dForLoopTest1());
        l.add(array4dForLoopTest1());

        Collections.sort(l);
        return l;
    }

    @ParameterizedTest
    @MethodSource("testGenerators")
    @DisplayName("Test differentiation of values whose calculation includes arrays")
    void runTests(DagTestGenerator g) throws NoSuchMethodException, SecurityException, IllegalAccessException,
            IllegalArgumentException, InvocationTargetException, IOException {
        for(DagTest t:g.generate(this))
            test(t);
        assertFalse(constructingResults, "Results are being written to files, this invalidates the tests.");
    }

    private static DagTestGenerator array1dTest1() {
        return new DagTestGenerator("1d_array_mk1") {
            public DagVars generateDag(InputGenerator g) {
                DoubleVariable a = g.getDouble("a");
                DoubleVariable b = g.getDouble("b");
                DoubleVariable c = g.getDouble("c");

                DoubleVariable powTwoA = a.times(a);
                DoubleVariable powThreeB = b.times(b.times(b));

                ArrayVariable<DoubleVariable> array = Variable.arrayVariable(null, VariableType.DoubleVariable,
                        IntVariable.intVariable(3));
                array.setAlias("array");
                array.put(0, DoubleVariable.doubleVariable(0.5));
                array.put(1, powTwoA);
                array.put(2, powThreeB);
                DoubleVariable d = array.get(0);
                d.setAlias("d");

                return new DagVars(List.of(d), List.of(a), List.of(b), List.of(c));
            }
        };
    }

    private static DagTestGenerator array1dTest2() {
        return new DagTestGenerator("1d_array_mk2") {
            public DagVars generateDag(InputGenerator g) {
                DoubleVariable a = g.getDouble("a");
                DoubleVariable b = g.getDouble("b");

                ArrayVariable<DoubleVariable> array = Variable.arrayVariable(null, VariableType.DoubleVariable,
                        IntVariable.intVariable(3));
                array.setAlias("array");
                array.put(0, a);
                array.put(1, a);
                array.put(2, DoubleVariable.doubleVariable(0.5));
                DoubleVariable d = array.get(1);
                d.setAlias("d");

                return new DagVars(List.of(d), List.of(a), List.of(b));
            }
        };
    }

    private static DagTestGenerator array1dTest3() {
        return new DagTestGenerator("1d_array_mk3") {
            public DagVars generateDag(InputGenerator g) {

                DoubleVariable a = g.getDouble("a");
                DoubleVariable b = g.getDouble("b");

                DoubleVariable powTwoA = a.times(a);
                DoubleVariable powThreeB = b.times(b.times(b));

                ArrayVariable<DoubleVariable> array = Variable.arrayVariable(null, VariableType.DoubleVariable,
                        IntVariable.intVariable(3));
                array.setAlias("array");

                array.put(0, powTwoA);
                DoubleVariable p1 = array.get(0);
                p1.setAlias("p1");

                array.put(1, powThreeB);
                DoubleVariable p2 = array.get(1);
                p2.setAlias("p2");

                array.put(2, a);
                DoubleVariable p3 = array.get(2);
                p3.setAlias("p3");

                return new DagVars(List.of(p1, p2, p3), List.of(a), List.of(b));
            }
        };
    }

    private static DagTestGenerator array2dTest1() {
        return new DagTestGenerator("2d_array_mk1") {
            public DagVars generateDag(InputGenerator g) {

                DoubleVariable a = g.getDouble("a");
                DoubleVariable b = g.getDouble("b");

                DoubleVariable powTwoA = a.times(a);
                DoubleVariable powThreeB = b.times(b.times(b));

                ArrayVariable<ArrayVariable<DoubleVariable>> array = Variable.arrayVariable(null,
                        VariableType.arrayType(VariableType.DoubleVariable), IntVariable.intVariable(2));
                array.setAlias("array");

                ArrayVariable<DoubleVariable> inner0 = Variable.arrayVariable(null, VariableType.DoubleVariable,
                        IntVariable.intVariable(1));
                inner0.setAlias("inner0");
                inner0.setPrivate();

                ArrayVariable<DoubleVariable> inner1 = Variable.arrayVariable(null, VariableType.DoubleVariable,
                        IntVariable.intVariable(1));
                inner1.setAlias("inner1");
                inner1.setPrivate();

                inner0.put(0, powTwoA);
                array.put(0, inner0);

                DoubleVariable p1 = array.get(0).get(0);
                p1.setAlias("p1");

                inner1.put(0, powThreeB);
                array.put(1, inner1);

                DoubleVariable p2 = array.get(1).get(0);
                p2.setAlias("p2");

                return new DagVars(List.of(p1, p2), List.of(a), List.of(b));
            }
        };
    }

    private static DagTestGenerator array2dSecondDifferentialTest1() {
        return new DagTestGenerator("2d_array_2nd_differential_mk1") {
            public DagVars generateDag(InputGenerator g) {
                BooleanVariable guardVar = g.getBoolean("guard");

                Wrapper ifBody = new Wrapper();
                Wrapper elseBody = new Wrapper();
                DoubleVariable c = Sandwood.ifElseLambdaAssignment(guardVar, () -> {
                    DoubleVariable a = g.getDouble("a");
                    ifBody.value = a;
                    return a;
                }, () -> {
                    DoubleVariable b = g.getDouble("b");
                    elseBody.value = b;
                    return b.times(b).times(b);
                }, null);
                c.setAlias("c");

                DoubleVariable d = g.getDouble("d");

                DoubleVariable powTwoD = d.times(d);

                ArrayVariable<ArrayVariable<DoubleVariable>> array = Variable.arrayVariable(null,
                        VariableType.arrayType(VariableType.DoubleVariable), IntVariable.intVariable(2));
                array.setAlias("array");

                ArrayVariable<DoubleVariable> inner0 = Variable.arrayVariable(null, VariableType.DoubleVariable,
                        IntVariable.intVariable(5));
                inner0.setAlias("inner0");
                inner0.setPrivate();

                ArrayVariable<DoubleVariable> inner1 = Variable.arrayVariable(null, VariableType.DoubleVariable,
                        IntVariable.intVariable(4));
                inner1.setAlias("inner1");
                inner1.setPrivate();

                inner0.put(0, powTwoD);
                inner0.put(1, powTwoD.subtract(d));
                inner0.put(2, powTwoD.add(powTwoD));
                inner0.put(3, powTwoD.add(10));
                inner0.put(4, powTwoD.add(10));
                array.put(0, inner0);

                inner1.put(0, d);
                inner1.put(1, c);
                array.put(1, inner1);

                DoubleVariable p1 = array.get(0).get(0);
                p1.setAlias("p1");
                return new DagVars(List.of(p1), List.of(ifBody.value), List.of(elseBody.value), List.of(c));
            }
        };
    }

    private static DagTestGenerator array3dTest1() {
        return new DagTestGenerator("3d_array_mk1") {
            public DagVars generateDag(InputGenerator g) {

                DoubleVariable a = g.getDouble("a");
                DoubleVariable b = g.getDouble("b");

                DoubleVariable powTwoA = a.times(a);
                DoubleVariable powThreeB = b.times(b.times(b));

                ArrayVariable<ArrayVariable<ArrayVariable<DoubleVariable>>> array = Variable.arrayVariable(null,
                        VariableType.arrayType(VariableType.arrayType(VariableType.DoubleVariable)),
                        IntVariable.intVariable(2));
                array.setAlias("array");

                ArrayVariable<ArrayVariable<DoubleVariable>> inner0 = Variable.arrayVariable(null,
                        VariableType.arrayType(VariableType.DoubleVariable), IntVariable.intVariable(1));
                inner0.setAlias("inner0");
                inner0.setPrivate();

                ArrayVariable<ArrayVariable<DoubleVariable>> inner1 = Variable.arrayVariable(null,
                        VariableType.arrayType(VariableType.DoubleVariable), IntVariable.intVariable(1));
                inner1.setAlias("inner1");
                inner1.setPrivate();

                ArrayVariable<DoubleVariable> inner2 = Variable.arrayVariable(null, VariableType.DoubleVariable,
                        IntVariable.intVariable(1));
                inner2.setAlias("inner2");
                inner2.setPrivate();

                ArrayVariable<DoubleVariable> inner3 = Variable.arrayVariable(null, VariableType.DoubleVariable,
                        IntVariable.intVariable(1));
                inner3.setAlias("inner3");
                inner3.setPrivate();

                // Prepare a 3D array.
                array.put(0, inner0);
                array.put(1, inner1);
                inner0.put(0, inner2);
                inner1.put(0, inner3);
                inner2.put(0, powTwoA);
                inner3.put(0, powThreeB);

                DoubleVariable p1 = array.get(0).get(0).get(0);
                p1.setAlias("p1");

                return new DagVars(List.of(p1), List.of(a), List.of(b));
            }
        };
    }

    private static DagTestGenerator array3dTest2() {
        return new DagTestGenerator("3d_array_mk2") {
            public DagVars generateDag(InputGenerator g) {

                DoubleVariable a = g.getDouble("a");
                DoubleVariable b = g.getDouble("b");

                DoubleVariable powTwoA = a.times(a);
                DoubleVariable powThreeB = b.times(b.times(b));

                ArrayVariable<ArrayVariable<ArrayVariable<DoubleVariable>>> array = Variable.arrayVariable(null,
                        VariableType.arrayType(VariableType.arrayType(VariableType.DoubleVariable)),
                        IntVariable.intVariable(2));
                array.setAlias("array");

                ArrayVariable<ArrayVariable<DoubleVariable>> inner0 = Variable.arrayVariable(null,
                        VariableType.arrayType(VariableType.DoubleVariable), IntVariable.intVariable(1));
                inner0.setAlias("inner0");
                inner0.setPrivate();

                ArrayVariable<ArrayVariable<DoubleVariable>> inner1 = Variable.arrayVariable(null,
                        VariableType.arrayType(VariableType.DoubleVariable), IntVariable.intVariable(1));
                inner1.setAlias("inner1");
                inner1.setPrivate();

                ArrayVariable<DoubleVariable> inner2 = Variable.arrayVariable(null, VariableType.DoubleVariable,
                        IntVariable.intVariable(5));
                inner2.setAlias("inner2");
                inner2.setPrivate();

                ArrayVariable<DoubleVariable> inner3 = Variable.arrayVariable(null, VariableType.DoubleVariable,
                        IntVariable.intVariable(5));
                inner3.setAlias("inner3");
                inner3.setPrivate();

                BooleanVariable guardVar = g.getBoolean("guard");

                Wrapper ifBody = new Wrapper();
                Wrapper elseBody = new Wrapper();

                DoubleVariable e = Sandwood.ifElseLambdaAssignment(guardVar, () -> {
                    DoubleVariable c = g.getDouble("c");
                    ifBody.value = c;
                    return c;
                }, () -> {
                    DoubleVariable d = g.getDouble("d");
                    elseBody.value = d;
                    return d.times(d).times(d);
                }, null);
                e.setAlias("e");

                // Prepare 3D Array.
                inner2.put(0, powTwoA);
                inner2.put(1, powTwoA.add(powTwoA));
                inner2.put(2, a.subtract(b));
                inner2.put(3, a.times(b));
                inner2.put(4, IntVariable.intVariable(10));

                inner3.put(0, e.times(powThreeB));
                inner3.put(1, e.divide(e));
                inner3.put(2, DoubleVariable.doubleVariable(10.0));
                inner3.put(3, IntVariable.intVariable(10));
                inner3.put(4, Sigmoid.sigmoid(a));

                inner0.put(0, inner2);
                inner1.put(0, inner3);

                array.put(0, inner0);
                array.put(1, inner1);

                DoubleVariable p1 = array.get(0).get(0).get(0);
                p1.setAlias("p1");
                return new DagVars(List.of(p1), List.of(a), List.of(b), List.of(ifBody.value), List.of(elseBody.value));
            }
        };
    }

    private static DagTestGenerator array1dForLoopTest1() {
        return new DagTestGenerator("1d_array_for_loop_mk1") {
            public DagVars generateDag(InputGenerator g) {
                DoubleVariable a = g.getDouble("a");
                DoubleVariable b = g.getDouble("b");

                // Prepare 1D array.
                ArrayVariable<DoubleVariable> array = Variable.arrayVariable(null, VariableType.DoubleVariable,
                        IntVariable.intVariable(5));
                array.setAlias("array");

                array.put(0, DoubleVariable.doubleVariable(0.5));

                array.put(1, b.times(b));

                // Prepare loop.
                IntVariable start = IntVariable.intVariable(2);
                IntVariable end = IntVariable.intVariable(5);
                IntVariable step = IntVariable.intVariable(1);

                Sandwood.forLoop(start, end, step, true, (i) -> {
                    i.setAlias("i");
                    array.put(i, i.times(b).times(b));
                });

                DoubleVariable c = array.get(0);
                c.setAlias("c");

                return new DagVars(List.of(c), List.of(a), List.of(b));
            }
        };
    }

    private static DagTestGenerator array2dForLoopTest1() {
        return new DagTestGenerator("2d_array_for_loop_mk1") {
            public DagVars generateDag(InputGenerator g) {

                DoubleVariable a = g.getDouble("a");
                DoubleVariable b = g.getDouble("b");

                // Prepare 2D array.
                ArrayVariable<ArrayVariable<DoubleVariable>> array = Variable.arrayVariable(null,
                        VariableType.arrayType(VariableType.DoubleVariable), IntVariable.intVariable(10));
                array.setAlias("array");

                // Prepare loop.
                IntVariable start = IntVariable.intVariable(0);
                IntVariable end = IntVariable.intVariable(10);
                IntVariable step = IntVariable.intVariable(1);

                Wrapper bodyValue = new Wrapper();

                // Prepare double loop.
                Sandwood.forLoop(start, end, step, true, (i) -> {
                    i.setAlias("i");
                    ArrayVariable<DoubleVariable> inner = Variable.arrayVariable(null, VariableType.DoubleVariable,
                            IntVariable.intVariable(10));
                    inner.setAlias("inner");
                    array.put(i, inner);

                    Sandwood.forLoop(start, end, step, true, (j) -> {
                        j.setAlias("j");
                        array.get(i).put(j, a.times(i));
                    });

                    DoubleVariable c = inner.get(0);
                    c.setAlias("c");
                    c.setPublic();
                    bodyValue.value = c;
                });

                DoubleVariable d = array.get(0).get(0);
                d.setAlias("d");

                return new DagVars(List.of(bodyValue.value, d), List.of(a), List.of(b));
            }
        };
    }

    private static DagTestGenerator array3dForLoopTest1() {
        return new DagTestGenerator("3d_array_for_loop_mk1") {
            public DagVars generateDag(InputGenerator g) {

                DoubleVariable a = g.getDouble("a");
                DoubleVariable b = g.getDouble("b");

                // Prepare 3D array.
                ArrayVariable<ArrayVariable<ArrayVariable<DoubleVariable>>> array = Variable.arrayVariable(null,
                        VariableType.arrayType(VariableType.arrayType(VariableType.DoubleVariable)),
                        IntVariable.intVariable(10));
                array.setAlias("array");

                // Prepare loop.
                IntVariable start = IntVariable.intVariable(0);
                IntVariable end = IntVariable.intVariable(10);
                IntVariable step = IntVariable.intVariable(1);

                Wrapper bodyValue = new Wrapper();

                // Prepare double loop.
                Sandwood.forLoop(start, end, step, true, (i) -> {
                    i.setAlias("i");
                    ArrayVariable<ArrayVariable<DoubleVariable>> inner = Variable.arrayVariable(null,
                            VariableType.arrayType(VariableType.DoubleVariable), IntVariable.intVariable(10));
                    array.put(i, inner);

                    Sandwood.forLoop(start, end, step, true, (j) -> {
                        j.setAlias("j");
                        // Setup inner array to use in inner loop.
                        ArrayVariable<DoubleVariable> innerInner = Variable.arrayVariable(null,
                                VariableType.DoubleVariable, IntVariable.intVariable(10));
                        array.get(i).put(j, innerInner);

                        Sandwood.forLoop(start, end, step, true, (k) -> {
                            k.setAlias("k");
                            array.get(i).get(j).put(k, a.times(i.times(j)));
                        });
                    });

                    DoubleVariable c = inner.get(0).get(0);
                    c.setAlias("c");
                    c.setPublic();
                    bodyValue.value = c;
                });

                DoubleVariable d = array.get(0).get(0).get(0);
                d.setAlias("d");

                return new DagVars(List.of(bodyValue.value, d), List.of(a), List.of(b));
            }
        };
    }

    private static DagTestGenerator array4dForLoopTest1() {
        return new DagTestGenerator("4d_array_for_loop_mk1") {
            public DagVars generateDag(InputGenerator g) {

                DoubleVariable a = g.getDouble("a");
                DoubleVariable b = g.getDouble("b");

                // Prepare 4D array.
                IntVariable start = IntVariable.intVariable(0);
                IntVariable end = IntVariable.intVariable(10);
                IntVariable step = IntVariable.intVariable(1);

                Wrapper bodyValue = new Wrapper();

                ArrayVariable<ArrayVariable<ArrayVariable<ArrayVariable<DoubleVariable>>>> array = Variable
                        .arrayVariable(null,
                                VariableType.arrayType(
                                        VariableType.arrayType(VariableType.arrayType(VariableType.DoubleVariable))),
                                IntVariable.intVariable(10));
                array.setAlias("array");

                // Prepare quadruple loop.
                Sandwood.forLoop(start, end, step, true, (i) -> {
                    i.setAlias("i");

                    ArrayVariable<ArrayVariable<ArrayVariable<DoubleVariable>>> inner0 = Variable.arrayVariable(null,
                            VariableType.arrayType(VariableType.arrayType(VariableType.DoubleVariable)),
                            IntVariable.intVariable(10));
                    inner0.setAlias("inner0");
                    array.put(i, inner0);

                    Sandwood.forLoop(start, end, step, true, (j) -> {
                        j.setAlias("j");

                        ArrayVariable<ArrayVariable<DoubleVariable>> inner1 = Variable.arrayVariable(null,
                                VariableType.arrayType(VariableType.DoubleVariable), IntVariable.intVariable(10));
                        inner1.setAlias("inner1");
                        inner0.put(j, inner1);
                    });
                });

                // Prepare quadruple loop.
                Sandwood.forLoop(start, end, step, true, (i) -> {
                    i.setAlias("i");
                    Sandwood.forLoop(start, end, step, true, (j) -> {
                        j.setAlias("j");
                        Sandwood.forLoop(start, end, step, true, (k) -> {
                            k.setAlias("k");
                            // Setup inner array to use in inner loop.
                            ArrayVariable<DoubleVariable> inner = Variable.arrayVariable(null,
                                    VariableType.DoubleVariable, IntVariable.intVariable(10));
                            inner.setAlias("inner");
                            array.get(i).get(j).put(k, inner);
                            Sandwood.forLoop(start, end, step, true, (l) -> {
                                l.setAlias("l");
                                array.get(i).get(j).get(k).put(l,
                                        a.times(a).times(a).times(i).times(j).times(k).times(l));
                            });

                            DoubleVariable c = inner.get(0);
                            c.setAlias("c");
                            c.setPublic();
                            bodyValue.value = c;
                        });
                    });
                });

                DoubleVariable d = array.get(0).get(0).get(0).get(0);
                d.setAlias("d");

                return new DagVars(List.of(bodyValue.value, d), List.of(a, a, a), List.of(a, b, a), List.of(b));
            }
        };
    }
}