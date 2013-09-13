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

package com.espertech.esper.regression.enummethod;

import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.support.bean.SupportBean_ST0;
import com.espertech.esper.support.bean.SupportBean_ST0_Container;
import com.espertech.esper.support.bean.SupportCollection;
import com.espertech.esper.support.bean.lambda.LambdaAssertionUtil;
import com.espertech.esper.support.client.SupportConfigFactory;
import junit.framework.Assert;
import junit.framework.TestCase;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

public class TestEnumGroupBy extends TestCase {

    private EPServiceProvider epService;
    private SupportUpdateListener listener;

    public void setUp() {

        Configuration config = SupportConfigFactory.getConfiguration();
        config.addEventType("Bean", SupportBean_ST0_Container.class);
        config.addEventType("SupportCollection", SupportCollection.class);
        epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();
        listener = new SupportUpdateListener();
    }

    protected void tearDown() throws Exception {
        listener = null;
    }

    public void testKeySelectorOnly() {

        // - duplicate key allowed, creates a list of values
        // - null key & value allowed
        
        String eplFragment = "select contained.groupBy(c => id) as val from Bean";
        EPStatement stmtFragment = epService.getEPAdministrator().createEPL(eplFragment);
        stmtFragment.addListener(listener);
        LambdaAssertionUtil.assertTypes(stmtFragment.getEventType(), "val".split(","), new Class[]{Map.class});
        EPAssertionUtil.AssertionCollectionValueString extractorEvents = new EPAssertionUtil.AssertionCollectionValueString() {
            public String extractValue(Object collectionItem) {
                int p00 = ((SupportBean_ST0) collectionItem).getP00();
                return Integer.toString(p00);
            }
        };

        epService.getEPRuntime().sendEvent(SupportBean_ST0_Container.make2Value("E1,1", "E1,2", "E2,5"));
        EPAssertionUtil.assertMapOfCollection((Map) listener.assertOneGetNewAndReset().get("val"), "E1,E2".split(","),
                new String[]{"1,2", "5"}, extractorEvents);

        epService.getEPRuntime().sendEvent(SupportBean_ST0_Container.make2Value(null));
        assertNull(listener.assertOneGetNewAndReset().get("val"));

        epService.getEPRuntime().sendEvent(SupportBean_ST0_Container.make2Value());
        assertEquals(0, ((Map) listener.assertOneGetNewAndReset().get("val")).size());
        stmtFragment.destroy();

        // test scalar
        epService.getEPAdministrator().getConfiguration().addPlugInSingleRowFunction("extractAfterUnderscore", this.getClass().getName(), "extractAfterUnderscore");
        String eplScalar = "select strvals.groupBy(c => extractAfterUnderscore(c)) as val from SupportCollection";
        EPStatement stmtScalar = epService.getEPAdministrator().createEPL(eplScalar);
        stmtScalar.addListener(listener);
        LambdaAssertionUtil.assertTypes(stmtScalar.getEventType(), "val".split(","), new Class[]{Map.class});

        epService.getEPRuntime().sendEvent(SupportCollection.makeString("E1_2,E2_1,E3_2"));
        EPAssertionUtil.assertMapOfCollection((Map) listener.assertOneGetNewAndReset().get("val"), "2,1".split(","),
                new String[]{"E1_2,E3_2", "E2_1"}, getExtractorScalar());

        epService.getEPRuntime().sendEvent(SupportCollection.makeString(null));
        assertNull(listener.assertOneGetNewAndReset().get("val"));

        epService.getEPRuntime().sendEvent(SupportCollection.makeString(""));
        assertEquals(0, ((Map) listener.assertOneGetNewAndReset().get("val")).size());
    }

    public void testKeyValueSelector() {

        String eplFragment = "select contained.groupBy(k => id, v => p00) as val from Bean";
        EPStatement stmtFragment = epService.getEPAdministrator().createEPL(eplFragment);
        stmtFragment.addListener(listener);
        EPAssertionUtil.AssertionCollectionValueString extractor = new EPAssertionUtil.AssertionCollectionValueString() {
            public String extractValue(Object collectionItem) {
                int p00 = (Integer) collectionItem;
                return Integer.toString(p00);
            }
        };

        epService.getEPRuntime().sendEvent(SupportBean_ST0_Container.make2Value("E1,1", "E1,2", "E2,5"));
        EPAssertionUtil.assertMapOfCollection((Map)listener.assertOneGetNewAndReset().get("val"), "E1,E2".split(","),
                new String[]{"1,2", "5"}, extractor);

        epService.getEPRuntime().sendEvent(SupportBean_ST0_Container.make2Value(null));
        assertNull(listener.assertOneGetNewAndReset().get("val"));

        epService.getEPRuntime().sendEvent(SupportBean_ST0_Container.make2Value());
        assertEquals(0, ((Map) listener.assertOneGetNewAndReset().get("val")).size());

        // test scalar
        epService.getEPAdministrator().getConfiguration().addPlugInSingleRowFunction("extractAfterUnderscore", this.getClass().getName(), "extractAfterUnderscore");
        String eplScalar = "select strvals.groupBy(k => extractAfterUnderscore(k), v => v) as val from SupportCollection";
        EPStatement stmtScalar = epService.getEPAdministrator().createEPL(eplScalar);
        stmtScalar.addListener(listener);
        LambdaAssertionUtil.assertTypes(stmtScalar.getEventType(), "val".split(","), new Class[]{Map.class});

        epService.getEPRuntime().sendEvent(SupportCollection.makeString("E1_2,E2_1,E3_2"));
        EPAssertionUtil.assertMapOfCollection((Map) listener.assertOneGetNewAndReset().get("val"), "2,1".split(","),
                new String[]{"E1_2,E3_2", "E2_1"}, getExtractorScalar());

        epService.getEPRuntime().sendEvent(SupportCollection.makeString(null));
        assertNull(listener.assertOneGetNewAndReset().get("val"));

        epService.getEPRuntime().sendEvent(SupportCollection.makeString(""));
        assertEquals(0, ((Map) listener.assertOneGetNewAndReset().get("val")).size());
    }
    
    public static String extractAfterUnderscore(String string) {
        int indexUnderscore = string.indexOf("_");
        if (indexUnderscore == -1) {
            Assert.fail();
        }
        return string.substring(indexUnderscore + 1);
    }

    private static EPAssertionUtil.AssertionCollectionValueString getExtractorScalar() {
        return new EPAssertionUtil.AssertionCollectionValueString() {
            public String extractValue(Object collectionItem) {
                return collectionItem.toString();
            }
        };
    }
}
