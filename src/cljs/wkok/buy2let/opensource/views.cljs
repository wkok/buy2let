(ns wkok.buy2let.opensource.views
  (:require [re-frame.core :as rf]
            [wkok.buy2let.shared :as shared]
            [wkok.buy2let.backend.multimethods :as mm]
            [wkok.buy2let.site.events :as se]
            [wkok.buy2let.legal.opensource.bsd :as bsd]
            [wkok.buy2let.legal.opensource.mit :as mit]
            [wkok.buy2let.legal.opensource.epl :as epl]
            [wkok.buy2let.site.styles :refer [classes]]
            [reagent-mui.material.typography :refer [typography]]
            [reagent-mui.material.paper :refer [paper]]
            [reagent-mui.material.text-field :refer [text-field]]
            [reagent-mui.material.link :refer [link]]
            [reagent-mui.material.table-container :refer [table-container]]
            [reagent-mui.material.table-head :refer [table-head]]
            [reagent-mui.material.table-body :refer [table-body]]
            [reagent-mui.material.table-row :refer [table-row]]
            [reagent-mui.material.table-cell :refer [table-cell]]
            [reagent-mui.material.table :refer [table]]
            [reagent-mui.material.grid :refer [grid]]))

(def packages
  [{:name "reagent-project/reagent"
    :url "https://github.com/reagent-project/reagent"
    :license mit/mit
    :copyright ["Copyright (c) 2013-2017 Dan Holmsand"
                "Copyright (c) 2017 Reagent contributors"]}
   {:name "day8/re-frame"
    :url "https://github.com/day8/re-frame"
    :license mit/mit
    :copyright ["Copyright (c) 2015-2017 Michael Thompson"]}
   {:name "day8/re-frame-10x"
    :url "https://github.com/day8/re-frame-10x"
    :license mit/mit
    :copyright ["Copyright (c) 2016 Michael Thompson"]}
   {:name "day8/re-frame-test"
    :url "https://github.com/day8/re-frame-test"
    :license mit/mit
    :copyright ["Copyright (c) 2016 Mike Thompson"]}
   {:name "day8/re-frame-debux"
    :url "https://github.com/day8/re-frame-debux"
    :license mit/mit
    :copyright ["Copyright (c) 2015-2018 Young Tae Kim, 2018 Day 8 Technology"]}
   {:name "weavejester/compojure"
    :url "https://github.com/weavejester/compojure"
    :license epl/epl-1-0
    :copyright ["Copyright (c) 2020 James Reeves"]}
   {:name "ring-clojure/ring"
    :url "https://github.com/ring-clojure/ring"
    :license mit/mit
    :copyright ["Copyright (c) 2009-2010 Mark McGranaghan"
                "Copyright (c) 2009-2018 James Reeves"]}
   {:name "clj-commons/secretary"
    :url "https://github.com/clj-commons/secretary"
    :license epl/epl-1-0
    :copyright ["Contributors"]}
   {:name "zelark/nano-id"
    :url "https://github.com/zelark/nano-id"
    :license mit/mit
    :copyright ["Copyright (c) 2018 Aleksandr Zhuravlev"
                "Copyright (c) 2017 Andrey Sitnik"]}
   {:name "juxt/tick"
    :url "https://github.com/juxt/tick"
    :license mit/mit
    :copyright ["Copyright (c) 2016-2018 JUXT LTD."]}
   {:name "luciodale/fork"
    :url "https://github.com/luciodale/fork"
    :copyright ["Copyright (c) Lucio D'Alessandro"]}
   {:name "cemerick/url"
    :url "https://github.com/cemerick/url"
    :license epl/epl-1-0
    :copyright ["Copyright (c) 2012 Chas Emerick and other contributors"]}
   {:name "binaryage/cljs-devtools"
    :url "https://github.com/binaryage/cljs-devtools"
    :license mit/mit
    :copyright ["Copyright (c) BinaryAge Limited and contributors"]}
   {:name "thheller/shadow-cljs"
    :url "https://github.com/thheller/shadow-cljs"
    :license epl/epl-1-0
    :copyright ["Copyright (c) 2020 Thomas Heller"]}
   {:name "arttuka/reagent-material-ui"
    :url "https://github.com/arttuka/reagent-material-ui"
    :license epl/epl-2-0
    :copyright ["Copyright (c) Arttu Kaipiainen"]}
   {:name "js-joda/js-joda"
    :url "https://github.com/js-joda/js-joda"
    :license bsd/bsd
    :copyright ["Copyright (c) 2016, Philipp Thürwächter & Pattrick Hüper"]}
   {:name "mui-org/material-ui"
    :url "https://github.com/mui-org/material-ui"
    :license mit/mit
    :copyright ["Copyright (c) 2014 Call-Em-All"]}
   {:name "highlightjs/highlight.js"
    :url "https://github.com/highlightjs/highlight.js"
    :license bsd/bsd-3
    :copyright ["Copyright (c) 2006, Ivan Sagalaev."]}
   {:name "facebook/react"
    :url "https://github.com/facebook/react"
    :license mit/mit
    :copyright ["Copyright (c) Facebook, Inc. and its affiliates."]}
   {:name "bvaughn/react-highlight.js"
    :url "https://github.com/bvaughn/react-highlight.js"
    :license mit/mit
    :copyright ["Copyright (c) 2015 Juho Vepsalainen"]}])

(defn opensource
  []
  (rf/dispatch [:set-fab-actions nil])
  [paper {:class (:paper classes)}
   [grid {:container true
          :direction :column
          :spacing 1}
    [grid {:item true}
     [typography {:variant :subtitle1}
      "The following open source components are included (or used in developing) this software"]]
    [grid {:item true :xs 12}
     [table-container
      [table {:size :small}
       [table-head
        [table-row
         [table-cell {:class (:table-header classes)} "Package"]
         [table-cell {:class (:table-header classes)} "License"]
         [table-cell {:class (:table-header classes)} "Copyright"]]]
       [table-body
        (for [package (concat packages (mm/packages))]
          ^{:key package}
          [table-row
           [table-cell
            [link {:href (-> package :url)
                   :target "_blank"} (-> package :name)]]
           [table-cell
            [shared/anchor
             #(rf/dispatch [::se/dialog {:panel [text-field {:multiline true
                                                             :value (-> package :license :text)
                                                             :disabled true
                                                             :max-rows 1000
                                                             :style {:width "800px"}
                                                             :InputProps {:classes
                                                                          {:input (:legal classes)}}}
                                                 ]
                                         :buttons   {:middle {:text     "Close"
                                                              :on-click (fn [] (rf/dispatch [::se/dialog]))}}}])
             (get-in package [:license :name] "")]]
           [table-cell
            (for [c (-> package :copyright)]
              ^{:key c}
              [typography {:variant :body2} c])]])]]]]]])
