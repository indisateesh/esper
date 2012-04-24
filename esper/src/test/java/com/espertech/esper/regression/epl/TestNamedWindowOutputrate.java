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

import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.support.bean.SupportBean;
import com.espertech.esper.support.client.SupportConfigFactory;
import junit.framework.TestCase;

public class TestNamedWindowOutputrate extends TestCase
{
    private EPServiceProvider epService;
    private SupportUpdateListener listener;

    public void setUp()
    {
        Configuration config = SupportConfigFactory.getConfiguration();
        epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();
        listener = new SupportUpdateListener();
    }

    protected void tearDown() throws Exception {
        listener = null;
    }

    public void testOutputSnapshot() {
        epService.getEPAdministrator().createEPL("create schema SupportBean as " + SupportBean.class.getName());

        epService.getEPAdministrator().createEPL("create window MyWindowOne.win:keepall() as (theString string, intv int)");
        epService.getEPAdministrator().createEPL("insert into MyWindowOne select theString, intPrimitive as intv from SupportBean");

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(0));

        String[] fields = new String[] {"theString","c"};
        EPStatement stmtSelect = epService.getEPAdministrator().createEPL("select irstream theString, count(*) as c from MyWindowOne group by theString output snapshot every 1 second");
        stmtSelect.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("A", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("A", 2));
        epService.getEPRuntime().sendEvent(new SupportBean("B", 4));

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(1000));

        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{"A", 2L}, {"B", 1L}});

        epService.getEPRuntime().sendEvent(new SupportBean("B", 5));
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(2000));

        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{"A", 2L}, {"B", 2L}});

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(3000));

        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{"A", 2L}, {"B", 2L}});

        epService.getEPRuntime().sendEvent(new SupportBean("A", 5));
        epService.getEPRuntime().sendEvent(new SupportBean("C", 1));
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(4000));

        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{"A", 3L}, {"B", 2L}, {"C", 1L}});
    }
}
