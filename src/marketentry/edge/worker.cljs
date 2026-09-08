(ns marketentry.edge.worker
  "The BGR market-entry actor's Worker — routes only.

  Everything that is not specific to this actor lives in `marketplace.edge`:
  the kotobase client, the prefetch/run/flush bracket, the ledger ordinal,
  the fail-closed check for a missing seed. Second non-marketplace actor to
  use it, after cloud-itonami-isic-0111.

  ## What is NOT shared, measured rather than assumed

  This is one of 186 `iso3166-*` actors with a byte-identical
  `operation.cljc` and an identical 13-method `Store` protocol, which looks
  like one implementation would serve all of them. It would not. With
  docstrings, comments and string literals stripped, their `store.cljc` files
  still fall into 184 distinct code shapes and their `governor.cljc` into 184
  — each country's checks, thresholds and currency are in the control flow,
  not in a data table. The transport is shared; the actor is not.

  `KotobaseStore` therefore lives in `marketentry.store` rather than here:
  `commit-record!` needs `draft-filing!` and `submit-filing!`, which are
  private to that file.

  ## Approval is deliberately not a route

  The actor compiles with the default in-memory checkpointer, and a Worker
  isolate does not survive to the next request. A resume route would look
  like one and reliably find nothing to resume — an escalation that silently
  never completes is how a governor gets bypassed in practice. A durable
  checkpointer is what makes that route honest."
  (:require [kotoba.lang.text :as str]
            [langgraph.graph :as g]
            [marketentry.operation :as operation]
            [marketentry.store :as store]
            [marketplace.edge :as edge]))

(defn- run-operation
  "One supervised actor pass over a durable store.

  `:wants` names exactly the engagement the request named. The actor then
  runs synchronously — the same code path `marketentry.sim` and the tests
  drive — and every write it recorded is flushed in one transact."
  [client body]
  (let [eid (get body "engagement-id")
        request (cond-> {:op (keyword (get body "op"))}
                  eid (assoc :engagement-id eid)
                  (get body "claimed-fee") (assoc :claimed-fee (get body "claimed-fee"))
                  (get body "jurisdiction") (assoc :jurisdiction (get body "jurisdiction")))
        context (get body "context")]
    (edge/with-store
      {:client client
       ;; Assessments are taken wholesale: an engagement's assessment is keyed
       ;; by the engagement id, but the governor also reads prior ones to see
       ;; whether this filing repeats a refusal.
       :wants {:engagement (if eid [eid] []) :assessment :all}
       :store-fn store/kotobase-store}
      (fn [st]
        (let [actor (operation/build st)
              thread (or (get body "thread-id") (str "t-" (hash [request context])))]
          (g/run* actor
                  {:request request :context context}
                  {:thread-id thread}))))))

(defn- routes
  ;; Six parameters: marketplace.edge/serve passes [client request env method
  ;; path url].
  [client request env method path _url]
  (cond
    (and (= method "POST") (= path "/operations"))
    (if-not (edge/authorised? request env)
      (js/Promise.resolve (edge/json {:error "unauthorised"} 401))
      (-> (.json request)
          (.then #(run-operation client (js->clj %)))
          (.then #(edge/json % 200))))

    (and (= method "GET") (str/starts-with? path "/engagements/"))
    (-> (edge/read-doc client :engagement (subs path (count "/engagements/")))
        (.then (fn [e] (edge/json (or e {:error "not found"}) (if e 200 404)))))

    (and (= method "GET") (= path "/engagements"))
    (-> (edge/read-all client :engagement)
        (.then (fn [es] (edge/json {:engagements (mapv :id es)} 200))))

    ;; /escalations and /ledger, implemented once in marketplace.edge. Every
    ;; filing here can escalate rather than commit on a machine's say-so, and
    ;; without a way to READ those the gate is a black hole.
    :else (edge/ledger-routes client request env method path :marketentry)))

(def app
  (clj->js
   {:fetch (fn [request env _ctx]
             (edge/serve "cloud-itonami-iso3166-bgr" request env routes))}))
