# BeastLib Agent Instructions

## User Preferences

- Auto-correct obvious spelling mistakes in class names, method names, variable names, and comments.
- When a misspelled API name may already be used, keep backward compatibility by adding aliases/wrappers where practical.
- Prefer corrected naming in new code.

## Scheduler

- Public scheduler API is under `me.mraxetv.beastlib.api.scheduler`.
- Keep Folia integration reflection-only so BeastLib continues to compile for
  Java 8 and against legacy Spigot APIs.
- Use global tasks only for global state, entity/player tasks for entity work,
  and region/location tasks for location-owned work.
- Keep `ScheduladHandler` binary compatible and deprecated; use
  `SchedulerService` or the correctly named `SchedulerHandler` in new code.
- Scheduler lifecycle documentation is in `docs/scheduler.md`.
- Focused verification command:
  `mvn -pl Spigot/Core -am test`.

## Build Safety

- A normal `package` or `install` build copies the exported BeastLib jar to
  the configured local development servers and production archive.
- Each configured destination is overwritten so it always receives the
  artifact produced by the current build; no activation property is required.
- Exports use `beastlib.export.root` / `beastlib.export.skip` and all seven
  parent-defined destinations. Export the thin `Core-*-export.jar`, never the
  full developer `Core-*-shaded.jar` installed for provided consumer dependencies.
- Shared item/equipment contracts are in `docs/items-api.md`; public interfaces
  belong to BLAPI and must expose only Java/Bukkit types.

## Client Services

- Paper event capabilities must be detected before native binding initialization.
  When chunk load/unload, tick-end and unknown-entity events are available,
  forward Paper events without constructing NativeClientBinding or installing
  its packet observer. Chunk tracking requires the load/unload pair. Do not
  read getSentChunkKeys during join attachment: Paper can still have a null
  chunk loader. Read that optional snapshot lazily, retain actual observed
  receipts on temporary API failure, retry later, and refresh receipts after
  successful snapshots. Do not replace them with loaded/desired chunks or
  permanently fail input on snapshot unavailability. Preserve explicit malformed
  data/budget failures and rethrow reflected fatal JVM errors.
- Version 1.7.3 extends the native client services to release protocols 773..777
  (Paper/Spigot 1.21.9 through 26.3). Core's Java8 `MinecraftReflection` helper
  resolves versioned CraftBukkit classes and older Spigot member aliases. Keep
  native initialization lazy; ordinary legacy BeastLib startup stays separate.
  The normal thin runtime JAR and the provided shaded developer artifact must
  contain matching owned API/binding classes. See `docs/client-services.md`.
- Version 1.7.2 adds Java/Bukkit-only client-event and JSON display APIs in BLAPI;
  see `docs/client-services.md`. Keep modern native bindings lazy so legacy
  BeastLib startup and existing scheduler/Folia consumers remain compatible.
- Client observations support Paper/Spigot 1.21.9 through 26.3; the observer itself does not
  support Folia. Preserve Paper's real event path, per-boundary native FIFO
  ordering, connection/world/watcher fences and bounded failure handling.
- Do not infer sent chunks from server loaded/desired/pending chunk sets. Fresh
  Spigot observation starts without historical receipts; healthy observations
  remain BeastLib-owned across consumer reloads until disconnect/shutdown.
- Shared unrelocated Adventure 5.2.0 loads only on modern Spigot protocols >=776.
  Paper keeps its own API; legacy consumers retain the relocated Java 8 libraries.
- Follow the parent build-export instructions: write completed JARs directly
  to the configured parent `plugins/` folders, including the latests server.
  Keep replacement backups in system Temp and verify hashes. Exporting never
  starts, reloads, or restarts a server.

## GUI Binary Compatibility

- Preserve legacy Triumph GUI `Player` method descriptors alongside the
  `HumanEntity` methods. Consumers already compiled against older BeastLib
  must keep linking without a rebuild.
- Keep generated compatibility classes identical in the thin runtime and full
  developer JARs. Libby must not replace those classes with unpatched originals.
- Build-generation details and skip controls are in `docs/building.md`.
