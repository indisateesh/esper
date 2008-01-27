package com.espertech.esper.eql.core;

import com.espertech.esper.eql.expression.ExprEvaluator;
import com.espertech.esper.eql.expression.ExprValidationException;
import com.espertech.esper.eql.spec.SelectClauseElementCompiled;
import com.espertech.esper.eql.spec.SelectClauseElementWildcard;
import com.espertech.esper.eql.spec.SelectClauseExprCompiledSpec;
import com.espertech.esper.eql.spec.SelectClauseStreamCompiledSpec;
import com.espertech.esper.event.EventBean;
import com.espertech.esper.event.EventType;

import java.util.ArrayList;
import java.util.List;

/**
 * Works in conjunction with {@link SelectExprResultProcessor} to present
 * a result as an object array for 'natural' delivery.
 */
public class BindProcessor
{
    private ExprEvaluator[] expressionNodes;
    private Class[] expressionTypes;
    private String[] columnNamesAssigned;

    /**
     * Ctor.
     * @param selectionList the select clause
     * @param typesPerStream the event types per stream
     * @param streamNames the stream names
     * @throws ExprValidationException when the validation of the select clause failed
     */
    public BindProcessor(List<SelectClauseElementCompiled> selectionList,
                         EventType[] typesPerStream,
                         String[] streamNames)
            throws ExprValidationException
    {
        ArrayList<ExprEvaluator> expressions = new ArrayList<ExprEvaluator>();
        ArrayList<Class> types = new ArrayList<Class>();
        ArrayList<String> columnNames = new ArrayList<String>();

        for (SelectClauseElementCompiled element : selectionList)
        {
            // handle wildcards by outputting each stream's underlying event
            if (element instanceof SelectClauseElementWildcard)
            {
                for (int i = 0; i < typesPerStream.length; i++)
                {
                    final int streamNum = i;
                    expressions.add(new ExprEvaluator() {

                        public Object evaluate(EventBean[] eventsPerStream, boolean isNewData)
                        {
                            EventBean event = eventsPerStream[streamNum];
                            if (event != null)
                            {
                                return event.getUnderlying();
                            }
                            else
                            {
                                return null;
                            }
                        }
                    });
                    types.add(typesPerStream[streamNum].getUnderlyingType());
                    columnNames.add(streamNames[streamNum]);
                }
            }

            // handle stream wildcards by outputting the stream underlying event
            else if (element instanceof SelectClauseStreamCompiledSpec)
            {
                final SelectClauseStreamCompiledSpec streamSpec = (SelectClauseStreamCompiledSpec) element;
                expressions.add(new ExprEvaluator() {

                    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData)
                    {
                        EventBean event = eventsPerStream[streamSpec.getStreamNumber()];
                        if (event != null)
                        {
                            return event.getUnderlying();
                        }
                        else
                        {
                            return null;
                        }
                    }
                });
                types.add(typesPerStream[streamSpec.getStreamNumber()].getUnderlyingType());
                columnNames.add(streamNames[streamSpec.getStreamNumber()]);
            }

            // handle expressions
            else if (element instanceof SelectClauseExprCompiledSpec)
            {
                SelectClauseExprCompiledSpec expr = (SelectClauseExprCompiledSpec) element;
                expressions.add(expr.getSelectExpression());
                types.add(expr.getSelectExpression().getType());
                if (expr.getAssignedName() != null)
                {
                    columnNames.add(expr.getAssignedName());
                }
                else
                {
                    columnNames.add(expr.getSelectExpression().toExpressionString());
                }
            }
            else
            {
                throw new IllegalStateException("Unrecognized select expression element of type " + element.getClass());
            }
        }

        expressionNodes = expressions.toArray(new ExprEvaluator[0]);
        expressionTypes = types.toArray(new Class[0]);
        columnNamesAssigned = columnNames.toArray(new String[0]);
    }

    public Object[] process(EventBean[] eventsPerStream, boolean isNewData)
    {
        Object[] parameters = new Object[expressionNodes.length];

        for (int i = 0; i < parameters.length; i++)
        {
            Object result = expressionNodes[i].evaluate(eventsPerStream, isNewData);
            parameters[i] = result;
        }

        return parameters;
    }

    public Class[] getExpressionTypes() {
        return expressionTypes;
    }

    public String[] getColumnNamesAssigned() {
        return columnNamesAssigned;
    }
}
