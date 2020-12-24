(ns wkok.buy2let.currencies
  (:require [reagent.core :as ra]
            [reagent-material-ui.lab.autocomplete :refer [autocomplete]]
            ["@material-ui/core" :as mui]))

(def currencies
  [{:code "IQD", :name "Iraqi Dinar", :symbol "IQD", :symbol_native "د.ع.‏"} 
   {:code "MAD", :name "Moroccan Dirham", :symbol "MAD", :symbol_native "د.م.‏"} 
   {:code "CHF", :name "Swiss Franc", :symbol "CHF", :symbol_native "CHF"} 
   {:code "GNF", :name "Guinean Franc", :symbol "FG", :symbol_native "FG"} 
   {:code "DOP", :name "Dominican Peso", :symbol "RD$", :symbol_native "RD$"} 
   {:code "SGD", :name "Singapore Dollar", :symbol "S$", :symbol_native "$"} 
   {:code "KHR", :name "Cambodian Riel", :symbol "KHR", :symbol_native "៛"} 
   {:code "MKD", :name "Macedonian Denar", :symbol "MKD", :symbol_native "MKD"} 
   {:code "GBP", :name "British Pound Sterling", :symbol "£", :symbol_native "£"}
   {:code "TOP", :name "Tongan Paʻanga", :symbol "T$", :symbol_native "T$"} 
   {:code "HNL", :name "Honduran Lempira", :symbol "HNL", :symbol_native "L"} 
   {:code "KWD", :name "Kuwaiti Dinar", :symbol "KD", :symbol_native "د.ك.‏"} 
   {:code "PAB", :name "Panamanian Balboa", :symbol "B/.", :symbol_native "B/."}
   {:code "KES", :name "Kenyan Shilling", :symbol "Ksh", :symbol_native "Ksh"}
   {:code "AMD", :name "Armenian Dram", :symbol "AMD", :symbol_native "դր."} 
   {:code "NIO", :name "Nicaraguan Córdoba", :symbol "C$", :symbol_native "C$"}
   {:code "PKR", :name "Pakistani Rupee", :symbol "PKRs", :symbol_native "₨"} 
   {:code "MYR", :name "Malaysian Ringgit", :symbol "RM", :symbol_native "RM"} 
   {:code "KZT", :name "Kazakhstani Tenge", :symbol "KZT", :symbol_native "тңг."}
   {:code "ZMK", :name "Zambian Kwacha", :symbol "ZK", :symbol_native "ZK"} 
   {:code "BOB", :name "Bolivian Boliviano", :symbol "Bs", :symbol_native "Bs"} 
   {:code "CRC", :name "Costa Rican Colón", :symbol "₡", :symbol_native "₡"} 
   {:code "JOD", :name "Jordanian Dinar", :symbol "JD", :symbol_native "د.أ.‏"} 
   {:code "ERN", :name "Eritrean Nakfa", :symbol "Nfk", :symbol_native "Nfk"} 
   {:code "CZK", :name "Czech Republic Koruna", :symbol "Kč", :symbol_native "Kč"}
   {:code "LVL", :name "Latvian Lats", :symbol "Ls", :symbol_native "Ls"} 
   {:code "HKD", :name "Hong Kong Dollar", :symbol "HK$", :symbol_native "$"}
   {:code "LYD", :name "Libyan Dinar", :symbol "LD", :symbol_native "د.ل.‏"} 
   {:code "XAF", :name "CFA Franc BEAC", :symbol "FCFA", :symbol_native "FCFA"}
   {:code "GTQ", :name "Guatemalan Quetzal", :symbol "GTQ", :symbol_native "Q"}
   {:code "DJF", :name "Djiboutian Franc", :symbol "Fdj", :symbol_native "Fdj"} 
   {:code "UAH", :name "Ukrainian Hryvnia", :symbol "₴", :symbol_native "₴"} 
   {:code "RWF", :name "Rwandan Franc", :symbol "RWF", :symbol_native "FR"} 
   {:code "BWP", :name "Botswanan Pula", :symbol "BWP", :symbol_native "P"} 
   {:code "CLP", :name "Chilean Peso", :symbol "CL$", :symbol_native "$"} 
   {:code "ZWL", :name "Zimbabwean Dollar", :symbol "ZWL$", :symbol_native "ZWL$"}
   {:code "OMR", :name "Omani Rial", :symbol "OMR", :symbol_native "ر.ع.‏"}
   {:code "PLN", :name "Polish Zloty", :symbol "zł", :symbol_native "zł"} 
   {:code "MZN", :name "Mozambican Metical", :symbol "MTn", :symbol_native "MTn"} 
   {:code "AFN", :name "Afghan Afghani", :symbol "Af", :symbol_native "؋"} 
   {:code "PYG", :name "Paraguayan Guarani", :symbol "₲", :symbol_native "₲"}
   {:code "TRY", :name "Turkish Lira", :symbol "TL", :symbol_native "TL"}
   {:code "BZD", :name "Belize Dollar", :symbol "BZ$", :symbol_native "$"} 
   {:code "MDL", :name "Moldovan Leu", :symbol "MDL", :symbol_native "MDL"} 
   {:code "JPY", :name "Japanese Yen", :symbol "¥", :symbol_native "￥"} 
   {:code "INR", :name "Indian Rupee", :symbol "Rs", :symbol_native "টকা"} 
   {:code "RSD", :name "Serbian Dinar", :symbol "din.", :symbol_native "дин."} 
   {:code "TTD", :name "Trinidad and Tobago Dollar", :symbol "TT$", :symbol_native "$"} 
   {:code "BIF", :name "Burundian Franc", :symbol "FBu", :symbol_native "FBu"} 
   {:code "SEK", :name "Swedish Krona", :symbol "Skr", :symbol_native "kr"} 
   {:code "IDR", :name "Indonesian Rupiah", :symbol "Rp", :symbol_native "Rp"}
   {:code "ARS", :name "Argentine Peso", :symbol "AR$", :symbol_native "$"}
   {:code "VND", :name "Vietnamese Dong", :symbol "₫", :symbol_native "₫"} 
   {:code "MUR", :name "Mauritian Rupee", :symbol "MURs", :symbol_native "MURs"} 
   {:code "NGN", :name "Nigerian Naira", :symbol "₦", :symbol_native "₦"}
   {:code "KRW", :name "South Korean Won", :symbol "₩", :symbol_native "₩"} 
   {:code "MGA", :name "Malagasy Ariary", :symbol "MGA", :symbol_native "MGA"}
   {:code "KMF", :name "Comorian Franc", :symbol "CF", :symbol_native "FC"} 
   {:code "BYN", :name "Belarusian Ruble", :symbol "Br", :symbol_native "руб."}
   {:code "AED", :name "United Arab Emirates Dirham", :symbol "AED", :symbol_native "د.إ.‏"}
   {:code "EGP", :name "Egyptian Pound", :symbol "EGP", :symbol_native "ج.م.‏"}
   {:code "THB", :name "Thai Baht", :symbol "฿", :symbol_native "฿"} 
   {:code "DZD", :name "Algerian Dinar", :symbol "DA", :symbol_native "د.ج.‏"}
   {:code "TZS", :name "Tanzanian Shilling", :symbol "TSh", :symbol_native "TSh"}
   {:code "LKR", :name "Sri Lankan Rupee", :symbol "SLRs", :symbol_native "SL Re"} 
   {:code "YER", :name "Yemeni Rial", :symbol "YR", :symbol_native "ر.ي.‏"}
   {:code "NZD", :name "New Zealand Dollar", :symbol "NZ$", :symbol_native "$"} 
   {:code "USD", :name "US Dollar", :symbol "$", :symbol_native "$"}
   {:code "UGX", :name "Ugandan Shilling", :symbol "USh", :symbol_native "USh"} 
   {:code "TWD", :name "New Taiwan Dollar", :symbol "NT$", :symbol_native "NT$"} 
   {:code "CAD", :name "Canadian Dollar", :symbol "CA$", :symbol_native "$"} 
   {:code "ILS", :name "Israeli New Sheqel", :symbol "₪", :symbol_native "₪"} 
   {:code "MMK", :name "Myanma Kyat", :symbol "MMK", :symbol_native "K"} 
   {:code "CNY", :name "Chinese Yuan", :symbol "CN¥", :symbol_native "CN¥"} 
   {:code "MXN", :name "Mexican Peso", :symbol "MX$", :symbol_native "$"}
   {:code "PEN", :name "Peruvian Nuevo Sol", :symbol "S/.", :symbol_native "S/."} 
   {:code "IRR", :name "Iranian Rial", :symbol "IRR", :symbol_native "﷼"} 
   {:code "CDF", :name "Congolese Franc", :symbol "CDF", :symbol_native "FrCD"} 
   {:code "GHS", :name "Ghanaian Cedi", :symbol "GH₵", :symbol_native "GH₵"} 
   {:code "SYP", :name "Syrian Pound", :symbol "SY£", :symbol_native "ل.س.‏"} 
   {:code "SOS", :name "Somali Shilling", :symbol "Ssh", :symbol_native "Ssh"} 
   {:code "BDT", :name "Bangladeshi Taka", :symbol "Tk", :symbol_native "৳"} 
   {:code "EUR", :name "Euro", :symbol "€", :symbol_native "€"}
   {:code "RUB", :name "Russian Ruble", :symbol "RUB", :symbol_native "₽."}
   {:code "UZS", :name "Uzbekistan Som", :symbol "UZS", :symbol_native "UZS"} 
   {:code "RON", :name "Romanian Leu", :symbol "RON", :symbol_native "RON"}
   {:code "ALL", :name "Albanian Lek", :symbol "ALL", :symbol_native "Lek"}
   {:code "NAD", :name "Namibian Dollar", :symbol "N$", :symbol_native "N$"} 
   {:code "NOK", :name "Norwegian Krone", :symbol "Nkr", :symbol_native "kr"} 
   {:code "NPR", :name "Nepalese Rupee", :symbol "NPRs", :symbol_native "नेरू"} 
   {:code "LBP", :name "Lebanese Pound", :symbol "LB£", :symbol_native "ل.ل.‏"} 
   {:code "SDG", :name "Sudanese Pound", :symbol "SDG", :symbol_native "SDG"} 
   {:code "ISK", :name "Icelandic Króna", :symbol "Ikr", :symbol_native "kr"} 
   {:code "BHD", :name "Bahraini Dinar", :symbol "BD", :symbol_native "د.ب.‏"} 
   {:code "HRK", :name "Croatian Kuna", :symbol "kn", :symbol_native "kn"} 
   {:code "GEL", :name "Georgian Lari", :symbol "GEL", :symbol_native "GEL"} 
   {:code "MOP", :name "Macanese Pataca", :symbol "MOP$", :symbol_native "MOP$"} 
   {:code "PHP", :name "Philippine Peso", :symbol "₱", :symbol_native "₱"} 
   {:code "BND", :name "Brunei Dollar", :symbol "BN$", :symbol_native "$"} 
   {:code "HUF", :name "Hungarian Forint", :symbol "Ft", :symbol_native "Ft"}
   {:code "TND", :name "Tunisian Dinar", :symbol "DT", :symbol_native "د.ت.‏"} 
   {:code "LTL", :name "Lithuanian Litas", :symbol "Lt", :symbol_native "Lt"} 
   {:code "SAR", :name "Saudi Riyal", :symbol "SR", :symbol_native "ر.س.‏"} 
   {:code "COP", :name "Colombian Peso", :symbol "CO$", :symbol_native "$"} 
   {:code "UYU", :name "Uruguayan Peso", :symbol "$U", :symbol_native "$"} 
   {:code "CVE", :name "Cape Verdean Escudo", :symbol "CV$", :symbol_native "CV$"} 
   {:code "BAM", :name "Bosnia-Herzegovina Convertible Mark", :symbol "KM", :symbol_native "KM"}
   {:code "AZN", :name "Azerbaijani Manat", :symbol "man.", :symbol_native "ман."} 
   {:code "AUD", :name "Australian Dollar", :symbol "AU$", :symbol_native "$"} 
   {:code "BRL", :name "Brazilian Real", :symbol "R$", :symbol_native "R$"} 
   {:code "JMD", :name "Jamaican Dollar", :symbol "J$", :symbol_native "$"}
   {:code "DKK", :name "Danish Krone", :symbol "Dkr", :symbol_native "kr"} 
   {:code "ETB", :name "Ethiopian Birr", :symbol "Br", :symbol_native "Br"}
   {:code "QAR", :name "Qatari Rial", :symbol "QR", :symbol_native "ر.ق.‏"} 
   {:code "ZAR", :name "South African Rand", :symbol "R", :symbol_native "R"}
   {:code "VEF", :name "Venezuelan Bolívar", :symbol "Bs.F.", :symbol_native "Bs.F."} 
   {:code "BGN", :name "Bulgarian Lev", :symbol "BGN", :symbol_native "лв."} 
   {:code "EEK", :name "Estonian Kroon", :symbol "Ekr", :symbol_native "kr"} 
   {:code "XOF", :name "CFA Franc BCEAO", :symbol "CFA", :symbol_native "CFA"}])


(defn select-currency
  [{:keys [value on-change error helper-text]}]
  (let [options (sort-by :code currencies)]
    [autocomplete
     {:options options
      :render-input (fn [^js params]
                      (set! (.-label params) "Currency")
                      (set! (.-error params) error)
                      (set! (.-helperText params) helper-text)
                      (ra/create-element mui/TextField params))
      :render-option #(let [option (js->clj %)]
                        (str (get option "code") " - " (get option "name")))
      :get-option-label #(let [option (js->clj %)]
                           (str (get option "code") " - " (get option "name")))
      :get-option-selected #(let [option (js->clj %1)
                                  value (js->clj %2)]
                              (= (get option "code") (get value "code")))
      :value (->> options
                  (filter #(= (:code %) value))
                  first)
      :on-change (fn [_ value _]
                   (let [selected (js->clj value)
                         code (get selected "code")]
                     (on-change code)))}]))