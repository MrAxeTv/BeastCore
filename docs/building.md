# Building BeastLib

Build behavior is intentionally explicit:

```text
mvn test                                  # tests only; no server copies
mvn package                               # builds and copies the thin runtime JAR
mvn package "-Dbeastlib.export.skip=true"  # builds without copying to servers
mvn install                               # package behavior plus local-Maven developer JAR installation
```

The Spigot Core artifacts have separate purposes:

| Artifact | Shade execution | Purpose | Destination |
| --- | --- | --- | --- |
| `Core-${version}.jar` | Maven's normal JAR goal | Unshaded intermediate module artifact | `Spigot/Core/target` only |
| `Core-${version}-shaded.jar` | `build-developer-shaded-jar` | Full relocated developer/compile artifact | `target`; local Maven as classifier `shaded` during `mvn install` |
| `Core-${version}-shaded2.jar` | `build-diagnostic-full-shaded-jar` | Second full output for shade comparison/debugging | `target` only |
| `Core-${version}-export.jar` | `build-server-runtime-jar` | Thin live plugin; runtime libraries are supplied dynamically by Libby | `target`, configured servers, and production archive |

Never place either full developer JAR in a server's `plugins` directory.

Packaging also copies the exported plugin jar to the configured development
servers and production archive, overwriting the previous BeastLib jar:

```text
mvn package
```

No Maven profile or activation property is required. The
`export-plugin-jar` execution always copies
`Core-${version}-export.jar`; it never copies a developer artifact.

## Shared runtime libraries

The `Core` shaded developer artifact and the Libby declarations in `BeastLib`
must use identical versions and namespaces. Consumer plugins compile against
the shaded classifier with `provided` scope, while the installed BeastLib
plugin loads the matching libraries at runtime.

The storage/Redis graph currently includes:

- MongoDB/BSON 3.12.14 under `me.mraxetv.beastlib.lib.mongodb` and `.bson`;
- SQLite JDBC 3.46.1.0 in its original `org.sqlite` namespace;
- MySQL Connector/J 9.7.0 under `.mysql`, with Protobuf 4.31.1 under `.protobuf`;
- MariaDB Connector/J 3.5.10 under `.mariadb`;
- H2 2.2.224 under `.h2`;
- PostgreSQL JDBC 42.7.4 under `.postgresql`, with Checker Qual 3.42.0 under `.checkerframework`;
- Jedis 5.2.0 under `.jedis`, with Commons Pool 2.12.0 under `.pool2`, JSON-java 20240303 under `.jsonjava`, and Gson 2.9.0 under `.gson`;
- SLF4J 2.0.17 in its original `org.slf4j` namespace.

Primary-library declarations repeat the transitive relocation rules so their
bytecode looks for the separately loaded relocated transitive library. H2
2.2.224 is retained because it emits Java 8-compatible classes; H2 2.4.240
requires Java 11. SQLite is the intentional exception to the relocation policy:
Xerial's native JNI binary requires its original `org.sqlite` class names.
BeastLib first uses an already visible original driver, such as one supplied by
a modern server, and dynamically loads 3.46.1.0 without relocation when it is
missing on older Spigot or CraftBukkit servers.

The `build-server-runtime-jar` execution removes these dynamically loaded libraries
from the live server artifact while retaining them in the shaded developer
artifact. Its filters cover every primary and transitive namespace listed
above. JSON-java is filtered as `org/json/*.class` so the separately used
json-simple classes under `org/json/simple` remain available after their
existing relocation.

## GUI compatibility with existing plugins

Triumph GUI 3.1.13 accepts Bukkit `HumanEntity` in methods that previously
accepted `Player`. Existing compiled plugins still need the original JVM
method signatures. BeastLib 1.7.4 restores both signatures: the `Player` overloads
forward to the corresponding `HumanEntity` method with normal virtual dispatch.
This preserves subclass behavior for paginated, scrolling, and storage menus.

During `process-classes`, a build tool generates compatibility versions of
`BaseGui`, `PaginatedGui`, `ScrollingGui`, and `StorageGui` from the pinned Triumph dependency.
It adds the legacy open/close overloads and applies BeastLib's Triumph/Adventure
relocations. The tool's ASM dependency is used only during the build.

Both the thin runtime JAR and full developer JAR contain these same four
classes. Their original dependency entries are excluded from shading. The
remaining GUI classes continue to load through Libby; the compatibility classes
in BeastLib's own JAR take precedence over the dynamically appended library.
Do not disable all Maven Antrun executions when building: use
`beastlib.export.skip` to skip server copies while retaining compatibility generation.

Deploy the completed thin BeastLib JAR and restart the server to load the fixed
classes. Existing compiled BeastTokens and other callers of the legacy methods
do not need to be rebuilt for this compatibility fix. Class forwarding and
packaged signatures can be checked automatically; a complete shop still needs
an in-game check on the target server.
