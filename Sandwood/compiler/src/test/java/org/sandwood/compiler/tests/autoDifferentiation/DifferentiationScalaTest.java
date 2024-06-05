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
import org.sandwood.compiler.dataflowGraph.Math;
import org.sandwood.compiler.dataflowGraph.Sandwood;
import org.sandwood.compiler.dataflowGraph.tasks.returnTasks.Exp;
import org.sandwood.compiler.dataflowGraph.tasks.returnTasks.Log;
import org.sandwood.compiler.dataflowGraph.tasks.returnTasks.Max;
import org.sandwood.compiler.dataflowGraph.tasks.returnTasks.Min;
import org.sandwood.compiler.dataflowGraph.tasks.returnTasks.Sigmoid;
import org.sandwood.compiler.dataflowGraph.variables.Variable;
import org.sandwood.compiler.dataflowGraph.variables.scalarVariables.BooleanVariable;
import org.sandwood.compiler.dataflowGraph.variables.scalarVariables.DoubleVariable;
import org.sandwood.compiler.dataflowGraph.variables.scalarVariables.IntVariable;

public class DifferentiationScalaTest extends DifferentiationTestBase {

    private static final boolean constructingResults = false;

    private static class Wrapper {
        DoubleVariable value;
    }

    public DifferentiationScalaTest() {
        super(constructingResults);
    }

    private static List<DagTestGenerator> testGenerators() {
        List<DagTestGenerator> l = new ArrayList<>();

        l.add(subtractDD());
        l.add(subtractDI());
        l.add(doubleRemainder());
        l.add(doubleMax());
        l.add(complexDifferentiation());
        l.add(additionDD());
        l.add(additionDI());
        l.add(conditionalAssignment());
        l.add(doubleConstant());
        l.add(doubleDivide());
        l.add(doubleExp());
        l.add(doubleExpFunc());
        l.add(doubleForLoop());
        l.add(doubleLog());
        l.add(doubleLogFunc());
        l.add(doubleMin());
        l.add(doubleMultiplication());
        l.add(doubleSigmoid());
        l.add(doubleSigmoidFunc());
        l.add(doubleToInt());

        Collections.sort(l);
        return l;
    }

    @ParameterizedTest
    @MethodSource("testGenerators")
    @DisplayName("Test differentiation of scala only values")
    void runTests(DagTestGenerator g) throws NoSuchMethodException, SecurityException, IllegalAccessException,
            IllegalArgumentException, InvocationTargetException, IOException {
        for(DagTest t:g.generate(this))
            test(t);
        assertFalse(constructingResults, "Results are being written to files, this invalidates the tests.");
    }

    private static DagTestGenerator subtractDD() {
        return new DagTestGenerator("subtractDD") {
            public DagVars generateDag(InputGenerator g) {
                DoubleVariable a = g.getDouble("a");
                DoubleVariable b = g.getDouble("b");
                DoubleVariable c = g.getDouble("c");

                DoubleVariable d = a.subtract(b);
                d.setAlias("d");

                return new DagVars(List.of(d), List.of(a), List.of(b), List.of(c));
            }
        };
    }

    private static DagTestGenerator subtractDI() {
        return new DagTestGenerator("substractDI") {
            public DagVars generateDag(InputGenerator g) {
                DoubleVariable a = g.getDouble("a");
                IntVariable b = g.getInt("b");

                DoubleVariable d = a.subtract(b);
                d.setAlias("d");

                return new DagVars(List.of(d), List.of(a));
            }
        };
    }

    private static DagTestGenerator doubleRemainder() {
        return new DagTestGenerator("remainder") {
            public DagVars generateDag(InputGenerator g) {
                DoubleVariable a = g.getDouble("a");
                DoubleVariable b = g.getDouble("b");
                DoubleVariable c = g.getDouble("c");

                DoubleVariable d = a.remainder(b);
                d.setAlias("d");

                return new DagVars(List.of(d), List.of(c));
            }
        };
    }

    private static DagTestGenerator doubleMax() {
        return new DagTestGenerator("max") {
            public DagVars generateDag(InputGenerator g) {
                DoubleVariable a = g.getDouble("a");
                DoubleVariable b = g.getDouble("b");
                DoubleVariable c = g.getDouble("c");

                DoubleVariable d = Max.max(a, b);
                d.setAlias("d");
                return new DagVars(List.of(d), List.of(c));
            }
        };
    }

    private static DagTestGenerator complexDifferentiation() {
        return new DagTestGenerator("complexDifferential") {
            public DagVars generateDag(InputGenerator g) {
                DoubleVariable a = g.getDouble("a");
                DoubleVariable b = g.getDouble("b");

                // c = (a^4 - 5*a)/(7*b + 7*b)

                // a^4
                DoubleVariable aPowFour = Math.pow(a, Variable.intVariable(4));

                // 5*a
                DoubleVariable fiveTimesA = Variable.doubleVariable(5.0).times(a);

                // a^4 + 5*a
                DoubleVariable addNumerator = aPowFour.add(fiveTimesA);

                // 7*b
                DoubleVariable sevenTimesB = Variable.doubleVariable(7.0).times(b);

                // numerator/(7b + 7b)
                DoubleVariable c = addNumerator.divide(sevenTimesB.add(sevenTimesB));
                c.setAlias("c");

                return new DagVars(List.of(c), List.of(a, a), List.of(a, b), List.of(b, a), List.of(b, b));
            }
        };
    }

    private static DagTestGenerator additionDD() {
        return new DagTestGenerator("additionDD") {
            public DagVars generateDag(InputGenerator g) {
                DoubleVariable a = g.getDouble("a");
                DoubleVariable b = g.getDouble("b");
                DoubleVariable c = g.getDouble("c");

                DoubleVariable d = a.add(b);
                d.setAlias("d");

                return new DagVars(List.of(d), List.of(a), List.of(b), List.of(c));
            }
        };
    }

    private static DagTestGenerator additionDI() {
        return new DagTestGenerator("additionDI") {
            public DagVars generateDag(InputGenerator g) {
                DoubleVariable a = g.getDouble("a");
                IntVariable b = g.getInt("b");

                DoubleVariable d = a.add(b);
                d.setAlias("d");

                return new DagVars(List.of(d), List.of(d), List.of(a));
            }
        };
    }

    private static DagTestGenerator conditionalAssignment() {
        return new DagTestGenerator("conditionalAssignment") {
            public DagVars generateDag(InputGenerator g) {
                DoubleVariable a = g.getDouble("a");
                DoubleVariable b = g.getDouble("b");
                DoubleVariable c = g.getDouble("c");

                BooleanVariable guard = g.getBoolean("guard");

                DoubleVariable d = Sandwood.ifElseLambdaAssignment(guard, () -> {
                    return a;
                }, () -> {
                    return a.subtract(b.times(c));
                }, null);
                d.setAlias("d");

                return new DagVars(List.of(d), List.of(a), List.of(b), List.of(c));
            }
        };
    }

    private static DagTestGenerator doubleConstant() {
        return new DagTestGenerator("constant") {
            public DagVars generateDag(InputGenerator g) {
                DoubleVariable a = g.getDouble("a", 10.0);
                DoubleVariable b = g.getDouble("b", 50.0);

                return new DagVars(List.of(a), List.of(a), List.of(b));
            }
        };
    }

    private static DagTestGenerator doubleDivide() {
        return new DagTestGenerator("divide") {
            public DagVars generateDag(InputGenerator g) {
                DoubleVariable a = g.getDouble("a");
                DoubleVariable b = g.getDouble("b");
                DoubleVariable c = g.getDouble("c");

                DoubleVariable d = a.divide(b);
                d.setAlias("d");

                return new DagVars(List.of(d), List.of(a), List.of(b), List.of(c));
            }
        };
    }

    private static DagTestGenerator doubleExp() {
        return new DagTestGenerator("exp") {
            public DagVars generateDag(InputGenerator g) {
                DoubleVariable a = g.getDouble("a");
                DoubleVariable b = g.getDouble("b");

                DoubleVariable d = Exp.exp(a);
                d.setAlias("d");

                return new DagVars(List.of(d), List.of(a), List.of(b));
            }
        };
    }

    private static DagTestGenerator doubleExpFunc() {
        return new DagTestGenerator("expFunc") {
            public DagVars generateDag(InputGenerator g) {
                DoubleVariable a = g.getDouble("a");
                DoubleVariable b = g.getDouble("b");

                DoubleVariable d = Exp.exp(a.times(a.times(a)));
                d.setAlias("d");

                return new DagVars(List.of(d), List.of(a), List.of(b));
            }
        };
    }

    private static DagTestGenerator doubleForLoop() {
        return new DagTestGenerator("forLoop") {
            public DagVars generateDag(InputGenerator g) {
                DoubleVariable a = g.getDouble("a");
                DoubleVariable b = g.getDouble("b");

                Wrapper w = new Wrapper();
                Sandwood.forLoop(IntVariable.intVariable(0), IntVariable.intVariable(10), IntVariable.intVariable(1),
                        true, (IntVariable i) -> {
                            i.setAlias("i");
                            DoubleVariable d = a.times(i.add(Variable.intVariable(10)));
                            d.setAlias("d");
                            d.setPublic();
                            w.value = d;
                        });

                return new DagVars(List.of(w.value), List.of(a), List.of(b));
            }
        };
    }

    private static DagTestGenerator doubleLog() {
        return new DagTestGenerator("log") {
            public DagVars generateDag(InputGenerator g) {
                DoubleVariable a = g.getDouble("a");
                DoubleVariable b = g.getDouble("b");

                DoubleVariable d = Log.log(a);
                d.setAlias("d");

                return new DagVars(List.of(d), List.of(a), List.of(b));
            }
        };
    }

    private static DagTestGenerator doubleLogFunc() {
        return new DagTestGenerator("logFunc") {
            public DagVars generateDag(InputGenerator g) {
                DoubleVariable a = g.getDouble("a");
                DoubleVariable b = g.getDouble("b");

                DoubleVariable d = Log.log(a.times(a.times(a)));
                d.setAlias("d");

                return new DagVars(List.of(d), List.of(a), List.of(b));
            }
        };
    }

    private static DagTestGenerator doubleMin() {
        return new DagTestGenerator("min") {
            public DagVars generateDag(InputGenerator g) {
                DoubleVariable a = g.getDouble("a");
                DoubleVariable b = g.getDouble("b");
                DoubleVariable c = g.getDouble("c");

                DoubleVariable d = Min.min(a, b);
                d.setAlias("d");

                return new DagVars(List.of(d), List.of(c));
            }
        };
    }

    private static DagTestGenerator doubleMultiplication() {
        return new DagTestGenerator("multiplication") {
            public DagVars generateDag(InputGenerator g) {
                DoubleVariable a = g.getDouble("a");
                DoubleVariable b = g.getDouble("b");
                DoubleVariable c = g.getDouble("c");

                DoubleVariable d = a.times(b);
                d.setAlias("d");

                return new DagVars(List.of(d), List.of(a), List.of(b), List.of(c));
            }
        };
    }

    private static DagTestGenerator doubleSigmoid() {
        return new DagTestGenerator("sigmoid") {
            public DagVars generateDag(InputGenerator g) {
                DoubleVariable a = g.getDouble("a");
                DoubleVariable b = g.getDouble("b");

                DoubleVariable d = Sigmoid.sigmoid(a);
                d.setAlias("d");
                return new DagVars(List.of(d), List.of(a), List.of(b));
            }
        };
    }

    private static DagTestGenerator doubleSigmoidFunc() {
        return new DagTestGenerator("sigmoidFunc") {
            public DagVars generateDag(InputGenerator g) {
                DoubleVariable a = g.getDouble("a");
                DoubleVariable b = g.getDouble("b");

                DoubleVariable d = Sigmoid.sigmoid(a.times(a.times(a)));
                d.setAlias("d");
                return new DagVars(List.of(d), List.of(a), List.of(b));
            }
        };
    }

    private static DagTestGenerator doubleToInt() {
        return new DagTestGenerator("castToInt") {
            public DagVars generateDag(InputGenerator g) {
                DoubleVariable a = g.getDouble("a");
                DoubleVariable b = g.getDouble("b");

                IntVariable d = (a.castToInteger());
                d.setAlias("d");

                return new DagVars(List.of(d), List.of(b));
            }
        };
    }
}
