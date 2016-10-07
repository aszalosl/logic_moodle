;;;; The central part of logic is the zero order formulae.

(ns logic.formula
  (:require [clojure.string :as s]
            [clojure.set :as cs])
  (:gen-class))

;; ## Formulae generation (systematic)

(defn combine "combine the formulae of 2 lists with all operators"
  [s1 s2]
  (for [x s1, y s2, op [:imp :and :or :equ]]
    [op x y]))

(defn negated "generate the list of negated elements"
  [s]
  (for [x s] [:not x]))

;(def f0 "atomic formulae" '(:p :q :r))
;; alternative
;(def f0 "atomic formulae" '(:p :q :r :s))
;(def f0 "atomic formulae" '(:p :q :r :s :t))
;(def f0 "atomic formulae" '(:p :q :r :s :t :u))
; (def f0 "starting sets" '(:a :b :c :0))

(defn f1 "formulae written with one operator"
  [af]
  (concat
    (combine af af)
    (negated af)))

(defn f2 "formulae written with 2 operators"
  [af]
  (let [f1s (f1 af)]
    (concat
      (combine af f1s)
      (combine f1s af)
      (negated f1s))))

(defn f3 "formulae written written with 3 operators"
  [af]
  (let [f1s (f1 af)
        f2s (f2 af)]
    (concat
       (combine af f2s)
       (combine f1s f1s)
       (combine f2s af)
       (negated f2s))))

(defn form "generate a big set of formulae"
  [af]
  (concat af (f1 af) (f2 af)  (f3 af)))

;; ### Random formula

(defn random-formula "generate a random formula,
                     maybe with many connectives"
  [l vars]
  (if (= 0 l)
    (rand-nth vars)
    (let [op (rand-nth [:not :imp :and :or :equ])]
      (if (= op :not)
        [:not (random-formula (- l 1) vars)]
        (let [l1 (rand-int l)
              l2 (- l l1 1)]
          [op (random-formula l1 vars)
              (random-formula l2 vars)])))))

;; ## String representation of the formula

(defn write "MathJax compatible string of an expression"
  ([e]
   (if (vector? e)
     (let [op (first e)]
       (if (= :not op)
         (str "\\lnot " (write (second e)))
         (let [wop (case op :imp "\\supset " :and "\\land "
                    :or "\\lor " :equ "\\equiv ")
               w1 (write (second e))
               w2 (write (nth e 2))]
           (str "(" w1 wop w2 ")"))))
    (case e :p "p" :q "q" :r "r" :s "s" :t "t" :u "u"))))

(defn includes?
  [coll key]
  (if (vector? coll)
    (if (= :not (first coll))
      (includes? (second coll) key)
      (or (includes? (second coll) key)
          (includes? (nth coll 2) key)))
    (= coll key)))

(defn parameters "returns the parameters of a formulae"
  [f]
  (if (vector? f)
    (let [s1 (parameters (second f))]
      (if (= :not (first f))
        s1
        (cs/union s1 (parameters (nth f 2)))))
    (set [f])))
(parameters (random-formula 6 [:p :q]))

;; ### Quine truth table problems
;;

;; We code the 4 equivalence classes of formulae with
;; 1 parameters with numbers 0-3

(def cP1 2)                    ;01
(def cT1 "logical truth" 3)    ;11

(defn code1 "code of a formula"
  [f]
  (if (vector? f)
    (let [c1 (code1 (second f))
          op (first f)]
      (if (= :not op)
        (- cT1 c1)
        (let [c2 (code1 (nth f 2))]
          (case op
            :and (bit-and c1 c2)
            :or  (bit-or c1 c2)
            :imp (bit-or (- cT1 c1) c2)
            :equ (- cT1 (bit-xor c1 c2))))))

    (case f :p cP1 :f 0 :t cT1)))

;; We code the 16 equivalence classes of formulae with
;; 2 parameters with numbers 0-15

(def cP2 (+ 4 8))              ;0011
(def cQ2 (+ 2 8))              ;0101
(def cT2 "logical truth" 15)   ;1111

(defn code2 "code of a formula"
  [f]
  (if (vector? f)
    (let [c1 (code2 (second f))
          op (first f)]
      (if (= :not op)
        (- cT2 c1)
        (let [c2 (code2 (nth f 2))]
          (case op
            :and (bit-and c1 c2)
            :or  (bit-or c1 c2)
            :imp (bit-or c2 (- cT2 c1))
            :equ (- cT2 (bit-xor c1 c2))))))

    (case f :p cP2 :q cQ2 :f 0 :t cT2)))

;; We code the 256 equivalence classes of formulae with
;; 3 parameters with numbers 0-255

(def cP3 (+ 16 32 64 128))      ;00001111
(def cQ3 (+ 4 8 64 128))        ;00110011
(def cR3 (+ 2 8 32 128))        ;01010101
(def cT3 "logical truth" 255)   ;11111111

(defn code3 "code of a formula"
  [f]
  (if (vector? f)
    (let [c1 (code3 (second f))
          op (first f)]
      (if (= :not op)
        (- cT3 c1)
        (let [c2 (code3 (nth f 2))]
          (case op
            :and (bit-and c1 c2)
            :or  (bit-or c1 c2)
            :imp (bit-or c2 (- cT3 c1))
            :equ (- cT3 (bit-xor c1 c2))))))

    (case f :p cP3 :q cQ3 :r cR3 :f 0 :t cT3)))

(defn truth-table-main-column "values from interpretations 00...0 to 11...1"
  [code size]
  (s/join (take size (reverse (str "0000000" (Integer/toBinaryString code))))))

(defn ttmc2 "values from interpretations 00 to 11"
  [code]
  (truth-table-main-column code 4))

(defn quine-test "generate a random formulae with given parameters,
                  and construct the test based on it"
  [i pars f]
  (let [lp (count pars)
        p (count (parameters f))]
    (when (= lp p)
      (let [c (case p 1 (code1 f), 2 (code2 f), 3 (code3 f))
            size (case p 1 2, 2 4, 3 8)
            q (str
               "Mit tartalmaz a(z) \\( "
               (write f)
               " \\) igazságtáblájának a fő oszlopa az interpretációk"
               " <i>növekvő</i> sorrendje esetén?")
            ans (truth-table-main-column c size)
            id (str "QTT" p "-" i)
            fb (str "a megoldás: " ans)]
          [q ans id fb]))))

(defn quine-test1 "Test with 1 parameter"
  [i]
  (quine-test i [:p] (random-formula 3 [:p])))

(defn quine-test2 "Test with 2 parameter"
  [i]
  (quine-test i [:p :q] (random-formula 4 [:p :q])))

(defn quine-test3 "Test with 3 parameter"
  [i]
  (quine-test i [:p :q :r] (random-formula 5 [:p :q :r])))

;; ### Model quiz
;;
;; Only with 2 parameters

(def form2 "formulae with two parameter"
    (form [:p :q]))

(defn equ-class2 "Equivalence class of formulae with code n"
  [n]
  (for [x form2 :when (= n (code2 x))] x))

;(equ-class2 0)

;; #### some quiz for a formula

(defn print-good-items "print the good formulae in formatted way"
  [code n]
  (s/join (map #(str "    \"\\( " (write %) " \\)\"\n")
             (repeatedly n #(rand-nth (equ-class2 code))))))

(defn print-bad-items "print the not suitable formulae
                       and a feedback about its class"
  [code n]
  (s/join (map #(str "    [\"\\( " (write %) " \\)\""
                     " \"az igazságtábla főoszlopa: "
                     (ttmc2 code) "\"]\n")
             (repeatedly n #(rand-nth (equ-class2 code))))))

(defn cont-quiz2 "generate a semi-question for a quiz
                  about contradictory formulae"
  []
  (str "[{:question \"Jelölje meg a kielégíthetetlen formulákat!\"\n  :good [\n"
       (print-good-items 0 30)
       "  ]\n  :wrong [\n"
       (s/join (for [i (range 1 16)] (print-bad-items i 5)))
       "]}]\n"))

(comment (spit "contra2.clj" (cont-quiz2)))

(defn valid-quiz2 "generate a semi-question for a quiz about valid formulae"
  []
  (str "[{:question \"Jelölje meg az érvényes formulákat!\"\n  :good [\n"
       (print-good-items  15 30)
       "  ]\n  :wrong [\n"
       (s/join (for [i (range 0 15)] (print-bad-items i 5)))
       "]}]\n"))

(comment (spit "valid2.clj" (valid-quiz2)))

(defn sat-quiz2 "generate a semi-question for a quiz about satisfiable formulae"
  []
  (str "[{:question \"Jelölje meg a kielégíthető,"
       " de nem érvényes formulákat!\"\n  :good [\n"
       (s/join (for [i (range 1 15)] (print-good-items i 5)))
       "  ]\n  :wrong [\n"
       (s/join [(print-bad-items 0 30) (print-bad-items 15 30)])
       "]}]\n"))

(comment (spit "sat2.clj" (sat-quiz2)))

(defn logic_conseq "generate a semi-question for a quiz about
                    logical consequence of a formula"
  []
  (s/join
    (for [i (range 0 16)]
      (str " {:question \"Jelölje meg a \\( "
           (write (rand-nth (equ-class2 i)))
           " \\) formula logikai következményeit!\"\n  :good [\n"
           (s/join (for [j (range 0 16) :when (= 0 (bit-and i (- 15 j)))]
                     (print-good-items j 5)))
           "  ]\n  :wrong [\n"
         (s/join
           (for [k (range 0 4) j (range 0 16)
                  :when (not= 0 (bit-and i (- 15 j)))]
             (str
               "            [\"\\( " (write (rand-nth (equ-class2 j))) " \\)\""
               " \"a konklúzió igazságtáblájának főoszlopa: " (ttmc2 j)
               ", míg a hipotézis igazságtáblájának főoszlopa " (ttmc2 i)
               " \"]\n")))
         "]}\n"))))

(comment (spit "lc1a.clj" (logic_conseq)))

(defn logic_conseq2 "generate a semi-question for a quiz about
                    logical consequence of something"
  []
  (s/join
    (for [i (range 0 16)]
      (str " {:question \"Jelölje meg azokat a formulákat, melyeknek a \\( "
           (write (rand-nth (equ-class2 i)))
           " \\) formula logikai következménye! (Külön-külön)\"\n  :good [\n"
           (s/join (for [j (range 0 16) :when (= 0 (bit-and j (- 15 i)))]
                     (print-good-items j 5)))
           "  ]\n  :wrong [\n"
         (s/join
           (for [k (range 0 4) j (range 0 16)
                  :when (not= 0 (bit-and j (- 15 i)))]
             (str
               "            [\"\\( " (write (rand-nth (equ-class2 j))) " \\)\""
               " \"a hipotézis igazságtáblájának főoszlopa: " (ttmc2 j)
               ", míg a konklúzió igazságtáblájának főoszlopa " (ttmc2 i)
               " \"]\n")))
         "]}\n"))))

(comment (spit "lc1b.clj" (logic_conseq2)))

;; #### some quiz for set of formulae

(defn make-quiz2 "generate a semi-question for a quiz with criteria f"
  [f question]
  (str " {:question \"" question "\"\n  :good [\n"
       (s/join (for [x (range 0 16) y (range 0 16) :when (f x y)]
                 (str "         \"\\( \\{"
                      (write (rand-nth (equ-class2 x))) ",\\ "
                      (write (rand-nth (equ-class2 y))) "\\} \\)\"\n")))
       "  ]\n  :wrong [\n"
       (s/join (for [x (range 0 16) y (range 0 16) :when (not (f x y))]
                 (str "          [\"\\( \\{"
                      (write (rand-nth (equ-class2 x))) ",\\ "
                      (write (rand-nth (equ-class2 y))) "\\} \\)\" \""
                      "az igazságtáblák főoszlopai " (ttmc2 x) " és "
                      (ttmc2 y) "\"]\n")))
       "]}\n"))

(comment (spit "contG22.clj"
           (make-quiz2 #(= 0 (bit-and %1 %2))
             "Jelölje meg a kielégíthetetlen formulahalmazokat!")))

(comment (spit "satG22.clj"
           (make-quiz2 #(not= 0 (bit-and %1 %2))
             "Jelölje meg a kielégíthetető formulahalmazokat!")))

(defn logic_conseq3 "generate a semi-question for a quiz about
                    logical consequence of formulae"
  []
  (s/join
    (for [i (range 0 16) j (range 0 16) :when (< i j)]
      (str " {:question \"Jelölje meg a \\( \\{"
           (write (rand-nth (equ-class2 i))) "\\), \\("
           (write (rand-nth (equ-class2 j))) "\\} "
           " \\) formulahalmaz logikai következményeit!\"\n  :good [\n"
           (s/join (for [k (range 0 16) :when (= 0 (bit-and i j (- 15 k)))]
                     (print-good-items k 5)))
           "  ]\n  :wrong [\n"
         (s/join
           (for [l (range 0 4) k (range 0 16)
                  :when (not= 0 (bit-and i j (- 15 k)))]
             (str
               "            [\"\\( " (write (rand-nth (equ-class2 j))) " \\)\""
               " \"a konklúzió igazságtáblájának főoszlopa: " (ttmc2 k)
               ", míg a hipotézisek igazságtábláinak főoszlopai " (ttmc2 i)
               " és " (ttmc2 j) " \"]\n")))
         "]}\n"))))

(comment (spit "lc2.clj" (logic_conseq3)))


;; ### Subformulae quiz

(defn subformulae "set of subformulae of a formula"
  [f]
  (let [sf (set [(write f)])]
    (if (vector? f)
      (let [sf1 (subformulae (second f))
            op (first f)]
        (if (= :not op)
          (cs/union sf1 sf)
          (let [sf2 (subformulae (nth f 2))]
            (cs/union sf1 sf2 sf)))))
    sf))

(defn sub-quiz "generate a semi-question for a quiz about subformulae"
  [f]
  (str " {:question \"A felsoroltak közül melyek részformulái a \\( "
       (write f)
       " \\) formulának?\"\n  :good [\n"
       (s/join
         (map #(str "    \"\\( " % " \\)\"\n") (subformulae f)))
       "  ]\n  :wrong []}\n"))


(defn to-file "write into file out n semi-questions generated by func"
  [n func out]
  (doseq [i (range 0 n)]
    (let [x (func)]
      (spit out x :append true))))

(comment (to-file 50
                  (fn [] (sub-quiz (random-formula 7 [:p :q :r])))
                  "subform.clj"))
