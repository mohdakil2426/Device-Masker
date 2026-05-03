# Libxposed Skill — Structural Audit Report

**Skill:** `libxposed-modern-xposed-api`
**Audit Date:** 2026-05-03
**Scope:** File linking, cross-file sync, structure, format — no code changes

---

## 1. Complete File Map

```
libxposed/
├── SKILL.md                                      (190 lines, 9.1 KB) ← entry point
├── references/
│   ├── api-reference.md                          (476 lines, 15.9 KB)
│   ├── patterns-and-examples.md                  (574 lines, 17.4 KB)
│   ├── project-setup.md                          (283 lines, 9.2 KB)
│   └── service-reference.md                      (300 lines, 9.7 KB)
├── examples/
│   ├── raw-scraped/
│   │   ├── INDEX.md                              (68 lines) ← ground-truth index
│   │   ├── api-javadoc/
│   │   │   ├── 01-package-and-XposedInterface.md (299 lines)
│   │   │   ├── 02-Chain-HookBuilder-HookHandle-Hooker.md (149 lines)
│   │   │   ├── 03-Invoker-ExceptionMode-Module-Interface.md (381 lines)
│   │   │   └── 04-error-package.md               (62 lines)
│   │   ├── service-javadoc/
│   │   │   └── 01-service-complete.md            (294 lines)
│   │   └── lsposed-wiki/
│   │       └── 01-wiki-complete.md               (336 lines)
│   └── github/
│       ├── LSPosed.wiki/       ← 8 wiki .md files (full git clone)
│       ├── api/                ← full libxposed/api repo clone
│       ├── example/            ← full libxposed/example repo clone
│       ├── helper/             ← full libxposed/helper repo clone
│       ├── service/            ← full libxposed/service repo clone
│       └── references/         ← DUPLICATE of top-level references/ folder
│           ├── api-reference.md         (byte-for-byte identical, 16277 bytes)
│           ├── patterns-and-examples.md (byte-for-byte identical, 17841 bytes)
│           ├── project-setup.md         (byte-for-byte identical, 9400 bytes)
│           └── service-reference.md     (byte-for-byte identical, 9903 bytes)
└── docs/
    └── reports/
        └── skill-audit-report.md       ← this file
```

---

## 2. File Linking Issues

### L-01 [HIGH] SKILL.md has zero references to examples/ tree

SKILL.md (190 lines) references only the four `references/` files.
The entire `examples/` tree — 12+ files including raw Javadoc, wiki content, and full repo clones — is completely invisible to any agent reading this skill. No pointer, no mention, no discovery path.

Impact: Ground-truth verification is impossible from the skill's entry point.

### L-02 [MEDIUM] External LSPosed wiki URL with no offline fallback

SKILL.md line 29 cites:
```
https://github.com/LSPosed/LSPosed/wiki/Develop-Xposed-Modules-Using-Modern-Xposed-API
```
The full verbatim content of this page exists locally at:
- `examples/raw-scraped/lsposed-wiki/01-wiki-complete.md`
- `examples/github/LSPosed.wiki/Develop-Xposed-Modules-Using-Modern-Xposed-API.md`

The upstream repo was **archived read-only on March 27, 2026** (per raw-scraped/INDEX.md line 58).
Neither the archive status nor the offline copy paths are mentioned in SKILL.md.

### L-03 [MEDIUM] raw-scraped/INDEX.md has no hyperlinks

INDEX.md lists its sub-files in plain-text tables only. No clickable paths.
A reader must manually navigate the directory tree to find
`api-javadoc/01-package-and-XposedInterface.md` etc.

### L-04 [MEDIUM] references/ files have no citation to raw-scraped sources

None of the four reference files contains any link back to the raw-scraped ground truth.
When a reader encounters a signature in `api-reference.md`, there is no way to navigate
to the authoritative scrape file to verify it. The chain of evidence is fully broken.

| Reference file | Has raw-scrape citation? | Has Javadoc URL? |
|---|---|---|
| api-reference.md | No | Yes (per section) |
| patterns-and-examples.md | No | Top-level only |
| project-setup.md | No | Top-level only |
| service-reference.md | No | Top-level only |

### L-05 [MEDIUM] examples/github/ has no README or index

Six subdirectories exist under `examples/github/` (LSPosed.wiki, api, example,
helper, service, references). There is no README, no index file, and no explanation of:
- What each subdirectory contains
- How they relate to the skill's reference files
- Which files are useful to consult
- When they were cloned

`raw-scraped/INDEX.md` fills this role for the `raw-scraped/` tree.
`examples/github/` has no equivalent.

### L-06 [LOW] Native-Hook.md is fully orphaned

`examples/github/LSPosed.wiki/Native-Hook.md` (130 lines of C++ native hook documentation)
has zero inbound references from any other file in the skill.

### L-07 [LOW] Module-Scope.md is orphaned legacy content

`examples/github/LSPosed.wiki/Module-Scope.md` (legacy `xposedscope` metadata approach)
has zero inbound references and no deprecation label at the file level.

---

## 3. Cross-File Sync Issues

### S-01 [CRITICAL] OnServiceListener — wrong method names in reference files

| File | Method names |
|---|---|
| raw-scraped/service-javadoc/01-service-complete.md (lines 164-174) | `onServiceBind(XposedService service)` and `onServiceDied(XposedService service)` |
| raw-scraped/INDEX.md (lines 43-45) | Explicitly flags this as a known discrepancy |
| references/service-reference.md (lines 80-82) | `onServiceConnected(XposedService)` and `onServiceDisconnected()` — WRONG |
| references/patterns-and-examples.md (lines 435-444) | Same wrong names |
| examples/github/references/ copies | Same wrong names (duplicate) |

Note: `onServiceDisconnected()` in the reference takes NO parameter, but the correct
`onServiceDied(XposedService service)` takes the service instance. Doubly incorrect.

The INDEX.md explicitly discovered and documented this divergence but it was never
propagated back to fix the reference files.

### S-02 [CRITICAL] OnScopeEventListener — wrong method names and count

| File | Methods |
|---|---|
| raw-scraped/service-javadoc/01-service-complete.md (lines 188-197) | `onScopeRequestApproved(List<String> approved)` and `onScopeRequestFailed(String message)` — 2 methods |
| raw-scraped/INDEX.md (lines 47-49) | Explicitly flags this discrepancy |
| references/service-reference.md (lines 168-172) | `onScopeRequestGranted(List<String>)`, `onScopeRequestDenied(List<String>)`, `onScopeRequestError(List<String>, String)` — 3 methods, all wrong |
| references/patterns-and-examples.md (lines 480-494) | Same three wrong methods |

The failure callback is especially wrong: raw source shows `onScopeRequestFailed(String message)`
(single string parameter), reference files show `onScopeRequestError(List<String> packages, String reason)`
(two parameters, different types). Both name and signature are incorrect.

### S-03 [HIGH] XposedFrameworkError base class — three-way inconsistency

| File | Stated base class |
|---|---|
| raw-scraped/api-javadoc/04-error-package.md lines 18-23 | `Object → Throwable → Error → XposedFrameworkError` i.e. `extends Error` |
| references/api-reference.md line 435 | `public class XposedFrameworkError extends RuntimeException` — WRONG |
| examples/github/references/api-reference.md line 435 | Same wrong statement (duplicate) |
| SKILL.md | Not stated (only "do NOT catch" advice given) |

Raw scrape is authoritative: the class extends `Error`, not `RuntimeException`.
The distinction matters for catch block scoping and compiler warnings.

### S-04 [MEDIUM] exceptionMode in module.prop — wiki vs Javadoc discrepancy not annotated

- LSPosed wiki (raw-scraped/lsposed-wiki/01-wiki-complete.md lines 86-91): `exceptionMode` is NOT listed in the properties table.
- Package-summary Javadoc (raw-scraped/api-javadoc/01-package-and-XposedInterface.md line 37): `exceptionMode` IS listed as optional.
- references/project-setup.md lines 136-148: Lists `exceptionMode` correctly (sourced from Javadoc).

The reference file is correct, but there is no annotation explaining the source discrepancy.
A developer comparing project-setup.md to the wiki table will find a property in one but not the other, with no explanation of why.

### S-05 [MEDIUM] Missing concrete scope example from raw Javadoc

raw-scraped/api-javadoc/01-package-and-XposedInterface.md lines 55-56 explicitly states:
> "`com.android.providers.settings` is not a valid scope target; modules should use the `system` scope"

This concrete example does not appear in references/project-setup.md's scope section (lines 158-162),
which only states the general rule. The specific example is missing from the file a developer reads.

### S-06 [LOW] Scrape date inconsistency across all raw files

Every raw-scraped file header says `# Scraped: 2025-05-03`.
The LSPosed archive note in raw-scraped/INDEX.md line 58 says it was archived on `Mar 27, 2026`.
Files cannot have been scraped in 2025 and include a 2026 archive note as current information.
Either the year in every scrape header is wrong (should be 2026), or the archive date is wrong.
These two facts are mutually contradictory.

---

## 4. Format and Structure Issues

### F-01 [MEDIUM] api-reference.md — no table of contents (476 lines)

Skill-creator rule: "For large reference files (>300 lines), include a table of contents."
api-reference.md has 14 distinct class/interface sections across 476 lines and no TOC.
A reader must scroll the entire file to locate a specific class.

### F-02 [LOW] patterns-and-examples.md — no TOC (574 lines)

574 lines, 21 examples. Numbered headings (`## 1.` through `## 21.`) partially compensate
but there is no linked summary at the top.

### F-03 [MEDIUM] examples/github/references/ — unexplained duplicate folder

Four files byte-for-byte identical to references/:

| File | Size in references/ | Size in examples/github/references/ |
|---|---|---|
| api-reference.md | 16277 bytes | 16277 bytes |
| patterns-and-examples.md | 17841 bytes | 17841 bytes |
| project-setup.md | 9400 bytes | 9400 bytes |
| service-reference.md | 9903 bytes | 9903 bytes |

No README, no comment, no annotation explaining why this copy exists.
Any fix to S-01 through S-03 must be applied to both locations or the copies diverge silently.

### F-04 [MEDIUM] examples/github/ — no README

Six subdirectories with no discovery mechanism. No file explains:
- What each repo clone contains
- When it was cloned
- Which files inside are actually useful
- How these relate to the reference files

### F-05 [LOW] Legacy content in raw scrapes — no section-level deprecation labels

raw-scraped/lsposed-wiki/01-wiki-complete.md contains three pages merged into one file:
- Native Hook (lines 148-280) — uses legacy entry paths (assets/native_init, assets/xposed_init)
- Module-Scope (lines 103-146) — legacy xposedscope meta-data approach
- New XSharedPreferences (lines 284-336) — deprecated since v2.1.0

Only the file-level header at line 287 marks XSharedPreferences as legacy.
The Native Hook and Module-Scope sections have no inline deprecation markers.
A reader skimming to those sections encounters legacy content without warning.

### F-06 [LOW] Hardcoded API version contradicts "check latest" instruction

references/project-setup.md line 56 has:
```kotlin
compileOnly("io.github.libxposed:api:101.0.1")
```
Lines 82-86 of the same file say "Always check latest at [Maven Central URLs]."
The hardcoded version in the code example contradicts the prose. A developer copying the
block will use 101.0.1 without checking.

### F-07 [LOW] service-reference.md — vague version note

Line 28: "Last known release: March 2026 (check Maven Central for current)."
No version number is given. With the scrape date inconsistency (S-06) this note
cannot be reliably interpreted.

### F-08 [LOW] Heading style inconsistency within raw-scraped/

api-javadoc/ files use `# RAW SCRAPE — ClassName` as section delimiters.
lsposed-wiki/01-wiki-complete.md uses `# Page: Home`, `# Page: Module Scope` etc.
Inconsistent patterns make automation or programmatic parsing harder.

---

## 5. Issue Register Summary

| ID | Category | Severity | File(s) | Description |
|---|---|---|---|---|
| L-01 | Linking | High | SKILL.md | Zero references to examples/ tree |
| L-02 | Linking | Medium | SKILL.md | External wiki URL, no offline fallback, archive not noted |
| L-03 | Linking | Medium | raw-scraped/INDEX.md | No hyperlinks in sub-file table |
| L-04 | Linking | Medium | All references/ files | No citation to raw-scraped ground truth |
| L-05 | Linking | Medium | examples/github/ | No README or index |
| L-06 | Linking | Low | Native-Hook.md | Fully orphaned |
| L-07 | Linking | Low | Module-Scope.md | Orphaned legacy content |
| S-01 | Sync | Critical | service-reference.md, patterns-and-examples.md | OnServiceListener wrong method names |
| S-02 | Sync | Critical | service-reference.md, patterns-and-examples.md | OnScopeEventListener wrong names and count |
| S-03 | Sync | High | api-reference.md (both copies) | XposedFrameworkError extends RuntimeException vs Error |
| S-04 | Sync | Medium | project-setup.md | exceptionMode source discrepancy not annotated |
| S-05 | Sync | Medium | project-setup.md | Missing concrete scope example from raw Javadoc |
| S-06 | Sync | Low | All raw-scraped files | Scrape date 2025 vs archive date 2026 contradiction |
| F-01 | Format | Medium | api-reference.md | No TOC at 476 lines |
| F-02 | Format | Low | patterns-and-examples.md | No TOC at 574 lines |
| F-03 | Format | Medium | examples/github/references/ | Unexplained duplicate folder |
| F-04 | Format | Medium | examples/github/ | No README |
| F-05 | Format | Low | lsposed-wiki/01-wiki-complete.md | Legacy content without section-level deprecation labels |
| F-06 | Format | Low | project-setup.md | Hardcoded version vs "check latest" instruction |
| F-07 | Format | Low | service-reference.md | Vague version note |
| F-08 | Format | Low | raw-scraped/ files | Heading style inconsistency |

**Total issues: 21** (2 Critical, 2 High, 5 Medium, 12 Low)

---

## 6. Recommended Resolution Order

1. **S-01, S-02** — Fix OnServiceListener and OnScopeEventListener in all four affected files (two in references/, two in examples/github/references/). INDEX.md already has the correct names.
2. **S-03** — Fix XposedFrameworkError base class in both copies of api-reference.md.
3. **L-01** — Add a "Ground-Truth Sources" section to SKILL.md pointing into examples/.
4. **F-03** — Decide fate of examples/github/references/ duplicate. Add a README or remove it.
5. **F-01** — Add TOC to api-reference.md.
6. **L-05** — Add README.md to examples/github/ explaining the six sub-repos.
7. **L-02** — Note LSPosed archive status and offline copy path in SKILL.md.
8. All Low severity items as maintenance work.
