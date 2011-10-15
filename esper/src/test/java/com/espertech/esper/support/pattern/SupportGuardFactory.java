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

package com.espertech.esper.support.pattern;

import com.espertech.esper.epl.expression.ExprNode;
import com.espertech.esper.pattern.EvalStateNodeNumber;
import com.espertech.esper.pattern.MatchedEventConvertor;
import com.espertech.esper.pattern.MatchedEventMap;
import com.espertech.esper.pattern.PatternAgentInstanceContext;
import com.espertech.esper.pattern.guard.Guard;
import com.espertech.esper.pattern.guard.GuardFactory;
import com.espertech.esper.pattern.guard.GuardParameterException;
import com.espertech.esper.pattern.guard.Quitable;

import java.util.List;

public class SupportGuardFactory implements GuardFactory
{
    public void setGuardParameters(List<ExprNode> guardParameters, MatchedEventConvertor convertor) throws GuardParameterException
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Guard makeGuard(PatternAgentInstanceContext context, MatchedEventMap beginState, Quitable quitable, EvalStateNodeNumber stateNodeId, Object guardState) {
        return null;
    }
}
