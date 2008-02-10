package com.espertech.esper.regression.eql;

import junit.framework.TestCase;
import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.soda.*;
import com.espertech.esper.support.bean.SupportBean;
import com.espertech.esper.support.bean.SupportMarketDataBean;
import com.espertech.esper.support.bean.SupportBean_B;
import com.espertech.esper.support.client.SupportConfigFactory;
import com.espertech.esper.support.util.ArrayAssertionUtil;
import com.espertech.esper.support.util.SupportUpdateListener;

public class TestNamedWindowOM extends TestCase
{
    private EPServiceProvider epService;
    private SupportUpdateListener listenerWindow;
    private SupportUpdateListener listenerStmtOne;
    private SupportUpdateListener listenerOnSelect;

    public void setUp()
    {
        Configuration config = SupportConfigFactory.getConfiguration();
        config.getEngineDefaults().getThreading().setInternalTimerEnabled(false);

        epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();
        listenerWindow = new SupportUpdateListener();
        listenerStmtOne = new SupportUpdateListener();
        listenerOnSelect = new SupportUpdateListener();
    }

    public void testCompile()
    {
        String[] fields = new String[] {"key", "value"};
        String stmtTextCreate = "create window MyWindow.win:keepall() as select string as key, longBoxed as value from " + SupportBean.class.getName();
        EPStatementObjectModel modelCreate = epService.getEPAdministrator().compileEQL(stmtTextCreate);
        EPStatement stmtCreate = epService.getEPAdministrator().create(modelCreate);
        stmtCreate.addListener(listenerWindow);
        assertEquals("create window MyWindow.win:keepall() as select string as key, longBoxed as value from com.espertech.esper.support.bean.SupportBean", modelCreate.toEQL());

        String stmtTextOnSelect = "on " + SupportBean_B.class.getName() + " select mywin.* from MyWindow as mywin";
        EPStatementObjectModel modelOnSelect = epService.getEPAdministrator().compileEQL(stmtTextOnSelect);
        EPStatement stmtOnSelect = epService.getEPAdministrator().create(modelOnSelect);
        stmtOnSelect.addListener(listenerOnSelect);

        String stmtTextInsert = "insert into MyWindow select string as key, longBoxed as value from " + SupportBean.class.getName();
        EPStatementObjectModel modelInsert = epService.getEPAdministrator().compileEQL(stmtTextInsert);
        EPStatement stmtInsert = epService.getEPAdministrator().create(modelInsert);

        String stmtTextSelectOne = "select irstream key, value*2 as value from MyWindow(key != null)";
        EPStatementObjectModel modelSelect = epService.getEPAdministrator().compileEQL(stmtTextSelectOne);
        EPStatement stmtSelectOne = epService.getEPAdministrator().create(modelSelect);
        stmtSelectOne.addListener(listenerStmtOne);
        assertEquals("select irstream key, (value * 2) as value from MyWindow((key != null))", modelSelect.toEQL());

        // send events
        sendSupportBean("E1", 10L);
        ArrayAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[] {"E1", 20L});
        ArrayAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[] {"E1", 10L});

        sendSupportBean("E2", 20L);
        ArrayAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[] {"E2", 40L});
        ArrayAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[] {"E2", 20L});

        // create delete stmt
        String stmtTextDelete = "on " + SupportMarketDataBean.class.getName() + " as s0 delete from MyWindow as s1 where s0.symbol = s1.key";
        EPStatementObjectModel modelDelete = epService.getEPAdministrator().compileEQL(stmtTextDelete);
        epService.getEPAdministrator().create(modelDelete);
        assertEquals("on com.espertech.esper.support.bean.SupportMarketDataBean as s0 delete from MyWindow as s1 where (s0.symbol = s1.key)", modelDelete.toEQL());

        // send delete event
        sendMarketBean("E1");
        ArrayAssertionUtil.assertProps(listenerStmtOne.assertOneGetOldAndReset(), fields, new Object[] {"E1", 20L});
        ArrayAssertionUtil.assertProps(listenerWindow.assertOneGetOldAndReset(), fields, new Object[] {"E1", 10L});

        // send delete event again, none deleted now
        sendMarketBean("E1");
        assertFalse(listenerStmtOne.isInvoked());
        assertFalse(listenerWindow.isInvoked());

        // send delete event
        sendMarketBean("E2");
        ArrayAssertionUtil.assertProps(listenerStmtOne.assertOneGetOldAndReset(), fields, new Object[] {"E2", 40L});
        ArrayAssertionUtil.assertProps(listenerWindow.assertOneGetOldAndReset(), fields, new Object[] {"E2", 20L});

        // trigger on-select on empty window
        assertFalse(listenerOnSelect.isInvoked());
        epService.getEPRuntime().sendEvent(new SupportBean_B("B1"));
        assertFalse(listenerOnSelect.isInvoked());

        sendSupportBean("E3", 30L);
        ArrayAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[] {"E3", 60L});
        ArrayAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[] {"E3", 30L});

        // trigger on-select on the filled window
        epService.getEPRuntime().sendEvent(new SupportBean_B("B2"));
        ArrayAssertionUtil.assertProps(listenerOnSelect.assertOneGetNewAndReset(), fields, new Object[] {"E3", 30L});

        stmtSelectOne.destroy();
        stmtInsert.destroy();
        stmtCreate.destroy();
    }

    public void testOM()
    {
        String[] fields = new String[] {"key", "value"};

        // create window object model
        EPStatementObjectModel model = new EPStatementObjectModel();
        model.setCreateWindow(CreateWindowClause.create("MyWindow").addView("win", "keepall"));
        model.setSelectClause(SelectClause.create()
                .addWithAlias("string", "key")
                .addWithAlias("longBoxed", "value"));
        model.setFromClause(FromClause.create(FilterStream.create(SupportBean.class.getName())));

        EPStatement stmtCreate = epService.getEPAdministrator().create(model);
        stmtCreate.addListener(listenerWindow);

        String stmtTextCreate = "create window MyWindow.win:keepall() as select string as key, longBoxed as value from " + SupportBean.class.getName();
        assertEquals(stmtTextCreate, model.toEQL());

        String stmtTextInsert = "insert into MyWindow select string as key, longBoxed as value from " + SupportBean.class.getName();
        EPStatementObjectModel modelInsert = epService.getEPAdministrator().compileEQL(stmtTextInsert);
        EPStatement stmtInsert = epService.getEPAdministrator().create(modelInsert);

        // Consumer statement object model
        model = new EPStatementObjectModel();
        Expression multi = Expressions.multiply(Expressions.property("value"), Expressions.constant(2));
        model.setSelectClause(SelectClause.create().setStreamSelector(StreamSelector.RSTREAM_ISTREAM_BOTH)
                .add("key")
                .add(multi, "value"));
        model.setFromClause(FromClause.create(FilterStream.create("MyWindow", Expressions.isNotNull("value"))));

        EPStatement stmtSelectOne = epService.getEPAdministrator().create(model);
        stmtSelectOne.addListener(listenerStmtOne);
        String stmtTextSelectOne = "select irstream key, (value * 2) as value from MyWindow((value != null))";
        assertEquals(stmtTextSelectOne, model.toEQL());

        // send events
        sendSupportBean("E1", 10L);
        ArrayAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[] {"E1", 20L});
        ArrayAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[] {"E1", 10L});

        sendSupportBean("E2", 20L);
        ArrayAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[] {"E2", 40L});
        ArrayAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[] {"E2", 20L});

        // create delete stmt
        model = new EPStatementObjectModel();
        model.setOnExpr(OnClause.createOnDelete("MyWindow", "s1"));
        model.setFromClause(FromClause.create(FilterStream.create(SupportMarketDataBean.class.getName(), "s0")));
        model.setWhereClause(Expressions.eqProperty("s0.symbol", "s1.key"));
        epService.getEPAdministrator().create(model);
        String stmtTextDelete = "on " + SupportMarketDataBean.class.getName() + " as s0 delete from MyWindow as s1 where (s0.symbol = s1.key)";
        assertEquals(stmtTextDelete, model.toEQL());

        // send delete event
        sendMarketBean("E1");
        ArrayAssertionUtil.assertProps(listenerStmtOne.assertOneGetOldAndReset(), fields, new Object[] {"E1", 20L});
        ArrayAssertionUtil.assertProps(listenerWindow.assertOneGetOldAndReset(), fields, new Object[] {"E1", 10L});

        // send delete event again, none deleted now
        sendMarketBean("E1");
        assertFalse(listenerStmtOne.isInvoked());
        assertFalse(listenerWindow.isInvoked());

        // send delete event
        sendMarketBean("E2");
        ArrayAssertionUtil.assertProps(listenerStmtOne.assertOneGetOldAndReset(), fields, new Object[] {"E2", 40L});
        ArrayAssertionUtil.assertProps(listenerWindow.assertOneGetOldAndReset(), fields, new Object[] {"E2", 20L});

        // On-select object model
        model = new EPStatementObjectModel();
        model.setOnExpr(OnClause.createOnSelect("MyWindow", "s1"));
        model.setWhereClause(Expressions.eqProperty("s0.id", "s1.key"));
        model.setFromClause(FromClause.create(FilterStream.create(SupportBean_B.class.getName(), "s0")));
        model.setSelectClause(SelectClause.createStreamWildcard("s1"));
        EPStatement statement = epService.getEPAdministrator().create(model);
        statement.addListener(listenerOnSelect);
        String stmtTextOnSelect = "on " + SupportBean_B.class.getName() + " as s0 select s1.*  from MyWindow as s1 where (s0.id = s1.key)";
        assertEquals(stmtTextOnSelect, model.toEQL());

        // send some more events
        sendSupportBean("E3", 30L);
        sendSupportBean("E4", 40L);

        epService.getEPRuntime().sendEvent(new SupportBean_B("B1"));
        assertFalse(listenerOnSelect.isInvoked());

        // trigger on-select
        epService.getEPRuntime().sendEvent(new SupportBean_B("E3"));
        ArrayAssertionUtil.assertProps(listenerOnSelect.assertOneGetNewAndReset(), fields, new Object[] {"E3", 30L});

        stmtSelectOne.destroy();
        stmtInsert.destroy();
        stmtCreate.destroy();
    }

    private SupportBean sendSupportBean(String string, Long longBoxed)
    {
        SupportBean bean = new SupportBean();
        bean.setString(string);
        bean.setLongBoxed(longBoxed);
        epService.getEPRuntime().sendEvent(bean);
        return bean;
    }

    private void sendMarketBean(String symbol)
    {
        SupportMarketDataBean bean = new SupportMarketDataBean(symbol, 0, 0l, "");
        epService.getEPRuntime().sendEvent(bean);
    }
}
