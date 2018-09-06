(ns tasche.db)

(def default-db
  {:state :not-logged-in
   :balance nil
   :wallet nil
   :transactions {}
   :pendings {}})
