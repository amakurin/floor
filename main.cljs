(ns floor16.main
  (:require-macros [cljs.core.async.macros :refer [go alt!]]
                   [secretary.macros :refer [defroute]])
  (:require
   [goog.fx.Dragger]
   [goog.ui.ModalPopup]
   [goog.math.Rect]
   [goog.events :as events]
   [goog.events.EventType]
   [goog.dom.classlist :as gcls]
   [goog.dom :as gd]
   [cljs.core.async :refer [put! <! chan]]
   [om.core :as om :include-macros true]
   [om.dom :as dom :include-macros true]
   [secretary.core :as secretary]
   [floor16.ui.range-edit :as uire]
   [cljs.reader :as reader]
   )
  (:import [goog History]
           [goog.history EventType])
  )


(enable-console-print!)

(def server-state (reader/read-string(.-serverState js/window)))

(.log js/console server-state)


(def app-state (atom {:cities [{:id 0
                                :selected true
                                :code "moskva"
                                :caption "Москва"
                                :area-type :metro-station
                                :areas [{:id 55 :caption "Ул. Сергея Эйзенштейна" :selected true :custom-style "small" :line-break true :text-coords [631 129 679 149] :points [[628 117 640 129]]}
                                        {:id 187 :caption "Тимирязевская" :selected false :custom-style "small" :text-coords [373 131 433 143] :points [[374 142 386 154]]}
                                        {:id 51 :caption "Ул. Милашенкова" :selected true :custom-style "small" :text-coords [406 154 477 166] :points [[438 142 450 154]]}
                                        {:id 52 :caption "Телецентр" :selected false :custom-style "small" :text-coords [467 131 518 143] :points [[486 142 498 154]]}
                                        {:id 53 :caption "Ул. Академика Королёва" :selected false :custom-style "small center" :line-break true :text-coords [493 154 578 166] :points [[524 142 536 154]]}
                                        {:id 54 :caption "Выставочный центр" :selected false :custom-style "small right" :line-break true :text-coords [496 119 564 131] :points [[564 127 576 139]]}
                                        {:id 182 :caption "Кузнецкий мост" :selected false :custom-style "right" :line-break true :text-coords [459 402 534 422] :points [[531 387 547 402]]}
                                        {:id 183 :caption "Лубянка" :selected false :text-coords [552 421 627 436] :points [[547 404 563 419]]}
                                        {:id 177 :caption "Чкаловская" :selected false :text-coords [633 438 708 453] :points [[682 423 698 438]]}
                                        {:id 162 :caption "Александровский сад" :selected false :text-coords [414 515 534 533] :points [[398 522 414 537]]}
                                        {:id 159 :caption "Боровицкая" :selected false :text-coords [316 544 378 559] :points [[378 534 394 549]]}
                                        {:id 160 :caption "Библиотека им. Ленина" :selected false :text-coords [429 548 563 563] :points [[418 533 434 548]]}
                                        {:id 158 :caption "Кропоткинская" :selected false :text-coords [405 569 480 584] :points [[388 563 404 578]]}
                                        {:id 174 :caption "Третьяковская" :selected false :text-coords [491 598 566 613] :points [[556 580 572 595] [569 593 585 608]]}
                                        {:id 176 :caption "Китай-город" :selected false :text-coords [628 491 703 506] :points [[614 475 630 490] [632 475 648 490]]}
                                        {:id 172 :caption "Площадь революции" :selected false :text-coords [481 497 556 512] :points [[521 482 537 497]]}
                                        {:id 171 :caption "Театральная" :selected false :text-coords [521 459 596 474] :points [[508 469 524 484]]}
                                        {:id 170 :caption "Охотный ряд" :selected false :custom-style "right" :line-break true :text-coords [444 442 495 467] :points [[495 456 511 471]]}
                                        {:id 181 :caption "Цветной бульвар" :selected false :custom-style "right" :line-break true :text-coords [361 309 436 334] :points [[436 303 452 318]]}
                                        {:id 166 :caption "Тверская" :selected false :text-coords [351 418 407 437] :points [[397 407 413 422]]}
                                        {:id 169 :caption "Чеховская" :selected false :text-coords [432 422 507 437] :points [[420 407 436 422]]}
                                        {:id 168 :caption "Пушкинская" :selected false :text-coords [385 375 460 390] :points [[409 387 425 402]]}
                                        {:id 185 :caption "Тургеневская" :selected false :text-coords [490 366 565 381] :points [[565 364 581 379]]}
                                        {:id 186 :caption "Сретенский бульвар" :selected false :custom-style "center" :line-break true :text-coords [554 320 616 344] :points [[576 344 592 359]]}
                                        {:id 178 :caption "Красные ворота" :selected false :text-coords [635 345 685 370] :points [[623 329 639 344]]}
                                        {:id 184 :caption "Чистые пруды" :selected false :text-coords [600 378 675 393] :points [[588 364 604 379]]}
                                        {:id 179 :caption "Сухаревская" :selected false :text-coords [545 286 620 301] :points [[528 283 544 298]]}
                                        {:id 180 :caption "Трубная" :selected false :text-coords [476 299 551 314] :points [[460 303 476 318]]}
                                        {:id 167 :caption "Маяковская" :selected false :text-coords [356 347 431 362] :points [[334 345 350 360]]}
                                        {:id 36 :caption "Динамо" :selected false :text-coords [272 261 323 275] :points [[251 259 267 274]]}
                                        {:id 47 :caption "Новослободская" :selected false :text-coords [374 276 464 292] :points [[369 260 385 275]]}
                                        {:id 138 :caption "Добрынинская" :selected false :text-coords [364 680 439 695] :points [[436 666 452 681]]}
                                        {:id 150 :caption "Октябрьская" :selected false :text-coords [389 635 453 650] :points [[375 642 391 657] [375 623 391 638]]}
                                        {:id 157 :caption "Парк культуры" :selected false :text-coords [337 590 412 605] :points [[323 600 339 615] [324 575 340 590]]}
                                        {:id 163 :caption "Арбатская" :selected false :text-coords [394 488 469 503] :points [[398 499 414 514]]}
                                        {:id 164 :caption "Арбатская" :selected false :text-coords [368 467 443 482] :points [[351 475 367 490]]}
                                        {:id 161 :caption "Смоленская" :selected false :text-coords [298 516 373 531] :points [[323 499 339 514]]}
                                        {:id 165 :caption "Смоленская" :selected false :text-coords [292 451 367 466] :points [[320 466 336 482]]}
                                        {:id 20 :caption "Киевская" :selected false :text-coords [214 485 266 499] :points [[238 466 254 481] [238 499 254 514] [267 482 283 497]]}
                                        {:id 77 :caption "Курская" :selected false :text-coords [714 398 765 414] :points [[705 413 721 428] [691 400 707 415]]}
                                        {:id 85 :caption "Марксистская" :selected false :text-coords [689 578 764 593] :points [[706 563 722 578]]}
                                        {:id 86 :caption "Таганская" :selected false :text-coords [639 541 695 556] :points [[697 541 713 556] [684 554 700 569]]}
                                        {:id 84 :caption "Римская" :selected false :text-coords [709 488 760 503] :points [[742 503 758 518]]}
                                        {:id 118 :caption "Каховская" :selected false :text-coords [435 803 493 818] :points [[435 818 451 833]]}
                                        {:id 117 :caption "Варшавская" :selected false :text-coords [503 802 578 817] :points [[503 817 519 832]]}
                                        {:id 119 :caption "Бунинская аллея" :selected false :text-coords [222 1019 297 1034] :points [[263 1034 279 1049]]}
                                        {:id 120 :caption "Улица Горчакова" :selected false :text-coords [288 1049 363 1064] :points [[317 1034 333 1049]]}
                                        {:id 121 :caption "Бульвар адмирала Ушакова" :selected false :text-coords [393 1049 468 1064] :points [[403 1034 419 1049]]}
                                        {:id 123 :caption "Улица Старокачаловская" :selected false :custom-style "right" :line-break true :text-coords [332 992 435 1017] :points [[435 995 451 1010]]}
                                        {:id 122 :caption "Улица Скобелевская" :selected false :line-break true :text-coords [452 1017 529 1042] :points [[435 1020 451 1035]]}
                                        {:id 125 :caption "Лесопарковая" :selected false :text-coords [369 954 449 966] :points [[397 966 413 981]]}
                                        {:id 124 :caption "Битцевский парк" :selected false :text-coords [248 967 336 982] :points [[338 966 354 981]]}
                                        {:id 17 :caption "Деловой центр" :selected false :custom-style "small right" :line-break true :text-coords [90 432 128 449] :points [[128 430 144 445]]}
                                        {:id 16 :caption "Парк победы" :selected false :custom-style "right" :line-break true :text-coords [76 515 129 540] :points [[129 512 145 527] [116 499 132 514]]}
                                        {:id 15 :caption "Славянский бульвар" :selected false :custom-style "center" :line-break true :text-coords [9 515 77 540] :points [[48 499 64 514]]}
                                        {:id 14 :caption "Студенческая" :selected false :text-coords [126 481 201 497] :points [[146 466 162 481]]}
                                        {:id 13 :caption "Кутузовская" :selected false :text-coords [43 481 116 497] :points [[67 466 83 481]]}
                                        {:id 29 :caption "Баррикадная" :selected false :text-coords [293 375 368 390] :points [[286 387 302 402]]}
                                        {:id 30 :caption "Краснопресненская" :selected false :text-coords [289 403 362 425] :points [[270 404 286 419]]}
                                        {:id 28 :caption "Улица 1905 года" :selected false :custom-style "right" :line-break true :text-coords [152 365 211 389] :points [[219 368 235 383]]}
                                        {:id 27 :caption "Беговая" :selected false :text-coords [132 335 179 351] :points [[182 330 198 345]]}
                                        {:id 26 :caption "Полежаевская" :selected false :text-coords [161 290 241 305] :points [[139 288 155 303]]}
                                        {:id 37 :caption "Белорусская" :selected false :text-coords [224 315 299 328] :points [[300 327 316 342] [300 309 316 324]]}
                                        {:id 46 :caption "Менделеевская" :selected false :text-coords [374 222 459 236] :points [[369 236 385 251]]}
                                        {:id 63 :caption "Проспект мира" :selected false :text-coords [579 238 661 253] :points [[564 243 580 258] [564 225 580 240]]}
                                        {:id 69 :caption "Комсомольская" :selected false :text-coords [677 303 762 317] :points [[661 315 677 330] [661 290 677 305]]}
                                        {:id 76 :caption "Бауманская" :selected false :text-coords [756 360 831 375] :points [[739 352 755 367]]}
                                        {:id 75 :caption "Электрозаводская" :selected false :text-coords [789 327 891 342] :points [[772 319 788 334]]}
                                        {:id 110 :caption "Крестьянская застава" :selected false :custom-style "right" :line-break true :text-coords [667 616 742 641] :points [[743 620 759 635]]}
                                        {:id 87 :caption "Пролетарская" :selected false :text-coords [769 588 844 603] :points [[760 603 776 618]]}
                                        {:id 88 :caption "Волгоградский проспект" :selected false :line-break true :text-coords [793 607 877 630] :points [[773 615 789 630]]}
                                        {:id 83 :caption "Площадь Ильича" :selected false :text-coords [773 519 888 533] :points [[767 503 783 518]]}
                                        {:id 82 :caption "Авиамоторная" :selected false :text-coords [797 494 885 509] :points [[780 490 796 505]]}
                                        {:id 81 :caption "Шоссе энтузиастов" :selected false :text-coords [816 458 923 473] :points [[799 456 815 471]]}
                                        {:id 80 :caption "Перово" :selected false :text-coords [816 435 891 450] :points [[799 433 815 448]]}
                                        {:id 79 :caption "Новогиреево" :selected false :text-coords [816 412 891 427] :points [[799 410 815 425]]}
                                        {:id 78 :caption "Новокосино" :selected false :text-coords [816 390 891 405] :points [[799 390 815 405]]}
                                        {:id 89 :caption "Текстильщики" :selected false :text-coords [816 670 897 685] :points [[799 667 815 682]]}
                                        {:id 95 :caption "Котельники" :selected false :text-coords [816 879 891 894] :points [[799 876 815 891]]}
                                        {:id 94 :caption "Жулебино" :selected false :text-coords [816 849 891 864] :points [[799 847 815 862]]}
                                        {:id 93 :caption "Лермонтовский проспект" :selected false :line-break true :text-coords [816 808 897 833] :points [[799 810 815 825]]}
                                        {:id 92 :caption "Выхино" :selected false :text-coords [816 775 891 790] :points [[799 773 815 788]]}
                                        {:id 91 :caption "Рязанский проспект" :selected false :text-coords [816 733 897 758] :points [[799 736 815 751]]}
                                        {:id 90 :caption "Кузьминки" :selected false :text-coords [816 701 891 716] :points [[799 699 815 714]]}
                                        {:id 175 :caption "Новокузнецкая" :selected false :text-coords [595 570 670 585] :points [[578 570 594 585]]}
                                        {:id 115 :caption "Павелецкая" :selected false :text-coords [597 663 667 678] :points [[596 645 612 660] [578 661 594 676]]}
                                        {:id 114 :caption "Автозаводская" :selected false :text-coords [595 711 675 726] :points [[578 708 594 723]]}
                                        {:id 98 :caption "Домодедовская" :selected false :text-coords [599 911 691 926] :points [[580 910 596 925]]}
                                        {:id 101 :caption "Орехово" :selected false :text-coords [595 879 670 894] :points [[578 877 594 892]]}
                                        {:id 111 :caption "Царицыно" :selected false :text-coords [595 849 670 864] :points [[578 847 594 862]]}
                                        {:id 112 :caption "Кантемировская" :selected false :text-coords [595 822 679 837] :points [[578 820 594 835]]}
                                        {:id 116 :caption "Каширская" :selected false :text-coords [595 779 669 792] :points [[591 792 607 807] [578 779 594 794]]}
                                        {:id 113 :caption "Коломенская" :selected false :text-coords [595 741 670 756] :points [[578 738 594 753]]}
                                        {:id 97 :caption "Красногвардейская" :selected false :text-coords [630 957 738 971] :points [[706 942 722 957]]}
                                        {:id 107 :caption "Печатники" :selected false :text-coords [706 734 781 749] :points [[689 734 705 749]]}
                                        {:id 99 :caption "Зябликово" :selected false :text-coords [706 923 781 938] :points [[689 925 705 940]]}
                                        {:id 100 :caption "Шипиловская" :selected false :text-coords [706 890 781 905] :points [[689 890 705 905]]}
                                        {:id 96 :caption "Алма-Атинская" :selected false :text-coords [757 957 841 971] :points [[754 942 770 957]]}
                                        {:id 102 :caption "Борисово" :selected false :text-coords [706 864 781 879] :points [[689 864 705 879]]}
                                        {:id 103 :caption "Марьино" :selected false :text-coords [706 838 781 853] :points [[689 838 705 853]]}
                                        {:id 104 :caption "Братиславская" :selected false :text-coords [706 813 781 828] :points [[689 813 705 828]]}
                                        {:id 105 :caption "Люблино" :selected false :text-coords [706 786 781 801] :points [[689 786 705 801]]}
                                        {:id 106 :caption "Волжская" :selected false :text-coords [706 760 781 775] :points [[689 760 705 775]]}
                                        {:id 108 :caption "Кожуховская" :selected false :text-coords [715 704 790 719] :points [[694 703 710 718]]}
                                        {:id 109 :caption "Дубровка" :selected false :text-coords [733 680 808 695] :points [[716 680 732 695]]}
                                        {:id 126 :caption "Бульвар Дмитрия Донского" :selected false :line-break true :text-coords [470 972 570 997] :points [[453 978 469 993]]}
                                        {:id 127 :caption "Аннино" :selected false :text-coords [470 953 545 968] :points [[453 951 469 966]]}
                                        {:id 128 :caption "Улица академика Янгеля" :selected false :line-break true :text-coords [470 922 569 947] :points [[453 925 469 940]]}
                                        {:id 129 :caption "Пражская" :selected false :text-coords [470 902 545 917] :points [[453 899 469 914]]}
                                        {:id 130 :caption "Южная" :selected false :text-coords [470 881 545 896] :points [[453 879 469 894]]}
                                        {:id 131 :caption "Чертановская" :selected false :text-coords [470 860 545 875] :points [[453 858 469 873]]}
                                        {:id 132 :caption "Севастопольская" :selected false :text-coords [470 839 563 853] :points [[453 835 469 850]]}
                                        {:id 133 :caption "Нахимовский проспект" :selected false :line-break true :text-coords [470 773 547 798] :points [[453 776 469 791]]}
                                        {:id 134 :caption "Нагорная" :selected false :text-coords [470 753 545 768] :points [[453 751 469 766]]}
                                        {:id 135 :caption "Нагатинская" :selected false :text-coords [470 733 545 748] :points [[453 731 469 746]]}
                                        {:id 136 :caption "Тульская" :selected false :text-coords [470 713 545 728] :points [[453 711 469 726]]}
                                        {:id 137 :caption "Серпуховская" :selected false :text-coords [470 689 545 704] :points [[453 683 469 698]]}
                                        {:id 173 :caption "Полянка" :selected false :text-coords [470 641 545 656] :points [[453 639 469 654]]}
                                        {:id 151 :caption "Юго-западная" :selected false :text-coords [227 776 306 790] :points [[209 771 225 786]]}
                                        {:id 152 :caption "Проспект Вернадского" :selected false :line-break true :text-coords [227 736 299 761] :points [[209 739 225 754]]}
                                        {:id 153 :caption "Университет" :selected false :text-coords [226 707 301 722] :points [[209 704 225 719]]}
                                        {:id 154 :caption "Воробьевы горы" :selected false :text-coords [229 672 299 697] :points [[211 672 227 687]]}
                                        {:id 155 :caption "Спортивная" :selected false :text-coords [254 650 319 665] :points [[235 642 251 657]]}
                                        {:id 156 :caption "Фрунзенская" :selected false :text-coords [275 630 350 645] :points [[257 621 273 636]]}
                                        {:id 149 :caption "Новоясеневская" :selected false :text-coords [343 927 418 942] :points [[338 942 354 957]]}
                                        {:id 148 :caption "Ясенево" :selected false :text-coords [340 905 415 920] :points [[323 903 339 918]]}
                                        {:id 147 :caption "Тёплый стан" :selected false :text-coords [340 883 415 898] :points [[323 880 339 895]]}
                                        {:id 146 :caption "Коньково" :selected false :text-coords [340 861 415 876] :points [[323 858 339 873]]}
                                        {:id 145 :caption "Беляево" :selected false :text-coords [340 839 415 854] :points [[323 836 339 851]]}
                                        {:id 144 :caption "Калужская" :selected false :text-coords [340 816 415 831] :points [[323 814 339 829]]}
                                        {:id 143 :caption "Новые Черёмушки" :selected false :line-break true :text-coords [340 788 429 813] :points [[323 790 339 805]]}
                                        {:id 142 :caption "Профсоюзная" :selected false :text-coords [340 771 424 786] :points [[323 768 339 783]]}
                                        {:id 141 :caption "Академическая" :selected false :text-coords [340 749 426 764] :points [[323 746 339 761]]}
                                        {:id 140 :caption "Ленинский проспект" :selected false :text-coords [340 726 449 742] :points [[323 723 339 738]]}
                                        {:id 139 :caption "Шаболовская" :selected false :text-coords [340 704 415 719] :points [[323 701 339 716]]}
                                        {:id 74 :caption "Семеновская" :selected false :text-coords [816 277 891 292] :points [[799 275 815 290]]}
                                        {:id 68 :caption "Красносельская" :selected false :text-coords [706 248 794 263] :points [[689 247 705 262]]}
                                        {:id 67 :caption "Сокольники" :selected false :text-coords [706 223 773 236] :points [[689 221 705 236]]}
                                        {:id 66 :caption "Преображенская площадь" :selected false :line-break true :text-coords [706 181 796 205] :points [[689 182 705 197]]}
                                        {:id 65 :caption "Черкизовская" :selected false :text-coords [706 155 796 168] :points [[689 152 705 167]]}
                                        {:id 64 :caption "Улица Подбельского" :selected false :text-coords [706 119 796 144] :points [[689 119 705 134]]}
                                        {:id 73 :caption "Партизанская" :selected false :text-coords [816 247 897 262] :points [[799 244 815 259]]}
                                        {:id 72 :caption "Измайловская" :selected false :text-coords [816 217 897 232] :points [[799 214 815 229]]}
                                        {:id 71 :caption "Первомайская" :selected false :text-coords [816 188 897 203] :points [[799 185 815 200]]}
                                        {:id 70 :caption "Щелковская" :selected false :text-coords [816 155 885 170] :points [[799 155 815 170]]}
                                        {:id 49 :caption "Достоевская" :selected false :text-coords [477 206 548 221] :points [[460 204 476 219]]}
                                        {:id 48 :caption "Марьина роща" :selected false :text-coords [477 184 563 199] :points [[460 184 476 199]]}
                                        {:id 45 :caption "Савеловская" :selected false :text-coords [367 195 434 211] :points [[349 193 365 208]]}
                                        {:id 44 :caption "Дмитровская" :selected false :text-coords [366 168 441 182] :points [[349 166 365 181]]}
                                        {:id 62 :caption "Рижская" :selected false :text-coords [595 193 670 208] :points [[578 193 594 208]]}
                                        {:id 61 :caption "Алексеевская" :selected false :text-coords [595 167 670 182] :points [[578 164 594 179]]}
                                        {:id 60 :caption "ВДНХ" :selected false :text-coords [595 136 623 156] :points [[578 136 594 151]]}
                                        {:id 59 :caption "Ботанический сад" :selected false :text-coords [595 91 715 106] :points [[578 88 594 103]]}
                                        {:id 58 :caption "Свиблово" :selected false :text-coords [595 63 648 78] :points [[578 60 594 75]]}
                                        {:id 57 :caption "Бабушкинская" :selected false :text-coords [595 38 678 53] :points [[578 35 594 50]]}
                                        {:id 56 :caption "Медведково" :selected false :text-coords [595 15 661 30] :points [[578 15 594 30]]}
                                        {:id 43 :caption "Тимирязевская" :selected false :text-coords [366 116 452 131] :points [[349 115 365 130]]}
                                        {:id 42 :caption "Петровско-разумовская" :selected false :text-coords [366 96 495 111] :points [[349 95 365 110]]}
                                        {:id 41 :caption "Владыкино" :selected false :text-coords [366 75 428 90] :points [[349 74 365 89]]}
                                        {:id 40 :caption "Отрадное" :selected false :text-coords [366 55 420 70] :points [[349 53 365 68]]}
                                        {:id 39 :caption "Бибирево" :selected false :text-coords [366 34 424 49] :points [[349 33 365 48]]}
                                        {:id 38 :caption "Алтуфьево" :selected false :text-coords [366 15 426 30] :points [[349 14 365 30]]}
                                        {:id 35 :caption "Аэропорт" :selected false :text-coords [255 222 313 236] :points [[238 219 254 234]]}
                                        {:id 34 :caption "Сокол" :selected false :text-coords [256 195 294 208] :points [[238 192 255 207]]}
                                        {:id 33 :caption "Войковская" :selected false :text-coords [256 168 320 182] :points [[238 165 255 180]]}
                                        {:id 32 :caption "Водный стадион" :selected false :text-coords [256 143 340 156] :points [[238 140 255 155]]}
                                        {:id 31 :caption "Речной вокзал" :selected false :text-coords [256 118 331 132] :points [[238 118 255 134]]}
                                        {:id 8 :caption "Кунцевская" :selected false :text-coords [36 327 109 341] :points [[30 339 46 354] [17 326 33 341]]}
                                        {:id 19 :caption "Выставочная" :selected false :text-coords [171 434 247 448] :points [[153 431 169 446]]}
                                        {:id 18 :caption "Международная" :selected false :text-coords [171 414 260 427] :points [[153 413 169 428]]}
                                        {:id 12 :caption "Фили" :selected false :text-coords [47 430 82 443] :points [[30 428 46 443]]}
                                        {:id 11 :caption "Багратионовская" :selected false :text-coords [48 407 141 420] :points [[30 403 46 418]]}
                                        {:id 10 :caption "Филевский парк" :selected false :text-coords [48 385 137 398] :points [[30 381 46 396]]}
                                        {:id 9 :caption "Пионерская" :selected false :text-coords [48 362 116 376] :points [[30 359 46 374]]}
                                        {:id 25 :caption "Октябрьское поле" :selected false :text-coords [145 245 213 269] :points [[127 248 144 263]]}
                                        {:id 24 :caption "Щукинская" :selected false :text-coords [145 214 209 227] :points [[127 211 144 226]]}
                                        {:id 23 :caption "Тушинская" :selected false :text-coords [145 181 209 194] :points [[127 178 144 193]]}
                                        {:id 22 :caption "Сходненская" :selected false :text-coords [145 149 214 163] :points [[127 146 144 161]]}
                                        {:id 21 :caption "Планерная" :selected false :text-coords [145 119 209 133] :points [[127 119 144 134]]}
                                        {:id 7 :caption "Молодежная" :selected false :text-coords [35 299 106 313] :points [[17 298 33 313]]}
                                        {:id 6 :caption "Крылатское" :selected false :text-coords [35 277 106 291] :points [[17 274 33 289]]}
                                        {:id 5 :caption "Строгино" :selected false :text-coords [35 255 91 269] :points [[17 252 33 267]]}
                                        {:id 4 :caption "Мякинино" :selected false :text-coords [35 233 95 247] :points [[17 231 33 246]]}
                                        {:id 3 :caption "Волоколамская" :selected false :text-coords [35 211 116 225] :points [[17 208 33 223]]}
                                        {:id 2 :caption "Митино" :selected false :text-coords [35 189 83 201] :points [[17 186 33 201]]}
                                        {:id 1 :caption "Пятницкое шоссе" :selected false :text-coords [35 156 95 180] :points [[17 156 33 171]]}
                                        ]
                                }

                               {:id 1
                                :selected false
                                :code "samara"
                                :caption "Самара"
                                :area-type :district
                                :areas [{:id 0 :caption "Промышленный" :selected false}
                                        {:id 1 :caption "Кировский"   :selected false}
                                        {:id 2 :caption "Советский"   :selected false}
                                        {:id 3 :caption "Октябрьский"   :selected false}
                                        {:id 4 :caption "Железнодорожный"   :selected false}
                                        {:id 5 :caption "Красноглинский"   :selected false}
                                        {:id 6 :caption "Куйбышевский"   :selected false}
                                        {:id 7 :caption "Ленинский"   :selected false}
                                        {:id 8 :caption "Самарский"   :selected false}
                                        ]
                                }]
                      :object-types [{:id 0 :code "komnata" :caption "Комната" :selected false}
                                     {:id 1 :code "odmokomnatnaya-kvartira" :caption "1" :selected false}
                                     {:id 2 :code "dvuhkomnatnaya-kvartira" :caption "2" :selected false}
                                     {:id 3 :code "trehkomnatnaya-kvartira" :caption "3" :selected false}
                                     {:id 4 :code "chetirehkomnatnaya-kvartira" :caption "4+" :selected false}
                                     {:id 5 :code "dom" :caption "Дом" :selected false}]
                      :total-area-range {:bottom 0 :top 100}
                      :monthly-cost-range {:bottom 0 :top 70000}
                      :filter-mode :simple
                      :metro-select false
                      }))
(def app-config
  {:area-select {:area-types {:metro-station {:head "Станции метро"
                                              :chosen "Выбрано: "
                                              :choose "Выбрать станции на карте"
                                              }
                              :district {:head "Районы"
                                         :chosen "Выбрано: "
                                         :choose "Выберите из списка"
                                         }}
                }

   :modals {:open "omod"
            :close "cmod"}
  })

;;;;;;;;;Define routes;;;;;;;;;;;;;;;;;;;;;;;
;;;;Utils
(defn get-conf [path] (get-in app-config path))

(defn upd-state! [path value]
  (if (vector? path)
    (swap! app-state (fn [m] (assoc-in m path value)))
    (swap! app-state (fn [m] (assoc m path value)))))


(defn request-to-keywords [req]
  (into {} (for [[_ k v] (re-seq #"([^&=]+)=([^&]+)" req)]
    [(keyword k) (reader/read-string v)])))

;;;;Filter Modes
(defroute "!/search/:mode/:filt" [mode filt]
  ;(upd-state! :filter-mode (keyword mode))
  (.log js/console mode filt (request-to-keywords filt)))

(.log js/console (= 'mos 'mos))
;;;;Dialogs

;(defroute (str "/" (get-conf [:modals :open]) "/:dialog") {:keys [dialog]}
;  (let [body (. (gd/getElementsByTagNameAndClass "body")(item 0))]
;      (gcls/add body "noscroll")
;    )
;  (upd-state! (keyword dialog) true))

;(defroute (str "/" (get-conf [:modals :close]) "/:dialog") {:keys [dialog]}
;  (let [body (. (gd/getElementsByTagNameAndClass "body")(item 0))]
;      (gcls/remove body "noscroll")
;    )
;  (upd-state! (keyword dialog) false))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn dialog-route [dialog command]
  (str "/" (get-conf [:modals command]) "/" (name dialog)))

(defn dialog-url [dialog command] (str "#" (dialog-route dialog command)))


;;;;;;;;;Setup History;;;;;;;;;;;;;;;;;;;;;;;

(def history (History.))

(events/listen history EventType/NAVIGATE
  (fn [e] (secretary/dispatch! (.-token e))))

(.setEnabled history true)


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn radio-list [items owner {:keys [field]}]
  (om/component
   (dom/ul #js{:className "radio-list"}
           (into-array
            (map
             (fn [item]
               (let [{:keys [id caption selected]} item
                     update-items (fn [] (om/update! items (fn [s] (vec (map #(assoc % :selected (= id (:id %))) s)))))]
                 (dom/li nil
                         (dom/input #js {:name (name field) :type "radio" :checked selected
                                         :onChange (fn [_] (update-items))})
                         (dom/label nil caption)))) items)))))

(defn checkbox-list-item [{:keys [id caption selected] :as item} owner opts]
  (om/component
   (dom/li nil
           (dom/input #js {:type "checkbox" :checked selected
                           :onChange (fn [_](om/transact! item [:selected] #(not %)))})
           (dom/label nil caption))))

(defn checkbox-list [items owner opts]
  (om/component
   (dom/ul #js{:className "checkbox-list"}
           (om/build-all checkbox-list-item items {:key :id}))))

(defn dialog [app owner {:keys [dialog] :as opts}]
  (reify
    om/IWillUpdate
    (will-update [this next-props next-state]
                 ;(cls/addRemove ())
                 )
    om/IRender
    (render [_]
            (dom/div #js{:className "modal-wrap" :style #js{:display (if (dialog app) "block" "none")}}
                     (dom/div #js{:className "modal-outer"}
                     (dom/a #js{:href (dialog-url dialog :close)
                                :className "modal-overlay"
                                :title "Закрыть"})
                     (dom/div #js{:className "container"}
                              (dom/div #js{:className "modal"}
                                       (dom/a #js{:href (dialog-url dialog :close)
                                                  :className "close"
                                                  :title "Закрыть"
                                                  }
                                              "×"))))))))

(defn el-matcher [el]
  (fn [other] (identical? other el)))

(defn in? [e el]
  (let [target (.-target e)]
    (or (identical? target el)
        (not (nil? (gd/getAncestor target (el-matcher el)))))))

(defn cnt-sel ([path obj] (cnt-sel (if (vector? path) (get-in obj path) (path obj))))
  ([coll](count (filter :selected coll))))

(def ENTER 13)
(def UP_ARROW 38)
(def DOWN_ARROW 40)
(def TAB 9)
(def ESC 27)

(def KEYS #{UP_ARROW DOWN_ARROW ENTER TAB ESC})

(defn key-event->keycode [e]
  (.-keyCode e))

(defn key->keyword [code]
  (condp = code
    UP_ARROW   :prev
    DOWN_ARROW :next
    ENTER      :select
    TAB        :exit
    ESC        :exit))

(defn autocomplete [coll owner opt]
  (reify
    om/IInitState
    (init-state [this]
                {:search-string ""
                 :current -1})
    om/IRender
    (render [_]
            (let [fnd (filter (fn [ii] (not= (.indexOf (.toUpperCase (:caption ii))
                                                       (.toUpperCase (om/get-state owner [:search-string])))
                                             -1)) coll)
                  cnt-fnd (count fnd)
                  len-str (.-length (om/get-state owner [:search-string]))
                  curr (om/get-state owner [:current])
                  min-curr 0
                  handle-comm (fn []
                                (om/set-state! owner [:current] min-curr)
                                (om/set-state! owner [:search-string] ""))
                  handle-select (fn hs ([] (hs curr))
                                  ([c]
                                   (om/transact! (nth fnd c) [:selected] #(not %))
                                   (handle-comm)))
                  handle-exit (fn [] (handle-comm))
                  ]
              (dom/div #js{:className "five columns autocomplete-wrap"
                           :onBlur (fn [e] (handle-comm))
                           :onKeyDown (fn [e]
                                        (when (contains? KEYS (key-event->keycode e))
                                          (let [act (key->keyword (key-event->keycode e))]
                                            (cond
                                             (= :next act) (om/set-state! owner [:current] (min (dec cnt-fnd) (inc curr)))
                                             (= :prev act) (om/set-state! owner [:current] (max min-curr (dec curr)))
                                             (= :select act) (handle-select)
                                             (= :exit act) (handle-exit)
                                             ))))}
                       (dom/input #js{:type "text"
                                      :className "autocomplete"
                                      :placeholder "Поиск по названию..."
                                      :value (om/get-state owner [:search-string])
                                      :onChange (fn [e]
                                                  (om/set-state! owner [:search-string] (.. e -target -value))
                                                  (om/set-state! owner [:current] min-curr))})
                       (when (> len-str 1)
                         (dom/ul #js {:className "menu"}
                                 (if (= cnt-fnd 0)
                                   (dom/li #js{:className "not-found"} "Станции не найдены")
                                   (into-array
                                    (for [x (range cnt-fnd)]
                                      (dom/li #js{:className (when (= x curr) "current")
                                                  :onMouseEnter (fn [e] (om/set-state! owner [:current] x))
                                                  :onMouseDown (fn [e]
                                                                 (handle-select x)
                                                                 (.preventDefault e))
                                                  }
                                              (:caption (nth fnd x))))))))
                       )))))

(defn metro-station-item [{:keys [selected caption line-break custom-style text-coords points] :as station} owner opts]
  (reify
    om/IRender
    (render [_]
            (let [left (first text-coords)
                  top (second text-coords)]
              (dom/div #js{:className (str "station " (when selected " selected ") (if-let [style custom-style] style))
                           :style #js{:left left :top top
                                      :width (if line-break (- (nth text-coords 2) left) "auto")}}
                       (into-array
                        (conj (map (fn [p] (dom/div #js{:className (str "station-point" (when selected " selected"))
                                                        :style #js {:left (- (first p) left)
                                                                    :top (- (second p) top)}
                                                        :onClick (fn [_](om/transact! station [:selected] #(not %)))})) points)
                              (dom/span #js{:className "station-text"
                                            :onClick (fn [_](om/transact! station [:selected] #(not %)))} caption)
                              ))
                       )))))

(defn metro-select [areas]
  (reify
    om/IRender
    (render [_]
            (dom/div #js{:className (str "moscow-metro-map" (when-not (empty? (filter :selected areas)) " has-selected"))}
                     (om/build autocomplete areas)
                     (om/build-all metro-station-item areas {:key :id})))))


(defn popup-select [coll owner {:keys [conf builder drop-type size-type]}]
  ;TODO
  ;:horiz-align values are :left :right and :auto(default)
  ;:vert-align values are :top :bottom and :auto(default)
  ;:sizing values are :auto and :self(default means popup has same width as "link")
  ;:pop-component - function that renders popup content
  ;:self-component - function that renders "link" of popup
  (let [drop-type (if (nil? drop-type) :auto drop-type)
        size-type (if (nil? size-type) :self size-type)
        set-svis! (fn [o vis] (fn[e]
                                (om/set-state! o [:select-visible] vis)
                                (om/set-state! o [:adjusted] false)))]
    (reify
      om/IInitState
      (init-state [this]
                  {:mouse-handler nil
                   :select-visible false
                   :adjusted false})
      om/IDidMount
      (did-mount [this node]
                 (let [mouse-handler
                       (fn [e]
                         (if-let [popup (gd/getElementByClass "popup" node)]
                           (when-not (in? e node) ((set-svis! owner false) e))))]
                   (events/listen js/document.body goog.events.EventType.MOUSEDOWN mouse-handler)
                   (om/set-state! owner [:mouse-handler] mouse-handler)
                   ))
      om/IWillUnmount
      (will-unmount [this]
                    (if-let [mouse-handler (om/get-state owner [:mouse-handler])]
                      (events/unlisten js/document.body goog.events.EventType.MOUSEDOWN mouse-handler)))
      om/IDidUpdate
      (did-update [this prev-props prev-state root-node]
                  (if-let [popup (gd/getElementByClass "popup" root-node)]
                    (do
                      (when (and (= :auto drop-type)(not(om/get-state owner [:adjusted])))
                        (if (>(.. popup getBoundingClientRect -bottom)(.-height (gd/getViewportSize)))
                          (gcls/add popup "drop-up")(gcls/add popup "drop-down"))
                        ;;;;TODO Remove this adjusted crap
                        (om/set-state! owner [:adjusted] true))
                      ;;;;Solve scroll problem
                      ;(.log js/console (.-offsetTop popup))
                      ;(.scrollTo (gd/getWindow) -200)
                      )))
      om/IRender
      (render [_]
              (let [cnt (cnt-sel coll)
                    svis? (om/get-state owner [:select-visible])]
                (dom/div #js{:id "area-select"
                             :className (str "popup-select" (when (> cnt 0) " chosen") (when svis? " select-visible"))}

                         (dom/span #js{:className "choise"
                                       :title (:choose conf)
                                       :onClick (set-svis! owner (not svis?))}
                                   (if (> cnt 0)(str (:chosen conf) cnt) (:choose conf))
                                   (dom/span #js{:className "triangle" :title (if svis? "Завершить выбор" "Выбрать") } (if svis? "✔" "▼"))
                                   (when (> cnt 0)(dom/span #js{:className "cancel"
                                                                :title "Отменить выбор"
                                                                :onClick (fn [e]
                                                                           (om/update! coll (fn[c] (vec (map #(assoc % :selected false) c))))
                                                                           false)}
                                                            "×")))
                         (when svis?
                           (dom/div #js{:ref "popup" :className (str "popup "
                                                                     (when-not (= :auto drop-type)(name drop-type))
                                                                     (when (= :auto size-type) " autosize"))}
                                    (dom/h2 nil (:head conf))
                                    (dom/div #js{:className "five columns hole-fake"})
                                    (om/build builder coll)))
                         ))))))

(defn areas-block [city owner {:keys [b-id b-class]:as opts}]
  (reify
    om/IRender
    (render [_]
            (let [atype-conf ((:area-type city)(get-conf [:area-select :area-types]))
                  cnt (cnt-sel :areas city)]

              (dom/div #js{:id b-id :className b-class}
                       (dom/h2 nil (:head atype-conf))
                       (if (= (:area-type city) :district)
                         (om/build popup-select (:areas city) {:opts{:conf atype-conf :builder checkbox-list}})
                         (om/build popup-select (:areas city) {:opts{:conf atype-conf :builder metro-select
                                                                     :drop-type :drop-down
                                                                     :size-type :auto}})
                         ))))))

(defn search-app [app]
  (reify
    om/IRender
    (render [_]
            (let [city (first(filter :selected (:cities app)))]
            (dom/div nil
            (om/build radio-list (:cities app) {:opts {:field :cities}})
            (dom/div #js{:id "filter-wrapper"}
            (dom/div #js{:id "outer-filter"}
            (dom/div #js{:id "inner-filter"}
            (dom/div #js{:className "row"}
                     (dom/div #js{:id "filter-rooms" :className "five columns"}
                              (dom/h2 nil "Количество комнат")
                              (om/build checkbox-list (:object-types app)))
                     (dom/div #js{:id "filter-monthly-cost" :className "offset-by-one nine columns filter-rt-corner"}
                              (dom/h2 nil "Плата в месяц, р")
                              (om/build uire/range-editor (:monthly-cost-range app)
                                        {:opts {:min-bottom 0 :max-top 80000 :step 500 }}))
                     )
            (dom/div #js{:className "row"}
                     (om/build areas-block city {:opts {:b-id "filter-districts" :b-class "five columns"}})
                     (dom/div #js{:id "filter-area" :className "offset-by-one six columns"}
                              (dom/h2 nil "Площадь квартиры, м" (dom/sup nil 2))
                              (om/build uire/range-editor (:total-area-range app)
                                        {:opts {:min-bottom 0 :max-top 200 :step 1 }}))
                     (dom/div #js{:id "filter-submit" :className "three columns"}
                              (dom/button #js{:id "btn-submit-filter"} "Найти квартиру"))
                     )))
            (dom/a #js{:id "filter-to-extended" :href "#/fmode/extended" :title "Расширенный поиск"}))
                     )))))

(om/root app-state search-app (.getElementById js/document "filter"))
