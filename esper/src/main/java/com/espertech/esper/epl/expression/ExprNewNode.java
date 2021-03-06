/**************************************************************************************
 * Copyright (C) 2008 EsperTech, Inc. All rights reserved.                            *
 * http://esper.codehaus.org                                                          *
 * http://www.espertech.com                                                           *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the GPL license       *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package com.espertech.esper.epl.expression;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.util.JavaClassHelper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.StringWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Represents the bit-wise operators in an expression tree.
 */
public class ExprNewNode extends ExprNodeBase implements ExprEvaluatorTypableReturn {

    private static final long serialVersionUID = -210293632565665600L;

    private final String[] columnNames;
    private transient LinkedHashMap<String, Object> eventType;
    private transient ExprEvaluator[] evaluators;
    private boolean isAllConstants;

    /**
     * Ctor.
     */
    public ExprNewNode(String[] columnNames)
    {
        this.columnNames = columnNames;
    }

    public ExprEvaluator getExprEvaluator()
    {
        return this;
    }

    public void validate(ExprValidationContext validationContext) throws ExprValidationException
    {
        eventType = new LinkedHashMap<String, Object>();
        evaluators = ExprNodeUtility.getEvaluators(this.getChildNodes());

        for (int i = 0; i < columnNames.length; i++) {
            isAllConstants = isAllConstants && this.getChildNodes()[i].isConstantResult();
            if (eventType.containsKey(columnNames[i])) {
                throw new ExprValidationException("Failed to validate new-keyword property names, property '" + columnNames[i] + "' has already been declared");
            }

            Map<String, Object> eventTypeResult = null;
            if (evaluators[i] instanceof ExprEvaluatorTypableReturn) {
                eventTypeResult = ((ExprEvaluatorTypableReturn) evaluators[i]).getRowProperties();
            }
            if (eventTypeResult != null) {
                eventType.put(columnNames[i], eventTypeResult);
            }
            else {
                Class classResult = JavaClassHelper.getBoxedType(evaluators[i].getType());
                eventType.put(columnNames[i], classResult);
            }
        }
    }

    public String[] getColumnNames() {
        return columnNames;
    }

    public boolean isConstantResult()
    {
        return isAllConstants;
    }

    public Class getType()
    {
        return Map.class;
    }

    public LinkedHashMap<String, Object> getRowProperties() throws ExprValidationException {
        return eventType;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext)
    {
        Map<String, Object> props = new HashMap<String, Object>();
        for (int i = 0; i < evaluators.length; i++) {
            props.put(columnNames[i], evaluators[i].evaluate(eventsPerStream, isNewData, exprEvaluatorContext));
        }
        return props;
    }

    public Boolean isMultirow() {
        return false;   // New itself can only return a single row
    }

    public Object[] evaluateTypableSingle(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        Object[] rows = new Object[columnNames.length];
        for (int i = 0; i < columnNames.length; i++) {
            rows[i] = evaluators[i].evaluate(eventsPerStream, isNewData, context);
        }
        return rows;
    }

    public Object[][] evaluateTypableMulti(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        return null;
    }

    public boolean equalsNode(ExprNode node)
    {
        if (!(node instanceof ExprNewNode))
        {
            return false;
        }

        ExprNewNode other = (ExprNewNode) node;
        return Arrays.deepEquals(other.columnNames, columnNames);
    }

    public String toExpressionString()
    {
        StringWriter writer = new StringWriter();
        writer.write("new { ");
        String delimiter = "";
        for (int i = 0; i < this.getChildNodes().length; i++) {
            writer.append(delimiter);
            writer.append(columnNames[i]);
            ExprNode expr = this.getChildNodes()[i];

            boolean outputexpr = true;
            if (expr instanceof ExprIdentNode) {
                ExprIdentNode prop = (ExprIdentNode) expr;
                if (prop.getResolvedPropertyName().equals( columnNames[i])) {
                    outputexpr = false;
                }
            }

            if (outputexpr) {
                writer.append(" = ");
                writer.append(expr.toExpressionString());
            }
            delimiter = ", ";
        }
        writer.write(" }");
        return writer.toString();
    }

    private static final Log log = LogFactory.getLog(ExprNewNode.class);
}
