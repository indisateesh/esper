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

package com.espertech.esper.core.context.mgr;

import com.espertech.esper.event.EventAdapterService;

import java.util.List;

public interface ContextStateService {

    public ContextStateServiceBinding getBinding(Object bindingInfo);
    public void addContext(String contextName, int agentInstanceId, Object additionalInfo, ContextStateServiceBinding binding);
    public List<ContextState> getContexts(String contextName, ContextStateServiceBinding binding, EventAdapterService eventAdapterService);
    public void removeContext(String contextName);
    public void removeContextAgentInstance(String contextName, int agentInstanceId);
}
