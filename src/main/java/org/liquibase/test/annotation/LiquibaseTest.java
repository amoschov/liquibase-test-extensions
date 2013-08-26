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
package org.liquibase.test.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import liquibase.integration.spring.SpringLiquibase;

/**
 * Support for executing liquibase commands during test without any special
 * liquibase command in test code.</p>
 * 
 * @author Alexander Moschov
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface LiquibaseTest {

    /**
     * Invoke liquibase command clean before a init/migrate call.</p>
     * 
     * Default: false
     */
    public boolean dropFirst() default false;

    /**
     * Support to add location to the default location settings.
     * <p/>
     * Default: empty
     * <p/>
     */
    public String changeLog() default "";

    /**
     * The contexts
     * */
    public String contexts() default "";

    /**
     * The change log parameters (e.g "key1:value1,key2:value2")
     * */
    public String parameters() default "";

    /**
     * The default schema 
     * */
    public String defaultSchema() default "";

    /**
     * Invoke liquibase command migrate  
     * */
    public boolean shouldRun() default true;

    /**
     * The default changeLog suffix 
     * */
    public String changeLogSuffix() default ".xml";

    public Class<SpringLiquibase> springLiquibaseClass() default SpringLiquibase.class;
    

}
