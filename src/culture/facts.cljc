(ns culture.facts
  "Country-level regional-culture catalog for Bulgaria (BGR) -- national
  dishes, protected products, beverages, crafts, festivals and heritage
  sites, per ADR-2607171400 addendum 2 (cloud-itonami-municipality-
  culture-catalog Wave 1, in com-junkawasaki/root). Sibling namespace to
  `marketentry.facts` / `statute.facts` (ADR-2607141700); city-level
  counterparts live in the cloud-itonami-municipality-* repos.

  Catalog is keyed by UPPERCASE ISO3 (mirrors `statute.facts`); entries
  carry no :culture/municipality (that attribute is city-level only).

  Every entry cites a source URL that was actually fetched and read on
  :culture/retrieved-at -- never fabricated. Summaries state only what the
  cited source confirms. An item not in this table has NO spec-basis, full
  stop; extend `catalog`, do not invent an id/url.")

(def catalog
  "iso3 -> vector of culture entries."
  {"BGR"
   [{:culture/id "bgr.dish.banitsa"
     :culture/name "Banitsa"
     :culture/name-local "баница"
     :culture/country "BGR"
     :culture/kind :dish
     :culture/summary "Traditional pastry made in Bulgaria of whisked eggs, yogurt and white brined cheese layered between filo pastry; considered a symbol of Bulgarian cuisine and traditions."
     :culture/url "https://en.wikipedia.org/wiki/Banitsa"
     :culture/url-provenance :wikipedia-en
     :culture/retrieved-at "2026-07-17"}
    {:culture/id "bgr.dish.shopska-salad"
     :culture/name "Shopska salad"
     :culture/name-local "шопска салата"
     :culture/country "BGR"
     :culture/kind :dish
     :culture/summary "Cold salad of tomatoes, cucumbers, onions, peppers and sirene cheese; Bulgaria's most famous salad and national dish."
     :culture/url "https://en.wikipedia.org/wiki/Shopska_salad"
     :culture/url-provenance :wikipedia-en
     :culture/retrieved-at "2026-07-17"}
    {:culture/id "bgr.dish.tarator"
     :culture/name "Tarator"
     :culture/name-local "таратор"
     :culture/country "BGR"
     :culture/kind :dish
     :culture/summary "Cold cucumber-and-yogurt dish of the tzatziki family; in Bulgaria tarator is a traditional cold soup and a popular summer dish."
     :culture/url "https://en.wikipedia.org/wiki/Tzatziki"
     :culture/url-provenance :wikipedia-en
     :culture/retrieved-at "2026-07-17"}
    {:culture/id "bgr.beverage.rakia"
     :culture/name "Rakia"
     :culture/name-local "ракия"
     :culture/country "BGR"
     :culture/kind :beverage
     :culture/summary "Fruit brandy popular across Southeastern Europe; Bulgaria cites 14th-century pottery inscribed with the word rakiya, and the EU recognizes 12 brands of Bulgarian rakiya through PDO/PGI marks."
     :culture/url "https://en.wikipedia.org/wiki/Rakia"
     :culture/url-provenance :wikipedia-en
     :culture/retrieved-at "2026-07-17"}
    {:culture/id "bgr.product.bulgarian-yogurt"
     :culture/name "Bulgarian yogurt"
     :culture/name-local "кисело мляко"
     :culture/country "BGR"
     :culture/kind :product
     :culture/summary "Yogurt fermented with Lactobacillus delbrueckii subsp. bulgaricus; Bulgarian medical student Stamen Grigorov first examined the microflora of Bulgarian yogurt."
     :culture/url "https://en.wikipedia.org/wiki/Yogurt"
     :culture/url-provenance :wikipedia-en
     :culture/retrieved-at "2026-07-17"}
    {:culture/id "bgr.product.rose-oil"
     :culture/name "Bulgarian rose oil"
     :culture/country "BGR"
     :culture/kind :product
     :culture/summary "Essential oil extracted from rose petals, sold as 'Bulgarian Rose'; the Rose Valley near Kazanlak is among the major producers of attar of roses in the world."
     :culture/url "https://en.wikipedia.org/wiki/Rose_oil"
     :culture/url-provenance :wikipedia-en
     :culture/retrieved-at "2026-07-17"}
    {:culture/id "bgr.craft.chiprovtsi-kilim"
     :culture/name "Chiprovtsi kilim"
     :culture/country "BGR"
     :culture/kind :craft
     :culture/summary "Handmade flatwoven kilim rugs, part of Bulgarian national heritage, produced in Chiprovtsi since the 17th century; the kilim making of Chiprovtsi was inscribed on UNESCO's Intangible Cultural Heritage list in 2014."
     :culture/url "https://en.wikipedia.org/wiki/Chiprovtsi_kilim"
     :culture/url-provenance :wikipedia-en
     :culture/retrieved-at "2026-07-17"}
    {:culture/id "bgr.festival.kukeri"
     :culture/name "Kukeri"
     :culture/name-local "кукери"
     :culture/country "BGR"
     :culture/kind :festival
     :culture/summary "Bulgarian ritual in which elaborately costumed men with wooden animal masks and large bells dance through villages around New Year and before Lent to scare away evil spirits; related traditions appear across the Balkans."
     :culture/url "https://en.wikipedia.org/wiki/Kukeri"
     :culture/url-provenance :wikipedia-en
     :culture/retrieved-at "2026-07-17"}
    {:culture/id "bgr.heritage.rila-monastery"
     :culture/name "Rila Monastery"
     :culture/name-local "Рилски манастир"
     :culture/country "BGR"
     :culture/kind :heritage
     :culture/summary "The largest and most famous Eastern Orthodox monastery in Bulgaria, founded in the 10th century in the Rila Mountains; a UNESCO World Heritage Site since 1983."
     :culture/url "https://en.wikipedia.org/wiki/Rila_Monastery"
     :culture/url-provenance :wikipedia-en
     :culture/retrieved-at "2026-07-17"}]})

(defn spec-basis [iso3] (get catalog iso3))

(defn coverage
  ([] (coverage (keys catalog)))
  ([iso3s]
   (let [have (filter catalog iso3s)
         missing (remove catalog iso3s)]
     {:requested (count iso3s)
      :covered (count have)
      :covered-jurisdictions (vec (sort have))
      :missing-jurisdictions (vec (sort missing))
      :note (str "cloud-itonami-iso3166-bgr culture catalog "
                 "(ADR-2607171400 addendum 2, Wave 1): " (count (get catalog "BGR"))
                 " BGR entries, each with a fetched-and-read citation. "
                 "Extend `culture.facts/catalog`, never fabricate an id/url.")})))

(defn by-kind [iso3 kind]
  (filterv #(= (:culture/kind %) kind) (spec-basis iso3)))
