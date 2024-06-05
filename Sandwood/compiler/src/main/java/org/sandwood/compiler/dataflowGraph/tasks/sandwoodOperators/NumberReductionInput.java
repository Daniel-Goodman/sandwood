/*
 * Sandwood
 *
 * Copyright (c) 2019-2026, Oracle and/or its affiliates
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl/
 */

package org.sandwood.compiler.dataflowGraph.tasks.sandwoodOperators;

import org.sandwood.compiler.compilation.CompilationContext;
import org.sandwood.compiler.dataflowGraph.autoDiff.DifferentialInfo;
import org.sandwood.compiler.dataflowGraph.tasks.NumberProducingDataflowTask;
import org.sandwood.compiler.dataflowGraph.variables.Variable;
import org.sandwood.compiler.dataflowGraph.variables.arrayVariable.ArrayVariable;
import org.sandwood.compiler.dataflowGraph.variables.scalarVariables.DoubleVariable;
import org.sandwood.compiler.dataflowGraph.variables.scalarVariables.IntVariable;
import org.sandwood.compiler.dataflowGraph.variables.scalarVariables.NumberVariable;
import org.sandwood.compiler.exceptions.CompilerException;
import org.sandwood.compiler.srcTools.sourceToSource.Location;
import org.sandwood.compiler.trees.irTree.IRTreeReturn;

public class NumberReductionInput<A extends NumberVariable<A>> extends ReductionInput<A>
        implements NumberProducingDataflowTask<A> {

    public NumberReductionInput(IntVariable start, IntVariable end, Variable<A> emptyValue, ArrayVariable<A> a,
            boolean first, Location location) {
        super(start, end, emptyValue, a, first, location);
    }

    @Override
    public IRTreeReturn<A> getMax(CompilationContext compilationCtx) {
        throw new CompilerException("Max is not currently supported on reductions");
    }

    @Override
    public IRTreeReturn<A> getMin(CompilationContext compilationCtx) {
        throw new CompilerException("Min is not currently supported on reductions");
    }
    
    @Override
    public DoubleVariable getDifferential(DoubleVariable variable, CompilationContext compilationCtx) {
    	return Variable.doubleVariable(0);
    }


	@Override
	public boolean isDifferentiable(DoubleVariable variable) {
		return getDifferentialInfo(variable).isDifferentiable();
	}
	
	@Override
	public DifferentialInfo getDifferentialInfo(DoubleVariable variable) {
	    boolean contains =  containsVariable(variable);
		return new DifferentialInfo(!contains, contains);
	}
}
