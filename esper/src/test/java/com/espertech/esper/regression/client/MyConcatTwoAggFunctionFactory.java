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

import com.espertech.esper.client.hook.AggregationFunctionFactory;
import com.espertech.esper.epl.agg.aggregator.AggregationMethod;
import com.espertech.esper.epl.agg.service.AggregationValidationContext;

import java.io.Serializable;

public class MyConcatTwoAggFunctionFactory implements AggregationFunctionFactory, Serializable
{
    @Override
    public void validate(AggregationValidationContext validationContext)
    {
        // No need to check the expression node type
    }

    public Class getValueType()
    {
        return String.class;
    }


    public void setFunctionName(String functionName) {
        // don't care
    }

    public AggregationMethod newAggregator() {
        return new MyConcatTwoAggMethod();
    }
}
