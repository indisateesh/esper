package com.espertech.esper.example.servershell.jmx;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.UpdateListener;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class EPServiceProviderJMX implements EPServiceProviderJMXMBean
{
    private static Log log = LogFactory.getLog(EPServiceProviderJMX.class);
    private EPServiceProvider engine;

    public EPServiceProviderJMX(EPServiceProvider engine)
    {
        if (engine == null)
        {
            throw new IllegalArgumentException("No engine instance supplied");
        }
        this.engine = engine;
    }

    public void createEQL(String expression, String statementName)
    {
        log.info("Via Java Management JMX proxy: Creating statement '" + expression + "' named '" + statementName + "'");
        engine.getEPAdministrator().createEQL(expression, statementName);
    }

    public void createEQL(String expression, String statementName, UpdateListener listener)
    {
        log.info("Via Java Management JMX proxy: Creating statement '" + expression + "' named '" + statementName + "'");
        EPStatement stmt = engine.getEPAdministrator().createEQL(expression, statementName);
        stmt.addListener(listener);
    }

    public void destroy(String statementName)
    {
        log.info("Via Java Management JMX proxy: Destroying statement '" + statementName + "'");
        engine.getEPAdministrator().getStatement(statementName).destroy();
    }
}
