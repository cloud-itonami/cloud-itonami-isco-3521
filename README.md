# cloud-itonami-isco-3521

Open Occupation Blueprint for **ISCO-08 3521**: Broadcasting and Audiovisual Technicians.

This repository designs a forkable OSS business for an independent broadcast and media
syndication practice: a commercial media production actor that sits **downstream of
[kawaraban](https://github.com/etzhayyim/com-etzhayyim-kawaraban)** (瓦版), a separate,
non-commercial news-mirror actor. kawaraban mirrors the world's news outlets into an
append-only Datom log as `headline + canonical link + bounded fair-use excerpt` only — it
never stores full article bodies, never sells ads, never engagement-ranks and never
adjudicates truth. This practice takes that already charter-bounded, freely available
output and produces commercial derivative broadcast/media products from it: daily video
news-digest productions, podcast/audio briefings, a syndication API subscription for other
businesses that want a "world news ticker" widget, and white-label editorial-curation-as-a-
service. It never touches kawaraban's own charter: no full-text scraping, no impersonating
kawaraban or the mirrored outlets, and every derivative product always attributes back to
kawaraban and the original outlet link.

**Maturity: `:implemented`.** `src/media/` implements the
`MediaBroadcastActor` as a `langgraph.graph/state-graph` (`media.actor`)
wired to a `MediaAdvisor` (`media.advisor`) and an independent
`MediaBroadcastGovernor` (`media.governor`, matching this repo's
`blueprint.edn` `:itonami.blueprint/governor :media-broadcast-governor`),
following the itonami actor pattern (ADR-2607011000): `:intake -> :advise
-> :govern -> :decide -+-> :commit (:ok?) +-> :request-approval
(:escalate?, human-in-the-loop interrupt) +-> :hold (:hard?)`. 15 tests /
31 assertions green (`clojure -M:test`). HARD invariants (always hold,
never overridable): client/practice provenance, no-actuation (`:effect`
must be `:propose`), a registered kawaraban-article basis for any
derivative-product proposal, the proposed quoted excerpt not exceeding
the article's registered kawaraban fair-use bound (quoting beyond it is
full-text reproduction, not excerpting), and the proposed product's
attribution/link-out exactly matching what kawaraban published the
article under (anything else is misattribution or impersonation, not
syndication). Always-escalate ops (human sign-off regardless of
confidence, mapping this README's Trust Controls):
`:approve-publish-derivative-product` (publishing a derivative broadcast
product) and `:approve-live-on-air-switching` (any live on-air
switching).

## Robotics premise

All cloud-itonami verticals are designed on the premise that a **robot performs the
physical domain work**. Here a studio/production-booth robot handles camera framing,
audio-level riding, and asset ingest/transcode queue management during recording sessions —
narrow, procedural, physical-edge tasks only. The actual editorial and creative curation
work (selecting which kawaraban articles enter a digest, drafting scripts, composing an
episode) stays with a human-gated **Media Advisor -> Media Broadcast Governor** pair, not
the robot. The governor never dispatches hardware itself; `:high`/`:safety-critical` actions
(such as publishing a derivative broadcast product, or any live on-air switching) require
human sign-off.

A live sample of the operator console (robotics safety console, shared template) is rendered in [docs/samples/operator-console.html](docs/samples/operator-console.html) — pure-data HTML output of `kotoba.robotics.ui`.

## Core Contract

```text
kawaraban mirror feed (headline+link+excerpt)
        |
        v
Media Advisor -> Media Broadcast Governor -> produce/publish derivative broadcast product, or human sign-off
        |
        v
robot actions (gated) + operating records + audit ledger
```

No automated advice can dispatch a robot action the governor refuses, suppress an
operating record, or disclose sensitive data without governor approval and audit evidence.
Raw full-text scraping of mirrored articles, or misattribution of a derivative product's
source, is refused by construction — only kawaraban's own bounded excerpt and link-out may
ever enter the pipeline.

## Capability layer

Resolves via [`kotoba-lang/occupation`](https://github.com/kotoba-lang/occupation)
(ISCO-08 `3521`). Required capabilities:

- :robotics
- :identity
- :audit-ledger
- :media-pipeline

See [`docs/business-model.md`](docs/business-model.md) and
[`docs/operator-guide.md`](docs/operator-guide.md).

## License

AGPL-3.0-or-later.
