/*
 * Sandwood
 *
 * Copyright (c) 2019-2026, Oracle and/or its affiliates
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl/
 */

package org.sandwood.compiler.tests.autoDifferentiation;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;
import org.sandwood.compiler.dataflowGraph.scopes.IfScope;
import org.sandwood.compiler.dataflowGraph.scopes.ScopeStack;
import org.sandwood.compiler.dataflowGraph.tasks.returnTasks.Log;
import org.sandwood.compiler.dataflowGraph.tasks.returnTasks.Max;
import org.sandwood.compiler.dataflowGraph.tasks.returnTasks.Sigmoid;
import org.sandwood.compiler.dataflowGraph.tasks.sandwoodOperators.IfElseNumberAssignmentTask;
import org.sandwood.compiler.dataflowGraph.variables.Variable;
import org.sandwood.compiler.dataflowGraph.variables.scalarVariables.BooleanVariable;
import org.sandwood.compiler.dataflowGraph.variables.scalarVariables.DoubleVariable;
import org.sandwood.compiler.dataflowGraph.variables.scalarVariables.IntVariable;

class DifferentiableTest {

    private final DoubleVariable zero = Variable.doubleVariable(0.0);
    private final DoubleVariable doubleA = Variable.doubleVariable(5.0);
    private final DoubleVariable doubleB = Variable.doubleVariable(25.0);
    private final IntVariable intA = Variable.intVariable(30);
    private final IntVariable intB = Variable.intVariable(70);

    @Test
    public void testDoubleVariable() {
        // Check differentiation on same variable.
        assertTrue(doubleA.isDifferentiable(doubleA));

        // Check differentiation on different variable.
        assertTrue(doubleA.isDifferentiable(doubleB));
    }

    @Test
    public void testDoubleToIntCast() {
        // Check differentiation on same variable.
        IntVariable castDoubleA = doubleA.castToInteger();
        assertFalse(castDoubleA.isDifferentiable(doubleA));
    }

    @Test
    public void testIntVariable() {
        // Check differentiation on differentiable variable.
        assertTrue(intA.isDifferentiable(doubleB));
    }

    @Test
    public void testIntToDoubleCast() {
        // Check on same variable.
        DoubleVariable castIntA = intA.castToDouble();

        // Check on differentiable independent variable.
        assertTrue(castIntA.isDifferentiable(doubleA));
    }

    @Test
    public void testDoubleDoubleSum() {
        DoubleVariable doubleDoubleSum = doubleA.add(doubleA.add(doubleB));
        assertTrue(doubleDoubleSum.isDifferentiable(doubleA));
        assertTrue(doubleDoubleSum.isDifferentiable(doubleB));

        // Check for non-differentiable types.
        assertTrue(doubleDoubleSum.isDifferentiable(doubleB));
    }

    @Test
    public void testDoubleIntSum() {
        DoubleVariable doubleIntAdd = doubleA.add(intA);
        assertTrue(doubleIntAdd.isDifferentiable(doubleA));
        assertTrue(doubleIntAdd.isDifferentiable(doubleB));
    }

    @Test
    public void testIntIntSum() {
        IntVariable intIntSum = (IntVariable) intA.add(intB);
        assertTrue(intIntSum.isDifferentiable(doubleA));
    }

    @Test
    public void testDoubleIntConsequtiveSum() {
        DoubleVariable doubleIntAdd = doubleA.add(intA.add(intB));
        assertTrue(doubleIntAdd.isDifferentiable(doubleA));
        assertTrue(doubleIntAdd.isDifferentiable(doubleB));
    }

    @Test
    public void testdoubleDoubleSubtraction() {
        DoubleVariable doubleDoubleSub = (DoubleVariable) doubleA.subtract(doubleB);
        assertTrue(doubleDoubleSub.isDifferentiable(doubleA));
        assertTrue(doubleDoubleSub.isDifferentiable(doubleB));
    }

    @Test
    public void testDoubleIntSubtraction() {
        DoubleVariable doubleIntSub = (DoubleVariable) doubleA.subtract(intA);
        assertTrue(doubleIntSub.isDifferentiable(doubleA));
        assertTrue(doubleIntSub.isDifferentiable(doubleB));
    }

    @Test
    public void testIntIntSubtraction() {
        IntVariable intIntSub = (IntVariable) intA.subtract(intB);
        assertTrue(intIntSub.isDifferentiable(doubleA));
        assertTrue(intIntSub.isDifferentiable(doubleB));
    }

    @Test
    public void testDoubleDoubleMultiplication() {
        DoubleVariable doubleDoubleMul = (DoubleVariable) doubleA.times(doubleB);
        assertTrue(doubleDoubleMul.isDifferentiable(doubleA));
        assertTrue(doubleDoubleMul.isDifferentiable(doubleB));
    }

    @Test
    public void testDoubleIntMultiplication() {
        DoubleVariable doubleIntMul = (DoubleVariable) doubleA.times(intA);
        assertTrue(doubleIntMul.isDifferentiable(doubleA));
        assertTrue(doubleIntMul.isDifferentiable(doubleB));
    }

    @Test
    public void testIntIntMultiplication() {
        IntVariable intIntMul = (IntVariable) intA.times(intB);
        assertTrue(intIntMul.isDifferentiable(doubleA));
        assertTrue(intIntMul.isDifferentiable(doubleB));
    }

    @Test
    public void testDoubleDoubleDivision() {
        DoubleVariable doubleDoubleDiv = (DoubleVariable) doubleA.divide(doubleB);
        assertTrue(doubleDoubleDiv.isDifferentiable(doubleA));
        assertTrue(doubleDoubleDiv.isDifferentiable(doubleB));
    }

    @Test
    public void testDoubleIntDivision() {
        DoubleVariable doubleIntDiv = (DoubleVariable) doubleA.divide(intA);
        assertTrue(doubleIntDiv.isDifferentiable(doubleA));
        assertTrue(doubleIntDiv.isDifferentiable(doubleB));
    }

    @Test
    public void testIntIntDivision() {
        IntVariable intIntDiv = (IntVariable) intA.divide(intB);
        assertTrue(intIntDiv.isDifferentiable(doubleA));
        assertTrue(intIntDiv.isDifferentiable(doubleB));
    }

    @Test
    public void testNumberNegate() {
        // Double Negate check
        DoubleVariable doubleANegate = doubleA.negate();
        assertTrue(doubleANegate.isDifferentiable(doubleANegate));
        assertTrue(doubleANegate.isDifferentiable(doubleA));
        assertTrue(doubleANegate.isDifferentiable(doubleB));

        // Integer Negate check.
        IntVariable intANegate = intA.negate();
        assertTrue(intANegate.isDifferentiable(doubleA));
    }

    @Test
    public void testNumberRemainder() {
        DoubleVariable doubleRemainder = doubleA.remainder(doubleB);

        // Double mod Double case.
        assertFalse(doubleRemainder.isDifferentiable(doubleA));
        assertFalse(doubleRemainder.isDifferentiable(doubleB));

        // Double mod Integer case.
        DoubleVariable doubleIntRemainder = doubleA.remainder(intA);
        assertFalse(doubleIntRemainder.isDifferentiable(doubleA));

        // Int mod Integer case.
        IntVariable intIntRemainder = intA.remainder(intB);
        // The doubleA variable is not contained,
        // therefore remainder is differentiable.
        assertTrue(intIntRemainder.isDifferentiable(doubleA));
    }

    @Test
    public void testNumberLog() {
        DoubleVariable doubleLog = Log.log(doubleA);

        // Double mod Double case.
        assertTrue(doubleLog.isDifferentiable(doubleA));
        assertTrue(doubleLog.isDifferentiable(doubleB));

        // NOTE: We are aware that this will return true for the case
        // that the input is a 0, but the differential will essentially
        // fail upon execution, due to divide-by-zero error.
        // So the code generation is possible, but the code will fail.
        DoubleVariable zeroLog = Log.log(zero);
        assertTrue(zeroLog.isDifferentiable(zero));
    }

    @Test
    public void testNumberSigmoid() {
        DoubleVariable doubleASigmoid = Sigmoid.sigmoid(doubleA);
        assertTrue(doubleASigmoid.isDifferentiable(doubleA));
        assertTrue(doubleASigmoid.isDifferentiable(doubleB));
    }

    @Test
    public void testDoubleDoubleMax() {
        DoubleVariable doubleDoubleMax = Max.max(doubleA, doubleB);
        assertFalse(doubleDoubleMax.isDifferentiable(doubleA));
        assertFalse(doubleDoubleMax.isDifferentiable(doubleB));
    }

    @Test
    public void intIntMax() {

        IntVariable intIntMax = Max.max(intA, intB);
        assertTrue(intIntMax.isDifferentiable(doubleA));
    }

    @Test
    public void testIntIntMax() {
        IntVariable intIntSum = (IntVariable) intA.add(intB);

        assertTrue(intIntSum.isDifferentiable(doubleA));
    }

    @Test
    public void testIfElseAssignmentAdd() {

        IntVariable intVar1 = IntVariable.intVariable(10);
        BooleanVariable guard = intVar1.lessThanEqual(DoubleVariable.doubleVariable(10));
        IfScope ifScope = new IfScope(guard);
        ScopeStack.pushScope(ifScope);
        DoubleVariable doubleVar2 = DoubleVariable.doubleVariable(20.0);
        ScopeStack.popScope(ifScope);

        ScopeStack.pushScope(ifScope.elseScope);
        DoubleVariable doubleVar3 = DoubleVariable.doubleVariable(30.0);
        DoubleVariable doubleVar4 = DoubleVariable.doubleVariable(40.0);
        DoubleVariable doubleVar5 = DoubleVariable.doubleVariable(50.0);
        DoubleVariable calc = doubleVar5.add(doubleVar2.subtract(doubleVar3.times(doubleVar4.divide(10))));
        ScopeStack.popScope(ifScope.elseScope);

        IfElseNumberAssignmentTask<DoubleVariable> task = new IfElseNumberAssignmentTask<DoubleVariable>(guard,
                doubleVar2, calc, ifScope, null);
        // Guard does not contain variable, and both conditions are differentiable.
        assertTrue(task.isDifferentiable(doubleVar2));
        assertTrue(task.isDifferentiable(doubleVar3));
    }

}