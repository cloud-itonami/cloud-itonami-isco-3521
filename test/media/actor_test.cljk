(ns media.actor-test
  (:require [clojure.test :refer [deftest is testing]]
            [media.actor :as actor]
            [media.store :as store]))

(defn- fresh-store []
  (let [st (store/mem-store)]
    (store/register-client! st {:client-id "client-1" :name "Kobo Media Syndication"})
    (store/register-article! st {:article-id "A-1" :client-id "client-1"
                                 :headline "world news headline"
                                 :canonical-link "https://outlet.example/a-1"
                                 :attribution "Example Outlet"
                                 :max-excerpt-chars 280})
    st))

(deftest commits-a-within-bound-correctly-attributed-product
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:client-id "client-1" :op :approve-derivative-product :stake :low
                 :article-id "A-1" :excerpt-chars 100
                 :attribution "Example Outlet" :link-out "https://outlet.example/a-1"}
        result (actor/run-request! graph request {} "thread-1")]
    (is (= :done (:status result)))
    (is (some? (get-in result [:state :record])))
    (is (= 1 (count (store/records-of st "client-1"))))))

(deftest holds-an-excerpt-over-bound-product
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:client-id "client-1" :op :approve-derivative-product :stake :low
                 :article-id "A-1" :excerpt-chars 500
                 :attribution "Example Outlet" :link-out "https://outlet.example/a-1"}
        result (actor/run-request! graph request {} "thread-2")]
    (is (= :hold (:disposition (:state result))))
    (is (empty? (store/records-of st "client-1")))))

(deftest interrupts-then-approves-publish-on-human-approval
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:client-id "client-1" :op :approve-publish-derivative-product :stake :low
                 :article-id "A-1"}
        interrupted (actor/run-request! graph request {} "thread-3")]
    (is (= :interrupted (:status interrupted)))
    (is (empty? (store/records-of st "client-1")))
    (let [resumed (actor/approve! graph "thread-3")]
      (is (= :done (:status resumed)))
      (is (= 1 (count (store/records-of st "client-1")))))))
