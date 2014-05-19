(ns floor16.lang)


(def v {:ru {:ad  {1 "объявление" 2 "объявления" 5 "объявлений"}
             :times {1 "раз" 2 "раза" 5 "раз"}
             :data-pager-prev "< предыдущая"
             :data-pager-next "следующая >"
             :appartment "квартира"
             :studio "студия"
             :room {1 "комната" 2 "комнаты" 5 "комнат"}
             :select-value "выбрать"
             :selected "выбрано"
             :empty-search "По вашему запросу не найдено ни одного объявления. Попробуйте задать менее жесткие условия. Учтите, что далеко не у всех объявлений указаны все параметры. К примеру, в квартире может быть мебель, но в объявлении об этом может быть не сказано."
             :loading-search "Выполняется поиск объявлений по вашему запросу..."
             :rub "руб."
             :floor "этаж"
             :no-address "без указания адреса"
             :no-price "цена по договоренности"
             :city "Город"
             :flat "квартира"
             :district "район"
             :districts "районы"
             :metro "метро"
             :dont-care "не важно"
             :with-photo "только с фото"
             :balcony "балкон"
             :loggia "лоджия"
             :bow-window "эркер"
             :furniture "мебель"
             :internet "интернет"
             :tv "телевизор"
             :frige "холодильник"
             :washer "стиральная машина"
             :conditioner "кондиционер"
             :parking "парковка"
             :intercom "домофон"
             :security "охрана"
             :concierge "консьерж"
             :pets "можно с животными"
             :kids "можно с детьми"
             :no-pets "с животными нельзя"
             :no-kids "с детьми нельзя"
             :addiction "без вредных привычек"
             :additionals "дополнительные параметры"
             :area "площадь"
             :floors "этажность"
             :building-type "тип дома"
             :toilet "санузел"
             :facilities "удобства"
             :safety "безопасность"
             :distance "расстояние"
             :kidsnpets "дети и животные"
             :restrictions "предрассудки"
             :not-only-russo "без ограничений"
             :only-russo "только для славян"
             :has "есть"
             :hasnt "нет"
             :no "нет"
             :person-name "арендодатель"
             :phone-button "показать номер"
             }

        })

(defn vocabulary [] (:ru v))

(defn ru-plural-form [n]
  (let [d00(quot (rem n 100) 10)
        d0 (rem n 10)]
    (if (= 1 d00) 5
      (cond
       (or (= d0 0 ) (>= d0 5 )) 5
       (>= d0 2) 2
       :else 1))))

(defn ru-floor-ending [n]
  (get {1 "" 2 "-х" 5 "-ти"} (ru-plural-form n)))

(defn l [kw &[n]]
  (if-let [word (kw (vocabulary))]
    (if (map? word) (get word (ru-plural-form(or n 1))) word)
    (name kw)))

(defn ^:export lstr [s &[n]]
  (let [kw (keyword s)]
  (if-let [word (kw (vocabulary))]
    (if (map? word) (get word (ru-plural-form(or n 1))) word)
    s)))
