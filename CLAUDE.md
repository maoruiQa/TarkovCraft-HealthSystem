# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Development Commands

### Build & Test
- **Build the mod**: `./gradlew build`
- **Run client (development)**: `./gradlew runClient`
- **Run server (development)**: `./gradlew runServer`
- **Clean build**: `./gradlew clean build`

### Publishing
- **Publish to both platforms**: `./gradlew publishMod`
- **Publish to CurseForge only**: `./gradlew publishToCurseForge`
- **Publish to Modrinth only**: `./gradlew modrinth`

## Project Architecture

### Core Structure
This is a **NeoForge Minecraft mod** for Minecraft 1.21.8 that implements a comprehensive medical/health system. The mod is called "Medical System" (mod_id: `medsystem`) and provides detailed body part tracking, medical items, status effects, and an advanced **downed player system**.

### Key Dependencies
- **TarkovCraft Core** (`tarkovcraft_core`): Required core library dependency
- **Configuration Library** (`configuration`): Handles config management
- **NeoForge**: Minecraft modding framework (version 21.8.36)

### Main Entry Points
- **Server-side main class**: `MedicalSystem.java` - Main mod class, handles initialization and registry setup
- **Client-side main class**: `MedicalSystemClient.java` - Client-specific initialization and UI components

### Architecture Overview

#### Package Structure
```
tnt.tarkovcraft.medsystem/
├── api/                     # Public API interfaces
│   ├── event/              # Custom events
│   └── heal/               # Healing system APIs
├── client/                 # Client-side code
│   ├── config/             # Client configuration
│   ├── overlay/            # HUD overlays
│   ├── screen/             # GUI screens
│   └── GiveUpPromptLayer   # Downed player UI
├── common/                 # Shared server/client code
│   ├── config/             # Server configuration
│   ├── init/               # Registry initialization
│   ├── health/             # Core health system
│   ├── effect/             # Status effects (including DownedPlayerStatusEffect)
│   ├── rescue/             # Player rescue system
│   └── network/            # Network packet handling
└── network/                # Network communication
```

#### Key Systems
1. **Health System** (`HealthSystem.java`): Core body part tracking and health management
2. **Downed Player System**: Advanced player incapacitation and rescue mechanics
3. **Status Effects**: Custom effects like bleeding, fractures, downed state
4. **Medical Items**: Bandages, first aid kits, surgery kits, etc.
5. **Body Part Damage**: Separate hitboxes for head, legs, arms, torso with enhanced multipliers
6. **Rescue System**: Player-to-player rescue mechanics with progress tracking
7. **Healing API**: Extensible healing system for custom items

#### Downed Player System Details
The mod features a comprehensive downed player system with the following components:

**Core Mechanics:**
- Players enter downed state when head health < 15% or chest health < 10%
- Configurable health multipliers: Head +15%, Chest +10%
- Works in all game modes (single player, multiplayer, LAN)
- Death countdown timer (45 seconds default, configurable)

**Downed State Features:**
- **Visual**: Player forced into swimming/prone pose
- **UI**: Death countdown displayed above experience bar
- **Give Up**: Hold R key for 3 seconds to give up (with progress bar)
- **Rescue**: Other players can crouch nearby for 8 seconds to rescue
- **Monster Behavior**: Monsters ignore downed players completely
- **Restrictions**: No item usage, no attacking, no healing while downed
- **Skill System**: Resilience skill doesn't gain experience while downed

**Key Files:**
- `DownedPlayerStatusEffect.java` - Core downed state logic and death timer
- `RescueSystem.java` - Player rescue mechanics and progress tracking
- `GiveUpPromptLayer.java` - UI for death countdown and give up progress
- `HealthContainer.java` - Downed state detection and management
- `MedicalSystemEventHandler.java` - Event handling for restrictions and monster AI

#### Registry Pattern
The mod uses extensive registry pattern for all components:
- `MedSystemItems` - Medical items
- `MedSystemStatusEffects` - Status effects (including downed player effect)
- `MedSystemAttributes` - Entity attributes
- `MedSystemDataAttachments` - Data attachments
- Custom registries for transforms, reactions, and chance functions

#### Configuration System
- **Server config**: `MedSystemConfig` - Controls hit effects, chances, vanilla integration, and downed system settings
- **Client config**: `MedSystemClientConfig` - UI rendering preferences
- Both configs auto-sync between client and server

**Downed System Configuration Options:**
```java
// Health multipliers (server-side configurable)
public float headHealthMultiplier = 1.15F; // +15%
public float chestHealthMultiplier = 1.10F; // +10%

// Downed system settings
public boolean enableDownedSystem = true;
public float headDownedThreshold = 0.15F; // 15%
public float chestDownedThreshold = 0.10F; // 10%
public int downedDeathTimer = 45; // seconds
public int rescueTime = 8; // seconds
```

#### Datapack Support
The mod has extensive datapack support for:
- Custom entity hitboxes
- Medical item definitions
- Status effect configurations
- Health reaction responses
- Body part health multipliers

## Recent Development History

### Downed Player System Implementation
Recently implemented a comprehensive downed player system with the following features:

1. **Health System Enhancement**:
   - Added configurable body part health multipliers
   - Enhanced head and chest health by default
   - Integrated downed state detection into core health container

2. **Status Effect System**:
   - Created `DownedPlayerStatusEffect` with death countdown
   - Implemented give up mechanism with progress tracking
   - Added visual pose forcing (swimming/prone position)

3. **UI/UX Components**:
   - `GiveUpPromptLayer`: Death countdown and give up progress display
   - `DownedPlayerLayer`: Red overlay and status information
   - `RescuePromptLayer`: Rescue progress and instructions

4. **Rescue System**:
   - Player-to-player rescue mechanics
   - Network synchronization for rescue progress
   - Real-time UI updates for both rescuer and victim

5. **Monster AI Integration**:
   - Modified monster targeting to ignore downed players
   - Aggressive target clearing and navigation stopping
   - Comprehensive monster behavior modification

## Development Notes

### Mod Structure
This mod follows standard NeoForge conventions with clear separation between client and server code using the `@Mod(dist = Dist.CLIENT)` annotation pattern.

### Integration Points
- Integrates with TarkovCraft Core for navigation, stamina overlay, and weight system
- Uses Configuration library for auto-syncing configs
- Provides extensive API for other mods to integrate with the health system
- Advanced downed player system with rescue mechanics

### Key Features to Understand
- **Body Part System**: Entities can have separate health pools for different body parts
- **Downed Player System**: Advanced incapacitation mechanics with rescue options
- **Medical Items**: Various healing items with different effects and usage requirements
- **Status Effects**: Bleeding, fractures, downed state, and other medical conditions
- **Hitbox Customization**: Datapack-driven hitbox definitions per entity type
- **Vanilla Integration**: Optional enhancement of vanilla weapons with medical effects
- **Rescue Mechanics**: Player-to-player interaction system for emergencies

### API Compatibility Notes
- **Minecraft/NeoForge Version**: 1.21.8 / 21.8.36
- **MobEffects Constants**: Some effect constants may have different names (e.g., `DAMAGE_RESIST` vs `DAMAGE_RESISTANCE`)
- **Event API**: Some events like `LivingJumpEvent` may not be available in this version
- **Network Protocols**: Uses modern NeoForge networking with custom packet handling

### File Locations
- **Resources**: `src/main/resources/assets/medsystem/` - Textures, models, lang files
- **Data**: `src/main/resources/data/medsystem/` - Recipes, loot tables, tags
- **Templates**: `src/main/templates/` - Generated mod metadata files
- **Library**: `./library/` - Local JAR dependencies (TarkovCraft Core)

### Version Information
- **Minecraft Version**: 1.21.8
- **NeoForge Version**: 21.8.36
- **Java Version**: 21 (required)
- **Mod Version**: 1.4.2

### Testing
No explicit test framework detected. Testing should be done through:
1. `./gradlew runClient` - Test in development client
2. `./gradlew runServer` - Test in development server
3. Manual testing of medical system features
4. **Downed System Testing**: Test head/chest damage thresholds, rescue mechanics, give up functionality

### Known Issues & Workarounds
- **Status Effects**: Some MobEffects constants may need correct naming for this MC version
- **Jump Prevention**: `LivingJumpEvent` not available - alternative methods needed
- **Network Sync**: Give up mechanism uses simplified client-server communication
- **Pose System**: Uses `setForcedPose(SWIMMING)` for prone position - works reliably

## Best Practices for Development

1. **Always test downed system** after health-related changes
2. **Verify config synchronization** between client and server
3. **Test rescue mechanics** in multiplayer environments
4. **Check monster AI behavior** with downed players
5. **Validate UI rendering** at different screen resolutions
6. **Test give up mechanism** for proper network synchronization