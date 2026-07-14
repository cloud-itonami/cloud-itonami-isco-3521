(ns media.store
  "SSoT for the ISCO-08 3521 independent broadcast/media syndication
  practice actor (itonami actor pattern, ADR-2607011000 / CLAUDE.md
  Actors section; README's 'Robotics premise' + kawaraban downstream
  contract — a studio/production-booth robot handles camera framing,
  audio-level riding and asset ingest/transcode under this
  advisor/governor pair, which never dispatches hardware itself and
  never scrapes kawaraban full text). Modeled on
  cloud-itonami-isco-4311's bookkeeping.store.

  Domain:

    client  — a registered organization/practice (:client-id, :name)
    article — a registered kawaraban mirror-feed article, ingested by
              this practice: {:article-id :client-id :headline
              :canonical-link :attribution :max-excerpt-chars}.
              kawaraban itself already bounds the fair-use excerpt; a
              practice registers each article it intends to derive a
              product from, together with the outlet `:attribution`
              and `:canonical-link` kawaraban published it under and
              the `:max-excerpt-chars` bound kawaraban's own excerpt
              carries — this practice's derivative products may never
              exceed that bound or diverge from that attribution/link.
    record  — a committed operating record (a produced/published
              derivative broadcast product) — written ONLY via
              commit-record!.
    ledger  — append-only audit trail, commit or hold."
  )

(defprotocol Store
  (client [s client-id])
  (article [s article-id])
  (records-of [s client-id])
  (ledger [s])
  (register-client! [s client])
  (register-article! [s a])
  (commit-record! [s record])
  (append-ledger! [s fact]))

(defrecord MemStore [a]
  Store
  (client [_ client-id] (get-in @a [:clients client-id]))
  (article [_ article-id] (get-in @a [:articles article-id]))
  (records-of [_ client-id] (filter #(= client-id (:client-id %)) (:records @a)))
  (ledger [_] (:ledger @a))
  (register-client! [s client]
    (swap! a assoc-in [:clients (:client-id client)] client) s)
  (register-article! [s art]
    (swap! a assoc-in [:articles (:article-id art)] art) s)
  (commit-record! [s record]
    (swap! a update :records (fnil conj []) record) s)
  (append-ledger! [s fact]
    (swap! a update :ledger (fnil conj []) fact) s))

(defn mem-store
  ([] (mem-store {}))
  ([seed] (->MemStore (atom (merge {:clients {} :articles {} :records [] :ledger []}
                                   seed)))))
