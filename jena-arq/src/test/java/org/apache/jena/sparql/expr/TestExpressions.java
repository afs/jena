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

package org.apache.jena.sparql.expr;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QueryParseException;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.engine.binding.BindingFactory;
import org.apache.jena.sparql.function.FunctionEnv;
import org.apache.jena.sparql.function.FunctionEnvBase;
import org.apache.jena.sparql.util.ExprUtils;
import org.apache.jena.sys.JenaSystem;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.XSD;

/** Break expression testing suite into parts
* @see TestExpressions
* @see TestExpressions2
* @see TestExpressions3
* @see TestExprLib
* @see TestNodeValue
*/
public class TestExpressions
{
    static { JenaSystem.init(); }
    public final static int NO_FAILURE    = 100;
    public final static int PARSE_FAIL    = 250;   // Parser should catch it.
    public final static int EVAL_FAIL     = 200;   // Parser should pass it but eval should fail it

    static boolean flagVerboseWarning;
    @BeforeAll static public void beforeClass() {
        flagVerboseWarning = NodeValue.VerboseWarnings;
        NodeValue.VerboseWarnings = false;
    }

    @AfterAll static public void afterClass() { NodeValue.VerboseWarnings = flagVerboseWarning; }

    @Test public void var_1() { testVar("?x", "x"); }
    @Test public void var_2() { testVar("$x", "x"); }
    @Test public void var_3() { testVar("?name", "name"); }
    @Test public void var_4() { testVar("$name", "name"); }

    @Test public void var_5() { testVar("?x_", "x_"); }
    @Test public void var_6() { testVar("?x.", "x"); }
    @Test public void var_7() { testVar("?x.x", "x"); }
    @Test public void var_8() { testVar("?0", "0"); }
    @Test public void var_9() { testVar("?0x", "0x"); }
    @Test public void var_10() { testVar("?x0", "x0"); }
    @Test public void var_11() { testVar("?_", "_"); }

    @Test public void syntax_good_1()  { testSyntax("?x11"); }
    @Test public void syntax_good_2()  { testSyntax("1+2"); }

    @Test public void syntax_bad_2()  { assertThrows(QueryParseException.class, ()-> testSyntax("1:b") ); }
    @Test public void syntax_bad_3()  { assertThrows(QueryParseException.class, ()-> testSyntax("?") ); }
    @Test public void syntax_bad_4()  { assertThrows(QueryParseException.class, ()-> testSyntax("??") ); }
    @Test public void syntax_bad_5()  { assertThrows(QueryParseException.class, ()-> testSyntax("?.") ); }
    @Test public void syntax_bad_6()  { assertThrows(QueryParseException.class, ()-> testSyntax("?#") ); }
    @Test public void syntax_bad_7()  { assertThrows(QueryParseException.class, ()-> testSyntax("_:") ); }
    @Test public void syntax_bad_8()  { assertThrows(QueryParseException.class, ()-> testSyntax("[]") ); }

    @Test public void numeric_1() { testNumeric("7", 7); }
    @Test public void numeric_2() { testNumeric("-3", -3); }
    @Test public void numeric_3() { testNumeric("+2", 2); }
    @Test public void numeric_4() { assertThrows(QueryParseException.class, ()-> testNumeric("0xF", 0xF) ); }
    @Test public void numeric_5() { assertThrows(QueryParseException.class, ()-> testNumeric("0x12", 0x12) ); }
    @Test public void numeric_6() { testNumeric("3--4", 3-(-4)); }
    @Test public void numeric_7() { testNumeric("3++4", 3+(+4)); }
    @Test public void numeric_8() { testNumeric("3-+4", 3-+4); }
    @Test public void numeric_9() { testNumeric("3+-4", 3+-4); }
    @Test public void numeric_10() { testNumeric("3-(-4)", 3-(-4)); }
    @Test public void numeric_11() { testNumeric("3+4+5", 3+4+5); }
    @Test public void numeric_12() { testNumeric("(3+4)+5", 3+4+5); }
    @Test public void numeric_13() { testNumeric("3+(4+5)", 3+4+5); }
    @Test public void numeric_14() { testNumeric("3*4+5", 3*4+5); }
    @Test public void numeric_15() { testNumeric("3*(4+5)", 3*(4+5)); }
    @Test public void numeric_16() { testNumeric("10-3-5", 10-3-5); }
    @Test public void numeric_17() { testNumeric("(10-3)-5", (10-3)-5); }
    @Test public void numeric_18() { testNumeric("10-(3-5)", 10-(3-5)); }
    @Test public void numeric_19() { testNumeric("10-3+5", 10-3+5); }
    @Test public void numeric_20() { testNumeric("10-(3+5)", 10-(3+5)); }
    @Test public void numeric_21() { assertThrows(QueryParseException.class, ()-> testNumeric("1<<2", 1<<2) ); }
    @Test public void numeric_22() { assertThrows(QueryParseException.class, ()-> testNumeric("1<<2<<2", 1<<2<<2) ); }
    @Test public void numeric_23() { assertThrows(QueryParseException.class, ()-> testNumeric("10000>>2", 10000>>2) ); }
    @Test public void numeric_24() { testNumeric("1.5 + 2.5", 1.5+2.5); }
    @Test public void numeric_25() { testNumeric("1.5 + 2", 1.5+2); }
    @Test public void numeric_26() { testNumeric("4111222333444", 4111222333444L); }
    @Test public void numeric_27() { testNumeric("1234 + 4111222333444", 1234 + 4111222333444L); }

    @Test public void numeric_28() { testNumeric("+2.5", new BigDecimal("+2.5")); }
    @Test public void numeric_29() { testNumeric("-2.5", new BigDecimal("-2.5")); }
    @Test public void numeric_30() { testNumeric("10000000000000000000000000000+1", new BigInteger("10000000000000000000000000001")); }
    @Test public void numeric_31() { testNumeric("-10000000000000000000000000000+1", new BigInteger("-9999999999999999999999999999")); }

    @Test public void boolean_1() { testBoolean("4111222333444 > 1234", 4111222333444L > 1234); }
    @Test public void boolean_2() { testBoolean("4111222333444 < 1234", 4111222333444L < 1234L); }
    @Test public void boolean_3() { testBoolean("1.5 < 2", 1.5 < 2 ); }
    @Test public void boolean_4() { testBoolean("1.5 > 2", 1.5 > 2 ); }
    @Test public void boolean_5() { testBoolean("1.5 < 2.3", 1.5 < 2.3 ); }
    @Test public void boolean_6() { testBoolean("1.5 > 2.3", 1.5 > 2.3 ); }
    @Test public void boolean_7() { testBoolean("'true'^^<"+XSDDatatype.XSDboolean.getURI()+">", true); }
    @Test public void boolean_8() { testBoolean("'1'^^<"+XSDDatatype.XSDboolean.getURI()+">", true); }
    @Test public void boolean_9() { testBoolean("'false'^^<"+XSDDatatype.XSDboolean.getURI()+">", false); }
    @Test public void boolean_10() { testBoolean("'0'^^<"+XSDDatatype.XSDboolean.getURI()+">", false); }
    @Test public void boolean_11() { testBoolean("1 || false", true); }
    @Test public void boolean_12() { testBoolean("'foo'  || false", true); }
    @Test public void boolean_13() { testBoolean("0 || false", false); }
    @Test public void boolean_14() { testBoolean("'' || false", false); }
    @Test public void boolean_15() { assertThrows(ExprEvalException.class, ()-> testEval("!'junk'^^<urn:unknown:uri>") ); }
    @Test public void boolean_16() { testBoolean("2 < 3", 2 < 3); }
    @Test public void boolean_17() { testBoolean("2 > 3", 2 > 3); }
    @Test public void boolean_18() { testBoolean("(2 < 3) && (3<4)", (2 < 3) && (3<4)); }
    @Test public void boolean_19() { testBoolean("(2 < 3) && (3>=4)", (2 < 3) && (3>=4)); }
    @Test public void boolean_20() { testBoolean("(2 < 3) || (3>=4)", (2 < 3) || (3>=4)); }

    // ?x is unbound in the next few tests
    @Test public void boolean_21() { testBoolean("(2 < 3) || ?x > 2", true); }
    @Test public void boolean_22() { assertThrows(ExprEvalException.class, ()-> testEval("(2 > 3) || ?x > 2") ); }
    @Test public void boolean_23() { testBoolean("(2 > 3) && ?x > 2", false); }
    @Test public void boolean_24() { assertThrows(ExprEvalException.class, ()-> testEval("(2 < 3) && ?x > 2") ); }
    @Test public void boolean_25() { testBoolean("?x > 2 || (2 < 3)", true); }
    @Test public void boolean_26() { assertThrows(ExprEvalException.class, ()-> testEval("?x > 2 || (2 > 3)") ); }
    @Test public void boolean_27() { assertThrows(ExprEvalException.class, ()-> testEval("?x > 2 && (2 < 3)") ); }
    @Test public void boolean_28() { testBoolean("?x > 2 && (2 > 3)", false); }
    @Test public void boolean_29() { assertThrows(ExprEvalException.class, ()-> testEval("! ?x ") ); }

    @Test public void boolean_30() { testBoolean("! true ", false); }
    @Test public void boolean_31() { testBoolean("! false ", true); }
    @Test public void boolean_32() { testBoolean("2 = 3", 2 == 3); }
    @Test public void boolean_33() { testBoolean("!(2 = 3)", !(2 == 3)); }
    @Test public void boolean_34() { testBoolean("'2' = 2", false); }
    @Test public void boolean_35() { testBoolean("2 = '2'", false); }
    @Test public void boolean_36() { assertThrows(ExprEvalException.class, ()-> testEval("2 < '3'") ); }
    @Test public void boolean_37() { assertThrows(ExprEvalException.class, ()-> testEval("'2' < 3") ); }
    @Test public void boolean_38() { testBoolean("\"fred\" != \"joe\"", true ); }
    @Test public void boolean_39() { testBoolean("\"fred\" = \"joe\"", false ); }
    @Test public void boolean_40() { testBoolean("\"fred\" = \"fred\"", true ); }
    @Test public void boolean_41() { testBoolean("\"fred\" = 'fred'", true ); }
    @Test public void boolean_42() { testBoolean("true = true", true); }
    @Test public void boolean_43() { testBoolean("false = false", true); }
    @Test public void boolean_44() { testBoolean("true = false", false); }
    @Test public void boolean_45() { testBoolean("true > true", false); }
    @Test public void boolean_46() { testBoolean("true >= false", true); }
    @Test public void boolean_47() { testBoolean("false > false", false); }
    @Test public void boolean_48() { testBoolean("false >= false", true); }
    @Test public void boolean_49() { testBoolean("true > false", true); }
    @Test public void boolean_50() { testBoolean("1 = true", false); }
    @Test public void boolean_51() { testBoolean("1 != true", true); }
    @Test public void boolean_52() { testBoolean("'a' != false", true); }
    @Test public void boolean_53() { testBoolean("0 != false", true); }
    @Test public void boolean_54() { testBoolean(dateTime1+" = "+dateTime2, true); }
    @Test public void boolean_55() { testBoolean(dateTime1+" <= "+dateTime2, true); }
    @Test public void boolean_56() { testBoolean(dateTime1+" >= "+dateTime2, true); }
    @Test public void boolean_57() { testBoolean(dateTime3+" < "+dateTime1, true); }
    @Test public void boolean_58() { testBoolean(dateTime3+" > "+dateTime1, false); }
    @Test public void boolean_59() { testBoolean(dateTime4+" < "+dateTime1, false); }
    @Test public void boolean_60() { testBoolean(dateTime4+" > "+dateTime1, true); }
    @Test public void boolean_61() { testBoolean(time1+" = "+time2, true); }
    @Test public void boolean_62() { testBoolean(time1+" <= "+time2, true); }
    @Test public void boolean_63() { testBoolean(time1+" >= "+time2, true); }
    @Test public void boolean_64() { testBoolean(time3+" < "+time2, false); }
    @Test public void boolean_65() { testBoolean(time3+" > "+time2, true); }
    @Test public void boolean_66() { testBoolean(time4+" < "+time2, true); }
    @Test public void boolean_67() { testBoolean(time4+" > "+time2, false); }

    // xsd:dateTimeStamp
    static String dateTimeStamp1 = "'1999-10-26T19:32:52+00:00'^^<"+XSDDatatype.XSDdateTimeStamp.getURI()+">";
    static String dateTimeStamp2 = "'2000-01-01T00:00:00+00:00'^^<"+XSDDatatype.XSDdateTimeStamp.getURI()+">";
    @Test public void boolean_68() { testBoolean(dateTimeStamp1+" < "+dateTimeStamp2, true); }

    @Test public void boolean_70() { testBoolean("isNumeric(12)", true); }
    @Test public void boolean_71() { testBoolean("isNumeric('12')", false); }
    @Test public void boolean_72() { testBoolean("isNumeric('12'^^<"+XSDDatatype.XSDbyte.getURI()+">)", true); }
    @Test public void boolean_73() { testBoolean("isNumeric('1200'^^<"+XSDDatatype.XSDbyte.getURI()+">)", false); }

    @Test
    public void boolean_74()       { assertThrows(ExprEvalException.class, ()-> testBoolean("isNumeric(?x)", true) ); }

    // 24:00:00
    // Equal
    static String dateTime1999_24 = "'1999-12-31T24:00:00Z'^^<"+XSDDatatype.XSDdateTime.getURI()+">";
    static String dateTime2000_00 = "'2000-01-01T00:00:00Z'^^<"+XSDDatatype.XSDdateTime.getURI()+">";

    static String time_24 = "'24:00:00'^^<"+XSDDatatype.XSDtime.getURI()+">";
    static String time_00 = "'00:00:00'^^<"+XSDDatatype.XSDtime.getURI()+">";

    @Test public void dateTime24_01() { testBoolean(dateTime1999_24+" = "+dateTime2000_00 , true); }
    @Test public void time24_01()     { testBoolean(time_24+" = "+time_00 , true); }

    static String duration1 =  "'P1Y1M1DT1H1M1S"+"'^^<"+XSDDatatype.XSDduration.getURI()+">";
    static String duration2 =  "'P2Y1M1DT1H1M1S"+"'^^<"+XSDDatatype.XSDduration.getURI()+">";
    static String duration3 =  "'P1Y1M1DT1H1M1S"+"'^^<"+XSDDatatype.XSDduration.getURI()+">";

    static String duration4 =  "'PT1H1M1S"+"'^^<"+XSDDatatype.XSDduration.getURI()+">";
    static String duration5 =  "'PT1H1M1.9S"+"'^^<"+XSDDatatype.XSDduration.getURI()+">";
    static String duration5a = "'PT61M1.9S"+"'^^<"+XSDDatatype.XSDduration.getURI()+">";
    static String duration5b = "'PT3661.9S"+"'^^<"+XSDDatatype.XSDduration.getURI()+">";

    static String duration7 = "'-PT1H"+"'^^<"+XSDDatatype.XSDduration.getURI()+">";
    static String duration8 = "'PT0H0M0S"+"'^^<"+XSDDatatype.XSDduration.getURI()+">";

    @Test public void duration_01() { testBoolean(duration1+" = "+duration1, true); }

    // Extended - these are not dayTime nor yearMonth.
    @Test public void duration_02() { testBoolean(duration1+" < "+duration2, true); }
    @Test public void duration_03() { testBoolean(duration1+" > "+duration2, false); }
    @Test public void duration_04() { testBoolean(duration1+" < "+duration2, true); }
    @Test public void duration_05() { testBoolean(duration1+" = "+duration3, true); }

    @Test public void duration_06() { testBoolean(duration1+" <= "+duration3, true); }
    @Test public void duration_07() { testBoolean(duration1+" >= "+duration3, true); }
    @Test public void duration_08() { testBoolean(duration7+" < "+duration8, true); }

    // duration5* are the same duration length, written differently
    @Test public void duration_10() { testBoolean(duration5+" > "+duration4, true); }
    @Test public void duration_11() { testBoolean(duration5a+" = "+duration5, true); }
    @Test public void duration_12() { testBoolean(duration5a+" = "+duration5b, true); }
    @Test public void duration_13() { testBoolean(duration5b+" = "+duration5, true); }
    @Test public void duration_14() { testBoolean(duration5a+" > "+duration4, true); }

    @Test public void URI_1()       { testURI("<a>",     baseNS+"a" ); }
    @Test public void URI_2()       { testURI("<a\\u00E9>",     baseNS+"a\u00E9" ); }
    @Test public void URI_3()       { testURI("ex:b",     exNS+"b" ); }
    @Test public void URI_4()       { testURI("ex:b_",    exNS+"b_" ); }
    @Test public void URI_5()       { testURI("ex:a_b",   exNS+"a_b" ); }
    @Test public void URI_6()       { testURI("ex:", exNS ); }

    @Test
    public void URI_7()             { assertThrows(QueryParseException.class, ()-> testURI("x.:", xNS) ); }

    @Test public void URI_8()       { testURI("rdf:_2",   rdfNS+"_2" ); }
    @Test public void URI_9()       { testURI("rdf:__2",  rdfNS+"__2" ); }
    @Test public void URI_10()      { testURI(":b",       dftNS+"b" ); }
    @Test public void URI_11()      { testURI(":", dftNS ); }
    // These assume \-u processing by JavaCC
    // Migration to processing like Turtle, in strings and URIs only.
//    @Test public void URI_12()      { testURI(":\\u00E9", dftNS+"\u00E9" ); }
//    @Test public void URI_13()      { testURI("\\u0065\\u0078:", exNS ); }
    @Test public void URI_14()      { testURI("select:a", selNS+"a" ); }

    @Test public void URI_15()      { testURI("ex:a.",   exNS+"a"); }
    @Test public void URI_16()      { testURI("ex:a.a",  exNS+"a.a"); }

    @Test
    public void URI_17()      { assertThrows(QueryParseException.class, ()-> testURI("x.:a.a",  xNS+"a.a") ); }

    @Test public void URI_18()      { testURI("ex:2",    exNS+"2" ); }
    @Test public void URI_19()      { testURI("ex:2ab_c",    exNS+"2ab_c" ); }
    @Test public void boolean_76()  { testBoolean("'fred'@en = 'fred'", false ); }
    @Test public void boolean_77()  { testBoolean("'fred'@en = 'bert'", false ); }
    @Test public void boolean_78()  { testBoolean("'fred'@en != 'fred'", true ); }
    @Test public void boolean_79()  { testBoolean("'fred'@en != 'bert'", true ); }
    @Test public void boolean_80()  { testBoolean("'chat'@en = 'chat'@fr", false ); }
    @Test public void boolean_81()  { testBoolean("'chat'@en = 'maison'@fr", false ); }
    @Test public void boolean_82()  { testBoolean("'chat'@en != 'chat'@fr", true ); }
    @Test public void boolean_83()  { testBoolean("'chat'@en != 'maison'@fr", true ); }
    @Test public void boolean_84()  { testBoolean("'chat'@en = 'chat'@EN", true ); }
    @Test public void boolean_85()  { testBoolean("'chat'@en = 'chat'@en-uk", false ); }
    @Test public void boolean_86()  { testBoolean("'chat'@en != 'chat'@EN", false ); }
    @Test public void boolean_87()  { testBoolean("'chat'@en != 'chat'@en-uk", true ); }
    @Test public void boolean_88()  { testBoolean("'chat'@en = <http://example/>", false ); }

    @Test
    public void URI_20()      { assertThrows(QueryParseException.class, ()-> testURI("()", RDF.nil.getURI()) ); }

    @Test public void boolean_89() { testBoolean("'fred'^^<type1> = 'fred'^^<type1>", true ); }
    @Test public void boolean_90() { assertThrows(ExprEvalException.class, ()-> testEval("'fred'^^<type1> != 'joe'^^<type1>" ) ); }
    @Test public void boolean_91() { assertThrows(ExprEvalException.class, ()-> testEval("'fred'^^<type1> = 'fred'^^<type2>" ) ); }
    @Test public void boolean_92() { assertThrows(ExprEvalException.class, ()-> testEval("'fred'^^<type1> != 'joe'^^<type2>" ) ); }
    @Test public void boolean_93() { testBoolean("'fred'^^<"+XSDDatatype.XSDstring.getURI()+"> = 'fred'", true ); }
    @Test public void boolean_94() { assertThrows(ExprEvalException.class, ()-> testEval("'fred'^^<type1> = 'fred'" ) ); }
    @Test public void boolean_95() { assertThrows(ExprEvalException.class, ()-> testEval("'fred'^^<type1> != 'fred'" ) ); }
    @Test public void boolean_96() { assertThrows(ExprEvalException.class, ()-> testBoolean("'21'^^<int> = '21'", true ) ); }
    @Test public void numeric_51() { testNumeric("'21'^^<"+XSDDatatype.XSDinteger.getURI()+">", 21); }
    @Test public void boolean_97() { testBoolean("'21'^^<"+XSDDatatype.XSDinteger.getURI()+"> = 21", true); }
    @Test public void boolean_98() { testBoolean("'21'^^<"+XSDDatatype.XSDinteger.getURI()+"> = 22", false); }
    @Test public void boolean_99() { testBoolean("'21'^^<"+XSDDatatype.XSDinteger.getURI()+"> != 21", false); }
    @Test public void boolean_100() { testBoolean("'21'^^<"+XSDDatatype.XSDinteger.getURI()+"> != 22", true); }
    @Test public void boolean_101() { assertThrows(ExprEvalException.class, ()-> testEval("'x'^^<type1>  = 21") ); }
    @Test public void boolean_102() { assertThrows(ExprEvalException.class, ()-> testEval("'x'^^<type1> != 21") ); }
    @Test public void boolean_103() { assertThrows(ExprEvalException.class, ()-> testEval("'x'^^<http://example/unknown> = true") ); }
    @Test public void boolean_104() { assertThrows(ExprEvalException.class, ()-> testEval("'x'^^<http://example/unknown> != true") ); }
    @Test public void boolean_105() { testBoolean("'x'^^<http://example/unknown> = 'x'^^<http://example/unknown>", true); }
    @Test public void boolean_106() { assertThrows(ExprEvalException.class, ()-> testEval("'x'^^<http://example/unknown> = 'y'^^<http://example/unknown>") ); }
    @Test public void boolean_107() { testBoolean("'x'^^<http://example/unknown> != 'x'^^<http://example/unknown>", false); }
    @Test public void boolean_108() { assertThrows(ExprEvalException.class, ()-> testEval("'x'^^<http://example/unknown> != 'y'^^<http://example/unknown>") ); }
    @Test public void string_1() { testString("'a\\nb'", "a\nb"); }
    @Test public void string_2() { testString("'a\\n'", "a\n"); }
    @Test public void string_3() { testString("'\\nb'", "\nb"); }
    @Test public void string_4() { testString("'a\\tb'", "a\tb"); }
    @Test public void string_5() { testString("'a\\bb'", "a\bb"); }
    @Test public void string_6() { testString("'a\\rb'", "a\rb"); }
    @Test public void string_7() { testString("'a\\fb'", "a\fb"); }
    @Test public void string_8() { testString("'a\\\\b'", "a\\b"); }
    @Test public void string_9() { testString("'a\\u0020a'", "a a"); }
    @Test public void string_10() { testString("'a\\uF021'", "a\uF021"); }
    @Test public void string_11() { testString("'a\\U0000F021'", "a\uF021"); }

    @Test
    public void string_bad_1() { assertThrows(QueryParseException.class, ()-> testString("'a\\X'") ); }

    @Test
    public void string_bad_2() { assertThrows(QueryParseException.class, ()-> testString("'aaa\\'") ); }

    @Test
    public void string_bad_3() { assertThrows(QueryParseException.class, ()-> testString("'\\u'") ); }

    @Test
    public void string_bad_4() { assertThrows(QueryParseException.class, ()-> testString("'\\u111'") ); }

//    @Test public void boolean_109() { testBoolean("\"fred\\1\" = 'fred1'", false ); }
//    @Test public void boolean_110() { testBoolean("\"fred2\" = 'fred\\2'", true ); }
    @Test public void boolean_111() { testBoolean("'fred\\\\3' != \"fred3\"", true ); }
    @Test public void boolean_112() { testBoolean("'urn:ex:fred' = <urn:ex:fred>" , false); }
    @Test public void boolean_113() { testBoolean("'urn:ex:fred' != <urn:ex:fred>" , true); }
    @Test public void boolean_114() { testBoolean("'urn:ex:fred' = <urn:ex:fred>", false ); }
    @Test public void boolean_115() { testBoolean("'urn:ex:fred' != <urn:ex:fred>", true ); }
    @Test public void boolean_116() { testBoolean("REGEX('aabbcc', 'abbc')", true ); }
    @Test public void boolean_117() { testBoolean("REGEX('aabbcc' , 'a..c')", true ); }
    @Test public void boolean_118() { testBoolean("REGEX('aabbcc' , '^aabb')", true ); }
    @Test public void boolean_119() { testBoolean("REGEX('aabbcc' , 'cc$')", true ); }
    @Test public void boolean_120() { testBoolean("! REGEX('aabbcc' , 'abbc')", false ); }
    @Test public void boolean_121() { testBoolean("REGEX('aa\\\\cc', '\\\\\\\\')", true ); }
    @Test public void boolean_122() { testBoolean("REGEX('aab*bcc', 'ab\\\\*bc')", true ); }
    @Test public void boolean_123() { testBoolean("REGEX('aabbcc', 'ab\\\\\\\\*bc')", true ); }
    @Test public void boolean_124() { testBoolean("REGEX('aabbcc', 'B.*B', 'i')", true ); }
    @Test public void boolean_125() { assertThrows(ExprEvalException.class, ()-> testEval("2 < 'fred'") ); }
    @Test public void boolean_126() { testBoolean("datatype('fred') = <"+XSD.xstring.getURI()+">", true); }
    @Test public void boolean_127() { testBoolean("datatype('fred'^^<urn:test:foo>) = <urn:test:foo>", true); }
    @Test public void boolean_128() { testBoolean("datatype('fred'^^<foo>) = <Foo>", false); }

    @Test public void lang_01() { testString("LANG('tea time'@en)", "en"); }
    // Aside For some strange reason, the language code is GB not UK.
    // "The United Kingdom of Great Britain and Norther Ireland."
    // The four countries England, Scotland, Wales and Northern Ireland (since 1922).
    // It's complicated: https://en.wikipedia.org/wiki/United_Kingdom
    @Test public void lang_02() { testString("LANG('tea time'@en-gb)", "en-GB"); }
    @Test public void lang_03() { testString("LANG('tea time')", ""); }
    @Test public void lang_04() { testBoolean("hasLANG('tea time'@en-gb)", true); }
    @Test public void lang_05() { testBoolean("hasLANG('tea time')", false); }

    // hasLANG  hasLANGDIR  LANG  LANGDIR  STRLANGDIR
    @Test public void langdir_01() { testBoolean("hasLANGDIR('coffee time')", false); }
    @Test public void langdir_02() { testString("LANGDIR('coffee time')", ""); }
    @Test public void langdir_03() { testSyntax("STRLANGDIR('abc', 'fr', 'ltr')"); }

    @Test public void langdir_04() { testBoolean("hasLANGDIR( STRLANGDIR('abc', 'fr', 'ltr') )", true); }
    @Test public void langdir_05() { testString("LANGDIR( STRLANGDIR('abc', 'fr', 'ltr') )", "ltr"); }
    @Test public void langdir_06() { testString("LANG( STRLANGDIR('abc', 'fr', 'ltr') )", "fr"); }
    @Test public void langdir_07() { testString("LANGDIR( STRLANG('abc', 'fr--ltr') )", ""); }

    @Test public void langmatches_01() { testBoolean("LANGMATCHES('EN', 'en')", true); }
    @Test public void langmatches_02() { testBoolean("LANGMATCHES('en', 'en')", true); }
    @Test public void langmatches_03() { testBoolean("LANGMATCHES('EN', 'EN')", true); }
    @Test public void langmatches_04() { testBoolean("LANGMATCHES('en', 'EN')", true); }
    @Test public void langmatches_05() { testBoolean("LANGMATCHES('fr', 'EN')", false); }

    @Test public void langmatches_06() { testBoolean("LANGMATCHES('en', 'en-gb')", false); }
    @Test public void langmatches_07() { testBoolean("LANGMATCHES('en-GB', 'en-GB')", true); }
    @Test public void langmatches_08() { testBoolean("LANGMATCHES('en-Latn-gb', 'en-Latn')", true); }
    @Test public void langmatches_09() { testBoolean("LANGMATCHES('en-gb', 'en-Latn')", false); }

    @Test public void langmatches_10() { testBoolean("LANGMATCHES('', '*')", false); }
    @Test public void langmatches_11() { testBoolean("LANGMATCHES('en-us', '*')", true); }

    // RDF 1.2: triple terms.
    @Test public void tripleterm_01() { testEval("TRIPLE(<x:s>, <x:p>, 123)"); }
    @Test public void tripleterm_02() { testURI("SUBJECT( TRIPLE(<x:s>, <x:p>, 123) )", "x:s"); }
    @Test public void tripleterm_03() { testURI("PREDICATE( TRIPLE(<x:s>, <x:p>, 123) )", "x:p"); }

    @Test
    public void tripleterm_03a() { assertThrows(QueryParseException.class, ()-> testURI("PROPERTY( TRIPLE(<x:s>, <x:p>, 123) )", "x:p") ); }

    @Test public void tripleterm_04() { testNumeric("OBJECT( TRIPLE(<x:s>, <x:p>, 123) )", 123); }

    @Test public void boolean_129() { testBoolean("isURI(?x)", true, env); }
    @Test public void boolean_130() { testBoolean("isURI(?a)", false, env); }
    @Test public void boolean_131() { testBoolean("isURI(?b)", false, env); }

    // ?y is unbound
    @Test public void boolean_132() { assertThrows(ExprEvalException.class, ()-> testBoolean("isURI(?y)", false, env) ); }
    @Test public void boolean_133() { testBoolean("isURI(<urn:test:foo>)", true, env); }
    @Test public void boolean_134() { testBoolean("isURI('bar')", false, env); }
    @Test public void boolean_135() { testBoolean("isLiteral(?x)", false, env); }
    @Test public void boolean_136() { testBoolean("isLiteral(?a)", true, env); }
    @Test public void boolean_137() { testBoolean("isLiteral(?b)", false, env); }
    @Test public void boolean_138() { assertThrows(ExprEvalException.class, ()-> testBoolean("isLiteral(?y)", false, env) ); }
    @Test public void boolean_139() { testBoolean("isBlank(?x)", false, env); }
    @Test public void boolean_140() { testBoolean("isBlank(?a)", false, env); }
    @Test public void boolean_141() { testBoolean("isBlank(?b)", true, env); }
    @Test public void boolean_142() { assertThrows(ExprEvalException.class, ()-> testBoolean("isBlank(?y)", false, env) ); }
    @Test public void boolean_143() { testBoolean("bound(?a)", true, env); }
    @Test public void boolean_144() { testBoolean("bound(?b)", true, env); }
    @Test public void boolean_145() { testBoolean("bound(?x)", true, env); }
    @Test public void boolean_146() { testBoolean("bound(?y)", false, env); }
    @Test public void string_18()   { testString("str(<urn:ex:x>)", "urn:ex:x"); }
    @Test public void string_19()   { testString("str('')", ""); }
    @Test public void string_20()   { testString("str(15)", "15"); }
    @Test public void string_21()   { testString("str('15.20'^^<"+XSDDatatype.XSDdouble.getURI()+">)", "15.20"); }
    @Test public void string_22()   { testString("str('lex'^^<x:unknown>)", "lex"); }
    @Test public void boolean_147() { testBoolean("sameTerm(1, 1)", true, env); }
    @Test public void boolean_148() { testBoolean("sameTerm(1, 1.0)", false, env); }
    @Test public void numeric_52()  { testNumeric("<"+xsd+"integer>('3')", 3); }
    @Test public void numeric_53()  { testNumeric("<"+xsd+"byte>('3')", 3); }
    @Test public void numeric_54()  { testNumeric("<"+xsd+"int>('3')", 3); }
    @Test public void boolean_149() { testBoolean("<"+xsd+"double>('3') = 3", true); }
    @Test public void boolean_150() { testBoolean("<"+xsd+"float>('3') = 3", true); }
    @Test public void boolean_151() { testBoolean("<"+xsd+"double>('3') = <"+xsd+"float>('3')", true); }
    @Test public void boolean_152() { testBoolean("<"+xsd+"double>(str('3')) = 3", true); }

    @Test public void string_23()   { testString("'a'+'b'", "ab"); }

    // Not strict
    @Test
    public void string_24()         { assertThrows(ExprEvalException.class, ()-> testString("'a'+12") ); }
    public void string_25()         { testString("12+'a'"); }
    public void string_26()         { testString("<uri>+'a'"); }

    static String dateTime1 = "'2005-02-25T12:03:34Z'^^<"+XSDDatatype.XSDdateTime.getURI()+">";
    static String dateTime2 = "'2005-02-25T12:03:34Z'^^<"+XSDDatatype.XSDdateTime.getURI()+">";
    // Earlier
    static String dateTime3 = "'2005-01-01T12:03:34Z'^^<"+XSDDatatype.XSDdateTime.getURI()+">";
    // Later
    static String dateTime4 = "'2005-02-25T13:00:00Z'^^<"+XSDDatatype.XSDdateTime.getURI()+">";

    static String time1 = "'12:03:34Z'^^<" + XSDDatatype.XSDtime.getURI() + ">";
    static String time2 = "'12:03:34Z'^^<" + XSDDatatype.XSDtime.getURI() + ">";
    static String time3 = "'13:00:00Z'^^<" + XSDDatatype.XSDtime.getURI() + ">";
    static String time4 = "'11:03:34Z'^^<" + XSDDatatype.XSDtime.getURI() + ">";
    static String exNS = "http://example.org/";
    static String xNS  = "http://example.org/dot#";
    static String selNS = "http://select/";
    static String dftNS = "http://default/";
    static  String baseNS = "http://base/";
    static String rdfNS = RDF.getURI();
    static Query query = QueryFactory.make();
    static {
        query.setBaseURI(baseNS);

        query.setPrefix("ex",      exNS);
        query.setPrefix("rdf",     RDF.getURI());
        query.setPrefix("x.",      xNS);
        query.setPrefix("",        dftNS);
        query.setPrefix("select",  selNS);
    }
    static String xsd = XSDDatatype.XSD+"#";
    static Binding env = BindingFactory.binding(Var.alloc("a"), NodeFactory.createLiteralString("A"),
                                                Var.alloc("b"), NodeFactory.createBlankNode(),
                                                Var.alloc("x"), NodeFactory.createURI("urn:ex:abcd"));

    // Parse and ensure the whole string was used.
    private static Expr parseToEnd(String exprString) {
        return ExprUtils.parse(query, exprString, true);
    }

    // Parse, stopping when the expression ends.
    private static Expr parseAny(String exprString) {
        return ExprUtils.parse(query, exprString, false);
    }

    private static void testVar(String string, String rightVarName) {
        Expr expr = parseAny(string);
        assertTrue(expr.isVariable(), ()->"Not a NodeVar: " + expr);
        ExprVar v = (ExprVar)expr;
        assertEquals(rightVarName, v.getVarName(), ()->"Different variable names");
    }

    private static void testSyntax(String exprString) {
        parseToEnd(exprString);
    }

    // "should evaluate", don't care what the result is.
    private static void testEval(String string) {
        Expr expr = parseToEnd(string);
        Binding binding = BindingFactory.empty();
        FunctionEnv env = new FunctionEnvBase();
        NodeValue v = expr.eval(binding, env);
    }

    // All value testing should be parseToEnd
    private static void testNumeric(String string, int i) {
        Expr expr = parseToEnd(string);
        Binding binding = BindingFactory.empty();
        FunctionEnv env = new FunctionEnvBase();
        NodeValue v = expr.eval(binding, env);
        assertTrue(v.isInteger());
        assertEquals(i, v.getInteger().intValue());
    }

    private static void testNumeric(String string, BigDecimal decimal) {
        Expr expr = parseToEnd(string);
        Binding binding = BindingFactory.empty();
        FunctionEnv env = new FunctionEnvBase();
        NodeValue v = expr.eval(binding, env);
        assertTrue(v.isDecimal());
        assertEquals(decimal, v.getDecimal());
    }

    private static void testNumeric(String string, BigInteger integer) {
        Expr expr = parseToEnd(string);
        Binding binding = BindingFactory.empty();
        FunctionEnv env = new FunctionEnvBase();
        NodeValue v = expr.eval(binding, env);
        assertTrue(v.isInteger());
        assertEquals(integer, v.getInteger());
    }

    private static void testNumeric(String string, double d) {
        Expr expr = parseToEnd(string);
        Binding binding = BindingFactory.empty();
        FunctionEnv env = new FunctionEnvBase();
        NodeValue v = expr.eval(binding, env);
        assertTrue(v.isDouble());
        assertEquals(d, v.getDouble(), 0);
    }

    private static void testBoolean(String string, boolean b) {
        testBoolean(string, b, BindingFactory.empty());
    }

    private static void testBoolean(String string, boolean b, Binding binding) {
        Expr expr = parseToEnd(string);
        FunctionEnv env = new FunctionEnvBase();
        NodeValue v = expr.eval(binding, env);
        assertTrue(v.isBoolean());
        assertEquals(b, v.getBoolean());
    }

    private static void testURI(String string, String uri) {
        // Exception to the rule - parseAny
        Expr expr = parseAny(string);
        NodeValue v = expr.eval(env, new FunctionEnvBase());
        assertTrue(v.isIRI());
        assertEquals(uri, v.getNode().getURI());
    }

    private static void testString(String string, String string2) {
        Expr expr = parseToEnd(string);
        NodeValue v = expr.eval(env, new FunctionEnvBase());
        assertTrue(v.isString());
        assertEquals(string2, v.getString());
    }

    private static void testString(String string) {
        Expr expr = parseToEnd(string);
        NodeValue v = expr.eval(env, new FunctionEnvBase());
        assertTrue(v.isString());
    }
}
