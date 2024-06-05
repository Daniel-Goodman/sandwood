/*
 * Sandwood
 *
 * Copyexp (c) 2019-2026, Oracle and/or its affiliates
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl/
 */

package org.sandwood.compiler.dataflowGraph.tasks.returnTasks;

import org.sandwood.compiler.compilation.CompilationContext;
import org.sandwood.compiler.dataflowGraph.autoDiff.DifferentialInfo;
import org.sandwood.compiler.dataflowGraph.tasks.DFType;
import org.sandwood.compiler.dataflowGraph.tasks.DataflowTask;
import org.sandwood.compiler.dataflowGraph.tasks.NumberProducingDataflowTaskImplementation;
import org.sandwood.compiler.dataflowGraph.variables.VariableType;
import org.sandwood.compiler.dataflowGraph.variables.scalarVariables.BooleanVariable;
import org.sandwood.compiler.dataflowGraph.variables.scalarVariables.DoubleVariable;
import org.sandwood.compiler.dataflowGraph.variables.scalarVariables.IntVariable;
import org.sandwood.compiler.dataflowGraph.variables.scalarVariables.NumberVariable;
import org.sandwood.compiler.exceptions.CompilerException;
import org.sandwood.compiler.srcTools.sourceToSource.Location;
import org.sandwood.compiler.traces.guards.BackTraceInfo;
import org.sandwood.compiler.trees.irTree.IRTree;
import org.sandwood.compiler.trees.irTree.IRTreeReturn;

public abstract class Pow<A extends NumberVariable<A>, B extends NumberVariable<B>, C extends NumberVariable<C>>
        extends NumberProducingDataflowTaskImplementation<C> {

    protected final A base;
    protected final B exp;

    Pow(VariableType.Type<C> outputType, Location location, A a, B b) {
        super(DFType.POW, outputType, location, a, b);
        this.base = a;
        this.exp = b;
    }

    @Override
    public String checkInversionError(int argPos) {
        return "Power calculations are not invertible in the general case as the sign of the value being raised to the power is lost.";
    }

    @Override
    public String getSandwoodExpression(boolean compressSandwoodCode) {
        return "(" + base.getExpression(compressSandwoodCode) + "^" + exp.getExpression(compressSandwoodCode) + ")";
    }

    @Override
    public boolean equivalent(DataflowTask<?> other) {
        if(this.getType() != other.getType())
            return false;
        Pow<?, ?, ?> dft = (Pow<?, ?, ?>) other;
        return (base.equivalent(dft.base) && exp.equivalent(dft.exp));
    }

    @Override
    public final IRTreeReturn<?> getInverseIRInternal(int argPos, IRTreeReturn<C> taskOutput,
            BackTraceInfo backTraceInfo, CompilationContext compilationCtx) {
        throw new CompilerException(checkInversionError(argPos));
    }

    @Override
    public DifferentialInfo getDifferentialInfo(DoubleVariable variable) {
        DifferentialInfo baseInfo = base.getDifferentialInfo(variable);
        DifferentialInfo expInfo = exp.getDifferentialInfo(variable);

        return new DifferentialInfo(baseInfo.isDifferentiable() && expInfo.isDifferentiable(),
                baseInfo.containsVariable() || expInfo.containsVariable());
    }

    private static class PowDD extends Pow<DoubleVariable, DoubleVariable, DoubleVariable> {
        PowDD(DoubleVariable a, DoubleVariable b, Location location) {
            super(VariableType.DoubleVariable, location, a, b);
        }

        @Override
        public IRTreeReturn<DoubleVariable> getForwardIRinternal(CompilationContext compilationCtx) {
            return IRTree.powDD(base.getForwardIR(compilationCtx), exp.getForwardIR(compilationCtx));
        }

        @Override
        public IRTreeReturn<DoubleVariable> getMax(CompilationContext compilationCtx) {
            if(!exp.isDeterministic())
                return null;

            IRTreeReturn<DoubleVariable> baseMin = base.getMin(compilationCtx);
            IRTreeReturn<DoubleVariable> baseMax = base.getMax(compilationCtx);
            if(baseMin == null || baseMax == null)
                return null;

            // Test if this is possibly 0 to a positive power, in which case the minimum happens at zero and is zero if
            // the power is even, and happens a the minimum value if it is an odd power.
            IRTreeReturn<BooleanVariable> guard = IRTree.and(IRTree.lessThanEqual(baseMin, IRTree.constant(0.0)),
                    IRTree.lessThanEqual(IRTree.constant(0.0), baseMax),
                    IRTree.lessThan(exp.getForwardIR(compilationCtx), IRTree.constant(0.0)));
            // min(0, minbase^exp)
            IRTreeReturn<DoubleVariable> trueCase = IRTree.constant(Double.POSITIVE_INFINITY);
            // min(minbase^b, maxbase^b)
            IRTreeReturn<DoubleVariable> falseCase = IRTree.max(IRTree.powDD(baseMin, exp.getForwardIR(compilationCtx)),
                    IRTree.powDD(baseMax, exp.getForwardIR(compilationCtx)));
            return IRTree.conditionalAssignment(guard, trueCase, falseCase);
        }

        @Override
        public IRTreeReturn<DoubleVariable> getMin(CompilationContext compilationCtx) {
            if(!exp.isDeterministic())
                return null;

            IRTreeReturn<DoubleVariable> baseMin = base.getMin(compilationCtx);
            IRTreeReturn<DoubleVariable> baseMax = base.getMax(compilationCtx);
            if(baseMin == null || baseMax == null)
                return null;

            // Test if this is possibly 0 to a positive power, in which case the minimum happens at zero and is zero if
            // the power is even, and happens a the minimum value if it is an odd power.
            IRTreeReturn<BooleanVariable> guard = IRTree.and(IRTree.lessThanEqual(baseMin, IRTree.constant(0.0)),
                    IRTree.lessThanEqual(IRTree.constant(0.0), baseMax),
                    IRTree.lessThanEqual(IRTree.constant(0.0), exp.getForwardIR(compilationCtx)));
            // min(0, minbase^exp)
            IRTreeReturn<DoubleVariable> trueCase = IRTree.min(IRTree.constant(0.0),
                    IRTree.powDD(baseMin, exp.getForwardIR(compilationCtx)));
            // min(minbase^b, maxbase^b)
            IRTreeReturn<DoubleVariable> falseCase = IRTree.min(IRTree.powDD(baseMin, exp.getForwardIR(compilationCtx)),
                    IRTree.powDD(baseMax, exp.getForwardIR(compilationCtx)));
            return IRTree.conditionalAssignment(guard, trueCase, falseCase);
        }

        @Override
        protected DoubleVariable getDifferentialInternal(DoubleVariable variable, CompilationContext compilationCtx) {
            DifferentialInfo baseInfo = base.getDifferentialInfo(variable);
            DifferentialInfo expInfo = exp.getDifferentialInfo(variable);

            if(baseInfo.containsVariable()) {
                if(expInfo.containsVariable()) {
                    DoubleVariable baseDiff = base.getDifferential(variable, compilationCtx);
                    DoubleVariable expDiff = exp.getDifferential(variable, compilationCtx);
                    DoubleVariable expExpr = expDiff.times(Log.log(base));
                    DoubleVariable baseExpr = baseDiff.times(exp).divide(base);
                    return output.times(expExpr.add(baseExpr));
                } else {
                    DoubleVariable baseDiff = base.getDifferential(variable, compilationCtx);
                    DoubleVariable basePower = pow(base, exp.subtract(IntVariable.intVariable(1)), getLocation());
                    return exp.times(baseDiff.times(basePower));
                }
            } else {
                if(expInfo.containsVariable()) {
                    DoubleVariable expDiff = exp.getDifferential(variable, compilationCtx);
                    return output.times(expDiff).times(Log.log(base));
                } else {
                    return DoubleVariable.doubleVariable(0.0);
                }
            }
        }
    }

    private static class PowDI extends Pow<DoubleVariable, IntVariable, DoubleVariable> {
        PowDI(DoubleVariable a, IntVariable b, Location location) {
            super(VariableType.DoubleVariable, location, a, b);
        }

        @Override
        public IRTreeReturn<DoubleVariable> getForwardIRinternal(CompilationContext compilationCtx) {
            return IRTree.powDI(base.getForwardIR(compilationCtx), exp.getForwardIR(compilationCtx));
        }

        @Override
        public IRTreeReturn<DoubleVariable> getMax(CompilationContext compilationCtx) {
            if(!exp.isDeterministic())
                return null;

            IRTreeReturn<DoubleVariable> baseMin = base.getMin(compilationCtx);
            IRTreeReturn<DoubleVariable> baseMax = base.getMax(compilationCtx);
            if(baseMin == null || baseMax == null)
                return null;

            // Test if this is possibly 0 to a positive power, in which case the minimum happens at zero and is zero if
            // the power is even, and happens a the minimum value if it is an odd power.
            IRTreeReturn<BooleanVariable> guard = IRTree.and(IRTree.lessThanEqual(baseMin, IRTree.constant(0.0)),
                    IRTree.lessThanEqual(IRTree.constant(0.0), baseMax),
                    IRTree.lessThan(exp.getForwardIR(compilationCtx), IRTree.constant(0)));
            // min(0, minbase^exp)
            IRTreeReturn<DoubleVariable> trueCase = IRTree.constant(Double.POSITIVE_INFINITY);
            // min(minbase^b, maxbase^b)
            IRTreeReturn<DoubleVariable> falseCase = IRTree.max(IRTree.powDI(baseMin, exp.getForwardIR(compilationCtx)),
                    IRTree.powDI(baseMax, exp.getForwardIR(compilationCtx)));
            return IRTree.conditionalAssignment(guard, trueCase, falseCase);
        }

        @Override
        public IRTreeReturn<DoubleVariable> getMin(CompilationContext compilationCtx) {
            if(!exp.isDeterministic())
                return null;

            IRTreeReturn<DoubleVariable> baseMin = base.getMin(compilationCtx);
            IRTreeReturn<DoubleVariable> baseMax = base.getMax(compilationCtx);
            IRTreeReturn<IntVariable> expMax = exp.getMax(compilationCtx);
            if(baseMin == null || baseMax == null || expMax == null)
                return null;

            // Test if this is possibly 0 to a positive power, in which case the minimum happens at zero and is zero if
            // the power is even, and happens a the minimum value if it is an odd power.
            IRTreeReturn<BooleanVariable> guard = IRTree.and(IRTree.lessThanEqual(baseMin, IRTree.constant(0.0)),
                    IRTree.lessThanEqual(IRTree.constant(0.0), baseMax), IRTree.lessThan(IRTree.constant(0), expMax));
            // min(0, minbase^exp)
            IRTreeReturn<DoubleVariable> trueCase = IRTree.min(IRTree.constant(0.0),
                    IRTree.powDI(baseMin, exp.getForwardIR(compilationCtx)));
            // min(minbase^b, maxbase^b)
            IRTreeReturn<DoubleVariable> falseCase = IRTree.min(IRTree.powDI(baseMin, exp.getForwardIR(compilationCtx)),
                    IRTree.powDI(baseMax, exp.getForwardIR(compilationCtx)));
            return IRTree.conditionalAssignment(guard, trueCase, falseCase);
        }

        @Override
        protected DoubleVariable getDifferentialInternal(DoubleVariable variable, CompilationContext compilationCtx) {
            DifferentialInfo baseInfo = base.getDifferentialInfo(variable);

            if(baseInfo.containsVariable()) {
                DoubleVariable baseDiff = base.getDifferential(variable, compilationCtx);
                DoubleVariable basePower = pow(base, exp.subtract(IntVariable.intVariable(1)), getLocation());
                return exp.times(baseDiff.times(basePower));
            } else {
                return DoubleVariable.doubleVariable(0.0);
            }
        }
    }

    private static class PowID extends Pow<IntVariable, DoubleVariable, DoubleVariable> {
        PowID(IntVariable a, DoubleVariable b, Location location) {
            super(VariableType.DoubleVariable, location, a, b);
        }

        @Override
        public IRTreeReturn<DoubleVariable> getForwardIRinternal(CompilationContext compilationCtx) {
            return IRTree.powID(base.getForwardIR(compilationCtx), exp.getForwardIR(compilationCtx));
        }

        @Override
        public IRTreeReturn<DoubleVariable> getMax(CompilationContext compilationCtx) {
            if(!exp.isDeterministic())
                return null;

            IRTreeReturn<IntVariable> baseMin = base.getMin(compilationCtx);
            IRTreeReturn<IntVariable> baseMax = base.getMax(compilationCtx);
            if(baseMin == null || baseMax == null)
                return null;

            // Test if this is possibly 0 to a positive power, in which case the minimum happens at zero and is zero if
            // the power is even, and happens a the minimum value if it is an odd power.
            IRTreeReturn<BooleanVariable> guard = IRTree.and(IRTree.lessThanEqual(baseMin, IRTree.constant(0)),
                    IRTree.lessThanEqual(IRTree.constant(0), baseMax),
                    IRTree.lessThan(exp.getForwardIR(compilationCtx), IRTree.constant(0.0)));
            // min(0, minbase^exp)
            IRTreeReturn<DoubleVariable> trueCase = IRTree.constant(Double.POSITIVE_INFINITY);
            // min(minbase^b, maxbase^b)
            IRTreeReturn<DoubleVariable> falseCase = IRTree.max(IRTree.powID(baseMin, exp.getForwardIR(compilationCtx)),
                    IRTree.powID(baseMax, exp.getForwardIR(compilationCtx)));
            return IRTree.conditionalAssignment(guard, trueCase, falseCase);
        }

        @Override
        public IRTreeReturn<DoubleVariable> getMin(CompilationContext compilationCtx) {
            if(!exp.isDeterministic())
                return null;

            IRTreeReturn<IntVariable> baseMin = base.getMin(compilationCtx);
            IRTreeReturn<IntVariable> baseMax = base.getMax(compilationCtx);
            IRTreeReturn<DoubleVariable> expMax = exp.getMax(compilationCtx);
            if(baseMin == null || baseMax == null || expMax == null)
                return null;

            // Test if this is possibly 0 to a positive power, in which case the minimum happens at zero and is zero if
            // the power is even, and happens a the minimum value if it is an odd power.
            IRTreeReturn<BooleanVariable> guard = IRTree.and(IRTree.lessThanEqual(baseMin, IRTree.constant(0)),
                    IRTree.lessThanEqual(IRTree.constant(0), baseMax), IRTree.lessThan(IRTree.constant(0.0), expMax));
            // min(0, minbase^exp)
            IRTreeReturn<DoubleVariable> trueCase = IRTree.min(IRTree.constant(0.0),
                    IRTree.powID(baseMin, exp.getForwardIR(compilationCtx)));
            // min(minbase^b, maxbase^b)
            IRTreeReturn<DoubleVariable> falseCase = IRTree.min(IRTree.powID(baseMin, exp.getForwardIR(compilationCtx)),
                    IRTree.powID(baseMax, exp.getForwardIR(compilationCtx)));
            return IRTree.conditionalAssignment(guard, trueCase, falseCase);
        }

        @Override
        protected DoubleVariable getDifferentialInternal(DoubleVariable variable, CompilationContext compilationCtx) {
            DifferentialInfo expInfo = exp.getDifferentialInfo(variable);

            if(expInfo.containsVariable()) {
                DoubleVariable expDiff = exp.getDifferential(variable, compilationCtx);
                return output.times(expDiff).times(Log.log(base));
            } else {
                return DoubleVariable.doubleVariable(0.0);
            }
        }
    }

    private static class PowII extends Pow<IntVariable, IntVariable, IntVariable> {
        PowII(IntVariable a, IntVariable b, Location location) {
            super(VariableType.IntVariable, location, a, b);
        }

        @Override
        public IRTreeReturn<IntVariable> getForwardIRinternal(CompilationContext compilationCtx) {
            return IRTree.powII(base.getForwardIR(compilationCtx), exp.getForwardIR(compilationCtx));
        }

        @Override
        public IRTreeReturn<IntVariable> getMax(CompilationContext compilationCtx) {
            if(!exp.isDeterministic())
                return null;

            IRTreeReturn<IntVariable> baseMin = base.getMin(compilationCtx);
            IRTreeReturn<IntVariable> baseMax = base.getMax(compilationCtx);
            if(baseMin == null || baseMax == null)
                return null;

            // Test if this is possibly 0 to a positive power, in which case the minimum happens at zero and is zero if
            // the power is even, and happens a the minimum value if it is an odd power.
            IRTreeReturn<BooleanVariable> guard = IRTree.and(IRTree.lessThanEqual(baseMin, IRTree.constant(0)),
                    IRTree.lessThanEqual(IRTree.constant(0), baseMax),
                    IRTree.lessThan(exp.getForwardIR(compilationCtx), IRTree.constant(0)));
            // min(0, minbase^exp)
            IRTreeReturn<IntVariable> trueCase = IRTree.constant(Integer.MAX_VALUE);
            // min(minbase^b, maxbase^b)
            IRTreeReturn<IntVariable> falseCase = IRTree.max(IRTree.powII(baseMin, exp.getForwardIR(compilationCtx)),
                    IRTree.powII(baseMax, exp.getForwardIR(compilationCtx)));
            return IRTree.conditionalAssignment(guard, trueCase, falseCase);
        }

        @Override
        public IRTreeReturn<IntVariable> getMin(CompilationContext compilationCtx) {
            if(!exp.isDeterministic())
                return null;

            IRTreeReturn<IntVariable> baseMin = base.getMin(compilationCtx);
            IRTreeReturn<IntVariable> baseMax = base.getMax(compilationCtx);
            IRTreeReturn<IntVariable> expMax = exp.getMax(compilationCtx);
            if(baseMin == null || baseMax == null || expMax == null)
                return null;

            // Test if this is possibly 0 to a positive power, in which case the minimum happens at zero and is zero if
            // the power is even, and happens a the minimum value if it is an odd power.
            IRTreeReturn<BooleanVariable> guard = IRTree.and(IRTree.lessThanEqual(baseMin, IRTree.constant(0)),
                    IRTree.lessThanEqual(IRTree.constant(0), baseMax), IRTree.lessThan(IRTree.constant(0), expMax));
            // min(0, minbase^exp)
            IRTreeReturn<IntVariable> trueCase = IRTree.min(IRTree.constant(0),
                    IRTree.powII(baseMin, exp.getForwardIR(compilationCtx)));
            // min(minbase^b, maxbase^b)
            IRTreeReturn<IntVariable> falseCase = IRTree.min(IRTree.powII(baseMin, exp.getForwardIR(compilationCtx)),
                    IRTree.powII(baseMax, exp.getForwardIR(compilationCtx)));
            return IRTree.conditionalAssignment(guard, trueCase, falseCase);
        }

        @Override
        protected DoubleVariable getDifferentialInternal(DoubleVariable variable, CompilationContext compilationCtx) {
            return DoubleVariable.doubleVariable(0.0);
        }
    }

    /* Factory methods for construction */
    public static DoubleVariable pow(DoubleVariable a, DoubleVariable b, Location location) {
        return DoubleVariable.doubleVariable(new PowDD(a, b, location));
    }

    public static DoubleVariable pow(DoubleVariable a, IntVariable b, Location location) {
        return DoubleVariable.doubleVariable(new PowDI(a, b, location));
    }

    public static DoubleVariable pow(IntVariable a, DoubleVariable b, Location location) {
        return DoubleVariable.doubleVariable(new PowID(a, b, location));
    }

    public static IntVariable pow(IntVariable a, IntVariable b, Location location) {
        return IntVariable.intVariable(new PowII(a, b, location));
    }
}