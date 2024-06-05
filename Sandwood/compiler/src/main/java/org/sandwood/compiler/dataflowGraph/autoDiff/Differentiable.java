/*
 * Sandwood
 *
 * Copyright (c) 2019-2026, Oracle and/or its affiliates
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl/
 */

package org.sandwood.compiler.dataflowGraph.autoDiff;

import org.sandwood.compiler.compilation.CompilationContext;
import org.sandwood.compiler.dataflowGraph.variables.scalarVariables.DoubleVariable;

public interface Differentiable {
    DoubleVariable getDifferential(DoubleVariable variable, CompilationContext compilationCtx);

    boolean isDifferentiable(DoubleVariable variable);

    DifferentialInfo getDifferentialInfo(DoubleVariable variable);
}
