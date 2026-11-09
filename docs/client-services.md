# Client events and display transport

BeastLib 1.7.3 exposes `getClientEvents()` and `getClientDisplay()` through
`BeastLibAPI`, with static access through `ClientEventServices` and
`ClientDisplayServices`. Public contracts contain Java/Bukkit types only and
compile to Java 8 bytecode.

Call `ClientEventServices.get().watch(plugin, failureCallback)` on the server
thread and retain its `ClientEventRegistration`. Register ordinary Bukkit
listeners for `PlayerChunkSentEvent`, `PlayerClientTickEndEvent` and
`PlayerUnknownEntityUseEvent`. Closing the registration releases the consumer;
plugin disable closes all of that consumer's registrations automatically.

Paper adapters forward the actual Paper events. When Paper provides all three
event capabilities, BeastLib does not initialize its native binding or install a
packet observer. Chunk tracking requires both Paper's load and unload events;
missing capabilities alone use the fallback. Spigot 1.21.9 through 26.3
(protocols 773 through 777) uses a reflection-only connection observer. Tick observations enter the native server
FIFO after downstream packet handling, one task per actual client boundary.
Connection identity, wire-world epochs and watcher epochs reject retired work.
Queue admission is bounded; overflow stops the stream and calls the owner's
failure callback instead of dropping tick boundaries silently. This event
observer does not claim Folia support.

`sentChunks(player)` returns an immutable world UUID and packed chunk keys (X
in the low 32 bits, Z in the high 32 bits). These represent sent/currently retained
chunks, not acknowledgements or proof of rendering. Login/Respawn wire ordering
determines chunk world identity; server-loaded chunks never establish a receipt.
Paper's authoritative `getSentChunkKeys()` snapshot is read lazily when requested,
never during join attachment. Some Paper builds have no chunk loader yet during
`PlayerJoinEvent`; an unavailable snapshot leaves real Paper event receipts and
input forwarding active and is retried on subsequent requests. Successful
snapshots refresh retained receipts. No server-loaded or desired chunks are
substituted. A fresh Spigot observer starts empty
because desired tracking/pending/ready sets cannot prove historical sends.
After first use, chunk observers remain owned by BeastLib until disconnect or
shutdown, preserving healthy history when consumers close and reopen watches.

The display service accepts native JSON component strings for messages, action
bars, titles and text displays. It also opens native JSON dialogs and publishes
`PlayerDialogResponseEvent` with an action ID and raw JSON payload. Consumers
must validate their own action IDs, connection ownership, expiry and one-use
callbacks. Invoke display methods on the thread owning the receiving player or
entity. Unsupported features fail when invoked; modern native classes are not
required merely to enable BeastLib on older supported servers.

Spigot 26.2/26.3 receives shared, unrelocated Adventure 5.2.0 libraries through
BeastLib's runtime loader when the server does not already supply the API. These
load only on modern native protocols (776 and later), whose JVM meets their
requirements. The older protocol adapters exchange native JSON without requiring
that shared Adventure API. Paper keeps its own
Adventure API. BeastLib's existing relocated Java 8 libraries and scheduler
contracts remain available to older consumers. Install the thin exported
`Core-1.7.4-export.jar` as BeastLib; the developer shaded JAR is for compilation.

`MinecraftReflection` resolves supported Mojang class/member names on both
Mojang-mapped runtimes and versioned Spigot packages. Construct it from a server
or its class loader, then use `craftClass`, `type`, `method` and `field` rather
than copying obfuscated member names into plugins. Member lookup follows actual
superclasses and interfaces. The bundled aliases cover BeastLib and MiniMap's
native operations on Spigot 1.21.10 (also the current 1.21.9 BuildTools result)
and 1.21.11; they are not a promise that every future NMS member is mapped.
Native binding remains lazy and unsupported versions fail when used.
