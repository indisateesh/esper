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
import com.espertech.esper.client.EPOnDemandQueryResult;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.support.bean.SupportMarketDataIDBean;
import com.espertech.esper.support.bean.SupportSimpleBeanOne;
import com.espertech.esper.support.bean.SupportSimpleBeanTwo;
import com.espertech.esper.support.client.SupportConfigFactory;
import junit.framework.TestCase;

public class TestPerfFAFQueryJoin extends TestCase
{
    private EPServiceProvider epService;

    public void setUp()
    {
        Configuration config = SupportConfigFactory.getConfiguration();
        config.getEngineDefaults().getLogging().setEnableQueryPlan(false);
        config.addEventType("SSB1", SupportSimpleBeanOne.class);
        config.addEventType("SSB2", SupportSimpleBeanTwo.class);
        epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();
    }

    public void testPerfFAFJoin()
    {
        epService.getEPAdministrator().createEPL("create window W1.std:unique(s1) as SSB1");
        epService.getEPAdministrator().createEPL("insert into W1 select * from SSB1");

        epService.getEPAdministrator().createEPL("create window W2.std:unique(s2) as SSB2");
        epService.getEPAdministrator().createEPL("insert into W2 select * from SSB2");

        for (int i = 0; i < 1000; i++) {
            epService.getEPRuntime().sendEvent(new SupportSimpleBeanOne("A" + i, 0, 0, 0));
            epService.getEPRuntime().sendEvent(new SupportSimpleBeanTwo("A" + i, 0, 0, 0));
        }

        long start = System.currentTimeMillis();
        for (int i = 0; i < 100; i++)
        {
            EPOnDemandQueryResult result = epService.getEPRuntime().executeQuery("select * from W1 as w1, W2 as w2 " +
                    "where w1.s1 = w2.s2");
            assertEquals(1000, result.getArray().length);
        }
        long end = System.currentTimeMillis();
        long delta = end - start;
        System.out.println("Delta=" + delta);
        assertTrue("Delta=" + delta, delta < 1000);
    }
}
