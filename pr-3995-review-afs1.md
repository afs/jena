# Code review — PR #3995

**GH-3994: Improved TDB2 index access for single pattern queries**
apache/jena · `20260613-index-for-basic-distinct` → `main` · 31 files, +3469/−12 · author: Claus Stadler

## How this was reviewed

The PR's classes were compiled against the installed `6.3.0-SNAPSHOT` jars and queries were run
against a patched TDB2, comparing results with skip scan **off** against skip scan **on**. Findings
marked **Confirmed** below have a reproduced symptom, not just a reading of the code.

## Summary

| # | Severity | File | Issue |
|---|----------|------|-------|
| 1 | Wrong answers | `TransformDistinctPlacement:136` | Projection after DISTINCT push drops pattern variables |
| 2 | Wrong answers | `OpExecutorTDB2SkipScan:451` | Input-binding variables dropped from results |
| 3 | Wrong answers | `OpExecutorTDB2SkipScan:138` | Cache key substituted, candidate computed unsubstituted |
| 4 | Wrong answers | `NodeIdUtils:35` | `NodeIdMax` is not an upper bound for inline doubles |
| 5 | Exception | `TuplePatternSpec:93` | `equalityLinks`/`neededSlots` undersized → AIOOBE |
| 6 | Wrong answers | `Record:211` | `padRight` zero-pads the upper bound → empty range |
| 7 | Dead path | `OpExecutorTDB2SkipScan:252` | Constant-slot bit never cleared; residual conditions unreachable |
| 8 | Performance | `PatternQuery:59` | `record` with `Node[]` breaks cache equals/hashCode |
| 9 | Latent crash | `OpExecutorTDB2SkipScan:306` | Non-`TupleIndexRecord` index passes the guard, CCE in `exec` |
| 10 | Latent | `OpExecutorTDB2SkipScan:582` | `remapValueConditions` unmaps an index-space column |
| 11 | Latent | `BPTreeDistinctKeyPrefixIterator:61` | New 4-arg `create` drops `keyPrefixLength` validation |
| 12 | Latent | `BPTreeDistinctKeyPrefixIterator:158` | Subtree shortcut emits a page with no range check |
| 13 | Contract | `OpExtSkipScan:89` | `equals`/`hashCode` inconsistent; un-namespaced symbol; first-wins executor |
| 14 | Test gap | `TestOpExecutorTDB2SkipScan:138` | `addResourceObjectsAsSubjects` writes to a throwaway copy |
| 15 | Cleanup | `CandidateIndex:27` | Unused class and methods shipped with the new package |

Six of these (1–6) are wrong-answer or exception bugs reachable from ordinary SPARQL. Two structural
observations cut across the rest: the residual-condition machinery has never executed (#7), so #10
and the comparator's `numResidualConditions` tie-break are untested code; and the generative test
suite covers less than it appears to (#14), which is plausibly why several of these survived.

---

## 1. Projection after DISTINCT push drops pattern variables — **Confirmed**

`jena-tdb2/.../skipscan/TransformDistinctPlacement.java:136`

After pushing DISTINCT below an `OpExtend`, the re-applied projection uses **only** the
extend-defined variables, projecting away every pattern variable — including the DISTINCT / GROUP BY
variables themselves.

```sparql
SELECT DISTINCT ?o { ?s <ex/p> ?o BIND(?o AS ?z) }
```

rewrites to `(project (?x) (extend ((?x ?o)) (skipScan ...)))` and returns rows where `?o` is unbound
and only `?z` is set. The aggregate case is worse:

```sparql
SELECT ?p (MIN(?o) AS ?m) { ?s ?p ?o BIND(LANG(?o) AS ?l) } GROUP BY ?p
```

rewrites to `(group (?p) ((?.0 (min ?o))) (project (?l) (extend ...)))` and returns a **single empty
row** instead of one row per predicate.

**Fix:** project to `inScope(projectVars) ∪ extendVars`, not `extendVars` alone.

## 2. Input-binding variables dropped from results — **Confirmed**

`jena-tdb2/.../skipscan/OpExecutorTDB2SkipScan.java:451`

Result bindings are built with `new BindingNodeId()` — no parent binding — so every variable from the
input binding is silently discarded. The fail-over path uses `executeOp` with the binding and does
preserve them, so the two paths disagree.

```sparql
SELECT * { ?x <ex/q> ?y { SELECT DISTINCT ?p { ?s ?p ?o } } }
```

optimizes to `(sequence (bgp ...) (distinct (project (?p) ...)))`, so the `OpDistinct` runs with
non-empty input bindings. Skip scan off: `(?x=:a)(?y=:b)(?p=…)`. Skip scan on: rows contain only
`?p` — `?x` and `?y` are gone.

**Fix:** `new BindingNodeId(binding)`; the binding has to be threaded into `exec(TupleIndex, ...)`.

## 3. Cache key substituted, candidate computed unsubstituted — **Confirmed**

`jena-tdb2/.../skipscan/OpExecutorTDB2SkipScan.java:138`

The index cache is keyed on the **substituted** pattern (`cacheKey` from `subst`) but the candidate
is computed from the **unsubstituted** `patternQuery`, so the `IndexMatch`
(`keyLen`/`valLen`/residuals/`canDoDirectDistinct`) does not describe the pattern actually executed.

Data:

```turtle
:s1 :p :o1 . :s1 :p :o2 . :s2 :p2 :o1 . :s3 :p3 :o9 . :a :q :o1 .
```

```sparql
SELECT * { ?x <ex/q> ?o { SELECT DISTINCT ?p ?o { ?s ?p ?o } } }
```

Skip scan off → 3 rows. Skip scan on → **5 rows**, including `?p=:p` twice and `?p=:p3` (whose object
is `:o9`, not `:o1`). The substituted `?o = :o1` constraint is never applied — it is neither in the
key prefix nor in `residualConditions` — and the stale `canDoDirectDistinct=true` suppresses the dedup.

**Fix:** `findBestCandidate(subst, execCxt)`.

## 4. `NodeIdMax` is not an upper bound for inline doubles — **Confirmed**

`jena-tdb2/.../skipscan/NodeIdUtils.java:35`

`NodeId.createRaw(NodeIdType.SPECIAL, Long.MAX_VALUE)` encodes to `0xF0FFFFFFFFFFFFFF`, which is not
an upper bound over storable NodeIds: inline negative `xsd:double` values encode above it, so range
upper bounds silently exclude real records.

| Literal | Encoding |
|---|---|
| `"-1.0E6"^^xsd:double` | `F12E848000000000` |
| `"-1.0E7"^^xsd:double` | `F16312D000000000` |
| `"-INF"^^xsd:double`   | `FFF0000000000000` |

All exceed `F0FFFFFFFFFFFFFF` — true for any negative double with |v| ≥ 2¹⁷. End to end, with
`:s1 :p "-1.0E7"^^xsd:double` present, `SELECT ?o { ?s :p ?o }` returns 3 rows but
`SELECT DISTINCT ?o { ?s :p ?o }` returns only 2; the double is silently missing.

**Fix:** an all-`0xFF` key is the only safe max — but note `Record.incrementKey` throws on it, so
that interaction needs settling too.

## 5. `equalityLinks`/`neededSlots` undersized → AIOOBE — **Confirmed**

`jena-tdb2/.../skipscan/TuplePatternSpec.java:93`

Both arrays are sized `n` (tuple length), but the nested `i`/`j` loop can append up to `n(n−1)/2`
entries, overflowing them when a variable occurs three or more times.

```sparql
SELECT DISTINCT ?s { ?s ?s ?s }
```

throws `ArrayIndexOutOfBoundsException: Index 3 out of bounds for length 3` at
`TuplePatternSpec.java:93`, via `findBestCandidate`. `GRAPH ?s { ?s ?s ?s }` gives
`Index 4 out of bounds for length 4`.

**Fix:** size `n*(n-1)/2`, or `break` after the first match since the links are transitive.

## 6. `padRight` zero-pads the upper bound → empty range — **Confirmed**

`jena-db/jena-dboe-base/.../base/record/Record.java:211`

`padRight` pads with zero bytes. That is correct for a lower bound but makes an upper-bound record
too small. The `length < arr.length` branch is also dead/wrong: it returns a same-length copy, so
nothing is truncated, yet a new `Record` is forced.

```sparql
SELECT DISTINCT ?zz { <ex/s1> ?p ?o }
```

All needed slots are constants, so `n=1` while the SPO key is 3 slots. `max` is built as
`sId‖0x00×16` instead of `sId‖0xFF×16`, and the record-buffer range `[sId‖0…0, sId‖0…01)` matches
nothing. Skip scan off → 1 row; skip scan on → 0 rows.

**Fix:** pad the upper bound with `0xFF`, or use a separate `padRightMax` for the max record.

## 7. Constant-slot bit never cleared; residual conditions unreachable — **Confirmed**

`jena-tdb2/.../skipscan/OpExecutorTDB2SkipScan.java:252`

In `computeIndexMatch`, the `else` branch that records a constant as a residual condition never
clears that slot's bit in `uncoveredSlotsMask`. Any index with a constant after the first variable is
therefore rejected, and `residualConditions` can never be non-empty.

Probed directly: pattern `(?s, :p, ?o)` with projection `[?s]` → SPO returns `null`, OSP returns
`null`; only POS matches (`keyLen=1`, `canDoDirectDistinct=false`, `residualConditions=null`). SPO
would have given a 2-slot `(s,p)` prefix with a filter on `p`.

Consequences: the whole residual-condition path is unreachable dead code — `remapValueConditions`,
the `Iter.filter`, and `IndexMatch.numResidualConditions` in the comparator; worse plans are chosen;
and non-default TDB2 index configurations can fall through to the fail-over path.

**Fix:** add `uncoveredSlotsMask &= ~(1 << tupleSlot);` in the `else` branch.

## 8. `record` with `Node[]` breaks cache equals/hashCode — **Confirmed**

`jena-tdb2/.../skipscan/PatternQuery.java:59`

`PatternQuery` is a record with a `Node[] tuple` component, so the generated `equals`/`hashCode` use
array identity. `PatternQuery.toCacheKey(pq).equals(PatternQuery.toCacheKey(pq))` is `false` and the
hash codes differ.

The `ConcurrentHashMap<PatternQuery, SkipScanCandidate>` index cache therefore never hits:
`findBestCandidate` — which enumerates every index and runs `computeIndexMatch` — is re-run for every
input binding, and `symSkipScanBestIndexCache` accumulates one unbounded, never-evicted entry per
binding in the execution context.

**Fix:** override `equals`/`hashCode` with `Arrays.equals`/`Arrays.hashCode`, or key on a `List<Node>`.

## 9. Non-`TupleIndexRecord` index passes the guard → CCE in `exec` — **Plausible**

`jena-tdb2/.../skipscan/OpExecutorTDB2SkipScan.java:306`

`planCandidate` rejects an index only when it *is* a `TupleIndexRecord` whose range index is not a
`BPlusTree`. An index that is not a `TupleIndexRecord` at all fails the `instanceof` pattern, passes
the guard, produces a candidate, and then hits the unchecked cast at line 377
(`(TupleIndexRecord)tupleIndex`) → `ClassCastException` at query time.

**Fix:**

```java
if (!(tupleIndex instanceof TupleIndexRecord r) || !(r.getRangeIndex() instanceof BPlusTree))
    return null;
```

## 10. `remapValueConditions` unmaps an index-space column — **Plausible**

`jena-tdb2/.../skipscan/OpExecutorTDB2SkipScan.java:582`

`remapValueConditions` applies `tupleMap.unmapIdx()` to a column number that is already in index
space — the `j` stored by `computeIndexMatch` at line 252 is an index position, unlike
`equalityLinks`/`projs`, which hold tuple slots. For index OSP with a constant at index position 1,
`unmapIdx(1)` returns the position of tuple slot 1, so `extractNodeId(recordKey, idxx)` reads a
different column and the filter accepts or rejects the wrong rows.

Latent today only because #7 makes `residualConditions` unreachable; it becomes live the moment #7 is
fixed. Separately, the code tests `nodeId == null`, but `nodeTable.getNodeIdForNode` returns
`NodeId.NodeDoesNotExist`, never `null` — so an unknown constant yields a filter that matches nothing
rather than short-circuiting to an empty result. Use `NodeId.isDoesNotExist(nodeId)`.

## 11. New 4-arg `create` drops `keyPrefixLength` validation — **Plausible**

`jena-db/jena-dboe-trans-data/.../bplustree/BPTreeDistinctKeyPrefixIterator.java:61`

The new 4-arg `create` overload omits the `keyPrefixLength` validation (`>= 1` and
`<= recordFactory.keyLength()`) that the 2-arg overload enforces — and a length of 0 is genuinely
passed by the skip-scan caller.

`SELECT DISTINCT ?zz { ?s ?p ?o }` yields an `IndexMatch` with `keyLen=0`, `valLen=0`, so `exec`
calls `bpt.distinctByKeyPrefix(0, min, max)`. With length 0 every `Arrays.compare*` returns 0, so
`haveSamePrefix`/`comparePrefix` are vacuously true and the iterator happens to yield one record —
correct only by accident. A `keyPrefixLength` larger than the key length would read past the prefix
with no bounds check.

## 12. Subtree shortcut emits a page with no range check — **Plausible**

`jena-db/jena-dboe-trans-data/.../bplustree/BPTreeDistinctKeyPrefixIterator.java:158`

`moveOnCurrent`'s uniform-prefix subtree shortcut returns `singletonIterator(subtreeMin)` with no
`[minRecord, maxRecord]` check — while the sibling shortcut in `getRecordsIterator` was given exactly
such a check in this PR.

Correctness then rests entirely on `BPTreeNode.iterator(min, max)` never handing out a page outside
the range, but its own javadoc says "There may be other records as well", and `loadStack` passes
`maxRecord` (inclusive) to a method documented as taking `maxRec` exclusive. Any boundary subtree
whose records all share one prefix is emitted unconditionally, so a record outside the requested
range can be yielded.

**Fix:** apply the line-189 check here too:

```java
comparePrefix(subtreeMin, minRecord) >= 0 && comparePrefix(subtreeMax, maxRecord) <= 0
```

## 13. `equals`/`hashCode` inconsistent; un-namespaced, first-wins executor — **Confirmed**

`jena-tdb2/.../skipscan/OpExtSkipScan.java:89`

`hashCode()` delegates to `PatternQuery.hashCode()` — identity-based, per #8 — while `equalTo`
compares `effectiveOp()`. Two structurally identical `OpExtSkipScan` nodes compare equal but hash
differently, breaking any hash-based use of `Op` (algebra caches, `HashSet`/`HashMap` of ops) with
silent duplicates.

Separately, the executor is stashed under an un-namespaced `Symbol.create("opExecutor")`, and
`setOpExecutorIfAbsent` keeps the **first** `OpExecutor` for the whole execution. In a query with
skip scans under different active graphs —

```sparql
{ GRAPH :g1 { SELECT DISTINCT ... } } UNION { GRAPH :g2 { SELECT DISTINCT ... } }
```

— the fail-over path `opExecutor.executeOp(...)` runs against the wrong graph.

**Fix:** use `SystemTDB.allocSymbol(...)` for the symbol, and derive the hash from the effective op.

## 14. `addResourceObjectsAsSubjects` writes to a throwaway copy — **Confirmed**

`jena-tdb2/src/test/.../skipscan/TestOpExecutorTDB2SkipScan.java:138`

The method copies its argument into a fresh local `DatasetGraph dataDsg` and adds the extra
`?x rdf:type rdfs:Resource` quads to that copy, never to the parameter — so it is a no-op.

The caller at line 127 (`addResourceObjectsAsSubjects(dataDsg)`) expects the source dataset to be
enriched with objects-as-subjects so the generated find/query patterns exercise more lookup shapes.
Every write goes to a local copy dropped on return, so that coverage is silently never generated and
the suite is weaker than it looks.

**Fix:** write directly to `referenceDsg` — collect into a list first to avoid concurrent modification.

## 15. Dead code and smaller cleanups — **Confirmed**

`jena-tdb2/.../skipscan/CandidateIndex.java:27`

Never referenced anywhere; nor are `OpExtSkipScan.setOpExecutor`, `SkipScanRewrite.usesSkipScan`,
`TuplePatternSpec.getVar`, `TuplePatternSpec.getIdxVar`. An unused 32-line record file plus four
unused public methods have to be read and kept compiling by future maintainers.

Related, in the same change:

- `OpExecutorTDB2.java:50` — `import org.apache.jena.tdb2.solver.skipscan.OpExecutorTDB2SkipScan;` is unused.
- `SystemTDB` now imports `OpExecutorTDB2` purely for a javadoc `{@link}`, creating a sys → solver
  dependency where solver already depends on sys.
- `TransformDistinctPlacement.tryPush` takes a `BiPredicate` whose second argument both call sites ignore.
- `jena-tdb2/pom.xml` pulls in the whole `commons-math4-legacy` **beta** artifact only to enumerate
  `Combinations.of(4, k)` — 16 bitmask iterations would cover it.
- `Test_SPARQL_TDB.sparql10`'s `assertNotEquals(actualTable.size(), 1)` passes for 0 rows, so it would
  not catch the rewrite breaking.
