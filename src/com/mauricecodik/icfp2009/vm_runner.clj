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
                  :t (. vm getCurrentIteration) 
		  :current-radius (vec-norm (. vm getOutput 2) (. vm getOutput 3))}
	    output (inner data)]
	(println data " -> " output)
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
  { :vx (Math/abs (- x (:sx @prev-data)))
    :vy (Math/abs (- y (:sy @prev-data)))  })

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

(defn stabilize [next-solver]
     (dosync (ref-set current-solver next-solver))
     { :vx 0 :vy 0 })

(defn stabilize-next-iter [next-solver]
     (dosync (ref-set current-solver
		      (fn [data] (stabilize next-solver)))))

(defn exit-solver [data]
     (if (> 1000.0 (Math/abs (- (:target-radius data) (:current-radius data))))
       (let [deltav (hohman-exit-deltav (:current-radius data) (:target-radius data))
	     currentv (current-velocity (:sx data) (:sy data))
	     newv (apply-deltav currentv deltav)]
	 (println "hohmann exit: current altitude: " (:current-radius data)  " current v = " currentv 
		  " computed exit dv = " deltav " newv " newv)
	 (stabilize-next-iter noop)
	 newv)
       {}))

(defn stabilize-next-then-wait-until [time next-solver]
  (dosync (ref-set current-solver
		   (fn [foo] 
		       (dosync (ref-set current-solver 
					(fn [data]
					    (if (= time (:t data))
					      (do 
						(dosync (ref-set current-solver next-solver))
						(next-solver data))
					      {}))))
			       { :vx 0 :vy 0 }))))

(defn entry-solver [data]
  (if (= 3 (:t data)) 
    (let [deltav (hohman-entry-deltav (:current-radius data) (:target-radius data))
	  currentv (current-velocity (:sx data) (:sy data))
	  newv (apply-deltav currentv deltav)
	  eta (+ (:t data) (hohman-duration (:current-radius data) (:target-radius data)))]
      (println "hohmann entry: current altitude: " (:current-radius data) " current v = " currentv 
	       " computed entry dv = " deltav " newv " newv 
	       " eta " eta)
      (stabilize-next-then-wait-until eta exit-solver)
      newv)
    {}))

(defn solve-hohman [data] (@current-solver data))

(defn -main []
  (dosync (ref-set current-solver entry-solver))
  (doto (new VirtualMachine)
    (. load "problems/bin1.obf")
    (. run 1001 100000 (compute-hohman solve-hohman)))
)
