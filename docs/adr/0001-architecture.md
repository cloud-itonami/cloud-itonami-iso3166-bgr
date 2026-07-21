# ADR-0001: Architecture — Bulgaria market-entry compliance actor (`marketentry`)

**Status**: accepted
**Date**: 2026-07-21

## Context

`cloud-itonami-iso3166-bgr` was published as a `:blueprint` (docs +
`blueprint.edn` only, then a country-level `culture.facts` catalog in a
separate Wave 1 batch) but carried ZERO `src/marketentry` or
`src/statute` content -- its `:public-sector/market-entry-compliance`
domain, declared in `blueprint.edn`, was unimplemented. This ADR closes
that gap, following the pattern established by
`cloud-itonami-iso3166-jpn` (origin) and `cloud-itonami-iso3166-deu`
(the simpler, no-`goyoukiki` shape this blueprint also uses --
`blueprint.edn`'s `:required-technologies` does not list `:ontology`,
so this fork skips the `marketentry.goyoukiki` real-tender-fact bridge
JPN carries).

## Decision

Build the full governed-actor architecture for `marketentry`, mirroring
JPN/DEU's harness verbatim (StateGraph node names, governor hard/
escalate contract, phase 0-3 rollout, `Store` protocol with MemStore +
DatomicStore parity) and researching Bulgaria's own real market-entry
rules from scratch for the country-specific content:

- **Store**: `marketentry.store`, MemStore + DatomicStore, proven parity
  via contract test.
- **Registry**: `marketentry.registry`, pure DRAFT-certificate
  construction via `unsigned-certificate`, jurisdiction-scoped sequence
  numbering (`BGR-DFT-000000`, `BGR-SUB-000000`), plus
  `tax-arrears-de-minimis-threshold` / `tax-arrears-exceeds-threshold?`
  for the flagship check below.
- **Governor**: `:market-entry-compliance-governor` (family keyword from
  `blueprint.edn`).
- **Entity shape**: `engagement`, sequential draft -> submit on the same
  record. `high-stakes` = `#{:actuation/draft-filing
  :actuation/submit-filing}`.
- **Phase**: 0->3; `:filing/draft` and `:filing/submit` NEVER auto-
  commit at any phase.

### Flagship HARD check: `tax-arrears-exceeds-threshold` (genuinely new shape)

JPN's flagship check (`japan-resident-rep-missing`) and DEU's
(`local-rep-missing`) are both boolean ground-truth checks gated on a
`:requires-X-rep?` engagement flag. Researching Bulgaria's own Public
Procurement Act (Закон за обществените поръчки, ЗОП -- WebFetch-verified
against the Public Procurement Agency's own official hosted English
translation, `www2.aop.bg`) did not turn up comparably strong evidence
of a near-universal resident-representative mandate for Bulgaria (Art.
54(2)-(3) extends personal exclusion grounds to a participant's
representatives/attorneys-in-fact, but does not itself mandate having a
domestic representative) -- so rather than force JPN/DEU's rep-check
shape onto content that does not honestly support it, this fork's own
research surfaced a different, genuinely Bulgaria-specific and more
strongly evidenced rule to make the flagship check: ЗОП Art. 54(1) item
3 makes unpaid taxes/obligatory social-security contributions a
MANDATORY exclusion ground, and Art. 54(5) carves out a de-minimis
exception -- the ground does not apply where the unpaid amount is "up
to 1% of the sum of the annual total turnover for the last finished
financial year, but not more than BGN 50,000." `tax-arrears-exceeds-
threshold` independently recomputes that statutory formula against the
engagement's own declared `:annual-turnover` / `:tax-arrears-amount` and
HARD-holds `:filing/submit` when arrears exceed it. This is a numeric
statutory-threshold recompute, not a boolean flag -- a genuinely
different check SHAPE than either JPN's or DEU's flagship, reflecting
what Bulgaria's own law actually and verifiably supports rather than a
reflexive per-country copy.

### Other HARD checks (all unoverridable)

1. **spec-basis** -- never invent a jurisdiction's market-entry
   requirements (`marketentry.facts` G2 catalog: ЦАИС ЕОП / CAIS EOP,
   Търговски регистър, ЕИК for BGR).
2. **evidence-incomplete** -- draft/submit require a full assessment
   checklist on file.
3. **tax-arrears-exceeds-threshold** -- see above (FLAGSHIP).
4. **engagement-fee-mismatch** -- recompute `base-fee + monthly-rate ×
   monitoring-months` (ground-truth-recompute discipline).
5. **eik-unverified** -- conditional on `:requires-eik?` (ЕИК / Единен
   идентификационен код regime, assigned by the Registry Agency on
   Commercial Register entry).
6. **already-drafted / already-submitted** -- dedicated booleans, never
   a `:status` value.

### `statute.facts` (second, orthogonal catalog)

Three Bulgarian statutes, each cited against a government-hosted URL
that was actually fetched and parsed this iteration (not a secondary
summary): Commerce Act (Търговски закон, hosted directly by the
Ministry of Justice's `justice.government.bg` document API), Personal
Data Protection Act (Закон за защита на личните данни, hosted by the
Commission for Personal Data Protection, `cpdp.bg`), and Labour Code
(Кодекс на труда, hosted directly by `justice.government.bg`'s normdoc
portal). Bulgaria has no single consolidated-law portal exactly
equivalent to Japan's e-Gov or Germany's gesetze-im-internet.de, so
each entry cites whichever specific official government body actually
hosts that law's own text, rather than a single generic portal.

## Consequences

- `src/` now genuinely exists with real, tested, WebFetch-cited content
  for this blueprint's declared domain (`:public-sector/market-entry-
  compliance`) -- moves this repo's `manifest/itonami-fleet-audit.edn`
  `:prod-ready?` signal from `:stub` to `:active`.
- The existing `culture.facts` catalog (Wave 1, unrelated batch) is
  untouched.
- Sibling country blueprints can continue forking JPN/DEU/BGR and
  swapping in their own genuinely-researched `marketentry.facts` /
  `statute.facts` content and whichever flagship check their own law
  actually supports -- this ADR is itself evidence that the flagship
  check should be chosen from real research, not copied by rote.