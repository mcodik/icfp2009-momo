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
      (let [sx (. vm getOutput 2)
	    sy (. vm getOutput 3)
	    bx (. vm getOutput 4)
	    by (. vm getOutput 5)
	    px (- bx sx)
	    py (- by sy)
	    data { :score (. vm getOutput 0)
		  :fuel (. vm getOutput 1)
		  :sx sx
		  :sy sy
		  :bx bx
		  :by by
		  :px px
		  :py py
		  :current-radius (vec-norm sx sy)
		  :target-radius (vec-norm px py)
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

(defn their-current-velocity [x y] 
  { :vx (- x (:px @prev-data))
    :vy (- y (:py @prev-data))  })

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

(defn predict-circular-position [x y deltat]
     (let [r (vec-norm x y)
	   theta (Math/atan2 x y)
	   phi (+ theta (* deltat (Math/sqrt (/ mu (* r r r)))))]
       { :sx (* r (Math/sin phi)) :sy (* r (Math/cos phi)) }))

(defn hohman-mirror [x y current-radius target-radius]
  (let [r (/ (+ current-radius target-radius) 2.0)
	theta (Math/atan2 x y)
	deltat (hohman-duration current-radius target-radius)
	phi (+ theta (* deltat (Math/sqrt (/ mu (* r r r)))))]
    { :sx (* target-radius (Math/sin phi)) 
      :sy (* target-radius (Math/cos phi)) }))
  
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

(defn vec-diff [x1 y1 x2 y2]
     { :sx (- x2 x1) :sy (- y2 y1) })

(defn simulate-object [now until x y vx vy]
  (cond
   (= now until) { :sx x :sy y :vx vx :vy vy }
   :otherwise
     (let [theta (Math/atan2 x y)
	   r (vec-norm x y)
	   gt (/ mu (* r r))
	   gtx (* gt (Math/cos theta))
	   gty (* gt (Math/sin theta))
	   x1 (+ x vx (/ gtx 2.0))
	   y1 (+ y vy (/ gty 2.0))
	   r1 (vec-norm x1 y1)
	   gt1 (/ mu (* r1 r1))
	   gtx1 (* gt1 (Math/cos theta))
	   gty1 (* gt1 (Math/sin theta))
	   vx1 (+ vx (/ (+ gtx gtx1) 2.0))
	   vy1 (+ vy (/ (+ gty gty1) 2.0))]
;       (println "simulation" now "sx" x1 "sy" y1 "vx" vx1 "vy" vy1)
       (recur (+ 1 now) until x1 y1 vx1 vy1))))

(def min-error (ref {:error Double/MAX_VALUE :t 0}))

(defn mag-solver [data]
  (let [eta (hohman-duration (:current-radius data) (:target-radius data))
	arrival-pos (hohman-mirror (:sx data) (:sy data) (:current-radius data) (:target-radius data))
	their-pos (predict-circular-position (:px data) (:py data) eta)
	error (vec-norm (- (:sx arrival-pos) (:sx their-pos)) (- (:sy arrival-pos) (:sy their-pos)))]

    (if (< error (:error @min-error))
      (dosync (ref-set min-error { :error error :t (:t data) })))

    (println (:t data) "if we start now, we arrive at" arrival-pos "norm=" (vec-norm (:sx arrival-pos) (:sy arrival-pos))
	     "at time" (+ eta (:t data)) 
	     ". at that time, we think they will be at:" their-pos
	     "error" error " best so far was " (:error @min-error) 
	     "at t= " (:t @min-error) "they are currently at { :px " (:px data) ":py" (:py data) "}"
	     "norm=" (:target-radius data))

    (if (> 1000 error)
      (do (set-solver hohman-solver)
	  (hohman-solver data))
      {})))

(defn prediction-solver
  [data]
  (let [deltat 10
	futurepos (predict-circular-position (:sx data) (:sy data) deltat)]
    (println (:t data) "current px:" (:sx data) "current py:" (:sy data)
	     "guess at t=" (+ deltat (:t data)) "px:" (:sx futurepos) "py:" (:sy futurepos))
    {}))


(def last-simulation (ref {}))
(defn simulation-solver 
  [data]
  (let [theirv (their-current-velocity 	(:px data) (:py data))
	their-data (simulate-object (:t data) (+ 1 (:t data)) 
				    (:px data) (:py data) 
				    (:vx theirv) (:vy theirv))]
    (println "actual sx" (:px data) "sy" (:py data) "vx" (:vx theirv) "vy" (:vy theirv))
    (if (contains? @last-simulation :sy)
      (do 
	(println "pos error" (vec-norm (- (:sx @last-simulation) (:px data)) 
					 (- (:sy @last-simulation) (:py data))))
	(println "v error" (vec-norm (- (:vx @last-simulation) (:vx theirv)) 
				     (- (:vy @last-simulation) (:vy theirv))))))
    (println "------")
    (println "expected next" their-data)
    (dosync (ref-set last-simulation their-data))
    {}))
    
	    
(defn -main [configs]
;  (set-solver (waiting-solver 3 mag-solver mag-solver))
;  (set-solver (waiting-solver 3 simulation-solver simulation-solver))
;  (set-solver (waiting-solver 3 prediction-solver prediction-solver))
  (println "running config" configs) 
  (let [config (Integer/parseInt configs)]
    (cond
     (and (> 1000 config) (< 2000 config))
     (do
       (set-solver (waiting-solver 3 hohman-solver hohman-solver))
       (doto (new VirtualMachine)
	 (. load "problems/bin1.obf")
	 (. run config 100000 (compute-hohman solve))))
     :otherwise 
     (do
       (set-solver (waiting-solver 3 mag-solver mag-solver))
       (doto (new VirtualMachine)
	 (. load "problems/bin2.obf")
	 (. run config 100000 (compute-meet-and-greet solve)))))))