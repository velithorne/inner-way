# Phase 17A — Fold Echoes v0.1 Results

## Summary

Phase 17A introduces **Fold Echoes v0.1**: a conservative mechanism to attach weak passthrough files as low-cost echoes to already accepted template families.

## Implementation

### 1. Echo Model

- **Host family**: `template_skeleton` (optionally `mutation_chain` in future)
- **Echo member**: passthrough file that fits host structure (same const_blocks, same slot layout)
- **Echo payload**: slot values only (compact local deviation)
- **Net-positive only**: echo accepted only when `file_bytes - (slot_payload + overhead) >= min_net_gain`

### 2. Candidate Sourcing

- Runs after `template_skeleton` and `mutation_chain` commit
- Finds passthrough files that:
  - Same extension as host
  - Same line count as host
  - Same const_blocks (scaffold)
  - Different slot values (extracted deterministically)
- One echo per host; no file in multiple echo hosts

### 3. Echo Encoding

- `shared/echo_{i}.json`: `host_operator`, `host_idx`, `echo_path`, `slot_values`
- Maps record: `operator_id=fold_echo`, targets=[echo_path]
- Echo path excluded from passthrough inventory

### 4. Reconstruction

- Load host template from `shared/template_{host_idx}.json`
- Apply echo slot values via `reconstruct_echo_from_template`
- Byte-exact reconstruction

### 5. Integration

- `FoldEchoOperator` in orchestrator (after mutation_chain)
- `_ledger` injected into config for echo candidate discovery
- Package export, archive validate, explain, report updated

## Configuration

```json
"fold_echo": {
  "min_net_gain": 16,
  "min_echo_similarity": 0.85
}
```

## When Echoes Activate

Echoes attach when:
1. A template family exists (3+ members)
2. A passthrough file has the same structure (line count, const_blocks)
3. Net gain exceeds threshold

In practice, when all structurally similar files are in the same line-count bucket, the template accepts them all, leaving no passthrough. Echoes are most useful when:
- A file was in a rejected group (e.g. 2 files, below min_family_size) but shares structure with an accepted template
- Different grouping (e.g. by directory) creates separate buckets (future extension)

## Tests

- `test_extract_slot_values`: slot extraction from file matching template
- `test_reconstruct_echo`: reconstruction from const_blocks + slot_values
- `test_find_echo_candidates_requires_ledger`: empty without ledger
- `test_fold_echo_fixture_archive`: create, validate, reconstruct
- `test_fold_echo_explain_shows_echoes`: explain output

## Verification

- All Phase 17A tests pass
- Verification sweep passes
- Exact reconstruction preserved
- Strict validation preserved
