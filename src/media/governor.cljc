(ns media.governor
  "MediaBroadcastGovernor — the independent safety/traceability layer
  named in this repository's blueprint.edn
  (`:itonami.blueprint/governor :media-broadcast-governor`), gating
  every derivative broadcast/media product an advisor may propose from
  kawaraban's mirror feed. The governor never dispatches hardware
  itself and never allows raw full-text scraping of a mirrored
  article — only kawaraban's own bounded excerpt and link-out may ever
  enter the pipeline. Modeled on cloud-itonami-isco-4311's
  bookkeeping.governor. Task twist: a proposed derivative product's
  quoted excerpt length is an arithmetic ceiling against the
  registered kawaraban excerpt bound, and its attribution/link-out
  must exactly match what kawaraban published the article under —
  anything else is either full-text reproduction or misattribution,
  not syndication.

  HARD invariants (:hard? true, ALWAYS :hold, never overridable):
    1. client provenance   — the organization/practice must be
                             registered.
    2. no-actuation        — proposal :effect must be :propose (the
                             governor never dispatches hardware; it
                             only gates what the robot/pipeline may
                             execute).
    3. article basis       — a derivative-product proposal must cite a
                             REGISTERED kawaraban article belonging to
                             this client's practice.
    4. excerpt-length ceiling — the proposed quoted excerpt must not
                             exceed the article's registered
                             `:max-excerpt-chars` (kawaraban's own
                             fair-use bound) — quoting beyond it is
                             full-text reproduction, not excerpting.
    5. attribution/link-out match — the proposed product's
                             `:attribution` and `:link-out` must
                             exactly match the article's registered
                             `:attribution`/`:canonical-link` —
                             anything else is misattribution or
                             impersonation, not syndication.
  ESCALATION invariants (:escalate? true, ALWAYS human sign-off per
  README's Trust Controls — these are :high/:safety-critical
  regardless of confidence):
    6. :op :approve-publish-derivative-product (publishing a
                             derivative broadcast product requires
                             human sign-off).
    7. :op :approve-live-on-air-switching (any live on-air switching
                             requires human sign-off).
    8. low confidence (< `confidence-floor`)."
  (:require [media.store :as store]))

(def confidence-floor 0.6)

(def ^:private always-escalate-ops #{:approve-publish-derivative-product
                                     :approve-live-on-air-switching})

(defn- hard-violations [{:keys [request proposal]} client-record art]
  (let [{:keys [op excerpt-chars attribution link-out]} proposal
        product? (= :approve-derivative-product op)]
    (cond-> []
      (nil? client-record)
      (conj {:rule :no-client :detail "未登録 client/practice"})

      (not= :propose (:effect proposal))
      (conj {:rule :no-actuation :detail "effect は :propose のみ許可（governor はハードウェアを直接起動しない）"})

      (and product? (nil? art))
      (conj {:rule :unknown-article :detail "未登録 kawaraban article からの派生物は不可"})

      (and product? art (not= (:client-id art) (:client-id request)))
      (conj {:rule :article-wrong-client :detail "article が別 client/practice のもの"})

      (and product? art (number? excerpt-chars) (> excerpt-chars (:max-excerpt-chars art)))
      (conj {:rule :excerpt-exceeds-bound
             :detail (str "引用文字数 " excerpt-chars " > kawaraban 登録済み fair-use 上限 "
                          (:max-excerpt-chars art)
                          "（上限超過の引用は全文転載でありフェアユース抜粋ではない）")})

      (and product? art (or (not= attribution (:attribution art))
                            (not= link-out (:canonical-link art))))
      (conj {:rule :misattributed-source
             :detail "attribution/link-out が kawaraban 登録済みの出典と一致しない（誤帰属・なりすましは syndication ではない）"}))))

(defn check
  "Assess a proposal against `request`/`context`/`proposal` and a
  `store` implementing `media.store/Store`. Pure — never mutates the
  store, never dispatches the robot, never fetches full article text."
  [request context proposal store]
  (let [client-record (store/client store (:client-id request))
        art (some->> (:article-id proposal) (store/article store))
        hard (hard-violations {:request request :proposal proposal}
                              client-record art)
        hard? (boolean (seq hard))
        conf (or (:confidence proposal) 0.0)
        low? (< conf confidence-floor)
        always-risky? (contains? always-escalate-ops (:op proposal))]
    {:ok? (and (not hard?) (not low?) (not always-risky?))
     :violations hard
     :confidence conf
     :hard? hard?
     :escalate? (and (not hard?) (or low? always-risky?))}))
