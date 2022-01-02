(ns wkok.buy2let.site.styles
  (:require [clojure.walk :as w]))

(def classes
  (let [prefix "buy2let"]
    {:root   (str prefix "-root")
     :drawer (str prefix "-drawer")
     :app-bar (str prefix "-app-bar")
     :toolbar (str prefix "-toolbar")
     :title (str prefix "-title")
     :avatar-small (str prefix "-avatar-small")
     :avatar-medium (str prefix "-avatar-medium")
     :avatar-large (str prefix "-avatar-large")
     :pos (str prefix "-pos")
     :neg (str prefix "-neg")
     :owe (str prefix "-owe")
     :table-header (str prefix "-table-header")
     :table-header-alternate (str prefix "-table-header-alternate")
     :table-header-pos (str prefix "-table-header-pos")
     :table-header-neg (str prefix "-table-header-neg")
     :table-header-owe (str prefix "-table-header-owe")
     :table-header-alternate-pos (str prefix "-table-header-alternate-pos")
     :table-header-alternate-neg (str prefix "-table-header-alternate-neg")
     :table-header-alternate-owe (str prefix "-table-header-alternate-owe")
     :table-alternate (str prefix "-table-alternate")
     :brand-logo (str prefix "-brand-logo")
     :brand-name (str prefix "-brand-name")
     :menu-button (str prefix "-menu-button")
     :drawer-paper (str "MuiDrawer-paper")
     :reconcile-card (str prefix "-reconcile-card")
     :content (str prefix "-content")
     :buttons (str prefix "-buttons")
     :fab (str prefix "-fab")
     :splash (str prefix "-splash")
     :wizard-actions (str prefix "-wizard-actions")
     :who-pays-whom (str prefix "-who-pays-whom")
     :paper (str prefix "-paper")
     :legal (str prefix "-legal")
     :scroll-x (str prefix "-scroll-x")}))

(defn custom-styles [{:keys [theme]}]
  (let [{:keys [spacing breakpoints z-index palette]} theme
        up (:up breakpoints)
        drawer-width 200]
    {(str "&." (:root classes)) {:display :flex}
     (str "& ." (:drawer classes)) {(up "sm") {:width drawer-width, :flex-shrink 0}}
     (str "& ." (:app-bar classes)) {(up "sm") {:width (str "calc(100% - " drawer-width "px)") :margin-left drawer-width}}
     (str "& ." (:toolbar classes)) {:margin-top "-4px"}
     (str "& ." (:title classes)) {:flex-grow 1}
     (str "& ." (:avatar-small classes)) {:width (spacing 3)
                                         :height (spacing 3)}
     (str "& ." (:avatar-medium classes)) {:width (spacing 7)
                                          :height (spacing 7)}
     (str "& ." (:avatar-large classes)) {:width (spacing 10)
                                         :height (spacing 10)}
     (str "& ." (:pos classes)) {:color "blue"}
     (str "& ." (:neg classes)) {:color "red"}
     (str "& ." (:owe classes)) {:color "orange"}
     (str "& ." (:table-header classes)) {:font-weight 600}
     (str "& ." (:table-header-alternate classes)) {:font-weight 600
                                                   :background-color (get-in palette [:action :hover])}
     (str "& ." (:table-header-pos classes)) {:color "blue"
                                             :font-weight 600}
     (str "& ." (:table-header-neg classes)) {:color "red"
                                             :font-weight 600}
     (str "& ." (:table-header-owe classes)) {:color "orange"
                                             :font-weight 600}
     (str "& ." (:table-header-alternate-pos classes)) {:color "blue"
                                                       :font-weight 600
                                                       :background-color (get-in palette [:action :hover])}
     (str "& ." (:table-header-alternate-neg classes)) {:color "red"
                                                       :font-weight 600
                                                       :background-color (get-in palette [:action :hover])}
     (str "& ." (:table-header-alternate-owe classes)) {:color "orange"
                                                       :font-weight 600
                                                       :background-color (get-in palette [:action :hover])}
     (str "& ." (:table-alternate classes)) {:background-color (get-in palette [:action :hover])}
     (str "& ." (:brand-logo classes)) {:padding (spacing 1)}
     (str "& ." (:brand-name classes)) {:padding (spacing 1)}
     (str "& ." (:menu-button classes)) {(up "sm") {:display :none}
                                        :margin-right (spacing 2)}
     (str "& ." (:drawer-paper classes)) {:width drawer-width}
     (str "& ." (:reconcile-card classes)) {:height :7em}
     (str "& ." (:content classes)) {:flex-grow 1
                                    :padding (spacing 2)
                                    :padding-top (spacing 8)
                                    :padding-bottom (spacing 17)
                                    :overflow-x :hidden}
     (str "& ." (:buttons classes)) {:padding-top (spacing 1)}
     (str "& ." (:fab classes)) {(up "xs") {:position :fixed
                                           :bottom (spacing 8)
                                           :right (spacing 1)
                                           :z-index (+ (:drawer z-index) 1)}
                                (up "sm") {:position :fixed
                                           :bottom (spacing 2)
                                           :right (spacing 2)
                                           :z-index (+ (:drawer z-index) 1)}}
     (str "& ." (:splash classes)) {:z-index (+ (:drawer z-index) 1)}
     (str "& ." (:wizard-actions classes)) {:margin-top (spacing 2)}
     (str "& ." (:who-pays-whom classes)) {:padding-left (spacing 4)}
     (str "& ." (:paper classes)) {:padding (spacing 2)}
     (str "& ." (:legal classes)) {:font-size :0.8em
                                  :color (get-in palette [:text :primary])}
     (str "& ." (:scroll-x classes)) {:overflow-x :auto}}))

(defn from-theme
  [theme key]
  (-> theme js->clj w/keywordize-keys key))
