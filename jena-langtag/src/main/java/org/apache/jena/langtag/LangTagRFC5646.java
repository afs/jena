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

package org.apache.jena.langtag;

import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * An implementation of parsing and formatting.
 * <a href="https://datatracker.ietf.org/doc/html/rfc5646">RFC 5646</a>
 * <p>
 * This implementation does not replace languages by their preferred form (e.g.
 * "i-klingon" has preferred form of "tlh", "zh-xiang" has a preferred form of "hsn").
 * </p>
 * <p>
 * <a href="https://www.rfc-editor.org/info/rfc5646">RFC 5646: Tags for Identifying Languages</a>
 * </p>
 */
public final  class LangTagRFC5646 implements LangTag{
    // The language tag as given.
    private final String langTagString;

    // Grandfathered
    private boolean isGrandfathered = false;
    // Private use of the whole Language-Tag
    private boolean isPrivateUseLanguage = false;

    /* Formatting: https://datatracker.ietf.org/doc/html/rfc5646#section-2.1.1
     *
     * All subtags, including extension and private use subtags,
     * use lowercase letters with two exceptions: two-letter
     * and four-letter subtags that neither appear at the start of the tag
     * nor occur after singletons.  Such two-letter subtags are all
     * uppercase (as in the tags "en-CA-x-ca" or "sgn-BE-FR") and four-
     * letter subtags are titlecase (as in the tag "az-Latn-x-latn").
     *
     * See str()
     */

    // Helpers
    private enum CaseRule { TITLE, LOWER, UPPER }
    private enum CharSet { ALPHA, ALPHANUM }

    public static LangTag create(String string) {
        LangTagRFC5646 langtag = parser(string);
        return langtag;
    }

    // Start/Finish indexes, excluding the initial '-'
    private int language0 = -1 ;
    private int language1 = -1 ;

    private int script0 = -1 ;
    private int script1 = -1 ;

    private int region0 = -1 ;
    private int region1 = -1 ;

    private int variant0 = -1 ;
    private int variant1 = -1 ;

    // All extensions.
    private int extension0 = -1 ;
    private int extension1 = -1 ;

    // Private use sub tag (not private use of the whole language tag, which starts "x-").
    private int privateuse0 = -1 ;
    private int privateuse1 = -1 ;

    @Override
    public String getLanguage() {
        return getSubTag("Language", langTagString, language0, language1, CaseRule.LOWER);
    }

    @Override
    public String getScript() {
        return getSubTag("Script", langTagString, script0, script1, CaseRule.TITLE);
    }

    @Override
    public String getRegion() {
        return getSubTag("Region", langTagString, region0, region1, CaseRule.UPPER);
    }

    @Override
    public String getVariant() {
        return getSubTag("Variant", langTagString, variant0, variant1, CaseRule.LOWER);
    }

    @Override
    public String getExtension() {
        return getSubTag("Extension", langTagString, extension0, extension1, CaseRule.LOWER);
    }

    @Override
    public String getPrivateUse() {
        return getSubTag("Private", langTagString, privateuse0, privateuse1, CaseRule.LOWER);
    }

    @Override
    public int hashCode() {
        return Objects.hash(langTagString,
                            language0, language1, script0, script1, variant0, variant1,
                            extension0, extension1, privateuse0, privateuse1, isGrandfathered, isPrivateUseLanguage);
    }

    /**
     * {@code .equals} and {@code .hashCode}
     *  provide "same immutable object" semantics.
     * The language tags are treated case-sensitively.
     *
     * @See LangTagOps.sameLangTagAs for equivalent language tags.
     */
    @Override
    public boolean equals(Object obj) {
        if ( this == obj )
            return true;
        if ( !(obj instanceof LangTagRFC5646 other) )
            return false;
        // All but the string.
        boolean sameParsePoints =
                extension0 == other.extension0 && extension1 == other.extension1
                && isGrandfathered == other.isGrandfathered
                && isPrivateUseLanguage == other.isPrivateUseLanguage
                && language0 == other.language0 && language1 == other.language1
                && privateuse0 == other.privateuse0 && privateuse1 == other.privateuse1
                && region0 == other.region0 && region1 == other.region1
                && script0 == other.script0 && script1 == other.script1
                && variant0 == other.variant0 && variant1 == other.variant1;
        if ( ! sameParsePoints )
            return false;
        return Objects.equals(langTagString, other.langTagString);
    }

    /**
     * Return the lang tag exactly as given.
     * Use {@link #str()} for the language tag formatted by the rules of RFC 5646.
     */
    @Override
    public String toString() {
        return langTagString;
    }

    @Override
    public String str() {
        if ( isPrivateUseLanguage )
            return InternalLangTag.lowercase(langTagString);

        // Some irregular special cases.
        if ( InternalLangTag.caseInsensitivePrefix(langTagString, "sgn-") ) {
            // "sgn-BE-FR", "sgn-BE-NL", "sgn-CH-DE"
            if ( langTagString.equalsIgnoreCase("sgn-BE-FR") )
                return "sgn-BE-FR";
            if ( langTagString.equalsIgnoreCase("sgn-BE-NL") )
                return "sgn-BE-NL";
            if ( langTagString.equalsIgnoreCase("sgn-CH-DE") )
                return "sgn-CH-DE";
        }

        if ( langTagString.startsWith("i-") || langTagString.startsWith("I-") ) {
            String lcLangTagStr = InternalLangTag.lowercase(langTagString);
            if ( irregular_i.contains(lcLangTagStr) )
                return lcLangTagStr;
        }

        StringBuffer sb = new StringBuffer();
        add(sb, getLanguage());
        add(sb, getScript());
        add(sb, getRegion());
        add(sb, getVariant());
        add(sb, getExtension());
        add(sb, getPrivateUse());
        return sb.toString();
    }

    private void add(StringBuffer sb, String subtag) {
        if ( subtag == null )
            return;
        if ( ! sb.isEmpty() )
            sb.append('-');
        sb.append(subtag);
    }

    private static String getSubTag(String label, String string, int start, int finish, CaseRule format) {
        if ( start == -1 )
            return null;
        if ( finish == -1 )
            throw new InternalError(InternalLangTag.titlecase(label)+" start is set but not subtag end: "+string);
        if ( start >= finish )
            throw new InternalError(InternalLangTag.titlecase(label)+" start index is after "+InternalLangTag.lowercase(label)+" end index: "+string);
        String x = string.substring(start, finish);
        return switch(format) {
            case TITLE -> InternalLangTag.titlecase(x);
            case LOWER -> InternalLangTag.lowercase(x);
            case UPPER -> InternalLangTag.uppercase(x);
        };
    }

    private static LangTagRFC5646 parser(String string) {

        // A segment is a sequence of A2ZN characters separated by '-'.

        LangTagRFC5646 langtag = new LangTagRFC5646(string);
        final int N = string.length();
        // Language-Tag  = langtag             ; normal language tags
        //               / privateuse          ; private use tag
        //               / grandfathered       ; grandfathered tags

        // langtag       = language
        //                 ["-" script]
        //                 ["-" region]
        //                 *("-" variant)
        //                 *("-" extension)
        //                 ["-" privateuse]

        // script        = 4ALPHA              ; ISO 15924 code
        // region        = 2ALPHA              ; ISO 3166-1 code
        //               / 3DIGIT              ; UN M.49 code
        // variant       = 5*8alphanum         ; registered variants
        //               / (DIGIT 3alphanum)
        // extension     = singleton 1*("-" (2*8alphanum))

        if ( N == 0 )
            InternalLangTag.error("Empty string");

        // -------------------
        // language      = (2*3ALPHA [ extlang ]); shortest ISO 639 code
        //               / 4ALPHA                ; reserved for future use
        //               / 5*8ALPHA              ; registered language subtag
        // extlang       = 3ALPHA              ; selected ISO 639 codes
        //                 *2("-" 3ALPHA)      ; permanently reserved

        // Grandfathered
        // Must check first because the whole string (except "en-GB-oed") is the "language"

        if ( grandfathered(string) ) {
            // Regular:
            // "each tag, in its entirety, represents a language or collection of languages."
            //
            // Irregular:
            // With the exception of "en-GB-oed", which is a
            // variant of "en-GB", each of them, in its entirety,
            // represents a language.
            //
            langtag.language0 = 0;
            langtag.language1 = N;
            langtag.isGrandfathered = true;
            // Exception.
            if ( string.equalsIgnoreCase("en-GB-oed") ) {
                // "oed" is "Oxford English Dictionary spelling"
                // Better is the replacement "en-GB-oxendict"
                langtag.language0 = 0;
                langtag.language1 = 2;
                langtag.region0 = 3;
                langtag.region1 = 5;
                // Non-standard variant.
                langtag.variant0 = 6;
                langtag.variant1 = N;
            }
            return langtag;
        }

        // -- language

        int idx = 0;
        int idx2 = segmentNextFinish(string, N, idx);
        int segLen = segmentLength(N, idx, idx2);

        // Private use in the language position.
        if ( segLen == 1 ) {
            if ( string.startsWith("x-") || string.startsWith("X-") ) {
                /*
                The primary language subtag is the first subtag in a language tag and
                cannot be omitted, with two exceptions:

                o  The single-character subtag 'x' as the primary subtag indicates
                   that the language tag consists solely of subtags whose meaning is
                   defined by private agreement.  For example, in the tag "x-fr-CH",
                   the subtags 'fr' and 'CH' do not represent the French language or
                   the country of Switzerland (or any other value in the IANA
                   registry) unless there is a private agreement in place to do so.
                   See Section 4.6.
                */
                langtag.isPrivateUseLanguage = true;
                int idxPrivateUseStart = 0;
                int idxPrivateUseEnd = maybeSubtags(string, N, idxPrivateUseStart+segLen, 1, 8);
                langtag.privateuse0 = idxPrivateUseStart;
                langtag.privateuse1 = idxPrivateUseEnd;
                if ( langtag.privateuse1 < N )
                    InternalLangTag.error("Trailing characters in private langtag: '%s'", string.substring(langtag.privateuse1));
                return langtag;
            }
            InternalLangTag.error("Language part is 1 character: it must be 2-3 characters (4-8 reserved for future use), \"x-\", or a recognized grandfathered tag");
        }

        if ( idx2 < 0 ) {
            // language only.
            if ( segLen > 8 )
                InternalLangTag.error("Language too long (2-3 characters, 4-8 reserved for future use)");
            langtag.language0 = 0;
            langtag.language1 = N;
            InternalLangTag.checkAlpha(string, N, langtag.language0, langtag.language1);
            return langtag;
        }

        if ( idx == idx2 )
            InternalLangTag.error("Can not find the language subtag: '%s'", string);

        if ( segLen < 2 || segLen > 4 )
            InternalLangTag.error("Language: '%s'", string);

        langtag.language0 = idx;

        if ( segLen == 2 || segLen == 3 ) {
            // -- Language extension subtags/
//            language      = 2*3ALPHA            ; shortest ISO 639 code
//                            ["-" extlang]
//            extlang       = 3ALPHA              ; selected ISO 639 codes
//                            *2("-" 3ALPHA)      ; permanently reserved
            int extStart = idx+segLen;
            InternalLangTag.checkAlpha(string, N, langtag.language0, extStart);
            // Extensions are 1 to 3 3ALPHA subtags
            int extEnd = maybeSubtags(string, N, extStart, 3, 3);
            if ( extEnd > extStart ) {
                idx2 = extEnd;
                InternalLangTag.checkAlphaMinus(string, N, extStart, langtag.language1);
            }
        } else if ( segLen > 8 ) {
            InternalLangTag.error("Language too long (2-3 characters, 4-8 reserved for future use)");
        }
        // -- extlang
        langtag.language1 = idx2;
        // Info
        noteSegment("language", string, langtag.language0, langtag.language1);

        // Move on - next subtag
        idx = segmentNextStart(N, idx, idx2);
        idx2 = segmentNextFinish(string, N, idx);
        segLen = segmentLength(N, idx, idx2);
        // -- End langtag

        // ---- script
        // script        = 4ALPHA              ; ISO 15924 code
        if ( segLen == 4 && InternalLangTag.isAlpha(string.charAt(idx)) ) {
            // Script
            // Not a digit - which is a variant.
            // variant       = ... / (DIGIT 3alphanum)
            int start = idx;
            int finish = idx+segLen;

            langtag.script0 = idx;
            langtag.script1 = idx+segLen;
            InternalLangTag.checkAlpha(string, N, langtag.script0, langtag.script1);
            noteSegment("script", string, langtag.script0, langtag.script1);

            // Move on.
            idx = segmentNextStart(N, idx, idx2);
            idx2 = segmentNextFinish(string, N, idx);
            segLen = segmentLength(N, idx, idx2);
        }
        // -- End script

        // ---- region
        // region        = 2ALPHA              ; ISO 3166-1 code
        //               / 3DIGIT              ; UN M.49 code
        if ( segLen == 2 || segLen == 3 ) {
            // Region
            langtag.region0 = idx;
            langtag.region1 = idx+segLen;
            if ( segLen == 2 )
                InternalLangTag.checkAlpha(string, N, langtag.region0, langtag.region1);
            else
                InternalLangTag.checkDigits(string, N, langtag.region0, langtag.region1);
            noteSegment("region", string, langtag.region0, langtag.region1);

            // Move on.
            idx = segmentNextStart(N, idx, idx2);
            idx2 = segmentNextFinish(string, N, idx);
            segLen = segmentLength(N, idx, idx2);
        }
        // -- End region

        // ---- variant
        // variant       = 5*8alphanum         ; registered variants
        //               / (DIGIT 3alphanum)
        for ( ;; ) {
            if ( segLen >= 5 && segLen <= 8) {
                // variant 5*8alphanum
                if ( langtag.variant0 == -1 )
                    langtag.variant0 = idx;
                langtag.variant1 = idx+segLen;
                InternalLangTag.checkAlphaNum(string, N, idx, langtag.variant1);
                noteSegment("variant", string, langtag.variant0, langtag.variant1);
                // Move on.
                idx = segmentNextStart(N, idx, idx2);
                idx2 = segmentNextFinish(string, N, idx);
                segLen = segmentLength(N, idx, idx2);
                continue;
            }

            if ( segLen == 4 ) {
                // variant
                // DIGIT 3alphanum
                char ch = string.charAt(idx);
                if ( ch >= '0' || ch <= '9' ) {
                    if ( langtag.variant0 == -1 )
                        langtag.variant0 = idx;
                    langtag.variant1 = idx+segLen;
                    InternalLangTag.checkAlphaNum(string, N, idx, langtag.variant1);
                    noteSegment("variant", string, langtag.variant0, langtag.variant1);
                }
                // Move on.
                idx = segmentNextStart(N, idx, idx2);
                idx2 = segmentNextFinish(string, N, idx);
                segLen = segmentLength(N, idx, idx2);
                continue;
            }
            break;
        }
        // -- End variant

        // ---- extension and private use
        // extension     = singleton 1*("-" (2*8alphanum))
        // privateuse    = "x" 1*("-" (1*8alphanum))
        boolean inPrivateUseSubtag = false;
        Set<Character> extSingletons = null; new HashSet<>();
        while ( segLen == 1 ) {
            char singleton = string.charAt(idx);
            if ( singleton == 'x' || singleton == 'X' ) {
                inPrivateUseSubtag = true;
                break;
            }
            if ( extSingletons == null ) {
                extSingletons = new HashSet<>();
                extSingletons.add(singleton);
            } else {
                boolean newEntry = extSingletons.add(singleton);
                if ( ! newEntry )
                    InternalLangTag.error("Duplicate extension singleton: '"+singleton+"'");
            }

            if ( langtag.extension0 == -1 )
                langtag.extension0 = idx;
            // Extension.
            // 2*8 alphanum
            int idxExtStart = idx+segLen;
            int idxEndExtra = maybeSubtags(string, N, idxExtStart, 2, 8);

            // Expecting at least one subtag.
            if ( idxExtStart == idxEndExtra )
                InternalLangTag.error("Ill-formed extension");

            if ( idxEndExtra > idxExtStart )
                idx2 = idxEndExtra;
            langtag.extension1 = idx2;
            InternalLangTag.checkAlphaNumMinus(string, N, langtag.extension0, langtag.extension1);

            noteSegment("extension", string, langtag.extension0, langtag.extension1);
            // Move on.
            idx = segmentNextStart(N, idx, idx2);
            idx2 = segmentNextFinish(string, N, idx);
            segLen = segmentLength(N, idx, idx2);
            if ( segLen == 0 )
                InternalLangTag.error("Ill-formed extension. Trailing dash.");
        }

        // ---- private use
        if ( inPrivateUseSubtag ) {
            langtag.privateuse0 = idx;
            // privateuse    = "x" 1*("-" (1*8alphanum))
            int idxPrivateUseStart = idx+segLen;
            int idxPrivateUseEnd = maybeSubtags(string, N, idxPrivateUseStart, 1, 8);

            // Expecting at least one subtag.
            if ( idxPrivateUseStart == idxPrivateUseEnd )
                InternalLangTag.error("Ill-formed private use component");

            if ( idxPrivateUseEnd > idxPrivateUseStart )
                idx2 = idxPrivateUseEnd;
            langtag.privateuse1 = idx2;
            InternalLangTag.checkAlphaNumMinus(string, N, langtag.privateuse0, langtag.privateuse1);

            noteSegment("private use", string, langtag.privateuse0, langtag.privateuse1);
            // Private use runs to end of string. But do checking.
            // Move on.
            idx = segmentNextStart(N, idx, idx2);
            idx2 = segmentNextFinish(string, N, idx);
            segLen = segmentLength(N, idx, idx2);
            if ( segLen == 0 )
                InternalLangTag.error("Ill-formed private use subtag. Trailing dash.");
        }

        // -- End extension and privateuse

        // Did we process everything? No segment: idx == -1 idx2 == -1  seglen == -1

        if ( idx != -1 && idx < N )
            InternalLangTag.error("Trailing characters: '%s'", string.substring(idx));
        if ( idx2 >= 0 )
            InternalLangTag.error("Bad string: '%s'", string);
        return langtag;
    }

    private LangTagRFC5646(String string) {
        this.langTagString = string;
    }

    private LangTagRFC5646(String string,
                           int language0, int language1,
                           int script0, int script1,
                           int region0, int region1,
                           int variant0, int variant1,
                           int extension0, int extension1,
                           int privateuse0, int privateuse1,
                           boolean isGrandfathered) {
        this.langTagString = string;
        this.isGrandfathered = isGrandfathered;
        this.language0 = language0;
        this.language1 = language1;
        this.script0 = script0;
        this.script1 = script1;
        this.variant0 = variant0;
        this.variant1 = variant1;
        this.extension0 = extension0;
        this.extension1 = extension1;
        this.privateuse0 = privateuse0;
        this.privateuse1 = privateuse1;
    }

    /** Zero or more subtags, each between min and max length. */
    private static int maybeSubtags(String string, int N, int idxStart, int min, int max) {
        // Looking at the '-' or end of string.
        int numExt = 0;
        int count = 0;
        int x = idxStart;
        // Outer loop - each subtag segment, having read at the "-"
        while ( x >= 0 && x < N ) {
            char ch = string.charAt(x);
            if ( ch != '-' )
                break;
            int x1 = maybeSubtag1(string, N, x+1, min, max);
            if ( x1 <= 0 )
                break;
            if ( x1 == N ) {
                x = N;
                break;
            }
            x = x1;
        }
        return x;
    }

    /**
     * Peek for a segment between min and max in length.
     * The initial  "-" has been read.
     */
    private static int maybeSubtag1(String string, int N, int idxStart, int min, int max) {
        int idx = idxStart;
        if ( idx >= N )
            return -1;
        int idx2 = segmentNextFinish(string, N, idx);
        int segLen = segmentLength(N, idx, idx2);
        if ( segLen == 0 )
            InternalLangTag.error("Bad langtag. Found '--'");

        if ( segLen < min || segLen > max )
            return -1;
        if ( ! InternalLangTag.isAlpha(string, idxStart, idxStart+segLen) )
            return -1;
        return idxStart+segLen;
    }

    // Start/Finish indexes, excluding the initial '-'
    private static String getSegment(String string, int x0, int x1) {
        if ( x0 < 0 && x1 < 0 )
            return null;
        if ( x0 < 0 || x1 < 0 ) {
            InternalLangTag.error("Segment one undef index");
            return null;
        }
        return string.substring(x0,  x1);
    }

    /** Length of a segment, excluding any "-" */
    private static int segmentLength(int N, int idx, int idx2) {
        if ( idx < 0 )
            return -1;
        if ( idx2 < 0 )
            return N-idx;
        return idx2-idx;
    }

    /** Index of the start of the next segment. */
    private static int segmentNextStart(int N, int idx, int idx2) {
        if ( idx2 == -1 )
            return -1;
        idx = idx2;
        // Skip '-'
        idx++;
        return idx;
    }

    /** Note segment - development aid. */
    private static void noteSegment(String label, String string, int idx, int idx2) {
//        if ( idx2 < 0 ) {
//            System.out.printf("%-10s [%d,%d) '%s'\n", label, idx, idx2, string.substring(idx));
//            return;
//        }
//        System.out.printf("%-10s [%d,%d) '%s'\n",label, idx, idx2, string.substring(idx,  idx2));
    }

    /** Return the index of the next '-' or -1 */
    private static int segmentNextFinish(String x, int N, int idx) {
        if ( idx == -1 )
            return -1;
        if ( idx == N )
            return -1;
        for ( ; idx < N ; idx++ ) {
            char ch = x.charAt(idx);
            if ( ch == '-' ) {
                if ( idx == N-1 ) {
                    // The case of "subtag-"
                    InternalLangTag.error("Language tag string ends in '-'");
                }
                return idx;
            }
        }
        return -1;
    }

    // ---
    // RFC 5646: regular tags
    // Grandfathered tags that (appear to) match the 'langtag' production in
    // Figure 1 are considered 'regular' grandfathered tags.  These tags
    // contain one or more subtags that either do not individually appear in
    // the registry or appear but with a different semantic meaning: each
    // tag, in its entirety, represents a language or collection of
    // languages.

    private static boolean grandfathered(String s) {
        s = s.toLowerCase(Locale.ROOT);
        return grandfathered.contains(s) || regular.contains(s) ;
    }

    // These tags match the 'langtag' production, but their subtags are not extended
    // language or variant subtags: their meaning is defined by their registration and
    // all of these are deprecated in favor of a more modern subtag or sequence of
    // subtags

    private static Set<String> regular =
            Set.of("art-lojban", "cel-gaulish", "no-bok", "no-nyn", "zh-guoyu", "zh-hakka", "zh-min", "zh-min-nan", "zh-xiang");

    // RFC 5646: irregular tags do not match the 'langtag' production and would not be 'well-formed'
    // Grandfathered tags that do not match the 'langtag' production in the
    // ABNF and would otherwise be invalid are considered 'irregular'
    // grandfathered tags.  With the exception of "en-GB-oed", which is a
    // variant of "en-GB", each of them, in its entirety, represents a
    // language.

    private static Set<String> irregular =
            Set.of("en-GB-oed",
                   "i-ami", "i-bnn", "i-default", "i-enochian", "i-hak", "i-klingon",
                   "i-lux", "i-mingo", "i-navajo", "i-pwn", "i-tao", "i-tay", "i-tsu",
                   // These are irregular in that they are "primary subtag ("sgn" - sign language)
                   // then two region-like subtags.
                   // They do obey the basic formatting rule - two letters non-primary subtag is uppercase.
                   "sgn-BE-FR", "sgn-BE-NL", "sgn-CH-DE");

    // The "i-" irregulars.
    private static Set<String> irregular_i =
            Set.of("i-ami", "i-bnn", "i-default", "i-enochian", "i-hak", "i-klingon",
                   "i-lux", "i-mingo", "i-navajo", "i-pwn", "i-tao", "i-tay", "i-tsu");

    // ---

    private static Set<String> grandfathered = new HashSet<>(2*(regular.size()+irregular.size()));
    static {
        for ( String s : irregular )
            grandfathered.add(InternalLangTag.lowercase(s));
        for ( String s : regular )
            grandfathered.add(InternalLangTag.lowercase(s));
    }

    // @formatter:off
    /*
     RFC 5646 Section 2.1
     ABNF definition: https://datatracker.ietf.org/doc/html/rfc5646#section-2.1

     Language-Tag  = langtag             ; normal language tags
                   / privateuse          ; private use tag
                   / grandfathered       ; grandfathered tags

     langtag       = language
                     ["-" script]
                     ["-" region]
                     *("-" variant)
                     *("-" extension)
                     ["-" privateuse]

     language      = 2*3ALPHA            ; shortest ISO 639 code
                     ["-" extlang]       ; sometimes followed by
                                         ; extended language subtags
                   / 4ALPHA              ; or reserved for future use
                   / 5*8ALPHA            ; or registered language subtag

     extlang       = 3ALPHA              ; selected ISO 639 codes
                     *2("-" 3ALPHA)      ; permanently reserved

     script        = 4ALPHA              ; ISO 15924 code

     region        = 2ALPHA              ; ISO 3166-1 code
                   / 3DIGIT              ; UN M.49 code

     variant       = 5*8alphanum         ; registered variants
                   / (DIGIT 3alphanum)

     extension     = singleton 1*("-" (2*8alphanum))

                                         ; Single alphanumerics
                                         ; "x" reserved for private use
     singleton     = DIGIT               ; 0 - 9
                   / %x41-57             ; A - W
                   / %x59-5A             ; Y - Z
                   / %x61-77             ; a - w
                   / %x79-7A             ; y - z

     privateuse    = "x" 1*("-" (1*8alphanum))

     grandfathered = irregular           ; non-redundant tags registered
                   / regular             ; during the RFC 3066 era

     irregular     = "en-GB-oed"         ; irregular tags do not match
                   / "i-ami"             ; the 'langtag' production and
                   / "i-bnn"             ; would not otherwise be
                   / "i-default"         ; considered 'well-formed'
                   / "i-enochian"        ; These tags are all valid,
                   / "i-hak"             ; but most are deprecated
                   / "i-klingon"         ; in favor of more modern
                   / "i-lux"             ; subtags or subtag
                   / "i-mingo"           ; combination
                   / "i-navajo"
                   / "i-pwn"
                   / "i-tao"
                   / "i-tay"
                   / "i-tsu"
                   / "sgn-BE-FR"
                   / "sgn-BE-NL"
                   / "sgn-CH-DE"

     regular       = "art-lojban"        ; these tags match the 'langtag'
                   / "cel-gaulish"       ; production, but their subtags
                   / "no-bok"            ; are not extended language
                   / "no-nyn"            ; or variant subtags: their meaning
                   / "zh-guoyu"          ; is defined by their registration
                   / "zh-hakka"          ; and all of these are deprecated
                   / "zh-min"            ; in favor of a more modern
                   / "zh-min-nan"        ; subtag or sequence of subtags
                   / "zh-xiang"

     alphanum      = (ALPHA / DIGIT)     ; letters and numbers
     */
    // @formatter:on
}
