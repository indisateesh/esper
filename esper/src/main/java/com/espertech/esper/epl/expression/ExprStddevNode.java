/**************************************************************************************
 * Copyright (C) 2008 EsperTech, Inc. All rights reserved.                            *
 * http://esper.codehaus.org                                                          *
 * http://www.espertech.com                                                           *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the GPL license       *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package com.espertech.esper.epl.expression;

import com.espertech.esper.epl.agg.service.AggregationMethodFactory;

/**
 * Represents the stddev(...) aggregate function is an expression tree.
 */
public class ExprStddevNode extends ExprAggregateNodeBase
{
    private static final long serialVersionUID = 4732757426203628783L;

    private final boolean hasFilter;

    /**
     * Ctor.
     * @param distinct - flag indicating unique or non-unique value aggregation
     */
    public ExprStddevNode(boolean distinct, boolean hasFilter)
    {
        super(distinct);
        this.hasFilter = hasFilter;
    }

    public AggregationMethodFactory validateAggregationChild(ExprValidationContext validationContext) throws ExprValidationException
    {
        Class childType = super.validateNumericChildAllowFilter(validationContext.getStreamTypeService(), hasFilter);
        return new ExprStddevNodeFactory(super.isDistinct, childType, hasFilter);
    }

    public final boolean equalsNodeAggregate(ExprAggregateNode node)
    {
        if (!(node instanceof ExprStddevNode))
        {
            return false;
        }

        return true;
    }

    protected String getAggregationFunctionName()
    {
        return "stddev";
    }
}
