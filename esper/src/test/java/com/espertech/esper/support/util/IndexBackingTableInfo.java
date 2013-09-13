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

package com.espertech.esper.support.util;

import com.espertech.esper.epl.join.table.*;
import com.espertech.esper.support.epl.SupportQueryPlanIndexHook;

public interface IndexBackingTableInfo {
    public final static String INDEX_CALLBACK_HOOK = "@Hook(type=HookType.INTERNAL_QUERY_PLAN, hook='" + SupportQueryPlanIndexHook.resetGetClassName() + "')\n";

    public final static String BACKING_SINGLE_UNIQUE = PropertyIndexedEventTableSingleUnique.class.getSimpleName();
    public final static String BACKING_SINGLE_DUPS = PropertyIndexedEventTableSingle.class.getSimpleName();
    public final static String BACKING_SINGLE_DUPS_COERCEADD = PropertyIndexedEventTableSingleCoerceAdd.class.getSimpleName();
    public final static String BACKING_MULTI_UNIQUE = PropertyIndexedEventTableUnique.class.getSimpleName();
    public final static String BACKING_MULTI_DUPS = PropertyIndexedEventTable.class.getSimpleName();
    public final static String BACKING_MULTI_DUPS_COERCEADD = PropertyIndexedEventTableCoerceAdd.class.getSimpleName();
    public final static String BACKING_SORTED_COERCED = PropertySortedEventTableCoerced.class.getSimpleName();
    public final static String BACKING_SORTED = PropertySortedEventTable.class.getSimpleName();
    public final static String BACKING_UNINDEXED = UnindexedEventTable.class.getSimpleName();
    public final static String BACKING_COMPOSITE = PropertyCompositeEventTable.class.getSimpleName();
}
