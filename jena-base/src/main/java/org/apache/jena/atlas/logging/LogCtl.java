/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.jena.atlas.logging;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import org.apache.jena.atlas.AtlasException;
import org.slf4j.Logger;

/**
 * Setup and control of logging.
 * Sources of configuration:
 * <ul>
 * <li>Standard setup (e.g. for log4j2, property {@code log4j.configurationFile}
 * <li>jena-cmds: the shell scripts set logging to "apache-jena/log4j2.properties." (uses stderr)
 * <li>Default logging for log4j2: java resource src/main/resources/log4j-jena.properties (uses stdout)
 * </ul>
 * @implNote
 * This needs access to log4j2 binaries including log4j-core, which is encapsulated in LogCtlLog4j2.
 */
public class LogCtl {
    private static final boolean hasLog4j2   = hasClass("org.apache.logging.slf4j.Log4jLoggerFactory");
    private static final boolean hasLog4j1   = hasClass("org.slf4j.impl.Log4jLoggerFactory");
    // JUL always present but needs slf4j adapter.
    private static final boolean hasJUL      = hasClass("org.slf4j.impl.JDK14LoggerFactory");
    private static final boolean hasLogback  = hasClass("ch.qos.logback.core.LogbackException");
    // Put per-logging system code in separate classes to avoid needing them on the classpath.

    private static boolean hasClass(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public static void set(Logger logger, String level) {
        setLevel(logger.getName(), level);
    }

    public static void setLevel(Class<? > cls, String level) {
        setLevel(cls.getName(), level);
    }

    public static void setLevel(Logger logger, String level) {
        setLevel(logger.getName(), level);
    }

    public static void setLevel(String logger, String level) {
        // setLevelLog4j1(logger,level);
        setLevelLog4j2(logger, level);
        setLevelJUL(logger, level);
    }

    public static String getLevel(Logger logger) {
        return getLevel(logger.getName());
    }

    public static String getLevel(Class<? > logger) {
        return getLevel(logger.getName());
    }

    public static String getLevel(String logger) {
        String s2 = getLevelLog4j2(logger);
        if ( s2 != null )
            return s2;
        String s3 = getLevelJUL(logger);
        if ( s3 != null )
            return s3;
        return null;
    }

    static private String getLevelLog4j2(String logger) {
        if ( !hasLog4j2 )
            return null;
        return LogCtlLog4j2.getLoggerlevel(logger);
    }

    static private String getLevelJUL(String logger) {
        if ( ! hasJUL )
            return null;
        return LogCtlJUL.getLevelJUL(logger);
    }

    private static void setLevelJUL(String logger, String levelName) {
        if ( ! hasJUL )
            return ;
        LogCtlJUL.setLevelJUL(logger, levelName);
    }

    private static void setLevelLog4j2(String logger, String levelName) {
        if ( !hasLog4j2 )
            return;
        LogCtlLog4j2.setLoggerlevel(logger, levelName);
    }

    /**
     * Turn on a logger (all levels). Works for Log4j and Java logging as the logging
     * provider to Apache common logging or slf4j.
     */
    public static void enable(Logger logger) {
        enable(logger.getName());
    }

    public static void enable(String logger) {
        setLevel(logger, "all");
    }

    /**
     * Turn on a logger (all levels). Works for Log4j and Java logging as the logging
     * provider to Apache common logging or slf4j.
     */
    public static void enable(Class<? > logger) {
        setLevel(logger.getName(), "ALL");
    }

    /**
     * Turn on a logger (all levels). Works for Log4j and Java logging as the logging
     * provider to Apache common logging or slf4j.
     */
    public static void disable(Logger logger) {
        setLevel(logger.getName(), "OFF");
    }

    /**
     * Turn on a logger (all levels). Works for Log4j and Java logging as the logging
     * provider to Apache common logging or slf4j.
     */
    public static void disable(String logger) {
        setLevel(logger, "OFF");
    }

    /**
     * Turn on a logger (all levels). Works for Log4j and Java logging as the logging
     * provider to Apache common logging or slf4j.
     */
    public static void disable(Class<? > logger) {
        setLevel(logger.getName(), "OFF");
    }

    /**
     * Set to info level. Works for Log4j and Java logging as the logging provider to
     * Apache common logging or slf4j.
     */
    public static void setInfo(String logger) {
        setLevel(logger, "info");
    }

    /**
     * Set to info level. Works for Log4j and Java logging as the logging provider to
     * Apache common logging or slf4j.
     */
    public static void setInfo(Class<? > logger) {
        setLevel(logger.getName(), "info");
    }

    /**
     * Set to warning level. Works for Log4j and Java logging as the logging provider
     * to Apache common logging or slf4j.
     */
    public static void setWarn(String logger) {
        setLevel(logger, "warn");
    }

    /**
     * Set to warning level. Works for Log4j and Java logging as the logging provider
     * to Apache common logging or slf4j.
     */
    public static void setWarn(Class<? > logger) {
        setLevel(logger.getName(), "warn");
    }

    /**
     * Set to error level. Works for Log4j and Java logging as the logging provider
     * to Apache common logging or slf4j.
     */
    public static void setError(String logger) {
        setLevel(logger, "error");
    }

    /**
     * Set to error level. Works for Log4j and Java logging as the logging provider
     * to Apache common logging or slf4j.
     */
    public static void setError(Class<? > logger) {
        setLevel(logger.getName(), "error");
    }

    // ---- Setup

    /**
     * Set logging.
     * <p>
     * Normally, the logging provider mechanism should be used.
     * This call will insert some kind of logging set for JUL and Log4j2
     * when no configuration is setup; output is to stdout.
     * <p>
     * Ideally, initialize the logging provider using the mechanism specific to that provider.
     * For example, see the <a href="https://logging.apache.org/log4j/2.x/manual/configuration.html">log4j2 configuration documentation</a>.
     * <p>
     * To set application logging, choose one of:
     * <ul>
     * <li>For JUL logging, have a dependency on artifact {@code org.slf4j:slf4j-jdk14}.
     * <li>For log4j2 logging, have a dependency on artifact {@code org.apache.logging.log4j:log4j-slf4j-impl}.
     * </ul>
     */
    public static void setLogging() {
        // Assumes log4j2 or JUL (and slf4j adapters) on the classpath.
        if ( hasLog4j2 ) {
            setLog4j2();
            return;
        }
        if ( hasJUL ) {
            setJavaLogging();
            return;
        }
    }

    /**
     * @deprecated Use {@link #setLogging}.
     */
    @Deprecated(forRemoval = true)
    public static void setCmdLogging() {
        setLogging();
    }

    // ---- log4j2.

    /** The log4j2 configuration file - must be a file or URL, not a classpath java resource. */
    public static final String log4j2ConfigFileProperty = "log4j2.configurationFile";
    /** Legacy name for log4j2 configuration file */
    public static final String log4j2ConfigFilePropertyLegacy = "log4j.configurationFile";

    private static final String[] log4j2files = {"log4j2.properties", "log4j2.xml"};

    /**
     * Environment variable for debugging logging initialization.
     * Set "true" to get trace on stderr.
     */
    // Must be final for static LogLogging to work.
    public static final String envLogLoggingProperty = "JENA_LOGLOGGING";

    /**
     * Property variable for debugging logging initialization. Set "true" to get
     * trace on stderr.
     */
    // Must be final for static LogLogging to work.
    public static final String logLoggingProperty = "jena.logLogging";

    private static final boolean LogLogging =
            System.getenv(envLogLoggingProperty) != null ||
            System.getProperty(logLoggingProperty) != null;

    private static void logLogging(String fmt, Object ... args) {
        if ( LogLogging ) {
            System.err.print("Jena Logging: ");
            System.err.printf(fmt, args);
            System.err.println();
        }
    }

    private static boolean loggingInitialized   = false;

    /**
     * Setup log4j2, including looking for a file "log4j2.properties" or "log4j2.xml"
     * in the current working directory.
     * @see #setLogging()
     */
    public static void setLog4j2() {
        if ( loggingInitialized )
            return;
        loggingInitialized = true;

        if ( ! isSetLog4j2property() ) {
            logLogging("Set logging");
            setLog4j2property();
            if ( isSetLog4j2property() ) {
                return;
            }
            // Nothing found - built-in default.
            logLogging("Log4j2: built-in default");
            LogCtlLog4j2.reconfigureLog4j2fromString(LogCtlLog4j2.log4j2setup, LogCtlLog4j2.SyntaxHint.PROPERTIES);
        } else {
            if ( isSetLog4j2property(log4j2ConfigFilePropertyLegacy) )
                logLogging("Already set: %s=%s", log4j2ConfigFilePropertyLegacy, System.getProperty(log4j2ConfigFilePropertyLegacy));
            else
                logLogging("Already set: %s=%s", log4j2ConfigFileProperty, System.getProperty(log4j2ConfigFileProperty));
        }
    }

    /* package */ static boolean isSetLog4j2property() {
        // Either name,
        return isSetLog4j2property(log4j2ConfigFileProperty) ||
               isSetLog4j2property(log4j2ConfigFilePropertyLegacy);
    }

    private static boolean isSetLog4j2property(String systemProperty) {
        return System.getProperty(systemProperty) != null;
    }


    /** Set log4j, looking for files */
    /*package*/ static void setLog4j2property() {
        if ( isSetLog4j2property() )
            return;
        for ( String fn : log4j2files ) {
            File f = new File(fn);
            if ( f.exists() ) {
                String fileURL = "file:"+fn;
                logLogging("Set %s=%s", log4j2ConfigFileProperty, fileURL);
                System.setProperty(log4j2ConfigFileProperty, fileURL);
                return;
            }
        }
    }

    // ---- java.util.logging - because that's always present.
    // Need: org.slf4j:slf4j-jdk14

    private static String JUL_PROPERTY      = "java.util.logging.configuration";
    /**
     * Setup java.util.logging if it has not been set before; otherwise do nothing.
     */
    public static void setJavaLogging() {
        logLogging("Ensure java.util.logging setup");
        if ( System.getProperty(JUL_PROPERTY) != null ) {
            logLogging(JUL_PROPERTY+"="+System.getProperty(JUL_PROPERTY));
            return;
        }
        logLogging("java.util.logging reset logging");
        LogCtlJUL.resetJavaLogging();
    }

    /**
     * Setup java.util.logging with the configuration from a file.
     * @param filename
     */
    public static void setJavaLogging(String filename) {
        try {
            InputStream details = new FileInputStream(filename);
            details = new BufferedInputStream(details);
            LogCtlJUL.readJavaLoggingConfiguration(details);
        } catch (Exception ex) {
            throw new AtlasException(ex);
        }
    }

    /** Execute with a given logging level. */
    public static void withLevel(Logger logger, String execLevel, Runnable action) {
        String currentLevel = getLevel(logger);
        try {
            setLevel(logger, execLevel);
            action.run();
        } finally {
            setLevel(logger, currentLevel);
        }
    }
}
