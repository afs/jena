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

package org.apache.jena.riot.system.stream ;

import java.util.ArrayList ;
import java.util.Collections ;
import java.util.List ;

import org.apache.jena.atlas.lib.Lib;
import org.apache.jena.atlas.web.TypedInputStream ;
import org.apache.jena.riot.RiotNotFoundException ;
import org.apache.jena.riot.SysRIOT;
import org.apache.jena.sparql.util.Context;
import org.slf4j.Logger ;
import org.slf4j.LoggerFactory ;

/**
 * Management of stream opening, including redirecting through a location mapper
 * whereby a name (e.g. URL) is redirected to another name (e.g. local file).
 * Includes filename to IRI, handling ".gz" and "-"
 */

public class StreamManager {
    private static Logger        log           = LoggerFactory.getLogger(StreamManager.class) ;

    public static boolean        logAllLookups = true ;

    private List<Locator>        handlers      = new ArrayList<>() ;
    private LocationMapper       mapper        = null ;

    private static StreamManager globalStreamManager ;

    /**
     * Return a default configuration StreamManager with a {@link LocatorFile},
     * {@link LocatorHTTP}, {@link LocatorFTP} and {@link LocatorClassLoader}
     */
    public static StreamManager createStd() {
        StreamManager streamManager = new StreamManager()
            .locationMapper(JenaIOEnvironment.getLocationMapper())
            .addLocator(new LocatorFile())
            .addLocator(new LocatorHTTP())
            .addLocator(new LocatorFTP())
            .addLocator(new LocatorClassLoader(StreamManager.class.getClassLoader()));
        return streamManager ;
    }

    /**
     * Return the global {@code StreamManager}.
     */
    public static StreamManager get() {
        return globalStreamManager ;
    }

    /**
     * Return the {@code StreamManager} in a context, or the global one if the context is
     * null or does not contain a valid entry for a {@code StreamManager}.
     * <p>
     * The {@code StreamManager} is keyed in the context by
     * {@link SysRIOT#sysStreamManager}.
     */
    public static StreamManager get(Context context) {
        if ( context == null )
            return get();
        try {
            if ( context.isDefined(SysRIOT.sysStreamManager))
                return (StreamManager)context.get(SysRIOT.sysStreamManager);
        }
        catch (ClassCastException ex) {
            log.warn("Context symbol '" + SysRIOT.sysStreamManager + "' is not a " + Lib.classShortName(StreamManager.class));
        }
        return get();
    }

    /**
     * Set the global {@code StreamManager}.
     */
    public static void setGlobal(StreamManager streamManager) {
        globalStreamManager = streamManager ;
    }

    static { setGlobal(createStd()) ; }

    /** Create a {@code StreamManager} with no locator or location mapper. */
    public StreamManager() {}

    /** Create a deep copy of this StreamManager */
    @Override
    public StreamManager clone() {
        return clone(this) ;
    }

    private static StreamManager clone(StreamManager other) {
        StreamManager sm = new StreamManager() ;
        sm.handlers.addAll(other.handlers) ;
        sm.mapper = other.mapper == null ? null : other.mapper.clone() ;
        return sm ;
    }

    /**
     * Open a file using the locators of this StreamManager.
     * Returns null if not found.
     */
    public TypedInputStream open(String filenameOrURI) {
        if ( log.isDebugEnabled() )
            log.debug("open(" + filenameOrURI + ")") ;

        String uri = mapURI(filenameOrURI) ;

        if ( log.isDebugEnabled() && !uri.equals(filenameOrURI) )
            log.debug("open: mapped to " + uri) ;

        return openNoMapOrNull(uri) ;
    }

    /** Test whether a mapping exists */
    public boolean hasMapping(String filenameOrURI) {
        return mapper.containsMapping(filenameOrURI);
    }

    /** Apply the mapping of a filename or URI */
    public String mapURI(String filenameOrURI) {
        if ( mapper == null )
            return filenameOrURI ;

        String uri = mapper.altMapping(filenameOrURI, null) ;

        if ( uri == null ) {
            if ( StreamManager.logAllLookups && log.isDebugEnabled() )
                log.debug("Not mapped: " + filenameOrURI) ;
            uri = filenameOrURI ;
        } else {
            if ( log.isDebugEnabled() )
                log.debug("Mapped: " + filenameOrURI + " => " + uri) ;
        }
        return uri ;
    }

    /**
     * Open a file using the locators of this StreamManager but without location
     * mapping. Throws RiotNotFoundException if not found.
     */
    public TypedInputStream openNoMap(String filenameOrURI) {
        TypedInputStream in = openNoMapOrNull(filenameOrURI) ;
        if ( in == null )
            throw new RiotNotFoundException(filenameOrURI) ;
        return in ;
    }

    /**
     * Open a file using the locators of this StreamManager without location
     * mapping. Return null if not found
     */

    public TypedInputStream openNoMapOrNull(String filenameOrURI) {
        for (Locator loc : handlers) {
            TypedInputStream in = loc.open(filenameOrURI) ;
            if ( in != null ) {
                if ( log.isDebugEnabled() )
                    log.debug("Found: " + filenameOrURI + " (" + loc.getName() + ")") ;
                return in ;
            }
        }
        return null ;
    }

    /**
     * Set the location mapping
     * @deprecated Use {@link #locationMapper(LocationMapper)}
     */
    @Deprecated(forRemoval = true)
    public void setLocationMapper(LocationMapper mapper) {
        this.mapper = mapper ;
    }

    /**
     * Get the location mapping
     * @deprecated Use {@link #locationMapper()}
     */
    @Deprecated(forRemoval = true)
    public LocationMapper getLocationMapper() {
        return mapper ;
    }

    /** Set the location mapping */
    public StreamManager locationMapper(LocationMapper mapper) {
        this.mapper = mapper ;
        return this;
    }

    /** Set the location mapping */
    public LocationMapper locationMapper() {
        return mapper;
    }


    /** Return an immutable list of all the handlers */
    public List<Locator> locators() {
        return Collections.unmodifiableList(handlers) ;
    }

    /** Remove a locator. */
    public void remove(Locator loc) {
        handlers.remove(loc) ;
    }

    /** Remove all locators */
    public void clearLocators() {
        handlers.clear() ;
    }

    /**
     * Add a locator to the end of the locators list.
     * Returns {@code this} StreamManager.
     */
    public StreamManager addLocator(Locator loc) {
        handlers.add(loc) ;
        return this;
    }
}
