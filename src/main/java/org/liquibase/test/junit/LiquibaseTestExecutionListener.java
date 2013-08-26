/**
 * Copyright (C) 2011-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.liquibase.test.junit;

import static org.springframework.util.StringUtils.isEmpty;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import liquibase.integration.spring.SpringLiquibase;

import org.liquibase.test.annotation.LiquibaseTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;

/**
 * Spring test execution listener to get annotation {@link LiquibaseTest} up and
 * running
 * 
 * @author Alexander Moschov
 */
public class LiquibaseTestExecutionListener implements TestExecutionListener {

    /**
     * Used for logging inside test executions.
     */
    private final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * Invoke this method before test class will be created.</p>
     * 
     * <b>Attention:</b> This will be only invoked if spring version &gt;= 3.x are used.
     * 
     * @param testContext
     *            default test context filled from spring
     * 
     * @throws Exception
     *             if any error occurred
     */
    @Override
    public void beforeTestClass(final TestContext testContext) throws Exception {
        final Class<?> testClass = testContext.getTestClass();
        
        final Annotation annotation = testClass.getAnnotation(LiquibaseTest.class);

        executeChanglog(testContext, (LiquibaseTest) annotation);
    }

    /**
     * no implementation for annotation {@link LiquibaseTest} needed.
     * 
     * @param testContext
     *            default test context filled from spring
     * 
     * @throws Exception
     *             if any error occurred
     */
    @Override
    public void prepareTestInstance(final TestContext testContext) throws Exception {
        //nothing
    }

    /**
     * Called from spring before a test method will be invoked.
     * 
     * @param testContext
     *            default test context filled from spring
     * 
     * @throws Exception
     *             if any error occurred
     */
    @Override
    public void beforeTestMethod(final TestContext testContext) throws Exception {
        final Method testMethod = testContext.getTestMethod();

        final Annotation annotation = testMethod.getAnnotation(LiquibaseTest.class);

        executeChanglog(testContext, (LiquibaseTest) annotation);
    }

    /**
     * no implementation for annotation {@link LiquibaseTest} needed.
     * 
     * @param testContext
     *            default test context filled from spring
     * 
     * @throws Exception
     *             if any error occurred
     */
    @Override
    public void afterTestMethod(final TestContext testContext) throws Exception {
        //nothing
    }

    /**
     * no implementation for annotation {@link LiquibaseTest} needed.
     * 
     * @param testContext
     *            default test context filled from spring
     * 
     * @throws Exception
     *             if any error occurred
     */
    @Override
    public void afterTestClass(final TestContext testContext) throws Exception {
        //nothing
    }

    /**
     * Test the annotation an reset the database.
     * 
     * @param testContext
     *            default test context filled from spring
     * @param annotation
     *            founded
     */
    private void executeChanglog(final TestContext testContext, final LiquibaseTest annotation) throws Exception {
        if (annotation != null) {
            SpringLiquibase liquibase = null;

            final ApplicationContext applicationContext = testContext.getApplicationContext();

            final String executionInfo = getExecutionInformation(testContext);

            if (applicationContext != null) {
                
                liquibase = createLiquibase(applicationContext, annotation);

                if (liquibase != null) {
                    if (logger.isInfoEnabled()) {
                        logger.info("Start reset database for  '%s'.",executionInfo);
                    }

                    
                    if (annotation.dropFirst()) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("Use dropFirst for  '%s'.", executionInfo );
                        }
                    }
                    liquibase.setDropFirst(annotation.dropFirst());

                    if (annotation.shouldRun()) {
                        String changeLog = annotation.changeLog();
                        
                        if ( isEmpty(changeLog) ) {
                            final Class<?> testClass = testContext.getTestClass();
                            final String testChangeLog =  testClass.getName().replace(".", "/") + annotation.changeLogSuffix(); 
                            final URL resource = testContext.getTestClass().getClassLoader().getResource(testChangeLog);
                            if (resource != null) {
                                changeLog = testChangeLog;
                            }
                        }    

                        
                        if ( isEmpty(changeLog) ) {
                            if (logger.isDebugEnabled()) {
                                logger.debug("Default migrate database for  '%s'.", executionInfo);
                            }
                            
                            if (isEmpty(liquibase.getChangeLog())){
                                throw new IllegalArgumentException("Annotation " + annotation.getClass() + " was set, but no Liquibase configuration was given.");
                            }

                        } 
                        else {
                            if (logger.isDebugEnabled()) {
                                logger.debug("Start migration changeLog '%s'  for  '%s'.", changeLog,  executionInfo);
                            }
                            
                            liquibase.setChangeLog(changeLog);

                        }

                        final String contexts = annotation.contexts();
                        if (!isEmpty(contexts)) {
                            if (logger.isDebugEnabled()) {
                                logger.debug("Using contexts '%s'  for  '%s'.", contexts,  executionInfo);

                            }
                            liquibase.setContexts(contexts);
                        }

                        final Map<String, String> parameters = parseParameters(annotation.parameters());
                        if ( parameters != null) {
                            liquibase.setChangeLogParameters(parameters);
                        }

                        final String defaultSchema = annotation.defaultSchema();
                        if (!isEmpty(defaultSchema)) {
                            liquibase.setDefaultSchema(defaultSchema);
                        }

                        liquibase.afterPropertiesSet();
                    }
                    
                    if (logger.isInfoEnabled()) {
                        logger.info("Finished reset database  for  '%s'.", executionInfo);
                    }

                    return;
                }
                throw new IllegalArgumentException("Annotation " + annotation.getClass() + " was set, but no Liquibase configuration was given.");
            }
            throw new IllegalArgumentException("Annotation " + annotation.getClass() + " was set, but no configuration was given.");
        }
    }

    /**
     * Wrapper to get a method
     * <code>ApplicationContext.getBean(Class _class)</code> like in spring 3.0.
     * It will returns always the first instance of the founded class.
     * 
     * @param context
     *            from which the bean should be retrieved
     * @param classType
     *            class type that should be retrieved from the configuration
     *            file.
     * 
     * @return a object of the type or <code>null</code>
     */
    @SuppressWarnings("unchecked")
    private <T> T getBean(final ApplicationContext context, final Class<T> classType) {
        T result = null;

        String[] names = context.getBeanNamesForType(classType);

        if (names != null && names.length > 0) {
            // we always return the bean with the first name

            result = (T) context.getBean(names[0]);
        }

        return result;
    }
    
    /**
     * Helper method to build test execution information with test class and
     * method
     * 
     * @param testContext
     * 
     * @return String like &lt;Class Name&gt;[.&lt;Method Name&gt;]
     */
    public static String getExecutionInformation(TestContext testContext) {
        String result = "";
        Class<?> testClass = testContext.getTestClass();

        result = testClass.getName();

        // now check for method
        Method m = testContext.getTestMethod();
        if (m != null) {
            result = result + "." + m.getName();
        }

        return result;
    }
    
    protected Map<String,String> parseParameters(final String value) {
        if (! isEmpty(value)) {
            final Map<String, String> map = new HashMap<String, String>();
            for(final String entry : value.split(",")) {
                final String[] parts = entry.split(":");

                if ( parts == null || parts.length == 0) {
                    continue;
                }
    
                if ( parts.length != 2) {
                    throw new IllegalArgumentException("Invalid entry: " + entry);
                }
                
                map.put(parts[0], parts[1]);
            }        
            
            return map;
        }
        return null;
    }
    
    protected SpringLiquibase createLiquibase(final ApplicationContext applicationContext, final LiquibaseTest annotation) 
            throws InstantiationException, IllegalAccessException {
        SpringLiquibase liquibase;
        try {
            liquibase = getBean(applicationContext, SpringLiquibase.class);
        }
        catch (NoSuchBeanDefinitionException e) {
            
            if (applicationContext != null) {
                final DataSource dataSource = applicationContext.getBean(DataSource.class);
                final Class<SpringLiquibase> springLiquibaseClass = annotation.springLiquibaseClass();
                liquibase = springLiquibaseClass.newInstance();
                liquibase.setDataSource(dataSource);
                liquibase.setResourceLoader(applicationContext);
            }
            else {
                return null;
            } 
        }
        return liquibase;
    }
}
