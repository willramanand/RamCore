# RamCore 2.0 Module Boundaries

RamCore 2.0 keeps public APIs small, scheduler-aware, and explicit about platform risk. New features should land in the narrowest module that owns the behavior, and unstable platform details must stay behind RamCore interfaces.

## Feature Placement

| Area | Package Boundary | Release Role | Stability | Folia Rule |
| --- | --- | --- | --- | --- |
| Plugin lifecycle | `dev.willram.ramcore` | Core | Stable | Folia-safe when work is scheduled through RamCore contexts. |
| Terminables | `terminable` | Core | Stable | Folia-safe. |
| Services | `service` | Core | Stable | Folia-safe for lifecycle; service work must pick its own scheduler context. |
| Commands | `commands` | Core | Stable | Command registration is Paper lifecycle based; world/entity mutations must schedule explicitly. |
| Scheduling | `scheduler` | Core | Stable | Folia-safe by design. |
| Text and messages | `text`, `message` | Core | Stable | Folia-safe. |
| Data, config, JSON | `data`, `config`, `datatree`, `gson` | Core | Stable | Folia-safe for in-memory and file work; Bukkit object mutation still needs an owner context. |
| Cooldowns/selectors | `cooldown`, `selector` | Core | Stable | Pure filtering and state checks are Folia-safe; selected entities must be mutated on their context. |
| PDC helpers | `pdc` | Core | Stable | Holder mutation must run on the holder owner context. |
| Menus and item builders | `menu`, `item` | Core | Stable | Menu operations are viewer-scheduler anchored; direct inventory/entity access must stay anchored. |
| Item data components | `item.component` | Paper platform | Paper-experimental | Wraps Paper experimental APIs; use RamCore profiles/patches as the compatibility boundary. |
| Item NBT | `item.nbt` | Platform adapter | NMS-backed where raw SNBT is used | Binary/meta/PDC snapshots are safe; raw SNBT requires capability checks. |
| Resource packs | `resourcepack` | Core | Stable | Prompt tracking is event-driven and scheduler-safe. |
| Loot and trades | `loot`, `trade` | Core/gameplay | Stable | Pure generation is safe; world/entity application must schedule. |
| Diagnostics | `diagnostics` | Built-in optional surface | Stable command contract | Enabled by default, permission-gated, and removable with `-Dramcore.diagnostics=false`. |
| Integrations | `integration` | Optional integration boundary | Stable | Detection is safe; integration-specific calls follow that integration's threading rules. |
| Protocol and packets | `protocol`, `packet` | Optional integration | Experimental / adapter-backed | Logical state is safe; actual sends require a transport such as ProtocolLib. |
| NMS capability system | `nms.api`, `nms.reflect` | Internal platform boundary | NMS-backed | Always capability-gated; public APIs must expose Bukkit, Paper, or RamCore types. |
| AI, brain, path, entity, combat, world | `ai`, `brain`, `path`, `entity`, `combat`, `world` | Advanced platform utilities | Paper-experimental or NMS-backed where noted | Mutations require entity, region, chunk, or global scheduler ownership. |

## Core, Optional, Integrations, Examples

Core modules ship in the main artifact and must not require optional plugins at class-load time. Core includes lifecycle, commands, scheduling, terminables, services, text/messages, data/config, cooldowns, selectors, PDC, items, menus, resource packs, loot, trades, and the capability model.

Optional modules also ship in the main artifact but must degrade cleanly when their backend is absent. ProtocolLib packet transports, raw NBT/SNBT adapters, NMS reflection, and diagnostics integrations belong here.

Integration modules detect and wrap external plugins or platform features. They must expose explicit availability/capability results instead of throwing during normal startup when the integration is missing.

Examples are not a dependency boundary. Example modules document manual smoke-test surfaces and expected usage, but production behavior must live in core, optional, or integration packages.

## API Stability Levels

| Level | Contract |
| --- | --- |
| Stable | Source-compatible within a major release. Breaking changes require deprecation first unless a security or platform break forces removal. |
| Experimental | Public but still shaping. May change during minor releases; docs must call out the risk. |
| Paper-experimental | RamCore wraps Paper APIs that Paper marks experimental or version-sensitive. Prefer RamCore value objects over direct Paper experimental calls. |
| NMS-backed | Capability-gated and version-sensitive. Public APIs must not expose raw `net.minecraft` types; unsupported versions fail through diagnostics/self-tests. |

## Folia Safety

Folia-safe by design:

- Terminables, services lifecycle bookkeeping, text rendering, cooldown state, selectors, data repositories, config/data-tree reads, and pure loot generation.
- `Schedulers` and `TaskContext`, which are the required path for work that touches server state.
- Menu session updates when opened through RamCore menu APIs.

Requires explicit scheduler context:

- Entity, block, world, inventory, pathfinding, AI, brain, combat, presentation, trade application, and item/PDC mutations on live Bukkit objects.
- Command handlers that inspect or mutate player/world state.
- Diagnostics inspect commands that touch selected entities, held items, blocks, or scheduler ownership.

Not allowed in public APIs:

- Raw NMS handles as return types or required parameters.
- Direct Bukkit global scheduler use for live world/entity mutations.
- Optional plugin classes in core method signatures unless the package is explicitly an integration boundary.
