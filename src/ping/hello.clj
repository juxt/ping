;; Copyright © 2016, JUXT LTD.

(ns ping.hello
  "Demonstrating a simple example of a yada web resource"
  (:require
   [yada.yada :as yada]))

(defn hello-routes []
  ["" (yada/handler "Hello World!\n")]
  ["/hello" (yada/handler "Hello World!\n")])
