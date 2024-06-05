/*
 * Sandwood
 *
 * Copyright (c) 2019-2026, Oracle and/or its affiliates
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl/
 */

package org.sandwood.compiler.tests.autoDifferentiation;

import org.sandwood.compiler.dataflowGraph.variables.scalarVariables.BooleanVariable;
import org.sandwood.compiler.dataflowGraph.variables.scalarVariables.DoubleVariable;
import org.sandwood.compiler.dataflowGraph.variables.scalarVariables.IntVariable;

public interface InputGenerator {
    DoubleVariable getDouble(String alias);

    DoubleVariable getDouble(String alias, double value);

    IntVariable getInt(String alias);

    IntVariable getInt(String alias, int value);

    BooleanVariable getBoolean(String alias);

    void cleanState();
}
