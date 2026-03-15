# Origami Information Folding Engine — Master Blueprint

## 1. Premise

Build a software platform that treats information as a foldable structure rather than a flat file.

Instead of only compressing bytes, the system folds structure so that repeated, symmetric, hierarchical, and dependent relationships are brought together into a smaller and more useful representation.

## 2. Core Thesis

Standard compression asks:
- How can this data be represented with fewer bits?

The folding engine asks:
- How can this data be reshaped so distant but related structures become adjacent?
- How can repeated logic be folded into shared creases?
- How can the resulting object be unfolded later with minimal loss?
- Can folded structure be searched, reasoned over, or analyzed without fully unfolding it?

## 3. Version 1 Goal

Accept source-code folders and output:
- folded representation
- fold map
- reconstruction output
- compression statistics
- structure-preservation metrics
- visual/report summary

## 4. Why Source Code First

Source code contains:
- hierarchy
- repetition
- templates
- syntax trees
- dependencies
- symbols
- near-duplicates
- modular structure

## 5. Vocabulary

### Sheet
Original information object before folding.

### Crease
Transformation boundary defining where and how a fold can happen.

### Fold
Rule that maps multiple related structures into a shared representation.

### Layer
Information regions that overlap in the folded representation.

### Lock
Invariant preserved during folding.

### Stress
Cost or distortion introduced by a fold.

### Tear
Irrecoverable loss caused by an invalid or aggressive fold.

### Unfold
Reconstruction path used to recover the original or equivalent representation.

## 6. First Design Principle

A fold is only valid if it improves at least one of:
- compactness
- recoverability
- structure preservation
- searchability
- architectural insight

while keeping stress below threshold.

## 7. Phase 1 Success Criteria

The prototype succeeds if it demonstrates any of:
1. Better compression than ZIP/gzip on some structured codebases
2. Equal or near-equal compression while preserving richer structure metadata
3. Search capability inside the folded representation
4. Better duplicate/template discovery than standard compression
5. Architectural insights discovered through fold maps

## 8. In Scope

- Python projects
- JavaScript / TypeScript projects
- JSON / YAML
- Markdown / text

Out of scope for V1:
- binaries
- images
- video
- encrypted files
- already compressed archives

## 9. System Architecture

### Intake Layer
- scan folders
- classify files
- parse supported files
- build internal representations

### Fold Analyzer
- identify candidate creases
- exact repeats
- near-duplicates
- hierarchy similarity
- graph motifs
- estimated fold value

### Fold Engine
- apply fold operators
- create folded object
- maintain fold ledger
- preserve invariants

### Fold Ledger
Stores:
- all folds applied
- operator used
- source regions
- shared regions
- invariants
- stress scores
- unfold instructions

### Unfold Engine
- reconstruct original files
- verify output
- report fidelity

### Evaluation Layer
Measures:
- original size
- folded size
- ratio vs ZIP/gzip/zstd
- reconstruction fidelity
- semantic fidelity
- search cost
- fold density
- insight yield

### Visualization Layer
Displays:
- fold tree
- crease map
- repeated structure clusters
- stress heatmap
- unfold paths

## 10. Internal Data Model

### Project Sheet
- project_id
- file_nodes
- folder_nodes
- dependency_graph
- symbol_index
- fold_candidates
- fold_history

### File Sheet
- path
- language
- tokens
- ast
- symbols
- hashes
- repeated_regions
- template_signature

### Fold Record
- fold_id
- operator_type
- source_targets
- shared_representation
- invariants
- stress_score
- compression_gain
- unfold_recipe

## 11. Metrics

### Compression
- raw bytes in
- folded bytes out
- delta vs ZIP
- delta vs gzip
- delta vs zstd

### Recovery
- exact file reconstruction rate
- syntax-valid reconstruction rate
- token fidelity
- AST fidelity

### Structure
- repeated motif discovery count
- hierarchy merge count
- dependency motif reuse
- symbol compaction ratio

### Utility
- compressed search latency
- duplicate detection
- template family detection
- fold map explanation quality

## 12. Core Operators

### 12.1 Exact Repetition Fold
Purpose:
Collapse exact repeated regions into a single shared representation plus references.

Targets:
- duplicated helper functions
- repeated config blocks
- copied documentation sections
- repeated literal tables
- repeated code fragments

Invariants:
- exact recoverability in byte mode
- exact token recoverability in token mode
- source ordering preserved
- region boundaries preserved

### 12.2 Template Skeleton Fold
Purpose:
Extract common scaffolds from near-identical files and store variable slots separately.

Targets:
- boilerplate class files
- repeated API route files
- config templates
- repeated component skeletons

Invariants:
- syntax-valid reconstruction
- exact scaffold preservation
- slot ordering preserved
- file identity preserved

### 12.3 Symbol Table Fold
Purpose:
Compress repeated naming and symbol-reference structure into compact indexed maps without changing meaning.

Targets:
- repeated import aliases
- recurring class/function names
- enum keys
- config keys
- high-frequency symbols

Invariants:
- original symbol spelling recoverable
- reference targets preserved
- namespace context preserved
- scope preserved

### 12.4 Hierarchy Mirror Fold
Purpose:
Detect repeated folder/module shapes and compress them into reusable hierarchy templates.

Targets:
- repeated component directories
- mirrored service/controller/model structures
- duplicated package layouts
- repeated feature-module skeletons

Invariants:
- original path topology recoverable
- folder nesting preserved
- file membership preserved
- path reconstruction deterministic

### 12.5 Dependency Motif Fold
Purpose:
Compress repeated dependency subgraphs and recurring import/call patterns into shared motifs.

Targets:
- repeated import neighborhoods
- duplicated service wiring patterns
- recurring frontend dependency motifs
- repeated package dependency fragments

Invariants:
- edge direction preserved
- node identity recoverable
- dependency semantics preserved
- graph connectivity recoverable

## 13. Operator Interface

Every operator must implement:
- operator_id()
- operator_name()
- scope()
- detect_candidates()
- estimate_gain()
- estimate_stress()
- simulate()
- validate()
- apply()
- unfold()
- metrics()

Rules:
- deterministic in exact mode
- explicit invariant declaration
- explicit unfold recipe generation
- ledger serialization support
- simulation support before commit

## 14. Validation Pipeline

1. Candidate sanity check
2. Gain/stress estimation
3. Dry fold
4. Dry unfold
5. Fidelity checks
6. Hard rule check
7. Threshold check
8. Commit readiness check
9. Commit
10. Post-commit verification

Hard rules:
- no fold without unfold recipe
- no fold without declared invariants
- no fold if exact mode is enabled and exact recovery fails
- no silent rewrite of meaning-bearing symbols
- no merge across incompatible parse contexts

## 15. Folded Package Format

Directory-based package:

```text
folded_project_package/
├── manifest.json
├── ledger.json
├── shared/
├── maps/
├── reports/
└── snapshots/
```

## 16. Parser Contract

Each parser returns:
- raw text
- token stream
- parse success flag
- syntax tree if supported
- symbol references if supported
- import/dependency references if supported
- normalized fingerprints
- diagnostics

Minimum parser coverage:
- Python
- JavaScript / TypeScript
- JSON / YAML
- Markdown / text

## 17. Benchmark Plan

Datasets:
- Small Python utility project
- Medium Python service project
- Template-heavy frontend project
- Config-heavy infrastructure repo
- Synthetic repeated-boilerplate project
- Mixed docs + config + code project

Baselines:
- ZIP
- gzip
- zstd
- plain deduplication baseline

## 18. Repo Structure

```text
origami-fold-engine/
├── app/
├── core/
├── operators/
├── benchmarks/
├── schemas/
├── docs/
└── tests/
```

## 19. Engineering Rules

- build exact mode first
- every operator must be independently testable
- benchmark every new operator
- prefer explicit ledgering over hidden transformations
- preserve explainability in V1
- keep the system deterministic in exact mode
- avoid AI-driven fuzzy folding until rule-based core is solid

## 20. Phase 1 Definition of Done

Phase 1 is complete when:
- project folder can be imported
- supported files can be parsed
- first 5 operators run through full validation cycle
- folded package can be exported
- project can be reconstructed in exact mode for supported cases
- benchmark comparison against ZIP/gzip/zstd runs
- fold ledger is complete and inspectable
- at least one report view summarizes compression and insights
- automated tests cover parser, operator, validation, ledger, and end-to-end workflows

## 21. Next Move

Build the Phase 1 repository starter pack:
- folder structure
- base Python classes
- schema files
- config file
- placeholder operators
- parser stubs
- validation pipeline stubs
- benchmark harness stubs
