# cloud-itonami-iso3166-bgr

Open ISO 3166 Blueprint for **BGR**: Republic of Bulgaria --
**`:implemented`**.

This repository designs **and implements** a forkable OSS business for
an independent public-sector market-entry consultant: an already-
incorporated operator (e.g. a `cloud-itonami-cofog-{code}`,
`cloud-itonami-isco-{code}`, `cloud-itonami-unspsc-{segment}` or
`cloud-itonami-{ISIC}` blueprint fork) gets a Compliance Advisor +
independent **Market-Entry Compliance Governor** to navigate public-
procurement registration, local business/tax registration, and
regulatory-compliance rules in Bulgaria, so the operator can win and
service a government contract without hiring a full in-house compliance
department.

## Official surface

- Procurement: ЦАИС ЕОП / CAIS EOP -- the centralized electronic
  public-procurement platform (`https://www.eop.bg/`), operated under
  the Public Procurement Act (Закон за обществените поръчки, ЗОП) and
  administered by the Public Procurement Agency (АОП,
  `https://www2.aop.bg/`).
- Business registration: Търговски регистър (Commercial Register),
  operated by the Registry Agency
  (`https://portal.registryagency.bg/`, under the Ministry of Justice)
  -- issues the ЕИК (Единен идентификационен код / Unified
  Identification Code) on entry.
- Tax: VAT registration via the National Revenue Agency (NRA,
  `https://nra.bg/`).

## Implementation (R0)

| Piece | Location |
|---|---|
| Actor namespaces | `src/marketentry/*` |
| Governor | `:market-entry-compliance-governor` |
| Ops | `:engagement/intake` · `:jurisdiction/assess` · `:filing/draft` · `:filing/submit` |
| Flagship HARD check | `tax-arrears-exceeds-threshold` (ЗОП Art. 54(1)(3) + (5) de-minimis threshold, independently recomputed -- see `docs/adr/0001-architecture.md`) |
| Compliance catalog | `src/statute/facts.kotoba` -- Commerce Act, Personal Data Protection Act, Labour Code |
| Tests | `kbb -M:dev:test` (35 tests / 114 assertions) |
| Demo | `kbb -M:dev:run` |
| Architecture ADR | [`docs/adr/0001-architecture.md`](docs/adr/0001-architecture.md) |

`:filing/submit` is never in any phase's `:auto` set -- human sign-off
is structural, not a rollout milestone.

## No robotics premise -- digital/data service exemption

Market-entry and procurement-compliance navigation is a pure data/software
service with no physical-domain work (portal registration, document
checklists, regulatory-change monitoring) -- the same exemption class as
`cloud-itonami-6310` (HR SaaS replacement) and `cloud-itonami-gtin-*`.
`blueprint.edn` sets `:itonami.blueprint/robotics false` and
`:required-technologies` lists only real capabilities (`:identity`,
`:forms`, `:dmn`, `:bpmn`, `:audit-ledger`), no `:robotics`.

## Core Contract

```text
operator intake + prior filing history
        |
        v
Compliance Advisor -> Market-Entry Compliance Governor -> filing draft, or human sign-off
        |
        v
gated portal registration / filing submission + audit ledger
```

No automated proposal can submit a portal registration or filing the
governor refuses, suppress a compliance record, or claim a legal/tax
conclusion the governor has not cleared. `:filing/submit` is never in any
phase's `:auto` set -- it always requires human sign-off.

## What this is NOT

- **Not the government of Bulgaria.** This blueprint is an independent
  operator the government contracts with or that bids into its
  procurement -- never the government itself, and never an official
  channel.
- **Not legal or tax advice.** Every regulatory claim must cite the
  official source and route final filings to Bulgarian-licensed counsel
  or a registered agent where the law requires licensed representation.

## Capability layer

Required capabilities (`blueprint.edn`):

- :identity
- :forms
- :dmn
- :bpmn
- :audit-ledger

See [`docs/business-model.md`](docs/business-model.md) and
[`docs/operator-guide.md`](docs/operator-guide.md).

## License

AGPL-3.0-or-later.

## Culture catalog

Alongside the market-entry / statute catalogs, this repo carries a
**country-level regional-culture catalog** (ADR-2607171400 addendum 2,
`cloud-itonami-municipality-culture-catalog` Wave 1, in
`com-junkawasaki/root`) -- national dishes, protected products, beverages,
crafts, festivals and heritage sites for Bulgaria:

- `src/culture/facts.kotoba` -- the catalog, source of truth (keyed by
  uppercase ISO3, mirroring `statute.facts`).
- `schema/culture.edn` -- DataScript schema.
- `data/culture-tx.edn` -- derived DataScript tx-data (regenerated from
  the catalog, never hand-edited).

City-level counterparts live in the `cloud-itonami-municipality-*` repos.
Same provenance discipline as the compliance catalogs: every entry cites a
source URL that was actually fetched and read on `:culture/retrieved-at`;
summaries state only what the cited source confirms. An item not in
`culture.facts/catalog` has no spec-basis -- never fabricate one.
