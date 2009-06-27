(ns com.mauricecodik.icfp2009.vm-runner
    (:import (com.mauricecodik.icfp2009 VirtualMachine Visualizer)))

(defn compute-hohman [inner]
  (fn [vm]
      (let [data { :score (. vm getOutput 0)
		  :fuel (. vm getOutput 1)
		  :sx (. vm getOutput 2)
		  :sy (. vm getOutput 3)
		  :target-radius (. vm getOutput 4)
                  :t (. vm getCurrentIteration) }
	    output (inner data)]
	(print data) (print " -> ") (println output)
	(if (contains? output :vx) (. vm setInput 2 (:vx output)))
	(if (contains? output :vy) (. vm setInput 3 (:vy output))))))

(def earth-size 6357000.0)

(defn vec-norm [x y]
  (Math/sqrt (+ (* x x) (* y y))))

(defn earth-vec->coords [sx sy]
  (let [theta (Math/atan2 sy sx)
	norm (+ earth-size (vec-norm sx sy))]
    { :x (* norm (Math/sin theta))
     :y (* norm (Math/cos theta)) }))

(defn visualize-hohman [inner]
  (let [viz (new Visualizer 900 (/ 1.0 100000.0))]
    (fn [data]
	(if (= (mod (:t data) 25) 0)
	  (let [coords (earth-vec->coords (:sx data) (:sy data))]
	    (doto viz
	      (. addCircle 0 0 earth-size)
	      (. addCircle 0 0 (:target-radius data))
	      (. addPoint "me" (:x coords) (:y coords))
	      (. repaint))))
	  (inner data))))

(defn noop [data] {})

(defn what [data]
  (cond 
   (= 1 (:t data)) { :vx -1000.0 }
   (= 3 (:t data)) { :vx 0 }
   :otherwise {}))

(doto (new VirtualMachine)
  (. load "problems/bin1.obf")
  (. run 1001 100000 (compute-hohman (visualize-hohman noop))))