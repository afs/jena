/* Generated By:JavaCC: Do not edit this line. ARQParserConstants.java */
package org.apache.jena.sparql.lang.arq ;


/**
 * Token literal values and constants.
 * Generated by org.javacc.parser.OtherFilesGen#start()
 */
public interface ARQParserConstants {

  /** End of File. */
  int EOF = 0;
  /** RegularExpression Id. */
  int SINGLE_LINE_COMMENT = 6;
  /** RegularExpression Id. */
  int WS = 7;
  /** RegularExpression Id. */
  int WSC = 8;
  /** RegularExpression Id. */
  int BOM = 9;
  /** RegularExpression Id. */
  int IRIref = 10;
  /** RegularExpression Id. */
  int PNAME_NS = 11;
  /** RegularExpression Id. */
  int PNAME_LN = 12;
  /** RegularExpression Id. */
  int BLANK_NODE_LABEL = 13;
  /** RegularExpression Id. */
  int VAR1 = 14;
  /** RegularExpression Id. */
  int VAR2 = 15;
  /** RegularExpression Id. */
  int LANG_DIR = 16;
  /** RegularExpression Id. */
  int A2Z = 17;
  /** RegularExpression Id. */
  int A2ZN = 18;
  /** RegularExpression Id. */
  int KW_A = 19;
  /** RegularExpression Id. */
  int BASE = 20;
  /** RegularExpression Id. */
  int PREFIX = 21;
  /** RegularExpression Id. */
  int SELECT = 22;
  /** RegularExpression Id. */
  int DISTINCT = 23;
  /** RegularExpression Id. */
  int REDUCED = 24;
  /** RegularExpression Id. */
  int JSON = 25;
  /** RegularExpression Id. */
  int DESCRIBE = 26;
  /** RegularExpression Id. */
  int CONSTRUCT = 27;
  /** RegularExpression Id. */
  int ASK = 28;
  /** RegularExpression Id. */
  int LIMIT = 29;
  /** RegularExpression Id. */
  int OFFSET = 30;
  /** RegularExpression Id. */
  int ORDER = 31;
  /** RegularExpression Id. */
  int BY = 32;
  /** RegularExpression Id. */
  int VALUES = 33;
  /** RegularExpression Id. */
  int UNDEF = 34;
  /** RegularExpression Id. */
  int ASC = 35;
  /** RegularExpression Id. */
  int DESC = 36;
  /** RegularExpression Id. */
  int NAMED = 37;
  /** RegularExpression Id. */
  int FROM = 38;
  /** RegularExpression Id. */
  int WHERE = 39;
  /** RegularExpression Id. */
  int AND = 40;
  /** RegularExpression Id. */
  int GRAPH = 41;
  /** RegularExpression Id. */
  int OPTIONAL = 42;
  /** RegularExpression Id. */
  int UNION = 43;
  /** RegularExpression Id. */
  int MINUS_P = 44;
  /** RegularExpression Id. */
  int BIND = 45;
  /** RegularExpression Id. */
  int SERVICE = 46;
  /** RegularExpression Id. */
  int LET = 47;
  /** RegularExpression Id. */
  int LATERAL = 48;
  /** RegularExpression Id. */
  int SEMIJOIN = 49;
  /** RegularExpression Id. */
  int ANTIJOIN = 50;
  /** RegularExpression Id. */
  int UNFOLD = 51;
  /** RegularExpression Id. */
  int TRIPLE = 52;
  /** RegularExpression Id. */
  int IS_TRIPLE = 53;
  /** RegularExpression Id. */
  int SUBJECT = 54;
  /** RegularExpression Id. */
  int PREDICATE = 55;
  /** RegularExpression Id. */
  int OBJECT = 56;
  /** RegularExpression Id. */
  int EXISTS = 57;
  /** RegularExpression Id. */
  int NOT = 58;
  /** RegularExpression Id. */
  int AS = 59;
  /** RegularExpression Id. */
  int GROUP = 60;
  /** RegularExpression Id. */
  int HAVING = 61;
  /** RegularExpression Id. */
  int SEPARATOR = 62;
  /** RegularExpression Id. */
  int AGG = 63;
  /** RegularExpression Id. */
  int COUNT = 64;
  /** RegularExpression Id. */
  int MIN = 65;
  /** RegularExpression Id. */
  int MAX = 66;
  /** RegularExpression Id. */
  int SUM = 67;
  /** RegularExpression Id. */
  int AVG = 68;
  /** RegularExpression Id. */
  int MEDIAN = 69;
  /** RegularExpression Id. */
  int MODE = 70;
  /** RegularExpression Id. */
  int STDEV = 71;
  /** RegularExpression Id. */
  int STDEV_SAMP = 72;
  /** RegularExpression Id. */
  int STDEV_POP = 73;
  /** RegularExpression Id. */
  int VARIANCE = 74;
  /** RegularExpression Id. */
  int VAR_SAMP = 75;
  /** RegularExpression Id. */
  int VAR_POP = 76;
  /** RegularExpression Id. */
  int FOLD = 77;
  /** RegularExpression Id. */
  int SAMPLE = 78;
  /** RegularExpression Id. */
  int GROUP_CONCAT = 79;
  /** RegularExpression Id. */
  int FILTER = 80;
  /** RegularExpression Id. */
  int BOUND = 81;
  /** RegularExpression Id. */
  int COALESCE = 82;
  /** RegularExpression Id. */
  int IN = 83;
  /** RegularExpression Id. */
  int IF = 84;
  /** RegularExpression Id. */
  int BNODE = 85;
  /** RegularExpression Id. */
  int IRI = 86;
  /** RegularExpression Id. */
  int URI = 87;
  /** RegularExpression Id. */
  int CAST = 88;
  /** RegularExpression Id. */
  int CALL = 89;
  /** RegularExpression Id. */
  int MULTI = 90;
  /** RegularExpression Id. */
  int SHORTEST = 91;
  /** RegularExpression Id. */
  int STR = 92;
  /** RegularExpression Id. */
  int STRLANG = 93;
  /** RegularExpression Id. */
  int STRLANGDIR = 94;
  /** RegularExpression Id. */
  int STRDT = 95;
  /** RegularExpression Id. */
  int DTYPE = 96;
  /** RegularExpression Id. */
  int LANG = 97;
  /** RegularExpression Id. */
  int LANGMATCHES = 98;
  /** RegularExpression Id. */
  int LANGDIR = 99;
  /** RegularExpression Id. */
  int IS_URI = 100;
  /** RegularExpression Id. */
  int IS_IRI = 101;
  /** RegularExpression Id. */
  int IS_BLANK = 102;
  /** RegularExpression Id. */
  int IS_LITERAL = 103;
  /** RegularExpression Id. */
  int IS_NUMERIC = 104;
  /** RegularExpression Id. */
  int HAS_LANG = 105;
  /** RegularExpression Id. */
  int HAS_LANGDIR = 106;
  /** RegularExpression Id. */
  int REGEX = 107;
  /** RegularExpression Id. */
  int SAME_TERM = 108;
  /** RegularExpression Id. */
  int SAME_VALUE = 109;
  /** RegularExpression Id. */
  int RAND = 110;
  /** RegularExpression Id. */
  int ABS = 111;
  /** RegularExpression Id. */
  int CEIL = 112;
  /** RegularExpression Id. */
  int FLOOR = 113;
  /** RegularExpression Id. */
  int ROUND = 114;
  /** RegularExpression Id. */
  int MOD = 115;
  /** RegularExpression Id. */
  int IDIV = 116;
  /** RegularExpression Id. */
  int CONCAT = 117;
  /** RegularExpression Id. */
  int SUBSTR = 118;
  /** RegularExpression Id. */
  int STRLEN = 119;
  /** RegularExpression Id. */
  int REPLACE = 120;
  /** RegularExpression Id. */
  int UCASE = 121;
  /** RegularExpression Id. */
  int LCASE = 122;
  /** RegularExpression Id. */
  int ENCODE_FOR_URI = 123;
  /** RegularExpression Id. */
  int CONTAINS = 124;
  /** RegularExpression Id. */
  int STRSTARTS = 125;
  /** RegularExpression Id. */
  int STRENDS = 126;
  /** RegularExpression Id. */
  int STRBEFORE = 127;
  /** RegularExpression Id. */
  int STRAFTER = 128;
  /** RegularExpression Id. */
  int YEAR = 129;
  /** RegularExpression Id. */
  int MONTH = 130;
  /** RegularExpression Id. */
  int DAY = 131;
  /** RegularExpression Id. */
  int HOURS = 132;
  /** RegularExpression Id. */
  int MINUTES = 133;
  /** RegularExpression Id. */
  int SECONDS = 134;
  /** RegularExpression Id. */
  int TIMEZONE = 135;
  /** RegularExpression Id. */
  int TZ = 136;
  /** RegularExpression Id. */
  int ADJUST = 137;
  /** RegularExpression Id. */
  int NOW = 138;
  /** RegularExpression Id. */
  int UUID = 139;
  /** RegularExpression Id. */
  int STRUUID = 140;
  /** RegularExpression Id. */
  int VERSION = 141;
  /** RegularExpression Id. */
  int MD5 = 142;
  /** RegularExpression Id. */
  int SHA1 = 143;
  /** RegularExpression Id. */
  int SHA224 = 144;
  /** RegularExpression Id. */
  int SHA256 = 145;
  /** RegularExpression Id. */
  int SHA384 = 146;
  /** RegularExpression Id. */
  int SHA512 = 147;
  /** RegularExpression Id. */
  int TRUE = 148;
  /** RegularExpression Id. */
  int FALSE = 149;
  /** RegularExpression Id. */
  int DATA = 150;
  /** RegularExpression Id. */
  int INSERT = 151;
  /** RegularExpression Id. */
  int DELETE = 152;
  /** RegularExpression Id. */
  int INSERT_DATA = 153;
  /** RegularExpression Id. */
  int DELETE_DATA = 154;
  /** RegularExpression Id. */
  int DELETE_WHERE = 155;
  /** RegularExpression Id. */
  int LOAD = 156;
  /** RegularExpression Id. */
  int CLEAR = 157;
  /** RegularExpression Id. */
  int CREATE = 158;
  /** RegularExpression Id. */
  int ADD = 159;
  /** RegularExpression Id. */
  int MOVE = 160;
  /** RegularExpression Id. */
  int COPY = 161;
  /** RegularExpression Id. */
  int META = 162;
  /** RegularExpression Id. */
  int SILENT = 163;
  /** RegularExpression Id. */
  int DROP = 164;
  /** RegularExpression Id. */
  int INTO = 165;
  /** RegularExpression Id. */
  int TO = 166;
  /** RegularExpression Id. */
  int DFT = 167;
  /** RegularExpression Id. */
  int ALL = 168;
  /** RegularExpression Id. */
  int WITH = 169;
  /** RegularExpression Id. */
  int USING = 170;
  /** RegularExpression Id. */
  int DIGITS = 171;
  /** RegularExpression Id. */
  int INTEGER = 172;
  /** RegularExpression Id. */
  int DECIMAL = 173;
  /** RegularExpression Id. */
  int DOUBLE = 174;
  /** RegularExpression Id. */
  int INTEGER_POSITIVE = 175;
  /** RegularExpression Id. */
  int DECIMAL_POSITIVE = 176;
  /** RegularExpression Id. */
  int DOUBLE_POSITIVE = 177;
  /** RegularExpression Id. */
  int INTEGER_NEGATIVE = 178;
  /** RegularExpression Id. */
  int DECIMAL_NEGATIVE = 179;
  /** RegularExpression Id. */
  int DOUBLE_NEGATIVE = 180;
  /** RegularExpression Id. */
  int EXPONENT = 181;
  /** RegularExpression Id. */
  int QUOTE_3D = 182;
  /** RegularExpression Id. */
  int QUOTE_3S = 183;
  /** RegularExpression Id. */
  int ECHAR = 184;
  /** RegularExpression Id. */
  int UCHAR = 185;
  /** RegularExpression Id. */
  int UCHAR4 = 186;
  /** RegularExpression Id. */
  int UCHAR8 = 187;
  /** RegularExpression Id. */
  int STRING_LITERAL1 = 188;
  /** RegularExpression Id. */
  int STRING_LITERAL2 = 189;
  /** RegularExpression Id. */
  int STRING_LITERAL_LONG1 = 190;
  /** RegularExpression Id. */
  int STRING_LITERAL_LONG2 = 191;
  /** RegularExpression Id. */
  int LPAREN = 192;
  /** RegularExpression Id. */
  int RPAREN = 193;
  /** RegularExpression Id. */
  int NIL = 194;
  /** RegularExpression Id. */
  int LBRACE = 195;
  /** RegularExpression Id. */
  int RBRACE = 196;
  /** RegularExpression Id. */
  int LBRACKET = 197;
  /** RegularExpression Id. */
  int RBRACKET = 198;
  /** RegularExpression Id. */
  int ANON = 199;
  /** RegularExpression Id. */
  int SEMICOLON = 200;
  /** RegularExpression Id. */
  int COMMA = 201;
  /** RegularExpression Id. */
  int DOT = 202;
  /** RegularExpression Id. */
  int EQ = 203;
  /** RegularExpression Id. */
  int NE = 204;
  /** RegularExpression Id. */
  int GT = 205;
  /** RegularExpression Id. */
  int LT = 206;
  /** RegularExpression Id. */
  int LE = 207;
  /** RegularExpression Id. */
  int GE = 208;
  /** RegularExpression Id. */
  int L_TRIPLE = 209;
  /** RegularExpression Id. */
  int R_TRIPLE = 210;
  /** RegularExpression Id. */
  int LT2 = 211;
  /** RegularExpression Id. */
  int GT2 = 212;
  /** RegularExpression Id. */
  int L_ANN = 213;
  /** RegularExpression Id. */
  int R_ANN = 214;
  /** RegularExpression Id. */
  int TILDE = 215;
  /** RegularExpression Id. */
  int BANG = 216;
  /** RegularExpression Id. */
  int COLON = 217;
  /** RegularExpression Id. */
  int SC_OR = 218;
  /** RegularExpression Id. */
  int SC_AND = 219;
  /** RegularExpression Id. */
  int PLUS = 220;
  /** RegularExpression Id. */
  int MINUS = 221;
  /** RegularExpression Id. */
  int STAR = 222;
  /** RegularExpression Id. */
  int SLASH = 223;
  /** RegularExpression Id. */
  int DATATYPE = 224;
  /** RegularExpression Id. */
  int AT = 225;
  /** RegularExpression Id. */
  int ASSIGN = 226;
  /** RegularExpression Id. */
  int VBAR = 227;
  /** RegularExpression Id. */
  int CARAT = 228;
  /** RegularExpression Id. */
  int FPATH = 229;
  /** RegularExpression Id. */
  int RPATH = 230;
  /** RegularExpression Id. */
  int QMARK = 231;
  /** RegularExpression Id. */
  int SURROGATE_PAIR = 232;
  /** RegularExpression Id. */
  int PN_CHARS_BASE = 233;
  /** RegularExpression Id. */
  int PN_CHARS_U = 234;
  /** RegularExpression Id. */
  int PN_CHARS = 235;
  /** RegularExpression Id. */
  int PN_PREFIX = 236;
  /** RegularExpression Id. */
  int PN_LOCAL = 237;
  /** RegularExpression Id. */
  int VARNAME = 238;
  /** RegularExpression Id. */
  int PN_LOCAL_ESC = 239;
  /** RegularExpression Id. */
  int PLX = 240;
  /** RegularExpression Id. */
  int HEX = 241;
  /** RegularExpression Id. */
  int PERCENT = 242;
  /** RegularExpression Id. */
  int UNKNOWN = 243;

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
    "\"\\ufeff\"",
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
    "\"json\"",
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
    "\"LET\"",
    "\"lateral\"",
    "\"semijoin\"",
    "\"antijoin\"",
    "\"unfold\"",
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
    "\"median\"",
    "\"mode\"",
    "\"stdev\"",
    "\"stdev_samp\"",
    "\"stdev_pop\"",
    "\"variance\"",
    "\"var_samp\"",
    "\"var_pop\"",
    "\"fold\"",
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
    "\"cast\"",
    "\"call\"",
    "\"multi\"",
    "\"shortest\"",
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
    "\"MOD\"",
    "\"IDIV\"",
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
    "\"ADJUST\"",
    "\"NOW\"",
    "\"UUID\"",
    "\"STRUUID\"",
    "\"VERSION\"",
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
    "\":=\"",
    "\"|\"",
    "\"^\"",
    "\"->\"",
    "\"<-\"",
    "\"?\"",
    "<SURROGATE_PAIR>",
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
