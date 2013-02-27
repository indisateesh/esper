/*
 * *************************************************************************************
 *  Copyright (C) 2008 EsperTech, Inc. All rights reserved.                            *
 *  http://esper.codehaus.org                                                          *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

package com.espertech.esper.regression.client;

import com.espertech.esper.epl.agg.access.AggregationState;
import com.espertech.esper.epl.expression.ExprEvaluator;
import com.espertech.esper.plugin.PlugInAggregationMultiFunctionStateContext;
import com.espertech.esper.plugin.PlugInAggregationMultiFunctionStateFactory;

public class SupportAggMFStateArrayCollScalarFactory implements PlugInAggregationMultiFunctionStateFactory
{
    private final ExprEvaluator evaluator;

    public SupportAggMFStateArrayCollScalarFactory(ExprEvaluator evaluator) {
        this.evaluator = evaluator;
    }

    public AggregationState makeAggregationState(PlugInAggregationMultiFunctionStateContext stateContext) {
        return new SupportAggMFStateArrayCollScalar(this);
    }

    public ExprEvaluator getEvaluator() {
        return evaluator;
    }
}
