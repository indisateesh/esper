package com.espertech.esper.regression;

import junit.framework.TestCase;
import com.espertech.esper.client.*;
import com.espertech.esper.event.EventBean;
import com.espertech.esper.support.bean.SupportBean;
import com.espertech.esper.support.util.ArrayAssertionUtil;
import com.espertech.esper.support.util.NoActionUpdateListener;
import com.espertech.esper.support.client.SupportConfigFactory;
import com.espertech.esper.collection.ArrayDequeJDK6Backport;

import java.util.List;
import java.util.ArrayList;
import java.util.LinkedList;

// TODO: remove me
public class TestPerfCompareDispatch extends TestCase
{
    private EPServiceProvider epService;

    public void setUp()
    {
        Configuration config = SupportConfigFactory.getConfiguration();
        config.getEngineDefaults().getThreading().setInternalTimerEnabled(false);
        config.addEventTypeAlias("SupportBean", SupportBean.class.getName());
        epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();
    }

    public void testPerfRemoveStream()
    {
        String text = "select count(*) from SupportBean.win:length(10)";
        EPStatement stmt = epService.getEPAdministrator().createEPL(text);
        NoActionUpdateListener listener = new NoActionUpdateListener();
        stmt.addListener(listener);

        long start = System.currentTimeMillis();
        for (int i = 0; i < 500000; i++)
        {
            epService.getEPRuntime().sendEvent(new SupportBean("E1", 1000 + i));
        }
        long end = System.currentTimeMillis();
        System.out.println("delta=" + (end - start));
        System.out.println(stmt.iterator().next().get("count(*)"));
    }

    public void testPerfToArray()
    {
        LinkedList<String> col = new LinkedList<String>();
        col.add("abc");
        col.add("def");
        col.add("erg");
        col.add("yyy");

        long start = System.currentTimeMillis();
        for (int i = 0; i < 1000000; i++)
        {
            String[] str = col.toArray(new String[col.size()]);
        }
        long end = System.currentTimeMillis();
        System.out.println("delta=" + (end - start));
    }

    public void testPerfDequeLinkedList()
    {
        long start = System.currentTimeMillis();
        for (int i = 0; i < 10000000; i++)
        {
            LinkedList<Object> col = new LinkedList<Object>();
            col.add(new Object());
            col.add(new Object());

            for (Object o : col)
            {
                if (o == "")
                {
                    System.out.println("a");
                }
            }
        }
        long end = System.currentTimeMillis();
        System.out.println("delta=" + (end - start));
    }

    public void testPerfDequeArray()
    {
        long start = System.currentTimeMillis();
        for (int i = 0; i < 10000000; i++)
        {
            ArrayDequeJDK6Backport<Object> col = new ArrayDequeJDK6Backport<Object>();
            col.add(new Object());
            col.add(new Object());

            for (Object o : col)
            {
                if (o == "")
                {
                    System.out.println("a");
                }
            }
        }
        long end = System.currentTimeMillis();
        System.out.println("delta=" + (end - start));
    }

    public void testPerfArrayList()
    {
        long start = System.currentTimeMillis();
        for (int i = 0; i < 10000000; i++)
        {
            ArrayList<Object> col = new ArrayList<Object>();
            col.add(new Object());
            col.add(new Object());

            for (Object o : col)
            {
                if (o == "")
                {
                    System.out.println("a");
                }
            }
        }
        long end = System.currentTimeMillis();
        System.out.println("delta=" + (end - start));
    }

    public void testPerformanceSyntheticUndelivered()
    {
        final int NUM_LOOP = 100000;
        epService.getEPAdministrator().createEPL("select string, intPrimitive from SupportBean(intPrimitive > 10)");

        long start = System.currentTimeMillis();
        for (int i = 0; i < NUM_LOOP; i++)
        {
            epService.getEPRuntime().sendEvent(new SupportBean("E1", 1000 + i));
        }
        long end = System.currentTimeMillis();
        System.out.println("delta=" + (end - start));
    }

    public void testPerformanceSyntheticDelivered()
    {
        final int NUM_LOOP = 100000;
        EPStatement stmt = epService.getEPAdministrator().createEPL("select string, intPrimitive from SupportBean(intPrimitive > 10)");
        final List<Object[]> results = new ArrayList<Object[]>();

        UpdateListener listener = new UpdateListener() {
            public void update(EventBean[] newEvents, EventBean[] oldEvents)
            {
                String string = (String) newEvents[0].get("string");
                int val = (Integer) newEvents[0].get("intPrimitive");
                results.add(new Object[] {string, val});
            }
        };
        stmt.addListener(listener);

        long start = System.currentTimeMillis();
        for (int i = 0; i < NUM_LOOP; i++)
        {
            epService.getEPRuntime().sendEvent(new SupportBean("E1", 1000 + i));
        }
        long end = System.currentTimeMillis();

        assertEquals(NUM_LOOP, results.size());
        for (int i = 0; i < NUM_LOOP; i++)
        {
            ArrayAssertionUtil.assertEqualsAnyOrder(results.get(i), new Object[] {"E1", 1000 + i});
        }
        System.out.println("delta=" + (end - start));
    }
}
