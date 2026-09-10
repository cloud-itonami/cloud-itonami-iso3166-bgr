(ns marketentry.store
  "SSoT for the Bulgaria (BGR) market-entry compliance actor, behind a
  `Store` protocol so the backend is a swap, not a rewrite -- the same
  seam every prior cloud-itonami actor in this fleet uses.

    - `MemStore`     -- atom of EDN. The deterministic default for
                        dev/tests/demo (no deps).
    - `DatomicStore` -- backed by `langchain.db`, a Datomic-API-compatible
                        EAV store.

  Both implement the same protocol and pass the same contract
  (test/marketentry/store_contract_test.clj).

  The primary entity here is an `engagement` -- filing-draft and
  filing-submit actuation events apply SEQUENTIALLY to the SAME
  engagement record (draft first, submit later). Dedicated
  double-actuation-guard booleans (`:drafted?`/`:submitted?`, never a
  `:status` value).

  The ledger stays append-only on every backend."
  (:require #?(:clj  [clojure.edn :as edn]
               :cljs [cljs.reader :as edn])
            [marketentry.registry :as registry]
            [langchain.db :as d]
            [marketplace.persist :as persist]))

(defprotocol Store
  (engagement [s id])
  (all-engagements [s])
  (assessment-of [s engagement-id] "committed jurisdiction assessment, or nil")
  (ledger [s])
  (draft-history [s] "the append-only filing-draft history")
  (submit-history [s] "the append-only filing-submit history")
  (next-draft-sequence [s jurisdiction])
  (next-submit-sequence [s jurisdiction])
  (engagement-already-drafted? [s engagement-id])
  (engagement-already-submitted? [s engagement-id])
  (commit-record! [s record] "apply a committed op's record to the SSoT")
  (append-ledger! [s fact]   "append one immutable decision fact")
  (with-engagements [s engagements] "replace/seed the engagement directory"))

;; ----------------------------- demo data -----------------------------

(defn demo-data
  "A small, self-contained engagement set covering both actuation
  lifecycles (draft, submit) plus the governor's own new checks.
  `:annual-turnover` / `:tax-arrears-amount` are BGN, last finished
  financial year -- ground truth for the
  `tax-arrears-exceeds-threshold` flagship check (ЗОП Art. 54(1)(3) +
  (5))."
  []
  {:engagements
   {"eng-1" {:id "eng-1" :operator "Kita Fabrication EOOD" :portal "CAIS EOP"
             :base-fee 500000 :monthly-rate 30000 :monitoring-months 12
             :claimed-fee 860000.0
             :annual-turnover 2000000 :tax-arrears-amount 0
             :requires-eik? true :eik-verified? true
             :drafted? false :submitted? false
             :jurisdiction "BGR" :status :intake}
    "eng-2" {:id "eng-2" :operator "Atlantis LLC" :portal "CAIS EOP"
             :base-fee 500000 :monthly-rate 30000 :monitoring-months 12
             :claimed-fee 860000.0
             :annual-turnover 2000000 :tax-arrears-amount 0
             :requires-eik? true :eik-verified? true
             :drafted? false :submitted? false
             :jurisdiction "ATL" :status :intake}
    "eng-3" {:id "eng-3" :operator "Minami Systems" :portal "CAIS EOP"
             :base-fee 500000 :monthly-rate 30000 :monitoring-months 12
             :claimed-fee 999000.0
             :annual-turnover 2000000 :tax-arrears-amount 0
             :requires-eik? true :eik-verified? true
             :drafted? false :submitted? false
             :jurisdiction "BGR" :status :intake}
    "eng-4" {:id "eng-4" :operator "Higashi Export OOD" :portal "CAIS EOP"
             :base-fee 500000 :monthly-rate 30000 :monitoring-months 12
             :claimed-fee 860000.0
             :annual-turnover 1000000 :tax-arrears-amount 25000
             :requires-eik? true :eik-verified? true
             :drafted? false :submitted? false
             :jurisdiction "BGR" :status :intake}
    "eng-5" {:id "eng-5" :operator "Nishi Logistics" :portal "CAIS EOP"
             :base-fee 500000 :monthly-rate 30000 :monitoring-months 12
             :claimed-fee 860000.0
             :annual-turnover 2000000 :tax-arrears-amount 0
             :requires-eik? true :eik-verified? false
             :drafted? false :submitted? false
             :jurisdiction "BGR" :status :intake}
    "eng-6" {:id "eng-6" :operator "Chuo Civic Tech" :portal "CAIS EOP"
             :base-fee 400000 :monthly-rate 25000 :monitoring-months 6
             :claimed-fee 550000.0
             :annual-turnover 1200000 :tax-arrears-amount 0
             :requires-eik? true :eik-verified? true
             :drafted? false :submitted? false
             :jurisdiction "BGR" :status :intake}}})

;; ----------------------------- shared commit logic -----------------------------

(defn- draft-filing!
  [s engagement-id]
  (let [e (engagement s engagement-id)
        seq-n (next-draft-sequence s (:jurisdiction e))
        result (registry/register-draft engagement-id (:jurisdiction e) seq-n)]
    {:result result
     :engagement-patch {:drafted? true
                        :draft-number (get result "draft_number")}}))

(defn- submit-filing!
  [s engagement-id]
  (let [e (engagement s engagement-id)
        seq-n (next-submit-sequence s (:jurisdiction e))
        result (registry/register-submit engagement-id (:jurisdiction e) seq-n)]
    {:result result
     :engagement-patch {:submitted? true
                        :submit-number (get result "submit_number")}}))

;; ----------------------------- MemStore (default) -----------------------------

(defrecord MemStore [a]
  Store
  (engagement [_ id] (get-in @a [:engagements id]))
  (all-engagements [_] (sort-by :id (vals (:engagements @a))))
  (assessment-of [_ engagement-id] (get-in @a [:assessments engagement-id]))
  (ledger [_] (:ledger @a))
  (draft-history [_] (:draft-records @a))
  (submit-history [_] (:submit-records @a))
  (next-draft-sequence [_ jurisdiction] (get-in @a [:draft-sequences jurisdiction] 0))
  (next-submit-sequence [_ jurisdiction] (get-in @a [:submit-sequences jurisdiction] 0))
  (engagement-already-drafted? [_ engagement-id] (boolean (get-in @a [:engagements engagement-id :drafted?])))
  (engagement-already-submitted? [_ engagement-id] (boolean (get-in @a [:engagements engagement-id :submitted?])))
  (commit-record! [s {:keys [effect path value payload]}]
    (case effect
      :engagement/upsert
      (swap! a update-in [:engagements (:id value)] merge value)

      :assessment/set
      (swap! a assoc-in [:assessments (first path)] payload)

      :engagement/mark-drafted
      (let [engagement-id (first path)
            {:keys [result engagement-patch]} (draft-filing! s engagement-id)
            jurisdiction (:jurisdiction (engagement s engagement-id))]
        (swap! a (fn [state]
                   (-> state
                       (update-in [:draft-sequences jurisdiction] (fnil inc 0))
                       (update-in [:engagements engagement-id] merge engagement-patch)
                       (update :draft-records registry/append result))))
        result)

      :engagement/mark-submitted
      (let [engagement-id (first path)
            {:keys [result engagement-patch]} (submit-filing! s engagement-id)
            jurisdiction (:jurisdiction (engagement s engagement-id))]
        (swap! a (fn [state]
                   (-> state
                       (update-in [:submit-sequences jurisdiction] (fnil inc 0))
                       (update-in [:engagements engagement-id] merge engagement-patch)
                       (update :submit-records registry/append result))))
        result)
      nil)
    s)
  (append-ledger! [_ fact] (swap! a update :ledger conj fact) fact)
  (with-engagements [s engagements] (when (seq engagements) (swap! a assoc :engagements engagements)) s))

(defn seed-db
  "A MemStore seeded with the demo engagement set."
  []
  (->MemStore (atom (assoc (demo-data)
                           :assessments {}
                           :ledger [] :draft-sequences {} :draft-records []
                           :submit-sequences {} :submit-records []))))

;; ----------------------------- DatomicStore (langchain.db) -----------------------------

(def ^:private schema
  {:engagement/id                   {:db/unique :db.unique/identity}
   :assessment/engagement-id        {:db/unique :db.unique/identity}
   :ledger/seq                      {:db/unique :db.unique/identity}
   :draft-record/seq                {:db/unique :db.unique/identity}
   :submit-record/seq               {:db/unique :db.unique/identity}
   :draft-sequence/jurisdiction     {:db/unique :db.unique/identity}
   :submit-sequence/jurisdiction    {:db/unique :db.unique/identity}})

(defn- enc [v] (pr-str v))
(defn- dec* [s] (when s (edn/read-string s)))

(defn- engagement->tx [{:keys [id operator portal base-fee monthly-rate monitoring-months claimed-fee
                               annual-turnover tax-arrears-amount
                               requires-eik? eik-verified?
                               drafted? submitted?
                               jurisdiction status draft-number submit-number]}]
  (cond-> {:engagement/id id}
    operator                              (assoc :engagement/operator operator)
    portal                                (assoc :engagement/portal portal)
    base-fee                              (assoc :engagement/base-fee base-fee)
    monthly-rate                          (assoc :engagement/monthly-rate monthly-rate)
    monitoring-months                     (assoc :engagement/monitoring-months monitoring-months)
    claimed-fee                           (assoc :engagement/claimed-fee claimed-fee)
    annual-turnover                       (assoc :engagement/annual-turnover annual-turnover)
    (some? tax-arrears-amount)            (assoc :engagement/tax-arrears-amount tax-arrears-amount)
    (some? requires-eik?)                 (assoc :engagement/requires-eik? requires-eik?)
    (some? eik-verified?)                 (assoc :engagement/eik-verified? eik-verified?)
    (some? drafted?)                      (assoc :engagement/drafted? drafted?)
    (some? submitted?)                    (assoc :engagement/submitted? submitted?)
    jurisdiction                          (assoc :engagement/jurisdiction jurisdiction)
    status                                (assoc :engagement/status status)
    draft-number                          (assoc :engagement/draft-number draft-number)
    submit-number                         (assoc :engagement/submit-number submit-number)))

(def ^:private engagement-pull
  [:engagement/id :engagement/operator :engagement/portal :engagement/base-fee :engagement/monthly-rate
   :engagement/monitoring-months :engagement/claimed-fee
   :engagement/annual-turnover :engagement/tax-arrears-amount
   :engagement/requires-eik? :engagement/eik-verified?
   :engagement/drafted? :engagement/submitted?
   :engagement/jurisdiction :engagement/status :engagement/draft-number :engagement/submit-number])

(defn- pull->engagement [m]
  (when (:engagement/id m)
    {:id (:engagement/id m) :operator (:engagement/operator m) :portal (:engagement/portal m)
     :base-fee (:engagement/base-fee m) :monthly-rate (:engagement/monthly-rate m)
     :monitoring-months (:engagement/monitoring-months m) :claimed-fee (:engagement/claimed-fee m)
     :annual-turnover (:engagement/annual-turnover m)
     :tax-arrears-amount (:engagement/tax-arrears-amount m 0)
     :requires-eik? (boolean (:engagement/requires-eik? m))
     :eik-verified? (boolean (:engagement/eik-verified? m))
     :drafted? (boolean (:engagement/drafted? m)) :submitted? (boolean (:engagement/submitted? m))
     :jurisdiction (:engagement/jurisdiction m) :status (:engagement/status m)
     :draft-number (:engagement/draft-number m) :submit-number (:engagement/submit-number m)}))

(defrecord DatomicStore [conn]
  Store
  (engagement [_ id]
    (pull->engagement (d/pull (d/db conn) engagement-pull [:engagement/id id])))
  (all-engagements [_]
    (->> (d/q '[:find [?id ...] :where [?e :engagement/id ?id]] (d/db conn))
         (map #(pull->engagement (d/pull (d/db conn) engagement-pull [:engagement/id %])))
         (sort-by :id)))
  (assessment-of [_ engagement-id]
    (dec* (d/q '[:find ?p . :in $ ?eid
                :where [?a :assessment/engagement-id ?eid] [?a :assessment/payload ?p]]
              (d/db conn) engagement-id)))
  (ledger [_]
    (->> (d/q '[:find ?s ?f :where [?e :ledger/seq ?s] [?e :ledger/fact ?f]] (d/db conn))
         (sort-by first)
         (mapv (comp dec* second))))
  (draft-history [_]
    (->> (d/q '[:find ?s ?r :where [?e :draft-record/seq ?s] [?e :draft-record/record ?r]] (d/db conn))
         (sort-by first)
         (mapv (comp dec* second))))
  (submit-history [_]
    (->> (d/q '[:find ?s ?r :where [?e :submit-record/seq ?s] [?e :submit-record/record ?r]] (d/db conn))
         (sort-by first)
         (mapv (comp dec* second))))
  (next-draft-sequence [_ jurisdiction]
    (or (d/q '[:find ?n . :in $ ?j
              :where [?e :draft-sequence/jurisdiction ?j] [?e :draft-sequence/next ?n]]
            (d/db conn) jurisdiction)
        0))
  (next-submit-sequence [_ jurisdiction]
    (or (d/q '[:find ?n . :in $ ?j
              :where [?e :submit-sequence/jurisdiction ?j] [?e :submit-sequence/next ?n]]
            (d/db conn) jurisdiction)
        0))
  (engagement-already-drafted? [s engagement-id]
    (boolean (:drafted? (engagement s engagement-id))))
  (engagement-already-submitted? [s engagement-id]
    (boolean (:submitted? (engagement s engagement-id))))
  (commit-record! [s {:keys [effect path value payload]}]
    (case effect
      :engagement/upsert
      (d/transact! conn [(engagement->tx value)])

      :assessment/set
      (d/transact! conn [{:assessment/engagement-id (first path) :assessment/payload (enc payload)}])

      :engagement/mark-drafted
      (let [engagement-id (first path)
            {:keys [result engagement-patch]} (draft-filing! s engagement-id)
            jurisdiction (:jurisdiction (engagement s engagement-id))
            next-n (inc (next-draft-sequence s jurisdiction))]
        (d/transact! conn
                     [(engagement->tx (assoc engagement-patch :id engagement-id))
                      {:draft-sequence/jurisdiction jurisdiction :draft-sequence/next next-n}
                      {:draft-record/seq (count (draft-history s)) :draft-record/record (enc (get result "record"))}])
        result)

      :engagement/mark-submitted
      (let [engagement-id (first path)
            {:keys [result engagement-patch]} (submit-filing! s engagement-id)
            jurisdiction (:jurisdiction (engagement s engagement-id))
            next-n (inc (next-submit-sequence s jurisdiction))]
        (d/transact! conn
                     [(engagement->tx (assoc engagement-patch :id engagement-id))
                      {:submit-sequence/jurisdiction jurisdiction :submit-sequence/next next-n}
                      {:submit-record/seq (count (submit-history s)) :submit-record/record (enc (get result "record"))}])
        result)
      nil)
    s)
  (append-ledger! [s fact]
    (d/transact! conn [{:ledger/seq (count (ledger s)) :ledger/fact (enc fact)}])
    fact)
  (with-engagements [s engagements]
    (when (seq engagements) (d/transact! conn (mapv engagement->tx (vals engagements)))) s))

(defn datomic-store
  ([] (datomic-store {}))
  ([{:keys [engagements]}]
   (let [s (->DatomicStore (d/create-conn schema))]
     (with-engagements s engagements))))

(defn datomic-seed-db
  []
  (datomic-store (demo-data)))

;; ----------------------------- KotobaseStore (the Worker's) -----------------------------

(defrecord KotobaseStore [st seed]
  Store
  (engagement [_ id]
    (when id (persist/get-doc (persist/ctx st :engagement :engagement/id) id)))
  (all-engagements [_]
    (sort-by :id (persist/all-docs (persist/ctx st :engagement :engagement/id))))
  (assessment-of [_ engagement-id]
    (persist/get-doc (persist/ctx st :assessment :assessment/engagement-id) engagement-id))

  (ledger [_] (persist/read-events (persist/stream-ctx st :ledger)))
  (draft-history [_] (persist/read-events (persist/stream-ctx st :draft)))
  (submit-history [_] (persist/read-events (persist/stream-ctx st :submit)))

  ;; MemStore keeps a per-jurisdiction counter; here the count of the stream
  ;; IS that counter. This actor covers exactly one jurisdiction, and every
  ;; draft appends exactly one record, so the two agree - and a length is a
  ;; read where a counter document would be a read-modify-write that two
  ;; concurrent drafts would collide on.
  (next-draft-sequence [s _jurisdiction] (count (draft-history s)))
  (next-submit-sequence [s _jurisdiction] (count (submit-history s)))

  (engagement-already-drafted? [s engagement-id]
    (boolean (:drafted? (engagement s engagement-id))))
  (engagement-already-submitted? [s engagement-id]
    (boolean (:submitted? (engagement s engagement-id))))

  (commit-record! [s {:keys [effect path value payload]}]
    (case effect
      :engagement/upsert
      (let [c (persist/ctx st :engagement :engagement/id)
            existing (persist/get-doc c (:id value))]
        (persist/put-doc! c (merge existing value)))

      :assessment/set
      (persist/put-doc! (persist/ctx st :assessment :assessment/engagement-id)
                        (assoc payload :assessment/engagement-id (first path)))

      :engagement/mark-drafted
      (let [engagement-id (first path)
            {:keys [result engagement-patch]} (draft-filing! s engagement-id)
            c (persist/ctx st :engagement :engagement/id)]
        (persist/append-event! (persist/stream-ctx st :draft) seed result)
        (persist/put-doc! c (merge (persist/get-doc c engagement-id) engagement-patch))
        result)

      :engagement/mark-submitted
      (let [engagement-id (first path)
            {:keys [result engagement-patch]} (submit-filing! s engagement-id)
            c (persist/ctx st :engagement :engagement/id)]
        (persist/append-event! (persist/stream-ctx st :submit) seed result)
        (persist/put-doc! c (merge (persist/get-doc c engagement-id) engagement-patch))
        result)
      nil)
    s)

  (append-ledger! [_ fact]
    (persist/append-event! (persist/stream-ctx st :ledger) seed fact)
    fact)

  (with-engagements [s engagements]
    (let [c (persist/ctx st :engagement :engagement/id)]
      (doseq [[_ e] engagements] (persist/put-doc! c e)))
    s))

(defn kotobase-store
  "The durable Store over a HOST-INJECTED database api.

  `marketplace.persist/store` throws when `db-api` is missing or partial, so
  this actor cannot come up looking durable while writing to nothing."
  [{:keys [db-api seq-fn]}]
  (->KotobaseStore (persist/store {:db-api db-api :actor "marketentry-bgr"})
                   (or seq-fn (let [n (atom 0)] #(swap! n inc)))))
