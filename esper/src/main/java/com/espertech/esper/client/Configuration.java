/**************************************************************************************
 * Copyright (C) 2006 Esper Team. All rights reserved.                                *
 * http://esper.codehaus.org                                                          *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the GPL license       *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package com.espertech.esper.client;

import java.io.*;
import java.net.URL;
import java.util.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.*;

/**
 * An instance of <tt>Configuration</tt> allows the application
 * to specify properties to be used when
 * creating a <tt>EPServiceProvider</tt>. Usually an application will create
 * a single <tt>Configuration</tt>, then get one or more instances of
 * {@link EPServiceProvider} via {@link EPServiceProviderManager}.
 * The <tt>Configuration</tt> is meant
 * only as an initialization-time object. <tt>EPServiceProvider</tt>s are
 * immutable and do not retain any association back to the
 * <tt>Configuration</tt>.
 * <br>
 * The format of an Esper XML configuration file is defined in
 * <tt>esper-configuration-1.0.xsd</tt>.
 */
public class Configuration implements ConfigurationOperations, ConfigurationInformation, Serializable
{
	private static Log log = LogFactory.getLog( Configuration.class );

    /**
     * Default name of the configuration file.
     */
    protected static final String ESPER_DEFAULT_CONFIG = "esper.cfg.xml";

    /**
     * Map of event name and fully-qualified Java class name.
     */
	protected Map<String, String> eventClasses;

    /**
     * Map of event type alias and XML DOM configuration.
     */
	protected Map<String, ConfigurationEventTypeXMLDOM> eventTypesXMLDOM;

    /**
     * Map of event type alias and Legacy-type event configuration.
     */
	protected Map<String, ConfigurationEventTypeLegacy> eventTypesLegacy;

	/**
	 * The type aliases for events that result when maps are sent
	 * into the engine.
	 */
	protected Map<String, Properties> mapAliases;

	/**
	 * The java-style class and package name imports that
	 * will be used to resolve partial class names.
	 */
	protected List<String> imports;

    /**
     * The java-style class and package name imports that
     * will be used to resolve partial class names.
     */
    protected Map<String, ConfigurationDBRef> databaseReferences;

	/**
	 * True until the user calls addAutoImport().
	 */
	private boolean isUsingDefaultImports = true;

    /**
     * Optional classname to use for constructing services context.
     */
    protected String epServicesContextFactoryClassName;

    /**
     * List of configured plug-in views.
     */
    protected List<ConfigurationPlugInView> plugInViews;

    /**
     * List of configured plug-in pattern objects.
     */
    protected List<ConfigurationPlugInPatternObject> plugInPatternObjects;

    /**
     * List of configured plug-in aggregation functions.
     */
    protected List<ConfigurationPlugInAggregationFunction> plugInAggregationFunctions;

    /**
     * List of adapter loaders.
     */
    protected List<ConfigurationAdapterLoader> adapterLoaders;

    /**
     * Saves engine default configs such as threading settings
     */
    protected ConfigurationEngineDefaults engineDefaults;

    /**
     * Saves the Java packages to search to resolve event type aliases.
     */
    protected Set<String> eventTypeAutoAliasPackages;

    /**
     * Map of variables.
     */
    protected Map<String, ConfigurationVariable> variables;

    /**
     * Map of class name and configuration for method invocations on that class.
     */
	protected Map<String, ConfigurationMethodRef> methodInvocationReferences;

    /**
     * Constructs an empty configuration. The auto import values
     * are set by default to java.lang, java.math, java.text and
     * java.util.
     */
    public Configuration()
    {
        reset();
    }

    /**
     * Sets the class name of the services context factory class to use.
     * @param epServicesContextFactoryClassName service context factory class name
     */
    public void setEPServicesContextFactoryClassName(String epServicesContextFactoryClassName)
    {
        this.epServicesContextFactoryClassName = epServicesContextFactoryClassName;
    }

    public String getEPServicesContextFactoryClassName()
    {
        return epServicesContextFactoryClassName;
    }

    public void addPlugInAggregationFunction(String functionName, String aggregationClassName)
    {
        ConfigurationPlugInAggregationFunction entry = new ConfigurationPlugInAggregationFunction();
        entry.setFunctionClassName(aggregationClassName);
        entry.setName(functionName);
        plugInAggregationFunctions.add(entry);
    }

    /**
     * Add an alias for an event type represented by Java-bean plain-old Java object events.
     * @param eventTypeAlias is the alias for the event type
     * @param javaEventClassName fully-qualified class name of the event type
     */
    public void addEventTypeAlias(String eventTypeAlias, String javaEventClassName)
    {
        eventClasses.put(eventTypeAlias, javaEventClassName);
    }

    /**
     * Add an alias for an event type represented by Java-bean plain-old Java object events.
     * @param eventTypeAlias is the alias for the event type
     * @param javaEventClass is the Java event class for which to create the alias
     */
    public void addEventTypeAlias(String eventTypeAlias, Class javaEventClass)
    {
        addEventTypeAlias(eventTypeAlias, javaEventClass.getName());
    }

    /**
     * Add an alias for an event type represented by Java-bean plain-old Java object events,
     * and the alias is the simple class name of the class.
     * @param javaEventClass is the Java event class for which to create the alias
     */
    public void addEventTypeAliasSimpleName(Class javaEventClass)
    {
        addEventTypeAlias(javaEventClass.getSimpleName(), javaEventClass.getName());
    }

    /**
     * Add an alias for an event type that represents java.util.Map events.
     * @param eventTypeAlias is the alias for the event type
     * @param typeMap maps the name of each property in the Map event to the type
     * (fully qualified classname) of its value in Map event instances.
     */
    public void addEventTypeAlias(String eventTypeAlias, Properties typeMap)
    {
    	mapAliases.put(eventTypeAlias, typeMap);
    }

    /**
     * Add an alias for an event type that represents java.util.Map events, taking a Map of
     * event property and class name as a parameter.
     * <p>
     * This method is provided for convenience and is same in function to method
     * taking a Properties object that contain fully qualified class name as values.
     * @param eventTypeAlias is the alias for the event type
     * @param typeMap maps the name of each property in the Map event to the type of its value in the Map object
     */
    public void addEventTypeAlias(String eventTypeAlias, Map<String, Class> typeMap)
    {
        Properties properties = new Properties();
        for (Map.Entry<String, Class> entry : typeMap.entrySet())
        {
            properties.put(entry.getKey(), entry.getValue().getName());
        }
        addEventTypeAlias(eventTypeAlias, properties);
    }

    /**
     * Add an alias for an event type that represents org.w3c.dom.Node events.
     * @param eventTypeAlias is the alias for the event type
     * @param xmlDOMEventTypeDesc descriptor containing property and mapping information for XML-DOM events
     */
    public void addEventTypeAlias(String eventTypeAlias, ConfigurationEventTypeXMLDOM xmlDOMEventTypeDesc)
    {
        eventTypesXMLDOM.put(eventTypeAlias, xmlDOMEventTypeDesc);
    }

    /**
     * Add a database reference with a given database name.
     * @param name is the database name
     * @param configurationDBRef descriptor containing database connection and access policy information
     */
    public void addDatabaseReference(String name, ConfigurationDBRef configurationDBRef)
    {
        databaseReferences.put(name, configurationDBRef);
    }

    /**
     * Add an alias for an event type that represents legacy Java type (non-JavaBean style) events.
     * @param eventTypeAlias is the alias for the event type
     * @param javaEventClass fully-qualified class name of the event type
     * @param legacyEventTypeDesc descriptor containing property and mapping information for Legacy Java type events
     */
    public void addEventTypeAlias(String eventTypeAlias, String javaEventClass, ConfigurationEventTypeLegacy legacyEventTypeDesc)
    {
        eventClasses.put(eventTypeAlias, javaEventClass);
        eventTypesLegacy.put(eventTypeAlias, legacyEventTypeDesc);
    }

    public void addImport(String autoImport)
    {
		if(isUsingDefaultImports)
		{
			isUsingDefaultImports = false;
			imports.clear();
		}
    	imports.add(autoImport);
    }

    /**
     * Adds a cache configuration for a class providing methods for use in the from-clause.
     * @param className is the class name (simple or fully-qualified) providing methods
     * @param methodInvocationConfig is the cache configuration
     */
    public void addMethodRef(String className, ConfigurationMethodRef methodInvocationConfig)
    {
        this.methodInvocationReferences.put(className, methodInvocationConfig);
    }

    /**
     * Adds a cache configuration for a class providing methods for use in the from-clause.
     * @param clazz is the class providing methods
     * @param methodInvocationConfig is the cache configuration
     */
    public void addMethodRef(Class clazz, ConfigurationMethodRef methodInvocationConfig)
    {
        this.methodInvocationReferences.put(clazz.getName(), methodInvocationConfig);
    }

    public Map<String, String> getEventTypeAliases()
    {
        return eventClasses;
    }

    public Map<String, Properties> getEventTypesMapEvents()
    {
    	return mapAliases;
    }

    public Map<String, ConfigurationEventTypeXMLDOM> getEventTypesXMLDOM()
    {
        return eventTypesXMLDOM;
    }

    public Map<String, ConfigurationEventTypeLegacy> getEventTypesLegacy()
    {
        return eventTypesLegacy;
    }

	public List<String> getImports()
	{
		return imports;
	}

    public Map<String, ConfigurationDBRef> getDatabaseReferences()
    {
        return databaseReferences;
    }

    public List<ConfigurationPlugInView> getPlugInViews()
    {
        return plugInViews;
    }

    public List<ConfigurationAdapterLoader> getAdapterLoaders()
    {
        return adapterLoaders;
    }

    public List<ConfigurationPlugInAggregationFunction> getPlugInAggregationFunctions()
    {
        return plugInAggregationFunctions;
    }

    public List<ConfigurationPlugInPatternObject> getPlugInPatternObjects()
    {
        return plugInPatternObjects;
    }

    public Map<String, ConfigurationVariable> getVariables()
    {
        return variables;
    }

    public Map<String, ConfigurationMethodRef> getMethodInvocationReferences()
    {
        return methodInvocationReferences;
    }

    /**
     * Add an input/output adapter loader.
     * @param loaderName is the name of the loader
     * @param className is the fully-qualified classname of the loader class
     * @param configuration is loader cofiguration entries
     */
    public void addAdapterLoader(String loaderName, String className, Properties configuration)
    {
        ConfigurationAdapterLoader adapterLoader = new ConfigurationAdapterLoader();
        adapterLoader.setLoaderName(loaderName);
        adapterLoader.setClassName(className);
        adapterLoader.setConfigProperties(configuration);
        adapterLoaders.add(adapterLoader);
    }

    /**
     * Add a view for plug-in.
     * @param namespace is the namespace the view should be available under
     * @param name is the name of the view
     * @param viewFactoryClass is the view factory class to use
     */
    public void addPlugInView(String namespace, String name, String viewFactoryClass)
    {
        ConfigurationPlugInView configurationPlugInView = new ConfigurationPlugInView();
        configurationPlugInView.setNamespace(namespace);
        configurationPlugInView.setName(name);
        configurationPlugInView.setFactoryClassName(viewFactoryClass);
        plugInViews.add(configurationPlugInView);
    }

    /**
     * Add a pattern event observer for plug-in.
     * @param namespace is the namespace the observer should be available under
     * @param name is the name of the observer
     * @param observerFactoryClass is the observer factory class to use
     */
    public void addPlugInPatternObserver(String namespace, String name, String observerFactoryClass)
    {
        ConfigurationPlugInPatternObject entry = new ConfigurationPlugInPatternObject();
        entry.setNamespace(namespace);
        entry.setName(name);
        entry.setFactoryClassName(observerFactoryClass);
        entry.setPatternObjectType(ConfigurationPlugInPatternObject.PatternObjectType.OBSERVER);
        plugInPatternObjects.add(entry);
    }

    /**
     * Add a pattern guard for plug-in.
     * @param namespace is the namespace the guard should be available under
     * @param name is the name of the guard
     * @param guardFactoryClass is the guard factory class to use
     */
    public void addPlugInPatternGuard(String namespace, String name, String guardFactoryClass)
    {
        ConfigurationPlugInPatternObject entry = new ConfigurationPlugInPatternObject();
        entry.setNamespace(namespace);
        entry.setName(name);
        entry.setFactoryClassName(guardFactoryClass);
        entry.setPatternObjectType(ConfigurationPlugInPatternObject.PatternObjectType.GUARD);
        plugInPatternObjects.add(entry);
    }

    public void addEventTypeAutoAlias(String javaPackageName)
    {
        eventTypeAutoAliasPackages.add(javaPackageName);
    }

    public void addVariable(String variableName, Class type, Object initializationValue)
    {
        ConfigurationVariable configVar = new ConfigurationVariable();
        configVar.setType(type);
        configVar.setInitializationValue(initializationValue);
        variables.put(variableName, configVar);
    }

    public Set<String> getEventTypeAutoAliasPackages()
    {
        return eventTypeAutoAliasPackages;
    }

    public ConfigurationEngineDefaults getEngineDefaults()
    {
        return engineDefaults;
    }

    /**
	 * Use the configuration specified in an application
	 * resource named <tt>esper.cfg.xml</tt>.
     * @return Configuration initialized from the resource
     * @throws EPException thrown to indicate error reading configuration
     */
	public Configuration configure() throws EPException
    {
		configure('/' + ESPER_DEFAULT_CONFIG);
		return this;
	}

    /**
     * Use the configuration specified in the given application
     * resource. The format of the resource is defined in
     * <tt>esper-configuration-1.0.xsd</tt>.
     * <p/>
     * The resource is found via <tt>getConfigurationInputStream(resource)</tt>.
     * That method can be overridden to implement an arbitrary lookup strategy.
     * <p/>
     * See <tt>getResourceAsStream</tt> for information on how the resource name is resolved.
     * @param resource if the file name of the resource
     * @return Configuration initialized from the resource
     * @throws EPException thrown to indicate error reading configuration
     */
    public Configuration configure(String resource) throws EPException
    {
        if (log.isDebugEnabled())
        {
            log.debug( "configuring from resource: " + resource );
        }
        InputStream stream = getConfigurationInputStream( resource );
        ConfigurationParser.doConfigure(this, stream, resource );
        return this;
    }

    /**
     * Get the configuration file as an <tt>InputStream</tt>. Might be overridden
     * by subclasses to allow the configuration to be located by some arbitrary
     * mechanism.
     * <p>
     * See <tt>getResourceAsStream</tt> for information on how the resource name is resolved.
     * @param resource is the resource name
     * @return input stream for resource
     * @throws EPException thrown to indicate error reading configuration
     */
    protected static InputStream getConfigurationInputStream(String resource) throws EPException
    {
        if (log.isDebugEnabled())
        {
            log.debug( "Configuration resource: " + resource );
        }
        return getResourceAsStream(resource);
    }


	/**
	 * Use the configuration specified by the given URL.
	 * The format of the document obtained from the URL is defined in
	 * <tt>esper-configuration-1.0.xsd</tt>.
	 *
	 * @param url URL from which you wish to load the configuration
	 * @return A configuration configured via the file
	 * @throws EPException is thrown when the URL could not be access
	 */
	public Configuration configure(URL url) throws EPException
    {
        if (log.isDebugEnabled())
        {
            log.debug( "configuring from url: " + url.toString() );
        }
        try {
            ConfigurationParser.doConfigure(this, url.openStream(), url.toString());
            return this;
		}
		catch (IOException ioe) {
			throw new EPException("could not configure from URL: " + url, ioe );
		}
	}

	/**
	 * Use the configuration specified in the given application
	 * file. The format of the file is defined in
	 * <tt>esper-configuration-1.0.xsd</tt>.
	 *
	 * @param configFile <tt>File</tt> from which you wish to load the configuration
	 * @return A configuration configured via the file
	 * @throws EPException when the file could not be found
	 */
	public Configuration configure(File configFile) throws EPException
    {
        if (log.isDebugEnabled())
        {
            log.debug( "configuring from file: " + configFile.getName() );
        }
        try {
            ConfigurationParser.doConfigure(this, new FileInputStream(configFile), configFile.toString());
		}
		catch (FileNotFoundException fnfe) {
			throw new EPException( "could not find file: " + configFile, fnfe );
		}
        return this;
    }


	/**
	 * Use the mappings and properties specified in the given XML document.
	 * The format of the file is defined in
	 * <tt>esper-configuration-1.0.xsd</tt>.
	 *
	 * @param document an XML document from which you wish to load the configuration
	 * @return A configuration configured via the <tt>Document</tt>
	 * @throws EPException if there is problem in accessing the document.
	 */
	public Configuration configure(Document document) throws EPException
    {
        if (log.isDebugEnabled())
        {
		    log.debug( "configuring from XML document" );
        }
        ConfigurationParser.doConfigure(this, document);
        return this;
    }

    /**
     * Returns an input stream from an application resource in the classpath.
     * <p>
     * The method first removes the '/' character from the resource name if
     * the first character is '/'.
     * <p>
     * The lookup order is as follows:
     * <p>
     * If a thread context class loader exists, use <tt>Thread.currentThread().getResourceAsStream</tt>
     * to obtain an InputStream.
     * <p>
     * If no input stream was returned, use the <tt>Configuration.class.getResourceAsStream</tt>.
     * to obtain an InputStream.
     * <p>
     * If no input stream was returned, use the <tt>Configuration.class.getClassLoader().getResourceAsStream</tt>.
     * to obtain an InputStream.
     * <p>
     * If no input stream was returned, throw an Exception.
     *
     * @param resource to get input stream for
     * @return input stream for resource
     */
    protected static InputStream getResourceAsStream(String resource)
    {
        String stripped = resource.startsWith("/") ?
                resource.substring(1) : resource;

        InputStream stream = null;
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader!=null) {
            stream = classLoader.getResourceAsStream( stripped );
        }
        if ( stream == null ) {
            stream = Configuration.class.getResourceAsStream( resource );
        }
        if ( stream == null ) {
            stream = Configuration.class.getClassLoader().getResourceAsStream( stripped );
        }
        if ( stream == null ) {
            throw new EPException( resource + " not found" );
        }
        return stream;
    }

    /**
     * Reset to an empty configuration.
     */
    protected void reset()
    {
        eventClasses = new HashMap<String, String>();
        mapAliases = new HashMap<String, Properties>();
        eventTypesXMLDOM = new HashMap<String, ConfigurationEventTypeXMLDOM>();
        eventTypesLegacy = new HashMap<String, ConfigurationEventTypeLegacy>();
        databaseReferences = new HashMap<String, ConfigurationDBRef>();
        imports = new ArrayList<String>();
        addDefaultImports();
        isUsingDefaultImports = true;
        plugInViews = new ArrayList<ConfigurationPlugInView>();
        adapterLoaders = new ArrayList<ConfigurationAdapterLoader>();
        plugInAggregationFunctions = new ArrayList<ConfigurationPlugInAggregationFunction>();
        plugInPatternObjects = new ArrayList<ConfigurationPlugInPatternObject>();
        engineDefaults = new ConfigurationEngineDefaults();
        eventTypeAutoAliasPackages = new LinkedHashSet<String>();
        variables = new HashMap<String, ConfigurationVariable>();
        methodInvocationReferences = new HashMap<String, ConfigurationMethodRef>();
    }

    /**
     * Use these imports until the user specifies something else.
     */
    private void addDefaultImports()
    {
    	imports.add("java.lang.*");
    	imports.add("java.math.*");
    	imports.add("java.text.*");
    	imports.add("java.util.*");
    }

    /**
     * Enumeration of different resolution styles for resolving property names.
     */
    public static enum PropertyResolutionStyle
    {
        /**
         * Properties are only matched if the names are identical in name
         * and case to the original property name.
         */
        CASE_SENSITIVE,

        /**
         * Properties are matched if the names are identical.  A case insensitive
         * search is used and will choose the first property that matches
         * the name exactly or the first property that matches case insensitively
         * should no match be found.
         */
        CASE_INSENSITIVE,

        /**
         * Properties are matched if the names are identical.  A case insensitive
         * search is used and will choose the first property that matches
         * the name exactly case insensitively.  If more than one 'name' can be
         * mapped to the property an exception is thrown.
         */
        DISTINCT_CASE_INSENSITIVE;

        /**
         * Returns the default property resolution style.
         * @return is the case-sensitive resolution
         */
        public static PropertyResolutionStyle getDefault()
        {
            return CASE_SENSITIVE;
        }
    }
}
