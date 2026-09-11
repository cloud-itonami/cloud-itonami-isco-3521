(ns media.advisor
  "MediaAdvisor — the advisor named in this repository's README,
  proposing a derivative broadcast/media operation (produce a
  derivative product, publish it, or switch live on-air) from
  kawaraban's mirror feed (headline + canonical link + bounded
  fair-use excerpt). Swappable mock/llm; the advisor ONLY proposes —
  `media.governor` checks the excerpt-length bound and attribution/
  link-out match independently and always escalates publish/on-air
  decisions. Modeled on cloud-itonami-isco-4311's advisor.

  A proposal: {:op :approve-derivative-product|:approve-publish-derivative-product|:approve-live-on-air-switching
               :effect :propose :article-id str :excerpt-chars number
               :attribution str :link-out str :stake kw :confidence n
               :rationale str}"
  (:require #?(:clj [clojure.edn :as edn] :cljs [cljs.reader :as edn])))

(defprotocol Advisor
  (-advise [advisor store request] "request -> proposal map"))

(defn- infer [_store {:keys [op stake article-id excerpt-chars attribution link-out] :as request}]
  {:op op
   :effect :propose
   :article-id article-id
   :excerpt-chars excerpt-chars
   :attribution attribution
   :link-out link-out
   :stake (or stake :low)
   :confidence (case (or stake :low) :high 0.7 :medium 0.85 :low 0.95)
   :rationale (str "proposed " (name op) " for client " (:client-id request))})

(defn mock-advisor []
  (reify Advisor
    (-advise [_ store request] (infer store request))))

(def ^:private system-prompt
  "You are a broadcast/media-syndication advisor. Given a request,
   propose an :op, the :article-id, :excerpt-chars, :attribution and
   :link-out, an honest :confidence and a :stake. Never propose an
   excerpt beyond kawaraban's registered bound, or an attribution/
   link-out that diverges from the registered article — the governor
   checks both against the registered article record. Publishing a
   derivative product and live on-air switching always require human
   sign-off regardless of confidence.")

(defn- parse-proposal [content]
  (try
    (let [p (edn/read-string content)]
      (if (map? p)
        (assoc p :effect :propose)
        {:op :unknown :effect :propose :confidence 0.0 :stake :high
         :rationale "unparseable LLM response"}))
    (catch #?(:clj Exception :cljs js/Error) _
      {:op :unknown :effect :propose :confidence 0.0 :stake :high
       :rationale "LLM response parse failure"})))

(defn llm-advisor
  [chat-model model-generate-fn gen-opts]
  (reify Advisor
    (-advise [_ _store request]
      (let [msgs [{:role :system :content system-prompt}
                  {:role :user :content (str "operation request: " (pr-str request))}]
            resp (model-generate-fn chat-model msgs gen-opts)]
        (parse-proposal (:content resp))))))
