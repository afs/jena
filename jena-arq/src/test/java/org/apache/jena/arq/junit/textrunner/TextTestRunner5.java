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

package org.apache.jena.arq.junit.textrunner;

import java.util.function.Function;

import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.*;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.LoggingListener;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;

import org.apache.jena.arq.junit.EarlReport;
import org.apache.jena.arq.junit.manifest.EarlReporter;
import org.apache.jena.arq.junit.manifest.ManifestEntry;
import org.apache.jena.atlas.io.IndentedWriter;

public class TextTestRunner5 {

    public static void runOne(String manifestFile, Function<ManifestEntry, Runnable> testMaker) {
        runInternal(null, manifestFile);
    }

    public static void runOne(EarlReport report, String manifestFile, Function<ManifestEntry, Runnable> testMaker) {
        runInternal(report, manifestFile);
    }

    static void runInternal(EarlReport earlReport, String filename) {
        try ( IndentedWriter out = IndentedWriter.stdout.clone(); ) {
            runInternal0(out, earlReport, filename);
        }
    }

    // Either an EARL report xor text output.
    static void runInternal0(IndentedWriter out, EarlReport earlReport, String filename) {
        boolean produceEarlReport = earlReport!=null ;
        if ( produceEarlReport ) {
            EarlReporter.setEarlReport(earlReport);
        }

        SummaryGeneratingListener summaryListener = new SummaryGeneratingListener();
        LoggingListener loggerListener = LoggingListener.forBiConsumer((throwable,string)->{
            out.println(string.get());
        });

        //Best way?
        // JUnit creates a ManifestHolder, no arguments, which has one TestFactory method.
        // ManifestHolder.INIT picks up the manifest file from the configuration parameter,
        LauncherDiscoveryListener injectManifest = new ManifestHolder.INIT();

        LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                .selectors(DiscoverySelectors.selectClass(ManifestHolder.class))
                .configurationParameter(ManifestHolder.MANIFEST, filename)
                .listeners(injectManifest)
                .build();

        Launcher launcher = LauncherFactory.create();
        TestPlan testPlan = launcher.discover(request);

        ExecutionStats executionStats = new ExecutionStats();
        PrintExecutionListener printExecListener = new PrintExecutionListener(out);

        if ( produceEarlReport ) {
            // Build report, no output.
            launcher.registerTestExecutionListeners(executionStats);
        } else {
            launcher.registerTestExecutionListeners(executionStats, printExecListener, summaryListener);
        }

        // Run!
        launcher.execute(request);

        // For skips tests.
        TestExecutionSummary summary = summaryListener.getSummary();

        if ( summary != null ) {
            //summary.printTo(new PrintWriter(System.out));
            out.println();
            out.println("Manifests:  "+executionStats.getContainerCount());
            out.println("Tests pass: "+executionStats.getTestPasses());
            out.println("Tests fail: "+executionStats.getTestFailures());
            if ( summary.getTestsSkippedCount() > 0 )
                out.println("Tests skip: "+summary.getTestsSkippedCount());
        }

        if ( produceEarlReport ) {
            //RDFWriter.source(earlReport.getModel()).format(RDFFormat.TURTLE).output(System.out);
            EarlReporter.clearEarlReport();
        }
    }
}
