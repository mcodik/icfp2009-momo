(ns com.mauricecodik.icfp2009.vm_runner
    (:import (com.mauricecodik.icfp2009 VirtualMachine))
    (:gen-class))

(def prev-data (ref { :sx 0 :sy 0 }))

(defn vec-norm [x y]
  (Math/sqrt (+ (* x x) (* y y))))

(defn compute-hohman [inner]
  (fn [vm]
      (let [data { :score (. vm getOutput 0)
		  :fuel (. vm getOutput 1)
		  :sx (. vm getOutput 2)
		  :sy (. vm getOutput 3)
		  :target-radius (. vm getOutput 4)
		  :current-radius (vec-norm (. vm getOutput 2) (. vm getOutput 3))
                  :t (. vm getCurrentIteration) }
	    output (inner data)]
;	(println data " -> " output)
	(if (contains? output :vx) (. vm setInput 2 (double (:vx output))))
	(if (contains? output :vy) (. vm setInput 3 (double (:vy output))))
	(dosync (ref-set prev-data data))
)))


(defn compute-meet-and-greet [inner]
  (fn [vm]
      (let [data { :score (. vm getOutput 0)
		  :fuel (. vm getOutput 1)
		  :sx (. vm getOutput 2)
		  :sy (. vm getOutput 3)
		  :bx (. vm getOutput 4)
		  :by (. vm getOutput 5)
		  :current-radius (vec-norm (. vm getOutput 2) (. vm getOutput 3))
                  :t (. vm getCurrentIteration) }
	    output (inner data)]
;	(println data " -> " output)
	(if (contains? output :vx) (. vm setInput 2 (double (:vx output))))
	(if (contains? output :vy) (. vm setInput 3 (double (:vy output))))
	(dosync (ref-set prev-data data))
)))

(def earth-size 6357000.0)

(defn vec->coords [sx sy]
  (let [theta (Math/atan2 sy sx)
	norm (vec-norm sx sy)]
    { :x (* norm (Math/sin theta))
     :y (* norm (Math/cos theta)) }))

(defn current-velocity [x y] 
  { :vx (- x (:sx @prev-data))
    :vy (- y (:sy @prev-data))  })

(defn apply-deltav [currentv deltav] 
  (let [theta (Math/atan2 (:vx currentv) (:vy currentv))]
    { :vx (* deltav (Math/sin theta))
     :vy (* deltav (Math/cos theta)) }))
  
(def mu (* 6E24 6.67428E-11))

(defn hohman-entry-deltav [r1 r2]
  (* (Math/sqrt (/ mu r1)) (- 1 (Math/sqrt (/ (* 2 r2) (+ r1 r2))))))

(defn hohman-exit-deltav [r1 r2]
  (* (Math/sqrt (/ mu r2)) (- (Math/sqrt (/ (* 2 r1) (+ r1 r2))) 1)))

(defn hohman-duration [r1 r2]
  (int (Math/round (* Math/PI (Math/sqrt (/ (Math/pow (+ r1 r2) 3) (* 8 mu)))))))

(defn noop [data] {})

(def current-solver (ref noop))

(defn set-solver [f]
  (dosync (ref-set current-solver f)))

(defn solve [data] (@current-solver data))

(defn waiting-solver [stop-time action-at-stop next]
  (fn [data]
      (if (= stop-time (:t data))
	(do 
	  (println "waiting-solver transtion to " next) 
	  (set-solver next)
	  (action-at-stop data))
	{})))

(defn burn [data duration deltav next-solver]
  (let [currentv (current-velocity (:sx data) (:sy data))
	newv (apply-deltav currentv deltav)
	finalv { :vx (+ (:vx currentv) (:vx newv)) :vy (+ (:vy currentv) (:vy newv)) }
	until (+ (:t data) duration)]
    (println "burn until " until ". deltav= " deltav 
	     " currentv= " currentv " (norm= " (vec-norm (:vx currentv) (:vy currentv)) ")" 
	     "newv= " newv " (norm= " (vec-norm (:vx newv) (:vy newv)) ")" 
	     "finalv norm= " (+ (vec-norm (:vx currentv) (:vy currentv))
				(vec-norm (:vx newv) (:vy newv))))
    (println data)
    (set-solver (waiting-solver until (fn [data] { :vx 0 :vy 0 }) next-solver))
    newv))
  
(defn hohman-solver [data]
  (let [deltav (hohman-entry-deltav (:current-radius data) (:target-radius data))
	exit-deltav (hohman-exit-deltav (:current-radius data) (:target-radius data))
	eta (+ (:t data) (hohman-duration (:current-radius data) (:target-radius data)))
	exit-solver 
	  (fn [data2] 
	      (println "hohmann exit: current altitude: " (:current-radius data2)
		       " computed exit dv = " exit-deltav)
	      (burn data2 1 exit-deltav noop))]
    (println "hohmann entry: current altitude: " (:current-radius data) 
	     " computed entry dv = " deltav " eta " eta)
    (burn data 1 deltav (waiting-solver eta exit-solver exit-solver))))

(defn -main []
  ;(set-solver (waiting-solver 3 hohman-solver hohman-solver))

  (doto (new VirtualMachine)
    (. load "problems/bin2.obf")
    (. run 2001 100000 (compute-meet-and-greet solve)))
)
