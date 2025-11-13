# Project Structure

## Root Level

```
DashCamApp/
├── app/                    # Main application module
├── gradle/                 # Gradle wrapper and version catalog
├── build.gradle.kts        # Root build configuration
├── settings.gradle.kts     # Project settings and module inclusion
└── gradle.properties       # Gradle properties
```

## Application Module (`app/`)

### Source Code (`app/src/main/java/com/kasahirotech/dashcamapp/`)

The codebase follows a feature-based organization pattern:

```
com.kasahirotech.dashcamapp/
├── MainActivity.kt                    # Main entry point activity
├── interfaces/                        # Interface definitions
│   ├── CameraService.kt              # Camera service contract
│   ├── SettingItem.kt                # Settings item interface
│   └── VideoItem.kt                  # Video item interface
├── models/                           # Data models
│   └── AppVideo.kt                   # Video data model
├── screens/                          # UI screens/activities
│   ├── Gallery.kt                    # Gallery activity
│   └── GalleryAdapter.kt             # RecyclerView adapter for gallery
├── service/                          # Business logic and services
│   ├── Camera.kt                     # Camera operations
│   ├── GalleryService.kt             # Gallery data management
│   ├── PermissionHandler.kt          # Runtime permission handling
│   └── Storage.kt                    # File storage operations
└── settings/                         # Settings feature
    ├── Settings.kt                   # Settings activity
    ├── SettingsAdapter.kt            # RecyclerView adapter for settings
    └── SettingItemClasses.kt         # Setting item implementations
```

### Resources (`app/src/main/res/`)

- `layout/` - XML layout files
- `drawable/` - Vector drawables and images
- `mipmap-*/` - App launcher icons (multiple densities)
- `values/` - Strings, colors, themes, styles
- `values-night/` - Dark theme resources
- `xml/` - XML configurations (backup rules, data extraction)

### Testing

- `app/src/test/` - Unit tests
- `app/src/androidTest/` - Instrumented tests

## Architecture Patterns

### Layer Separation
- **Screens**: UI layer (Activities)
- **Service**: Business logic and platform interactions
- **Models**: Data structures
- **Interfaces**: Contracts and abstractions

### Key Conventions
- Activities are organized in `screens/` package
- Adapters are co-located with their corresponding screens
- Service classes handle platform-specific operations (camera, storage, permissions)
- Interfaces define contracts for services and data items
- View Binding is used for view access (no findViewById)

## Package Naming
- Base package: `com.kasahirotech.dashcamapp`
- Feature-based sub-packages for logical grouping
