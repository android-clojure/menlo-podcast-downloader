(ns adamdavislee.mpd.main (:require [neko.activity :refer [defactivity set-content-view!]] [neko.debug :refer [*a]] [neko.notify :refer [toast fire notification]] [neko.resource :as res] [neko.context :refer [get-service]] [neko.threading :refer [on-ui]] [neko.find-view :refer [find-view]] [neko.intent :refer [intent]] [clojure.xml :as xml] [clojure.string :as str] [clojure.java.io :as io] [clojure.edn :as edn]) (:import android.widget.EditText java.util.concurrent.TimeUnit java.util.concurrent.Executors java.util.Date java.util.Calendar android.app.AlarmManager android.content.Context android.content.Intent android.app.PendingIntent android.widget.Toast android.content.BroadcastReceiver java.text.SimpleDateFormat android.os.SystemClock android.app.IntentService))
(res/import-all)
(defn mpddir [] (str (android.os.Environment/getExternalStorageDirectory) "/menlo-podcast-downloader/"))
(defn my-toast [& messages] (on-ui (toast (*a) (apply str messages) :short)))
(defn pad-left [x n p] (str (apply str (repeat (- n (count (str x))) p)) x))
(defn filename [podcast] (str (:year (:date podcast)) "-" (pad-left (:month (:date podcast)) 2 0) "-" (pad-left (:day (:date podcast)) 2 0) " - " (str/replace (:title podcast) #" \| " " - ") ".mp3"))
(defn my-podcast-notify [podcast] (on-ui (fire (keyword (:title podcast)) (notification {:icon R$drawable/ic_launcher :ticker-text "Finished Downloading!" :content-title (str "Downloaded " (filename podcast)) :content-text (str (mpddir) (filename podcast)) :action [:activity "adamdavislee.mpd.Activity"]}))))
(defn notify [string] (on-ui (fire :notification (notification {:icon R$drawable/ic_launcher :ticker-text string :content-title string :content-text string :action [:activity "adamdavislee.mpd.Activity"]}))))
(defn download-podcast [podcast] (future (.mkdir (io/file (mpddir))) (with-open [in (clojure.java.io/input-stream (:url podcast)) out (clojure.java.io/output-stream (str (mpddir) "." (filename podcast)))] (clojure.java.io/copy in out)) (.renameTo (clojure.java.io/file (str (mpddir) "." (filename podcast))) (clojure.java.io/file (str (mpddir) (filename podcast)))) (my-podcast-notify podcast)))
(defn items [feed] (as-> @feed it (it :content) (first it) (it :content) (filter #(some #{[:tag :item]} %) it)))
(defn item-to-url [item] (->> item :content (filter #(= (first %) [:tag :enclosure])) first :attrs :url))
(defn item-to-title [item] (->> item :content (filter #(= (first %) [:tag :title])) first :content first))
(defn item-to-date [item] (->> item :content (filter #(= (first %) [:tag :pubDate])) first :content first))
(def parse-month-from-feed {"jun" 6, "sep" 9, "feb" 2, "jan" 1, "apr" 4, "nov" 11, "mar" 3, "dec" 12, "oct" 10, "may" 5, "aug" 8, "jul" 7})
(defn parse-date-from-feed [date] {:day (Integer. (first (re-seq #"\d+" date))) :month (parse-month-from-feed (apply str (take 3 (.toLowerCase ((vec (re-seq #"\w+" date)) 2))))) :year (Integer. ((vec (re-seq #"\d+" date)) 1))})
(defn parsed-items [feed] (filter #(:url %) (map (fn [item] {:url (item-to-url item) :date ((comp parse-date-from-feed item-to-date) item) :title (item-to-title item)}) (items feed))))
(declare regexed-podcasts)
(defn download-podcasts [] (dorun (map download-podcast (filter (fn [parsed-item] (not (some (set [(filename parsed-item)]) (vec (.list (clojure.java.io/file (mpddir))))))) regexed-podcasts))))
(defn find-podcasts [] (def regex (.getText (find-view (*a) ::regex))) (def regexed-podcasts (filter (fn [podcast] (re-find (re-pattern (str "(?i)" regex)) (filename podcast))) (parsed-items (future (xml/parse "http://podcast.menlo.church/feed/"))))) (on-ui (set-content-view! (*a) [:linear-layout {:orientation :vertical, :layout-width :fill, :layout-height :wrap} [:edit-text {:layout-width :fill, :id ::regex, :hint "FILTER WITH REGEX" :text regex}] [:linear-layout {:orientation :horizontal} [:button {:text "FILTER PODCASTS" :on-click (fn [_] (on-ui (my-toast "SEARCHING FOR PODCASTS") (future (find-podcasts))))}] [:button {:text "DOWNLOAD PODCASTS" :on-click (fn [_] (on-ui (my-toast "DOWNLOADING SELECTED PODCASTS") (future (download-podcasts))))}]] [:scroll-view {:layout-width :match-parent, :layout-height :match-parent} (concat [:linear-layout {:orientation :vertical, :layout-width :match-parent, :layout-height :match-parent}] (map (fn [podcast] [:text-view {:text (filename podcast), :padding 8}]) regexed-podcasts))]])))
(gen-class :name adamdavislee.mpd.BroadcastReceiver :extends android.content.BroadcastReceiver :prefix broadcast-receiver-)
(gen-class :name adamdavislee.mpd.Service :extends android.app.IntentService :init init :state state :constructors [[] []] :prefix service-)
(defn broadcast-receiver-onReceive [this context intent2] (.setInexactRepeating (get-service :alarm) AlarmManager/ELAPSED_REALTIME (SystemClock/elapsedRealtime) AlarmManager/INTERVAL_HALF_DAY (PendingIntent/getService context 0 (intent context '.Service {}) 0)))
(defn service-init [] [["NameForThread"] "NameForThread"]) 
(defn service-onHandleIntent [this i] (download-podcasts))
(defactivity adamdavislee.mpd.Activity :key :main (onCreate [this bundle] (.superOnCreate this bundle) (res/import-all) (on-ui (set-content-view! (*a) [:linear-layout {:orientation :vertical, :layout-width :fill, :layout-height :wrap} [:edit-text {:layout-width :fill, :id ::regex, :hint "FILTER WITH REGEX"}] [:button {:text "TEST ALARM" :on-click (fn [_])}] [:button {:text "FILTER PODCASTS" :on-click (fn [_] (on-ui (my-toast "SEARCHING FOR PODCASTS") (future (find-podcasts))))}] [:scroll-view {:layout-width :match-parent, :layout-height :match-parent} [:linear-layout {:orientation :vertical, :layout-width :match-parent, :layout-height :match-parent}]]]))))