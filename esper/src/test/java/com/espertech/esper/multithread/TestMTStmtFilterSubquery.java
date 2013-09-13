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

package com.espertech.esper.multithread;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.support.bean.SupportBean;
import com.espertech.esper.support.bean.SupportBean_S0;
import com.espertech.esper.support.client.SupportConfigFactory;
import junit.framework.TestCase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Test for multithread-safety for a simple aggregation case using count(*).
 */
public class TestMTStmtFilterSubquery extends TestCase
{
    private static final Log log = LogFactory.getLog(TestMTStmtFilterSubquery.class);

    private EPServiceProvider engine;

    public void setUp()
    {
        Configuration config = SupportConfigFactory.getConfiguration();
        engine = EPServiceProviderManager.getProvider(this.getClass().getName(), config);
        engine.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);
        engine.getEPAdministrator().getConfiguration().addEventType(SupportBean_S0.class);
    }

    public void tearDown()
    {
        engine.destroy();
    }

    public void testCount() throws Exception
    {
        tryNamedWindowFilterSubquery();
        tryStreamFilterSubquery();
    }

    public void tryNamedWindowFilterSubquery() throws Exception
    {
        engine.getEPAdministrator().createEPL("create window MyWindow.win:keepall() as SupportBean_S0");
        engine.getEPAdministrator().createEPL("insert into MyWindow select * from SupportBean_S0");

        String epl = "select * from pattern[SupportBean_S0 -> SupportBean(not exists (select * from MyWindow mw where mw.p00 = 'E'))]";
        engine.getEPAdministrator().createEPL(epl);
        engine.getEPRuntime().sendEvent(new SupportBean_S0(1));

        Thread insertThread = new Thread(new InsertRunnable(engine, 1000));
        Thread filterThread = new Thread(new FilterRunnable(engine, 1000));

        log.info("Starting threads");
        insertThread.start();
        filterThread.start();

        log.info("Waiting for join");
        insertThread.join();
        filterThread.join();

        engine.getEPAdministrator().destroyAllStatements();
    }

    public void tryStreamFilterSubquery() throws Exception
    {
        String epl = "select * from SupportBean(not exists (select * from SupportBean_S0.win:keepall() mw where mw.p00 = 'E'))";
        engine.getEPAdministrator().createEPL(epl);

        Thread insertThread = new Thread(new InsertRunnable(engine, 1000));
        Thread filterThread = new Thread(new FilterRunnable(engine, 1000));

        log.info("Starting threads");
        insertThread.start();
        filterThread.start();

        log.info("Waiting for join");
        insertThread.join();
        filterThread.join();

        engine.getEPAdministrator().destroyAllStatements();
    }

    public static class InsertRunnable implements Runnable {
        private final EPServiceProvider engine;
        private final int numInserts;

        public InsertRunnable(EPServiceProvider engine, int numInserts) {
            this.engine = engine;
            this.numInserts = numInserts;
        }

        public void run() {
            log.info("Starting insert thread");
            for (int i = 0; i < numInserts; i++) {
                engine.getEPRuntime().sendEvent(new SupportBean_S0(i, "E"));
            }
            log.info("Completed insert thread, " + numInserts + " inserted");
        }
    }

    public static class FilterRunnable implements Runnable {
        private final EPServiceProvider engine;
        private final int numEvents;

        public FilterRunnable(EPServiceProvider engine, int numEvents) {
            this.engine = engine;
            this.numEvents = numEvents;
        }

        public void run() {
            log.info("Starting filter thread");
            for (int i = 0; i < numEvents; i++) {
                engine.getEPRuntime().sendEvent(new SupportBean("G" + i, i));
            }
            log.info("Completed filter thread, " + numEvents + " completed");
        }
    }
}
