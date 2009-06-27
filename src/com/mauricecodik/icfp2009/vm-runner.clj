(ns com.mauricecodik.icfp2009.vm-runner
    (:import com.mauricecodik.icfp2009.VirtualMachine))

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


(defn noop [data] { :vx 10000.0 })

(doto (new VirtualMachine)
  (. load "problems/bin1.obf")
  (. run 1001 15 (compute-hohman noop)))