
(ns vm
    (:import (java.io BufferedInputStream FileInputStream)))

(defn make-vm [filename]
  (let [data-memory (double-array (Math/pow 2 14) (double 0.0))
	instruction-memory (int-array (Math/pow 2 14) (int 0))
	program-counter (long-array 1 0)
	status-register (long-array 1 0)]
    (let [program-stream (new 
    