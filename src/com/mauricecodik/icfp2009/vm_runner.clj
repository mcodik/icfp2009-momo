(ns com.mauricecodik.icfp2009.vm_runner
    (:import (com.mauricecodik.icfp2009 VirtualMachine Visualizer))
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
;	(println data " -> " output)
	(if (contains? output :vx) (. vm setInput 2 (double (:vx output))))
	(if (contains? output :vy) (. vm setInput 3 (double (:vy output))))
;	(dosync (ref-set prev-data data))
)))

(def earth-size 6357000.0)

(defn vec->coords [sx sy]
  (let [theta (Math/atan2 sy sx)
	norm (vec-norm sx sy)]
    { :x (* norm (Math/sin theta))
     :y (* norm (Math/cos theta)) }))

(defn visualize-hohman [inner]
  (let [viz (new Visualizer 900 (/ 1.0 100000.0))]
    (fn [data]
	(if (= (mod (:t data) 100) 0)
	  (let [coords (vec->coords (:sx data) (:sy data))]
	    (doto viz
	      (. addCircle 0 0 earth-size)
	      (. addCircle 0 0 (:target-radius data))
	      (. addPoint "me" (:x coords) (:y coords))
	      (. repaint))))
	  (inner data))))

(defn current-velocity [x y] 
  { :vx (- (:sx @prev-data) x) 
    :vy (- (:sy @prev-data) y)  })

(defn apply-deltav [currentv deltav] 
  (let [theta (Math/atan2 (:vx currentv) (:vy currentv))]
    { :vx (* deltav (Math/sin theta))
     :vy (* deltav (Math/cos theta)) }))
  
(def mu (* 6E24 6.67428E-11))

(defn hohman-entry-deltav [r1 r2]
  (* (Math/sqrt (/ mu r1)) (- 1 (Math/sqrt (/ (* 2 r2) (+ r1 r2))))))

(defn hohman-exit-deltav [r1 r2]
  (* (Math/sqrt (/ mu r1)) (- (Math/sqrt (/ (* 2 r2) (+ r1 r2))) 1)))

(defn solve-hohman [data]
  (cond 
   (= 3 (:t data)) 
     (let [current-orbit (:current-radius data)
	   target (:target-radius data)
	   deltav (hohman-entry-deltav current-orbit target)
	   currentv (current-velocity (:sx data) (:sy data))
	   newv (apply-deltav currentv deltav)]
       ;(println "computed: dv = " deltav " newv = " newv)
       newv)
   (= 4 (:t data)) { :vx 0 :vy 0 }
   :otherwise {}))

(defn hardcode-hohman [data]
  (cond
   (= 3 (:t data)) {:vx 10.1234123412 :vy 2466.1324123412}
   (= 4 (:t data)) { :vx 0 :vy 0 }
   :otherwise {}))

(defn noop [data] {})

(defn -main []
  (doto (new VirtualMachine)
    (. load "problems/bin1.obf")
    (. run 1001 100000 (compute-hohman (visualize-hohman solve-hohman)))
))
