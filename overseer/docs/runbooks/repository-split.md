# Runbook: move OVERSEER into its own repository

Status: **waiting for step 1 (user action).** Background: ADR 0002, decision 3.

## Step 1 - the single user action

On GitHub, create a new **empty private** repository:

- Owner: `velithorne`
- Name: `overseer`
- Visibility: **Private**
- Leave **"Add a README"**, **".gitignore"** and **"license"** all **off** (the repository must be
  completely empty so the history can be pushed as-is).

(Why: the Claude GitHub integration is refused when it tries to create repositories -
`403 Resource not accessible by integration`. If the Claude GitHub App is installed on "Only select
repositories", also add `overseer` to its repository access; with "All repositories" nothing more
is needed.)

Then tell Claude. Claude attaches the repository to the session and does steps 2-4.

## Step 2 - produce the history-preserving branch (in `inner-way`, nothing is modified)

```bash
git fetch origin claude/new-session-62gpdp
git checkout claude/new-session-62gpdp
git subtree split --prefix=overseer -b overseer-standalone
# overseer-standalone now holds only overseer/'s history, with overseer/ as the root
```

Verified: same tree hash as `overseer/` (`git rev-parse overseer-standalone^{tree}` equals
`git rev-parse HEAD:overseer`), commit messages and authorship preserved (SHAs change), no
Biofield Scanner files, canonical workflow at `.github/workflows/p0-gate.yml`.

## Step 3 - publish to the new repository

```bash
git push https://github.com/velithorne/overseer.git overseer-standalone:main
```

The new repository's CI (`p0-gate.yml`) starts automatically. Compare its result with the
`inner-way` run recorded in `docs/gates/P0.md`.

## Step 4 - only after the user accepts the new repository

Remove the temporary `.github/workflows/overseer-p0-gate.yml` from `inner-way` and, at the user's
discretion, `overseer/`. Nothing in the Biofield Scanner application is touched by any step.
