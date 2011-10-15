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

import com.espertech.esper.core.service.StatementContext;
import com.espertech.esper.pattern.PatternAgentInstanceContext;
import com.espertech.esper.pattern.PatternContext;
import com.espertech.esper.schedule.SchedulingService;
import com.espertech.esper.support.view.SupportStatementContextFactory;

public class SupportPatternContextFactory
{
    public static PatternAgentInstanceContext makePatternAgentInstanceContext() {
        return makePatternAgentInstanceContext(null);
    }

    public static PatternAgentInstanceContext makePatternAgentInstanceContext(SchedulingService scheduleService) {
        StatementContext stmtContext;
        if (scheduleService == null) {
            stmtContext = SupportStatementContextFactory.makeContext();
        }
        else {
            stmtContext = SupportStatementContextFactory.makeContext(scheduleService);
        }
        PatternContext context = new PatternContext(stmtContext, 1);
        return new PatternAgentInstanceContext(context, SupportStatementContextFactory.makeAgentInstanceContext(), false);
    }

    public static PatternContext makeContext()
    {
        StatementContext stmtContext = SupportStatementContextFactory.makeContext();
        return new PatternContext(stmtContext, 1);
    }
}
