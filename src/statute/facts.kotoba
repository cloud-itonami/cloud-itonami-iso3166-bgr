(ns statute.facts
  "General-law compliance catalog for Bulgaria (BGR) -- extends this
  repo's existing `marketentry.facts` (public-procurement market-entry
  only, narrow scope) with a second, orthogonal catalog of statutes a
  company operating in this jurisdiction must generally track for
  compliance. Mirrors cloud-itonami-iso3166-jpn/-deu's `statute.facts`
  (ADR-2607141700, cloud-itonami-compliance-fact-federation).

  Every entry cites an OFFICIAL Bulgarian government-hosted URL --
  never fabricated:

  - `deu`.gesetze-im-internet.de / `jpn`.laws.e-gov.go.jp both host a
    single consolidated-law portal; Bulgaria has no exact equivalent
    single portal, so each entry here cites the specific official
    government BODY that actually hosts/administers that law's text
    instead (WebFetch-verified 2026-07-21, downloaded and parsed
    directly, not taken from a secondary summary):
    - Commerce Act (Търговски закон) -- hosted directly by the
      Ministry of Justice's own normdoc/document-API endpoint
      (justice.government.bg).
    - Labour Code (Кодекс на труда) -- hosted directly by the Ministry
      of Justice's own normdoc portal (justice.government.bg).
    - Personal Data Protection Act (Закон за защита на личните данни) --
      hosted by the Commission for Personal Data Protection (CPDP), the
      dedicated statutory data-protection supervisory authority
      (cpdp.bg), on its own official legislation page.

  A law not in this table has NO spec-basis, full stop; extend
  `catalog`, do not invent an id/url.")

(def catalog
  "iso3 -> vector of statute entries. `:statute/url` + `:statute/law-number`
  are the citation the governor requires before any compliance-fact
  proposal referencing this law can commit."
  {"BGR"
   [{:statute/id "bgr.commerce-act"
     :statute/title "Търговски закон (Commerce Act)"
     :statute/jurisdiction "BGR"
     :statute/kind :law
     :statute/law-number "ТЗ, обн. ДВ. бр.48 от 18.06.1991 г."
     :statute/url "https://justice.government.bg/api/part/GetBlob?hash=FD5CC887FC3AAF4F77F63C81D1B1D96B"
     :statute/url-provenance :official-ministry-of-justice
     :statute/enacted-date "1991-07-01"
     :statute/retrieved-at "2026-07-21"
     :statute/topic #{:corporate-governance :incorporation}}
    {:statute/id "bgr.personal-data-protection-act"
     :statute/title "Закон за защита на личните данни (Personal Data Protection Act)"
     :statute/jurisdiction "BGR"
     :statute/kind :law
     :statute/law-number "ЗЗЛД, обн. ДВ. бр.1 от 04.01.2002 г."
     :statute/url "https://cpdp.bg/en/legislation/personal-data-protection-act/"
     :statute/url-provenance :official-cpdp
     :statute/enacted-date "2002-01-04"
     :statute/retrieved-at "2026-07-21"
     :statute/topic #{:data-protection :privacy}}
    {:statute/id "bgr.labour-code"
     :statute/title "Кодекс на труда (Labour Code)"
     :statute/jurisdiction "BGR"
     :statute/kind :law
     :statute/law-number "КТ, обн. ДВ. бр.27 от 04.04.1986 г."
     :statute/url "https://www.justice.government.bg/home/normdoc/1594373121"
     :statute/url-provenance :official-ministry-of-justice
     :statute/enacted-date "1986-04-04"
     :statute/retrieved-at "2026-07-21"
     :statute/topic #{:labor :employment}}]})

(defn spec-basis
  "The jurisdiction's statute vector, or nil -- nil means NO spec-basis
  for that jurisdiction yet."
  [iso3]
  (get catalog iso3))

(defn coverage
  "Honest coverage report, same shape/discipline as `marketentry.facts/coverage`:
  never report a missing jurisdiction as covered."
  ([] (coverage (keys catalog)))
  ([iso3s]
   (let [have (filter catalog iso3s)
         missing (remove catalog iso3s)]
     {:requested (count iso3s)
      :covered (count have)
      :covered-jurisdictions (vec (sort have))
      :missing-jurisdictions (vec (sort missing))
      :note (str "cloud-itonami-iso3166-bgr statute.facts Wave 0 (ADR-2607141700): "
                 (count (get catalog "BGR")) " BGR statutes seeded with an "
                 "official government-hosted citation. Extend "
                 "`statute.facts/catalog`, never fabricate a law-id or URL.")})))

(defn by-topic
  "Statutes for `iso3` tagged with `topic` (e.g. :labor, :data-protection)."
  [iso3 topic]
  (filterv #(contains? (:statute/topic %) topic) (spec-basis iso3)))
