package com.espertech.esper.epl.expression;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.epl.enummethod.dot.ExprDotEvalTypeInfo;
import com.espertech.esper.util.JavaClassHelper;
import net.sf.cglib.reflect.FastMethod;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.lang.reflect.InvocationTargetException;

public class ExprDotMethodEvalNoDuck implements ExprDotEval
{
    private static final Log log = LogFactory.getLog(ExprDotMethodEvalNoDuck.class);

    private final String statementName;
    private final FastMethod method;
    private final ExprEvaluator[] parameters;

    public ExprDotMethodEvalNoDuck(String statementName, FastMethod method, ExprEvaluator[] parameters)
    {
        this.statementName = statementName;
        this.method = method;
        this.parameters = parameters;
    }

    public Object evaluate(Object target, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        if (target == null) {
            return null;
        }

		Object[] args = new Object[parameters.length];
		for(int i = 0; i < args.length; i++)
		{
			args[i] = parameters[i].evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
		}

		try
		{
            return method.invoke(target, args);
		}
		catch (InvocationTargetException e)
		{
            String message = JavaClassHelper.getMessageInvocationTarget(statementName, method.getJavaMethod(), target.getClass().getName(), args, e);
            log.error(message, e.getTargetException());
		}
        return null;
    }

    public ExprDotEvalTypeInfo getTypeInfo() {
        return ExprDotEvalTypeInfo.scalarOrUnderlying(method.getReturnType());
    }
}
