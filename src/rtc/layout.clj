;; Basic page layouts for rendering server-side.
(ns rtc.layout
  (:require
   [clojure.java.io :as io]
   [hiccup.core :refer [html]]
   [hiccup.page :refer [doctype]]
   [markdown.core :as md]
   [ring.middleware.anti-forgery :refer [*anti-forgery-token*]]
   [rtc.assets.core :as assets]))


(defn error-page
  "For those uh-oh moments."
  [opts]
  (let [{:keys [err req]} opts]
    {:status 400
     :headers {"Content-Type" "text/html; charset=utf-8"}
     :body
     (html
      (doctype :html5)
      [:html
       [:head
        [:title (str "ERROR: " err)]
        [:meta {:charset "utf-8"}]
        [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
        [:meta {:name "csrf-token" :content (:anti-forgery-token req)}]]
       [:body
        [:pre (str req)]]])}))


(defn page
  "Central page layout. If the server responds with HTML, this is what's
   getting called to produce the HTML document, unless there was a server error."
  [opts]
  (let [{:keys [title head content footer-content req]} opts]
    {:status 200
     :headers {"Content-Type" "text/html; charset=utf-8"}
     :body
     (html
      (doctype :html5)
      [:html
       (vec (concat [:head
                     [:title (if title (str title " | Radical Telehealth Collective") "Radical Telehealth Collective")]
                     [:meta {:charset "utf-8"}]
                     [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
                     [:meta {:name "csrf-token" :content *anti-forgery-token*}]
                     [:link {:href "https://fonts.googleapis.com/css2?family=Libre+Franklin:wght@400;700&display=swap" :rel "stylesheet"}]
                     [:link {:href (assets/style-href :intake) :rel "stylesheet"}]]
                    head))
       (conj [:body
              content]
             footer-content)])}))

(defn- file->html [file]
  (-> (str "markdown/" file) io/resource slurp md/md-to-html-string))

(comment
  (io/resource "markdown/home.md")
  (file->html "home.md"))

(defn static-lang-selector []
  [:aside.lang-selector
   [:label.field-label {:for "select-language"}
    [:span {:data-lang "en"} "Select a language"]
    [:span {:data-lang "es" :style {:display :none}} "Seleccione un idioma"]]
   [:select {:id "select-language"}
    [:option {:selected true :value "en"} "English"]
    [:option {:value "es"} "Español"]]])

(defn markdown-page
  "Render a markdown file as an HTML page. Calls (page) passing the rendered Markdown as :content."
  [{:keys [title file before after] :as opts}]
  (page
   (merge
    opts
    {:content [:div.container.page-container
               (static-lang-selector)
               [:header
                [:h1 (or title "Radical Telehealth Collective")]]
               [:main.prose
                (when before
                  [:div.before
                   before])
                [:section {:data-lang "en"}
                 (file->html (str "en/" file))]
                [:section {:data-lang "es"
                           :style {:display :none}}
                 (file->html (str "es/" file))]
                (when after
                  [:div.after
                   after])]]
     :footer-content (assets/inline-js "lang.js")})))


(defn intake-page
  "Render the intake Single-Page Application (SPA)."
  [opts]
  (page
   (merge
    opts
    {:title          "Get Care"
     :head           [[:link {:rel "stylesheet" :href "https://cdn.jsdelivr.net/npm/fullcalendar@5.4.0/main.min.css"}]
                      [:link {:rel "stylesheet" :href "https://cdn.jsdelivr.net/npm/@fullcalendar/list@5.4.0/main.min.css"}]
                      [:link {:rel "stylesheet" :href "https://cdn.jsdelivr.net/npm/@fullcalendar/timegrid@5.4.0/main.min.css"}]]
     :content        [:div#rtc-intake-app]
     :footer-content [:div
                      [:script {:src (assets/js-src :shared) :type "text/javascript"}]
                      [:script {:src (assets/js-src :intake) :type "text/javascript"}]]})))


(defn admin-page
  "Render the admin AKA Comrades Single-Page Application (SPA)."
  [opts]
  (page
   (merge
    opts
    {:head           [[:link {:rel "stylesheet" :href "https://cdn.jsdelivr.net/npm/fullcalendar@5.4.0/main.min.css"}]
                      [:link {:rel "stylesheet" :href (assets/style-href :admin)}]]
     :content        [:div#rtc-admin-app]
     :footer-content [:div
                      [:script {:src "https://cdn.jsdelivr.net/npm/fullcalendar@5.4.0/main.min.js" :type "text/javascript"}]
                      [:script {:src (assets/js-src :shared) :type "text/javascript"}]
                      [:script {:src (assets/js-src :admin) :type "text/javascript"}]]})))


(defn login-page
  "Render the login page."
  [{:keys [error req]}]
  (let [{:keys [form-params query-string]} req
        {:keys [email password]} form-params]
    (page
     {:title "Login"
      :req req
      :content
      [:div.container.container--login
       [:header
        [:h1 "Radical Telehealth Collective"]
        [:h2 "Login"]]
       [:main
        [:form.stack {:action (str "/login?" query-string) :method "POST"}
         (when error
           [:div.error
            [:p error]])
         [:div.flex-field
          [:label.field-label {:for "email"} "Email"]
          [:input {:type :email
                   :name "email"
                   :id "email"
                   :value (or email "")
                   :placeholder "me@example.com"}]]
         [:div.flex-field
          [:label.field-label {:for "password"} "Password"]
          [:input {:type :password
                   :name "password"
                   :id "password"
                   :value (or password "")}]]
         [:div
          [:button {:type :submit} "Login"]]
         [:input {:type :hidden
                  :name :__anti-forgery-token
                  :value *anti-forgery-token*}]]]]})))


(defn two-factor-page
  "Render the second step of the login form, 2-Factor Authentication (2FA)."
  [{:keys [req error]}]
  (let [dest (get-in req [:query-params "next"])]
    (page
     {:title "Verify Token"
      :content
      [:div.container.container--2fa
       [:header
        [:h1 "Radical Telehealth Collective"]
        [:h2 "Login"]]
       [:form.stack {:action (str "/login?next=" dest) :method "POST"}
        [:div "Please confirm the token shown in your authenticator app."]
        (when error
          [:div.error-message
           [:p error]])
        [:div
         [:input {:type :text
                  :name "token"
                  :value ""
                  :placeholder "12 345 678"}]]
        [:div
         [:button {:type :submit} "Confirm"]]
        [:div
         [:p
          [:a.text-button {:href "/logout"} "Cancel"]]]
        [:input {:type :hidden
                 :name :__anti-forgery-token
                 :value *anti-forgery-token*}]]]})))
