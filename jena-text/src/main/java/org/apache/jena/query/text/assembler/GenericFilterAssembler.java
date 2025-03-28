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

package org.apache.jena.query.text.assembler;

import java.util.List;

import org.apache.jena.assembler.Assembler;
import org.apache.jena.assembler.Mode;
import org.apache.jena.assembler.assemblers.AssemblerBase;
import org.apache.jena.atlas.logging.Log ;
import org.apache.jena.query.text.TextIndexException;
import org.apache.jena.query.text.assembler.Params.ParamSpec;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;

/**
 * Creates generic filters given a fully qualified Class name and a list
 * of parameters for a constructor of the Class.
 * <p>
 * The parameters may be of the following types:
 * <pre>
 *     text:TypeString        String
 *     text:TypeSet           org.apache.lucene.analysis.util.CharArraySet
 *     text:TypeFile          java.io.FileReader
 *     text:TypeInt           int
 *     text:TypeBoolean       boolean
 *     text:TypeTokenStream   TokenStream
 *     text:TypeAnalyzer      org.apache.lucene.analysis.Analyzer
 * </pre>
 * 
 * Although the list of types is not exhaustive it is a simple matter
 * to create a wrapper Analyzer that reads a file with information that can
 * be used to initialize any sort of parameters that may be needed for
 * a given Analyzer. The provided types cover the vast majority of cases.
 * <p>
 * For example, <code>org.apache.lucene.analysis.ja.JapaneseAnalyzer</code>
 * has a constructor with 4 parameters: a <code>UserDict</code>,
 * a <code>CharArraySet</code>, a <code>JapaneseTokenizer.Mode</code>, and a 
 * <code>Set&lt;String&gt;</code>. So a simple wrapper can extract the values
 * needed for the various parameters with types not available in this
 * extension, construct the required instances, and instantiate the
 * <code>JapaneseAnalyzer</code>.
 * <p>
 * Adding custom Analyzers such as the above wrapper analyzer is a simple
 * matter of adding the Analyzer class and any associated filters and tokenizer
 * and so on to the classpath for Jena - usually in a jar. Of course, all of 
 * the Analyzers that are included in the Lucene distribution bundled with Jena
 * are available as generic Analyzers as well.
 * <p>
 * Each parameter object is specified with:
 * <ul>
 * <li>an optional <code>text:paramName</code> that may be used to document which 
 * parameter is represented</li>
 * <li>a <code>text:paramType</code> which is one of: <code>text:TypeString</code>, 
 * <code>text:TypeSet</code>, <code>text:TypeFile</code>, <code>text:TypeInt</code>, 
 * <code>text:TypeBoolean</code>, <code>text:TypeAnalyzer</code>.</li>
 * <li>a text:paramValue which is an xsd:string, xsd:boolean or xsd:int or resource.</li>
 * </ul>
 * <p>
 * A parameter of type <code>text:TypeSet</code> <i>must have</i> a list of zero or 
 * more <code>String</code>s.
 * <p>
 * A parameter of type <code>text:TypeString</code>, <code>text:TypeFile</code>, 
 * <code>text:TypeBoolean</code>, <code>text:TypeInt</code> or <code>text:TypeAnalyzer</code> 
 * <i>must have</i> a single <code>text:paramValue</code> of the appropriate type.
 * <p>
 * Examples:
 * <pre>
 * {@code
    <#indexLucene> a text:TextIndexLucene ;
        text:directory <file:Lucene> ;
        text:entityMap <#entMap> ;
        text:defineAnalyzers (
            [text:addLang "sa-x-iast" ;
             text:analyzer [ . . . ]]
            [text:defineAnalyzer <#foo> ;
             text:analyzer [ . . . ]]
            [text:defineFilter <#bar> ;
             text:filter [
               a text:GenericFilter ;
               text:class "org.apache.jena.query.text.filter.SelectiveFoldingFilter" ;
               text:params (
                    [ text:paramName "whitelisted" ;
                      text:paramType text:TypeSet ;
                      text:paramValue ("ç") ]
                    )
              ]
            ]
        )
   }
 * </pre>
 */
public class GenericFilterAssembler extends AssemblerBase {
    /*
    <#indexLucene> a text:TextIndexLucene ;
        text:directory <file:Lucene> ;
        text:entityMap <#entMap> ;
        text:defineAnalyzers (
            [text:addLang "sa-x-iast" ;
             text:analyzer [ . . . ]]
            [text:defineAnalyzer <#foo> ;
             text:analyzer [ . . . ]]
            [text:defineFilter <#bar> ;
             text:filter [
               a text:GenericFilter ;
               text:class "org.apache.jena.query.text.filter.SelectiveFoldingFilter" ;
               text:params (
                    [ text:paramName "whitelisted" ;
                      text:paramType text:TypeSet ;
                      text:paramValue ("ç") ]
                    )
              ]
            ]
        )
     */

    @Override
    public FilterSpec open(Assembler a, Resource root, Mode mode) {
        if (root.hasProperty(TextVocab.pClass)) {
            // text:class is expected to be a string literal
            String className = root.getProperty(TextVocab.pClass).getString();

            // is the class accessible?
            Class<?> clazz = null;
            try {
                clazz = Class.forName(className);
            } catch (ClassNotFoundException e) {
                Log.error(this, "Filter class " + className + " not found. " + e.getMessage(), e);
                return null;
            }

            // Is the class an Analyzer?
            if (!TokenFilter.class.isAssignableFrom(clazz)) {
                Log.error(this, clazz.getName() + " has to be a subclass of " + TokenFilter.class.getName());
                return null;
            }

            if (root.hasProperty(TextVocab.pParams)) {
                RDFNode node = root.getProperty(TextVocab.pParams).getObject();
                if (! node.isResource()) {
                    throw new TextIndexException("text:params must be a list of parameter resources: " + node);
                }

                List<ParamSpec> specs = Params.getParamSpecs((Resource) node);

                // split the param specs into classes and values for constructor lookup
                // add an initial param for the TokenStream source. The source value is
                // set to null and the actual value supplied in ConfigurableAnalyzer when
                // used.
                final Class<?> paramClasses[] = new Class<?>[specs.size()+1];
                paramClasses[0] = TokenStream.class;
                final Object paramValues[] = new Object[specs.size()+1];
                paramValues[0] = null;
                for (int i = 0; i < specs.size(); i++) {
                    ParamSpec spec = specs.get(i);
                    paramClasses[i+1] = spec.getValueClass();
                    paramValues[i+1] = spec.getValue();
                }

                // Create spec for new filter
                return new FilterSpec(clazz, paramClasses, paramValues);

            } else {
                // use the TokenStream constructor for the new filter
                return new FilterSpec(clazz, new Class<?>[] { TokenStream.class }, new Object[] { null });
            }
        } else {
            throw new TextIndexException("text:class property is required by GenericFilter: " + root);
        }
    }
    
    public static class FilterSpec {
        public Class<?> clazz;
        public Class<?>[] paramClasses;
        public Object[] paramValues;
        
        public FilterSpec(Class<?> clazz, Class<?>[] paramClasses, Object[] paramValues) {
            this.clazz = clazz;
            this.paramClasses = paramClasses;
            this.paramValues = paramValues;
        }
    }
}
