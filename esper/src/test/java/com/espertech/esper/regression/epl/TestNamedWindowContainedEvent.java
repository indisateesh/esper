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

package com.espertech.esper.regression.epl;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EPStatementException;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.core.service.EPServiceProviderSPI;
import com.espertech.esper.core.service.EPStatementSPI;
import com.espertech.esper.core.service.StatementType;
import com.espertech.esper.epl.named.NamedWindowProcessor;
import com.espertech.esper.support.bean.*;
import com.espertech.esper.support.bean.bookexample.OrderBean;
import com.espertech.esper.support.client.SupportConfigFactory;
import com.espertech.esper.util.EventRepresentationEnum;
import junit.framework.TestCase;

import java.util.LinkedList;
import java.util.List;

public class TestNamedWindowContainedEvent extends TestCase
{
    private EPServiceProviderSPI epService;

    public void setUp()
    {
        Configuration config = SupportConfigFactory.getConfiguration();
        config.getEngineDefaults().getLogging().setEnableQueryPlan(true);
        epService = (EPServiceProviderSPI) EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);
        epService.getEPAdministrator().getConfiguration().addEventType(OrderBean.class);
    }

    public void testInvalid() {
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean_A", SupportBean_A.class);

        epService.getEPAdministrator().createEPL("create window OrderWindow.win:time(30) as OrderBean");

        try {
            String epl = "select * from SupportBean unidirectional, OrderWindow[books]";
            epService.getEPAdministrator().createEPL(epl);
            fail();
        }
        catch (EPStatementException ex) {
            assertEquals("Error starting statement: Failed to validate named window use in join, contained-event is only allowed for named windows when marked as unidirectional [select * from SupportBean unidirectional, OrderWindow[books]]", ex.getMessage());
        }

        try {
            String epl = "select *, (select bookId from OrderWindow[books] where sb.theString = bookId) " +
                    "from SupportBean sb";
            epService.getEPAdministrator().createEPL(epl);
            fail();
        }
        catch (EPStatementException ex) {
            assertEquals("Error starting statement: Failed to validate named window use in subquery, contained-event is only allowed for named windows when not correlated [select *, (select bookId from OrderWindow[books] where sb.theString = bookId) from SupportBean sb]", ex.getMessage());
        }
    }
}
