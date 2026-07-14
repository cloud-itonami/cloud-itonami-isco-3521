(ns media.governor-test
  (:require [clojure.test :refer [deftest is testing]]
            [media.store :as store]
            [media.governor :as governor]))

(defn- fresh-store []
  (let [st (store/mem-store)]
    (store/register-client! st {:client-id "client-1" :name "Kobo Media Syndication"})
    (store/register-article! st {:article-id "A-1" :client-id "client-1"
                                 :headline "world news headline"
                                 :canonical-link "https://outlet.example/a-1"
                                 :attribution "Example Outlet"
                                 :max-excerpt-chars 280})
    st))

(defn- product-op [excerpt-chars attribution link-out]
  {:op :approve-derivative-product :effect :propose :article-id "A-1"
   :excerpt-chars excerpt-chars :attribution attribution :link-out link-out
   :confidence 0.9 :stake :low})

(def ^:private req {:client-id "client-1"})

(defn- ok-op [] (product-op 100 "Example Outlet" "https://outlet.example/a-1"))

(deftest ok-within-excerpt-bound-and-matching-attribution
  (let [st (fresh-store)
        v (governor/check req {} (ok-op) st)]
    (is (:ok? v))))

(deftest ok-at-exact-excerpt-boundary
  (testing "the excerpt-length bound is inclusive"
    (let [st (fresh-store)
          v (governor/check req {} (product-op 280 "Example Outlet" "https://outlet.example/a-1") st)]
      (is (:ok? v)))))

(deftest hard-on-excerpt-exceeds-bound
  (testing "quoting beyond the registered fair-use bound is full-text reproduction, not excerpting"
    (let [st (fresh-store)
          v (governor/check req {} (assoc (product-op 500 "Example Outlet" "https://outlet.example/a-1") :confidence 0.99) st)]
      (is (:hard? v))
      (is (some #(= :excerpt-exceeds-bound (:rule %)) (:violations v))))))

(deftest hard-on-misattributed-source-wrong-attribution
  (testing "attribution that diverges from kawaraban's own is misattribution, not syndication"
    (let [st (fresh-store)
          v (governor/check req {} (assoc (product-op 100 "Some Other Outlet" "https://outlet.example/a-1") :confidence 0.99) st)]
      (is (:hard? v))
      (is (some #(= :misattributed-source (:rule %)) (:violations v))))))

(deftest hard-on-misattributed-source-wrong-link
  (testing "a diverging link-out is misattribution, not syndication"
    (let [st (fresh-store)
          v (governor/check req {} (assoc (product-op 100 "Example Outlet" "https://impersonator.example/a-1") :confidence 0.99) st)]
      (is (:hard? v))
      (is (some #(= :misattributed-source (:rule %)) (:violations v))))))

(deftest hard-on-unknown-article
  (let [st (fresh-store)
        v (governor/check req {} (assoc (ok-op) :article-id "A-ghost") st)]
    (is (:hard? v))
    (is (some #(= :unknown-article (:rule %)) (:violations v)))))

(deftest hard-on-foreign-article
  (let [st (fresh-store)]
    (store/register-client! st {:client-id "client-2" :name "Other"})
    (let [v (governor/check {:client-id "client-2"} {} (ok-op) st)]
      (is (:hard? v))
      (is (some #(= :article-wrong-client (:rule %)) (:violations v))))))

(deftest hard-on-unregistered-client
  (let [st (fresh-store)
        v (governor/check {:client-id "nobody"} {} (ok-op) st)]
    (is (:hard? v))
    (is (some #(= :no-client (:rule %)) (:violations v)))))

(deftest hard-on-no-actuation-violation
  (let [st (fresh-store)
        v (governor/check req {} (assoc (ok-op) :effect :direct-write) st)]
    (is (:hard? v))
    (is (some #(= :no-actuation (:rule %)) (:violations v)))))

(deftest always-escalates-publish-derivative-product-even-at-high-confidence
  (testing "publishing a derivative broadcast product requires human sign-off"
    (let [st (fresh-store)
          v (governor/check req {} {:op :approve-publish-derivative-product :effect :propose
                                    :article-id "A-1" :confidence 0.99 :stake :low} st)]
      (is (not (:hard? v)))
      (is (:escalate? v)))))

(deftest always-escalates-live-on-air-switching-even-at-high-confidence
  (testing "any live on-air switching requires human sign-off"
    (let [st (fresh-store)
          v (governor/check req {} {:op :approve-live-on-air-switching :effect :propose
                                    :article-id "A-1" :confidence 0.99 :stake :low} st)]
      (is (not (:hard? v)))
      (is (:escalate? v)))))

(deftest escalates-low-confidence
  (let [st (fresh-store)
        v (governor/check req {} (assoc (ok-op) :confidence 0.3) st)]
    (is (not (:hard? v)))
    (is (:escalate? v))))
