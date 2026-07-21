# Operator Guide

## First Deployment

1. Confirm the client's incorporation/legal-entity status is complete
   (route to `cloud-itonami-M6910` or local counsel first if not).
2. Register the client's intake: business type, target public function,
   prior filing history in Bulgaria if any.
3. Run the advisor in read-only mode against ЦАИС ЕОП / CAIS EOP (the
   centralized electronic public-procurement platform,
   `https://www.eop.bg/`), governed by the Public Procurement Act
   (Закон за обществените поръчки, ЗОП).
4. Compare the checklist against the client's current documentation
   (Търговски регистър entry / ЕИК, VAT registration, ЦАИС ЕОП profile).
5. Enable gated filing-draft assistance once the Market-Entry Compliance
   Governor contract is trusted; actual submission always requires human
   sign-off.

## Minimum Production Controls

- client-owned data store for business/tax registration documents
- clear provenance (official portal/regulation citation) for every
  requirement surfaced
- approval workflow for any portal registration or filing submission
- independent recomputation of the ЗОП Art. 54(5) tax/social-security
  arrears de-minimis threshold before any `:filing/submit` -- never
  trust a claimed arrears figure
- named referral relationship with Bulgarian-licensed counsel or a
  registered agent for anything beyond checklist/draft assistance
- monthly audit export

## Certification

Certified operators must prove data provenance, audit traceability, that
automated actions cannot bypass the Market-Entry Compliance Governor, and
a working referral relationship with Bulgarian-licensed counsel or a
registered agent for whatever licensed representation the law of
Bulgaria requires for actual public-procurement filings.
