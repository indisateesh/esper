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

package com.espertech.esper.regression.pattern;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.support.util.SupportUpdateListener;
import com.espertech.esper.support.bean.SupportBean;
import com.espertech.esper.support.client.SupportConfigFactory;
import com.espertech.esper.core.service.EPServiceProviderSPI;
import com.espertech.esper.core.service.StatementType;
import com.espertech.esper.core.service.EPStatementSPI;
import junit.framework.TestCase;

import java.util.Set;

public class TestPatternStartStop extends TestCase
{
    private EPServiceProvider epService;
    private SupportUpdateListener listener;

    public void setUp()
    {
        listener = new SupportUpdateListener();
        epService = EPServiceProviderManager.getDefaultProvider(SupportConfigFactory.getConfiguration());
        epService.initialize();
    }

    protected void tearDown() throws Exception {
        listener = null;
    }

    public void testStartStop()
    {
        String viewExpr = "every tag=" + SupportBean.class.getName();
        EPStatement patternStmt = epService.getEPAdministrator().createPattern(viewExpr, "MyPattern");
        assertEquals(StatementType.PATTERN, ((EPStatementSPI) patternStmt).getStatementMetadata().getStatementType());

        // Pattern started when created
        assertFalse(patternStmt.iterator().hasNext());

        // Stop pattern
        patternStmt.stop();
        sendEvent();
        assertNull(patternStmt.iterator());

        // Start pattern
        patternStmt.start();
        assertFalse(patternStmt.iterator().hasNext());

        // Send event
        SupportBean event = sendEvent();
        assertSame(event, patternStmt.iterator().next().get("tag"));

        // Stop pattern
        patternStmt.stop();
        assertNull(patternStmt.iterator());

        // Start again, iterator is zero
        patternStmt.start();
        assertFalse(patternStmt.iterator().hasNext());

        // assert statement-eventtype reference info
        EPServiceProviderSPI spi = (EPServiceProviderSPI) epService;
        assertTrue(spi.getStatementEventTypeRef().isInUse(SupportBean.class.getName()));
        Set<String> stmtNames = spi.getStatementEventTypeRef().getStatementNamesForType(SupportBean.class.getName());
        assertTrue(stmtNames.contains("MyPattern"));

        patternStmt.destroy();

        assertFalse(spi.getStatementEventTypeRef().isInUse(SupportBean.class.getName()));
        stmtNames = spi.getStatementEventTypeRef().getStatementNamesForType(SupportBean.class.getName());
        assertFalse(stmtNames.contains("MyPattern"));
    }

    public void testAddRemoveListener()
    {
        String viewExpr = "every tag=" + SupportBean.class.getName();
        EPStatement patternStmt = epService.getEPAdministrator().createPattern(viewExpr, "MyPattern");
        assertEquals(StatementType.PATTERN, ((EPStatementSPI) patternStmt).getStatementMetadata().getStatementType());

        // Pattern started when created

        // Add listener
        patternStmt.addListener(listener);
        assertNull(listener.getLastNewData());
        assertFalse(patternStmt.iterator().hasNext());

        // Send event
        SupportBean event = sendEvent();
        assertEquals(event, listener.getAndResetLastNewData()[0].get("tag"));
        assertSame(event, patternStmt.iterator().next().get("tag"));

        // Remove listener
        patternStmt.removeListener(listener);
        event = sendEvent();
        assertSame(event, patternStmt.iterator().next().get("tag"));
        assertNull(listener.getLastNewData());

        // Add listener back
        patternStmt.addListener(listener);
        event = sendEvent();
        assertSame(event, patternStmt.iterator().next().get("tag"));
        assertEquals(event, listener.getAndResetLastNewData()[0].get("tag"));
    }

    private SupportBean sendEvent()
    {
        SupportBean event = new SupportBean();
        epService.getEPRuntime().sendEvent(event);
        return event;
    }
}
