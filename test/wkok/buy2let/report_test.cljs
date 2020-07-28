(ns wkok.buy2let.report-test
  (:require [cljs.test :refer (deftest testing is)]
            [re-frame.core :as rf]
            [day8.re-frame.test :as rf-test]
            [wkok.buy2let.site.subs :as ss]
            [wkok.buy2let.site.events :as se]
            [wkok.buy2let.report.subs :as rs]
            [wkok.buy2let.report.views :as rv]
            [wkok.buy2let.backend.demo :as demo]
            wkok.buy2let.db.events))


(deftest set-prop-from-dash-nav-to-report-test
  (testing "Test navigation to report"
    (rf-test/run-test-sync
     (rf/dispatch [:initialize-db demo/demo-db])
     (let [active-property (rf/subscribe [::ss/active-property])
           active-page (rf/subscribe [::ss/active-page])
           report (rf/subscribe [::rs/report])
           heading (rf/subscribe [::ss/heading])]
       (testing "after the property has been set from Dashboard"
         (rf/dispatch [::se/set-active-property "property-one-id"])
         (is (= :property-one-id @active-property)))
       (testing "after navigating to Report"
         (rf/dispatch [:set-active-page :report "Report"])
         (is (= :report @active-page)))
       (testing "that the heading is Report"
         (is (= "Report" @heading)))
       (testing "that report result have been populated after view rendered"
         (rv/report)
         (is (contains? @report :result)))))))


(comment 
  (cljs.test/run-tests 'wkok.buy2let.report-test))