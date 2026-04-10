# HC_CommandItem

Adds items that execute chat commands when used. Players are automatically given a locked menu item in hotbar slot 9 on connect. The item cannot be moved or dropped and triggers a configurable command interaction when activated. This is used to provide quick-access menu shortcuts to players.

## Features

- Custom `ExecuteCommand` interaction type registered via the Hytale Interaction CODEC system
- Automatic menu item placement in hotbar slot 9 on player connect
- Slot protection filters that prevent removal or dropping of the command menu item
- Existing items in slot 9 are safely moved to storage or dropped on the ground
- Item data defined via standard Hytale item JSON with the `ExecuteCommand` interaction type

## Dependencies

- **EntityModule** (required) -- Hytale entity system

## Building

```bash
./gradlew build
```
