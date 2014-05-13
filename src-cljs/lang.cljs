(ns floor16.lang)


(def v {:ru {:ad  {1 "объявление" 2 "объявления" 5 "объявлений"}
             :data-pager-prev "< предыдущая"
             :data-pager-next "следующая >"
             :appartment "квартира"
             :studio "студия"
             :room {1 "комната" 2 "комнаты" 5 "комнат"}
             :select-value "выбрать"
             :selected "выбрано"
             :empty-search "По вашему запросу ничего не найдено"
             :rub "руб."
             :floor "этаж"
             :no-address "без указания адреса"
             :no-price "-"
             :city "Город"
             :flat "квартира"
             :district "район"
             :districts "районы"
             :metro "метро"
             :dont-care "не важно"
             :with-photo "только с фото"
             :balcony "балкон"
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

(defn l [kw &[n]]
  (if-let [word (kw (vocabulary))]
    (if (map? word) (get word (ru-plural-form(or n 1))) word)
    (name kw)))

(defn ^:export lstr [s &[n]]
  (let [kw (keyword s)]
  (if-let [word (kw (vocabulary))]
    (if (map? word) (get word (ru-plural-form(or n 1))) word)
    s)))
