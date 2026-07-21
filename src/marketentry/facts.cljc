(ns marketentry.facts
  "Per-jurisdiction public-procurement market-entry regulatory catalog
  -- the G2-style spec-basis table the Market-Entry Compliance Governor
  checks every `:jurisdiction/assess` proposal against ('did the advisor
  cite an OFFICIAL public source for this jurisdiction's requirements,
  or did it invent one?').

  Bulgaria's real market-entry surface (WebFetch-verified 2026-07-21,
  see each entry's own citation): the ЦАИС ЕОП / CAIS EOP centralized
  electronic public-procurement platform (mandatory under the Public
  Procurement Act -- Закон за обществените поръчки, ЗОП -- Art. 39a),
  registration in the Търговски регистър (Commercial Register) and/or a
  professional register per ЗОП Art. 60(1), the ЕИК (Единен
  идентификационен код / Unified Identification Code) issued by the
  Registry Agency on Commercial Register entry (the functional successor
  to a standalone БУЛСТАТ number for Commercial-Register-registered
  merchants), and VAT registration with the National Revenue Agency
  (NRA) where applicable. As an EU member state, Bulgaria's public
  procurement is additionally bound by the TFEU principles the ЗОП
  itself codifies in Art. 2(1) (free movement, freedom of establishment,
  non-discrimination) -- no blanket national-content quota against other
  EU/EEA economic operators.

  Coverage is reported HONESTLY (see `coverage`): a jurisdiction not in
  this table has NO spec-basis, full stop -- the advisor must not
  fabricate one, and the governor holds if it tries.")

(def catalog
  "iso3 -> requirement map. `:required-evidence` mirrors the generic
  intake/portal-registration/filing evidence set; `:legal-basis` /
  `:owner-authority` / `:provenance` are the G2 citation the governor
  requires before any `:jurisdiction/assess` proposal can commit.
  `:rep-owner-authority` / `:rep-legal-basis` / `:rep-provenance` are the
  SEPARATE representative-related citation `facts/rep-spec-basis`
  exposes -- for BGR this is honestly scoped to what ЗОП Art. 54(2)-(3)
  actually says (the personal exclusion-grounds check extends to the
  natural persons who represent the participant/candidate, including a
  person acting under a Power of Attorney), NOT a claim that Bulgaria
  mandates a resident/domestic representative the way some other
  jurisdictions in this catalog do -- that stronger claim is not
  something this iteration could verify for Bulgaria, so it is not made."
  {"BGR" {:name "Bulgaria"
          :owner-authority "Агенция по обществените поръчки (Public Procurement Agency, AOP) / ЦАИС ЕОП (CAIS EOP centralized electronic public-procurement platform)"
          :legal-basis "Закон за обществените поръчки (ЗОП, Public Procurement Act, Prom. SG No.13/16.02.2016, in force from 15.04.2016) Art. 39a (mandatory centralized electronic platform) + Art. 60(1) (Commercial Register / professional-register requirement)"
          :national-spec "ЦАИС ЕОП (CAIS EOP) supplier/participant profile registration and award-procedure participation via the centralized electronic platform (ЗОП Art. 39a(4))"
          :provenance "https://www.eop.bg/"
          :required-evidence ["Търговски регистър (Commercial Register) extract / ЕИК (Unified Identification Code) record"
                              "ЦАИС ЕОП (CAIS EOP) supplier/participant-profile registration record"
                              "ДДС (VAT) registration record (Национална агенция за приходите / NRA, where applicable)"
                              "Декларация за представителство / Power-of-Attorney representative record (ЗОП чл. 54, ал. 2-3)"]
          :rep-owner-authority "Възложителят (contracting authority) / АОП (Public Procurement Agency)"
          :rep-legal-basis "ЗОП чл. 54, ал. 2 и 3 -- personal exclusion grounds under Art. 54(1) items 1, 2 and 7 extend to the natural persons who represent the participant/candidate (per its register entry or constitutive documents) and, where representation is by Power of Attorney, to that attorney-in-fact too. NOT a standalone resident/domestic-representative mandate."
          :rep-provenance "https://www2.aop.bg/wp-content/uploads/2024/04/Public-Procurement-Act.pdf"
          :corporate-number-owner-authority "Агенция по вписванията (Registry Agency, under the Ministry of Justice)"
          :corporate-number-legal-basis "ЕИК (Единен идентификационен код / Unified Identification Code), assigned on Commercial Register entry -- the functional successor identifier to a standalone БУЛСТАТ number for Commercial-Register-registered merchants"
          :corporate-number-provenance "https://portal.registryagency.bg/en/home-cr"}
   "USA" {:name "United States"
          :owner-authority "U.S. General Services Administration (GSA) / SAM.gov"
          :legal-basis "Federal Acquisition Regulation (FAR); System for Award Management"
          :national-spec "SAM.gov entity registration + NAICS self-certification"
          :provenance "https://sam.gov/"
          :required-evidence ["EIN record"
                              "SAM.gov registration record"
                              "State business registration record"
                              "Authorized-representative record"]}
   "DEU" {:name "Germany"
          :owner-authority "Beschaffungsamt des BMI / e-Vergabe platforms"
          :legal-basis "Gesetz gegen Wettbewerbsbeschränkungen (GWB) / VgV"
          :national-spec "e-Vergabe supplier registration under EU procurement directives"
          :provenance "https://www.evergabe-online.de/"
          :required-evidence ["Handelsregister extract"
                              "e-Vergabe registration record"
                              "USt-IdNr record"
                              "Authorized-representative record"]}})

(defn spec-basis
  "The jurisdiction's requirement map, or nil -- nil means NO spec-basis,
  and the governor must hold any proposal that tries to assess or file
  on it."
  [iso3]
  (get catalog iso3))

(defn coverage
  "Honest coverage report: how many of the requested jurisdictions actually
  have a spec-basis entry. Never report a missing jurisdiction as covered."
  ([] (coverage (keys catalog)))
  ([iso3s]
   (let [have (filter catalog iso3s)
         missing (remove catalog iso3s)]
     {:requested (count iso3s)
      :covered (count have)
      :covered-jurisdictions (vec (sort have))
      :missing-jurisdictions (vec (sort missing))
      :note (str "cloud-itonami-iso3166-bgr R0: " (count catalog)
                 " jurisdictions seeded with an official spec-basis. "
                 "This is a starting catalog for market-entry navigation, "
                 "not a survey of all ~194 jurisdictions -- extend "
                 "`marketentry.facts/catalog`, never fabricate a "
                 "jurisdiction's requirements.")})))

(defn required-evidence-satisfied?
  "Does `submitted` (a set/coll of evidence keywords or strings) satisfy
  every evidence item listed for `iso3`? Missing spec-basis -> never
  satisfied."
  [iso3 submitted]
  (when-let [{:keys [required-evidence]} (spec-basis iso3)]
    (let [need (count required-evidence)
          have (count (filter (set submitted) required-evidence))]
      (= need have))))

(defn evidence-checklist [iso3]
  (:required-evidence (spec-basis iso3) []))

(defn rep-spec-basis
  "The jurisdiction's representative-related requirement map, or nil when
  this catalog has no such regime. For BGR this is real but intentionally
  narrow -- see the `catalog` docstring."
  [iso3]
  (when-let [sb (spec-basis iso3)]
    (when (:rep-owner-authority sb)
      (select-keys sb [:rep-owner-authority :rep-legal-basis :rep-provenance]))))

(defn corporate-number-spec-basis
  "The jurisdiction's corporate-number / tax-id regime, or nil."
  [iso3]
  (when-let [sb (spec-basis iso3)]
    (when (:corporate-number-owner-authority sb)
      (select-keys sb [:corporate-number-owner-authority
                       :corporate-number-legal-basis
                       :corporate-number-provenance]))))
