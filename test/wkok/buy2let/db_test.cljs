(ns wkok.buy2let.db-test
  (:require [cljs.test :refer (deftest testing is)]
            [re-frame.core :as rf]
            [day8.re-frame.test :as rf-test]
            [wkok.buy2let.site.subs :as ss]
            wkok.buy2let.db.events))

; Run tests in REPL:
;    (cljs.test/run-tests 'wkok.buy2let.db-test)

(deftest initialize-db-test
  (testing "Test that :initialize-db initialises"
    (rf-test/run-test-sync
     (rf/dispatch [:initialize-db])
     (let [heading (rf/subscribe [::ss/heading])]
       (testing "the heading to Dashboard"
         (is (= "Dashboard" @heading)))))))