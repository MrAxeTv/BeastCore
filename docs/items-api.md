# Shared custom item and equipment APIs

BeastLib 1.6.32 supplies the Java 8 interfaces in `me.mraxetv.beastlib.api.items`.
Consumer plugins use `me.mraxetv.beastlib:Core:1.6.32:shaded` with `provided`
scope and depend on the BeastLib plugin. Do not shade BeastLib into a consumer.
The API uses Bukkit and Java types only, without consumer core classes or relocated libraries.

Providers register their services with Bukkit `ServicesManager` using their own plugin as
owner and unregister them on disable. `EquipmentService` is the real equipment authority;
`EquipmentItemProvider` validates another plugin's genuine items. Equipment authorities
query all enabled provider registrations, passing detached item copies and isolating provider
failures. Validation never calls back into the equipment authority's `accepts` method.
The optional `EquipmentItemProvider.describe` method resolves genuine typed identity without
player permission, equipment lookup, item creation or charge consumption. Its default is empty;
consumers isolate failures and revalidate the enabled registration after reading a detached item.

`CustomItemService` publishes immutable `ItemDefinition` descriptions, resolves typed item
identity and creates fresh owned stacks. `ItemEnchantmentService` publishes immutable
`EnchantmentDefinition` descriptions, inspects stored levels and returns cloned results for
apply/remove operations. Its `lore` method returns only provider-managed rendered lines.
Providers preserve unrelated metadata and user lore when updating their own contribution.
`baseLore` removes only a verified provider-owned span from a detached description;
`withBaseLore` replaces the caller's base lines on a clone and rebuilds the managed contribution
and receipt. Item owners use these when charge counters change. Implementations preserve levels,
foreign identity and hide-flag ownership, and reject a modified managed span safely. The default
methods support providers without managed lore and reject managed lore until overridden.
IDs are stable namespace-qualified strings; descriptive names, lore and models are not proof
of item ownership. Removed or disabled definitions must not make stored equipment irretrievable.

`EquipmentAppearanceService` is an optional factory owned by the equipment plugin. It lists
immutable descriptions and creates fresh real visual templates. BeastRPGInventory exposes its
built-in and deployed Studio accessories through this service. Consumers retain the owner's
material, model and identity data while adding their own stats or enchants. Container templates
receive a new unique identity per creation. Template creation neither equips the item nor grants
another provider's protection, and it does not assemble or send a pack.
Template IDs retain their owner's documented local IDs (for example `aether_ring`); custom-item
and enchantment IDs use the provider namespace.

`ItemAttributeService` returns immutable finite numeric custom-stat maps for genuine provider
items and genuine equipped totals. Keys are namespace-qualified. Bukkit attribute modifiers
remain separate actual gameplay modifiers; a custom statistic only affects a mechanic when
its consuming plugin implements that mechanic. Equipped totals never adopt carried items,
cursor items, decorative previews or a failed equipment authority.

All service calls involving Bukkit state or item stacks run on the server thread. Immutable
description objects may be retained or read asynchronously. Stack reads and writes use detached
snapshots, never GUI icons. Equipment transfers and charge updates retain exact metadata,
capacity, curses and rollback. An installed but unavailable authority fails closed; its absence
is separate from an empty slot. No provider automatically adopts carried items or mirrors them.

Build the developer API with `mvn -pl Spigot/Core -am install`. The full
`Spigot/Core/target/Core-1.6.32-shaded.jar` is installed as classifier `shaded` for compilation.
The thin `Core-1.6.32-export.jar` is the server plugin and retains BeastLib's dynamic library
bootstrap. Normal package/install exports that thin artifact to seven configured destinations.
Use `-Dbeastlib.export.skip=true` or `-Dbeastlib.export.root=<isolated path>` for isolation;
Windows PowerShell requires quoting these dotted property arguments. Builds never start or
reload servers. This interface addition does not establish new server/Folia compatibility claims.
