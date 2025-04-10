/* Generated By:JavaCC: Do not edit this line. SPARQLParser12Constants.java */
package org.apache.jena.sparql.lang.sparql_12 ;


/**
 * Token literal values and constants.
 * Generated by org.javacc.parser.OtherFilesGen#start()
 */
public interface SPARQLParser12Constants {

  /** End of File. */
  int EOF = 0;
  /** RegularExpression Id. */
  int SINGLE_LINE_COMMENT = 6;
  /** RegularExpression Id. */
  int WS = 7;
  /** RegularExpression Id. */
  int WSC = 8;
  /** RegularExpression Id. */
  int IRIref = 9;
  /** RegularExpression Id. */
  int PNAME_NS = 10;
  /** RegularExpression Id. */
  int PNAME_LN = 11;
  /** RegularExpression Id. */
  int BLANK_NODE_LABEL = 12;
  /** RegularExpression Id. */
  int VAR1 = 13;
  /** RegularExpression Id. */
  int VAR2 = 14;
  /** RegularExpression Id. */
  int LANG_DIR = 15;
  /** RegularExpression Id. */
  int A2Z = 16;
  /** RegularExpression Id. */
  int A2ZN = 17;
  /** RegularExpression Id. */
  int KW_A = 18;
  /** RegularExpression Id. */
  int BASE = 19;
  /** RegularExpression Id. */
  int PREFIX = 20;
  /** RegularExpression Id. */
  int SELECT = 21;
  /** RegularExpression Id. */
  int DISTINCT = 22;
  /** RegularExpression Id. */
  int REDUCED = 23;
  /** RegularExpression Id. */
  int DESCRIBE = 24;
  /** RegularExpression Id. */
  int CONSTRUCT = 25;
  /** RegularExpression Id. */
  int ASK = 26;
  /** RegularExpression Id. */
  int LIMIT = 27;
  /** RegularExpression Id. */
  int OFFSET = 28;
  /** RegularExpression Id. */
  int ORDER = 29;
  /** RegularExpression Id. */
  int BY = 30;
  /** RegularExpression Id. */
  int VALUES = 31;
  /** RegularExpression Id. */
  int UNDEF = 32;
  /** RegularExpression Id. */
  int ASC = 33;
  /** RegularExpression Id. */
  int DESC = 34;
  /** RegularExpression Id. */
  int NAMED = 35;
  /** RegularExpression Id. */
  int FROM = 36;
  /** RegularExpression Id. */
  int WHERE = 37;
  /** RegularExpression Id. */
  int AND = 38;
  /** RegularExpression Id. */
  int GRAPH = 39;
  /** RegularExpression Id. */
  int OPTIONAL = 40;
  /** RegularExpression Id. */
  int UNION = 41;
  /** RegularExpression Id. */
  int MINUS_P = 42;
  /** RegularExpression Id. */
  int BIND = 43;
  /** RegularExpression Id. */
  int SERVICE = 44;
  /** RegularExpression Id. */
  int TRIPLE = 45;
  /** RegularExpression Id. */
  int IS_TRIPLE = 46;
  /** RegularExpression Id. */
  int SUBJECT = 47;
  /** RegularExpression Id. */
  int PREDICATE = 48;
  /** RegularExpression Id. */
  int OBJECT = 49;
  /** RegularExpression Id. */
  int EXISTS = 50;
  /** RegularExpression Id. */
  int NOT = 51;
  /** RegularExpression Id. */
  int AS = 52;
  /** RegularExpression Id. */
  int GROUP = 53;
  /** RegularExpression Id. */
  int HAVING = 54;
  /** RegularExpression Id. */
  int SEPARATOR = 55;
  /** RegularExpression Id. */
  int AGG = 56;
  /** RegularExpression Id. */
  int COUNT = 57;
  /** RegularExpression Id. */
  int MIN = 58;
  /** RegularExpression Id. */
  int MAX = 59;
  /** RegularExpression Id. */
  int SUM = 60;
  /** RegularExpression Id. */
  int AVG = 61;
  /** RegularExpression Id. */
  int STDEV = 62;
  /** RegularExpression Id. */
  int STDEV_SAMP = 63;
  /** RegularExpression Id. */
  int STDEV_POP = 64;
  /** RegularExpression Id. */
  int VARIANCE = 65;
  /** RegularExpression Id. */
  int VAR_SAMP = 66;
  /** RegularExpression Id. */
  int VAR_POP = 67;
  /** RegularExpression Id. */
  int SAMPLE = 68;
  /** RegularExpression Id. */
  int GROUP_CONCAT = 69;
  /** RegularExpression Id. */
  int FILTER = 70;
  /** RegularExpression Id. */
  int BOUND = 71;
  /** RegularExpression Id. */
  int COALESCE = 72;
  /** RegularExpression Id. */
  int IN = 73;
  /** RegularExpression Id. */
  int IF = 74;
  /** RegularExpression Id. */
  int BNODE = 75;
  /** RegularExpression Id. */
  int IRI = 76;
  /** RegularExpression Id. */
  int URI = 77;
  /** RegularExpression Id. */
  int STR = 78;
  /** RegularExpression Id. */
  int STRLANG = 79;
  /** RegularExpression Id. */
  int STRLANGDIR = 80;
  /** RegularExpression Id. */
  int STRDT = 81;
  /** RegularExpression Id. */
  int DTYPE = 82;
  /** RegularExpression Id. */
  int LANG = 83;
  /** RegularExpression Id. */
  int LANGMATCHES = 84;
  /** RegularExpression Id. */
  int LANGDIR = 85;
  /** RegularExpression Id. */
  int IS_URI = 86;
  /** RegularExpression Id. */
  int IS_IRI = 87;
  /** RegularExpression Id. */
  int IS_BLANK = 88;
  /** RegularExpression Id. */
  int IS_LITERAL = 89;
  /** RegularExpression Id. */
  int IS_NUMERIC = 90;
  /** RegularExpression Id. */
  int HAS_LANG = 91;
  /** RegularExpression Id. */
  int HAS_LANGDIR = 92;
  /** RegularExpression Id. */
  int REGEX = 93;
  /** RegularExpression Id. */
  int SAME_TERM = 94;
  /** RegularExpression Id. */
  int SAME_VALUE = 95;
  /** RegularExpression Id. */
  int RAND = 96;
  /** RegularExpression Id. */
  int ABS = 97;
  /** RegularExpression Id. */
  int CEIL = 98;
  /** RegularExpression Id. */
  int FLOOR = 99;
  /** RegularExpression Id. */
  int ROUND = 100;
  /** RegularExpression Id. */
  int CONCAT = 101;
  /** RegularExpression Id. */
  int SUBSTR = 102;
  /** RegularExpression Id. */
  int STRLEN = 103;
  /** RegularExpression Id. */
  int REPLACE = 104;
  /** RegularExpression Id. */
  int UCASE = 105;
  /** RegularExpression Id. */
  int LCASE = 106;
  /** RegularExpression Id. */
  int ENCODE_FOR_URI = 107;
  /** RegularExpression Id. */
  int CONTAINS = 108;
  /** RegularExpression Id. */
  int STRSTARTS = 109;
  /** RegularExpression Id. */
  int STRENDS = 110;
  /** RegularExpression Id. */
  int STRBEFORE = 111;
  /** RegularExpression Id. */
  int STRAFTER = 112;
  /** RegularExpression Id. */
  int YEAR = 113;
  /** RegularExpression Id. */
  int MONTH = 114;
  /** RegularExpression Id. */
  int DAY = 115;
  /** RegularExpression Id. */
  int HOURS = 116;
  /** RegularExpression Id. */
  int MINUTES = 117;
  /** RegularExpression Id. */
  int SECONDS = 118;
  /** RegularExpression Id. */
  int TIMEZONE = 119;
  /** RegularExpression Id. */
  int TZ = 120;
  /** RegularExpression Id. */
  int NOW = 121;
  /** RegularExpression Id. */
  int UUID = 122;
  /** RegularExpression Id. */
  int STRUUID = 123;
  /** RegularExpression Id. */
  int MD5 = 124;
  /** RegularExpression Id. */
  int SHA1 = 125;
  /** RegularExpression Id. */
  int SHA224 = 126;
  /** RegularExpression Id. */
  int SHA256 = 127;
  /** RegularExpression Id. */
  int SHA384 = 128;
  /** RegularExpression Id. */
  int SHA512 = 129;
  /** RegularExpression Id. */
  int TRUE = 130;
  /** RegularExpression Id. */
  int FALSE = 131;
  /** RegularExpression Id. */
  int DATA = 132;
  /** RegularExpression Id. */
  int INSERT = 133;
  /** RegularExpression Id. */
  int DELETE = 134;
  /** RegularExpression Id. */
  int INSERT_DATA = 135;
  /** RegularExpression Id. */
  int DELETE_DATA = 136;
  /** RegularExpression Id. */
  int DELETE_WHERE = 137;
  /** RegularExpression Id. */
  int LOAD = 138;
  /** RegularExpression Id. */
  int CLEAR = 139;
  /** RegularExpression Id. */
  int CREATE = 140;
  /** RegularExpression Id. */
  int ADD = 141;
  /** RegularExpression Id. */
  int MOVE = 142;
  /** RegularExpression Id. */
  int COPY = 143;
  /** RegularExpression Id. */
  int META = 144;
  /** RegularExpression Id. */
  int SILENT = 145;
  /** RegularExpression Id. */
  int DROP = 146;
  /** RegularExpression Id. */
  int INTO = 147;
  /** RegularExpression Id. */
  int TO = 148;
  /** RegularExpression Id. */
  int DFT = 149;
  /** RegularExpression Id. */
  int ALL = 150;
  /** RegularExpression Id. */
  int WITH = 151;
  /** RegularExpression Id. */
  int USING = 152;
  /** RegularExpression Id. */
  int DIGITS = 153;
  /** RegularExpression Id. */
  int INTEGER = 154;
  /** RegularExpression Id. */
  int DECIMAL = 155;
  /** RegularExpression Id. */
  int DOUBLE = 156;
  /** RegularExpression Id. */
  int INTEGER_POSITIVE = 157;
  /** RegularExpression Id. */
  int DECIMAL_POSITIVE = 158;
  /** RegularExpression Id. */
  int DOUBLE_POSITIVE = 159;
  /** RegularExpression Id. */
  int INTEGER_NEGATIVE = 160;
  /** RegularExpression Id. */
  int DECIMAL_NEGATIVE = 161;
  /** RegularExpression Id. */
  int DOUBLE_NEGATIVE = 162;
  /** RegularExpression Id. */
  int EXPONENT = 163;
  /** RegularExpression Id. */
  int QUOTE_3D = 164;
  /** RegularExpression Id. */
  int QUOTE_3S = 165;
  /** RegularExpression Id. */
  int ECHAR = 166;
  /** RegularExpression Id. */
  int UCHAR = 167;
  /** RegularExpression Id. */
  int UCHAR4 = 168;
  /** RegularExpression Id. */
  int UCHAR8 = 169;
  /** RegularExpression Id. */
  int STRING_LITERAL1 = 170;
  /** RegularExpression Id. */
  int STRING_LITERAL2 = 171;
  /** RegularExpression Id. */
  int STRING_LITERAL_LONG1 = 172;
  /** RegularExpression Id. */
  int STRING_LITERAL_LONG2 = 173;
  /** RegularExpression Id. */
  int LPAREN = 174;
  /** RegularExpression Id. */
  int RPAREN = 175;
  /** RegularExpression Id. */
  int NIL = 176;
  /** RegularExpression Id. */
  int LBRACE = 177;
  /** RegularExpression Id. */
  int RBRACE = 178;
  /** RegularExpression Id. */
  int LBRACKET = 179;
  /** RegularExpression Id. */
  int RBRACKET = 180;
  /** RegularExpression Id. */
  int ANON = 181;
  /** RegularExpression Id. */
  int SEMICOLON = 182;
  /** RegularExpression Id. */
  int COMMA = 183;
  /** RegularExpression Id. */
  int DOT = 184;
  /** RegularExpression Id. */
  int EQ = 185;
  /** RegularExpression Id. */
  int NE = 186;
  /** RegularExpression Id. */
  int GT = 187;
  /** RegularExpression Id. */
  int LT = 188;
  /** RegularExpression Id. */
  int LE = 189;
  /** RegularExpression Id. */
  int GE = 190;
  /** RegularExpression Id. */
  int L_TRIPLE = 191;
  /** RegularExpression Id. */
  int R_TRIPLE = 192;
  /** RegularExpression Id. */
  int LT2 = 193;
  /** RegularExpression Id. */
  int GT2 = 194;
  /** RegularExpression Id. */
  int L_ANN = 195;
  /** RegularExpression Id. */
  int R_ANN = 196;
  /** RegularExpression Id. */
  int TILDE = 197;
  /** RegularExpression Id. */
  int BANG = 198;
  /** RegularExpression Id. */
  int COLON = 199;
  /** RegularExpression Id. */
  int SC_OR = 200;
  /** RegularExpression Id. */
  int SC_AND = 201;
  /** RegularExpression Id. */
  int PLUS = 202;
  /** RegularExpression Id. */
  int MINUS = 203;
  /** RegularExpression Id. */
  int STAR = 204;
  /** RegularExpression Id. */
  int SLASH = 205;
  /** RegularExpression Id. */
  int DATATYPE = 206;
  /** RegularExpression Id. */
  int AT = 207;
  /** RegularExpression Id. */
  int VBAR = 208;
  /** RegularExpression Id. */
  int CARAT = 209;
  /** RegularExpression Id. */
  int FPATH = 210;
  /** RegularExpression Id. */
  int RPATH = 211;
  /** RegularExpression Id. */
  int QMARK = 212;
  /** RegularExpression Id. */
  int PN_CHARS_BASE = 213;
  /** RegularExpression Id. */
  int PN_CHARS_U = 214;
  /** RegularExpression Id. */
  int PN_CHARS = 215;
  /** RegularExpression Id. */
  int PN_PREFIX = 216;
  /** RegularExpression Id. */
  int PN_LOCAL = 217;
  /** RegularExpression Id. */
  int VARNAME = 218;
  /** RegularExpression Id. */
  int PN_LOCAL_ESC = 219;
  /** RegularExpression Id. */
  int PLX = 220;
  /** RegularExpression Id. */
  int HEX = 221;
  /** RegularExpression Id. */
  int PERCENT = 222;
  /** RegularExpression Id. */
  int UNKNOWN = 223;

  /** Lexical state. */
  int DEFAULT = 0;

  /** Literal token values. */
  String[] tokenImage = {
    "<EOF>",
    "\" \"",
    "\"\\t\"",
    "\"\\n\"",
    "\"\\r\"",
    "\"\\f\"",
    "<SINGLE_LINE_COMMENT>",
    "<WS>",
    "<WSC>",
    "<IRIref>",
    "<PNAME_NS>",
    "<PNAME_LN>",
    "<BLANK_NODE_LABEL>",
    "<VAR1>",
    "<VAR2>",
    "<LANG_DIR>",
    "<A2Z>",
    "<A2ZN>",
    "\"a\"",
    "\"base\"",
    "\"prefix\"",
    "\"select\"",
    "\"distinct\"",
    "\"reduced\"",
    "\"describe\"",
    "\"construct\"",
    "\"ask\"",
    "\"limit\"",
    "\"offset\"",
    "\"order\"",
    "\"by\"",
    "\"values\"",
    "\"undef\"",
    "\"asc\"",
    "\"desc\"",
    "\"named\"",
    "\"from\"",
    "\"where\"",
    "\"and\"",
    "\"graph\"",
    "\"optional\"",
    "\"union\"",
    "\"minus\"",
    "\"bind\"",
    "\"service\"",
    "\"TRIPLE\"",
    "\"isTRIPLE\"",
    "\"SUBJECT\"",
    "\"PREDICATE\"",
    "\"OBJECT\"",
    "\"exists\"",
    "\"not\"",
    "\"as\"",
    "\"group\"",
    "\"having\"",
    "\"separator\"",
    "\"agg\"",
    "\"count\"",
    "\"min\"",
    "\"max\"",
    "\"sum\"",
    "\"avg\"",
    "\"stdev\"",
    "\"stdev_samp\"",
    "\"stdev_pop\"",
    "\"variance\"",
    "\"var_samp\"",
    "\"var_pop\"",
    "\"sample\"",
    "\"group_concat\"",
    "\"filter\"",
    "\"bound\"",
    "\"coalesce\"",
    "\"in\"",
    "\"if\"",
    "\"bnode\"",
    "\"iri\"",
    "\"uri\"",
    "\"str\"",
    "\"strlang\"",
    "\"strlangdir\"",
    "\"strdt\"",
    "\"datatype\"",
    "\"lang\"",
    "\"langmatches\"",
    "\"langdir\"",
    "\"isURI\"",
    "\"isIRI\"",
    "\"isBlank\"",
    "\"isLiteral\"",
    "\"isNumeric\"",
    "\"hasLang\"",
    "\"hasLangDir\"",
    "\"regex\"",
    "\"sameTerm\"",
    "\"sameValue\"",
    "\"RAND\"",
    "\"ABS\"",
    "\"CEIL\"",
    "\"FLOOR\"",
    "\"ROUND\"",
    "\"CONCAT\"",
    "\"SUBSTR\"",
    "\"STRLEN\"",
    "\"REPLACE\"",
    "\"UCASE\"",
    "\"LCASE\"",
    "\"ENCODE_FOR_URI\"",
    "\"CONTAINS\"",
    "\"STRSTARTS\"",
    "\"STRENDS\"",
    "\"STRBEFORE\"",
    "\"STRAFTER\"",
    "\"YEAR\"",
    "\"MONTH\"",
    "\"DAY\"",
    "\"HOURS\"",
    "\"MINUTES\"",
    "\"SECONDS\"",
    "\"TIMEZONE\"",
    "\"TZ\"",
    "\"NOW\"",
    "\"UUID\"",
    "\"STRUUID\"",
    "\"MD5\"",
    "\"SHA1\"",
    "\"SHA224\"",
    "\"SHA256\"",
    "\"SHA384\"",
    "\"SHA512\"",
    "\"true\"",
    "\"false\"",
    "\"data\"",
    "\"insert\"",
    "\"delete\"",
    "<INSERT_DATA>",
    "<DELETE_DATA>",
    "<DELETE_WHERE>",
    "\"load\"",
    "\"clear\"",
    "\"create\"",
    "\"add\"",
    "\"move\"",
    "\"copy\"",
    "\"meta\"",
    "\"silent\"",
    "\"drop\"",
    "\"into\"",
    "\"to\"",
    "\"default\"",
    "\"all\"",
    "\"with\"",
    "\"using\"",
    "<DIGITS>",
    "<INTEGER>",
    "<DECIMAL>",
    "<DOUBLE>",
    "<INTEGER_POSITIVE>",
    "<DECIMAL_POSITIVE>",
    "<DOUBLE_POSITIVE>",
    "<INTEGER_NEGATIVE>",
    "<DECIMAL_NEGATIVE>",
    "<DOUBLE_NEGATIVE>",
    "<EXPONENT>",
    "\"\\\"\\\"\\\"\"",
    "\"\\\'\\\'\\\'\"",
    "<ECHAR>",
    "<UCHAR>",
    "<UCHAR4>",
    "<UCHAR8>",
    "<STRING_LITERAL1>",
    "<STRING_LITERAL2>",
    "<STRING_LITERAL_LONG1>",
    "<STRING_LITERAL_LONG2>",
    "\"(\"",
    "\")\"",
    "<NIL>",
    "\"{\"",
    "\"}\"",
    "\"[\"",
    "\"]\"",
    "<ANON>",
    "\";\"",
    "\",\"",
    "\".\"",
    "\"=\"",
    "\"!=\"",
    "\">\"",
    "\"<\"",
    "\"<=\"",
    "\">=\"",
    "\"<<(\"",
    "\")>>\"",
    "\"<<\"",
    "\">>\"",
    "\"{|\"",
    "\"|}\"",
    "\"~\"",
    "\"!\"",
    "\":\"",
    "\"||\"",
    "\"&&\"",
    "\"+\"",
    "\"-\"",
    "\"*\"",
    "\"/\"",
    "\"^^\"",
    "\"@\"",
    "\"|\"",
    "\"^\"",
    "\"->\"",
    "\"<-\"",
    "\"?\"",
    "<PN_CHARS_BASE>",
    "<PN_CHARS_U>",
    "<PN_CHARS>",
    "<PN_PREFIX>",
    "<PN_LOCAL>",
    "<VARNAME>",
    "<PN_LOCAL_ESC>",
    "<PLX>",
    "<HEX>",
    "<PERCENT>",
    "<UNKNOWN>",
  };

}
