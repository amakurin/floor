(ns lobos.config
  (:use lobos.connectivity)
  (:require [floor16.dal.schema :as schema]))

(open-global schema/db-spec)