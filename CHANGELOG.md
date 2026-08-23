# Changelog

## 1.1.0

- Added Mod Menu configuration support.
- Added global cutoff distance configuration.
- Added per-entity enable/disable settings.
- Added per-entity custom cutoff distances.
- Added inheritance of the global value for entities without overrides.
- Added reset-to-global behavior for entity overrides.
- Added entity search.
- Reworked the entity list to use scrolling instead of page-by-page navigation.
- Added Japanese and English GUI translations.
- Improved labels and hints for distance input fields.
- Changed the new-install default cutoff distance from 32 to 24 blocks.
- Added ETF as a compile/runtime dependency required by EMF's entity interface.

## Development build fixes

- Updated entity registry lookups for the 1.21.11 mappings.
- Removed invalid casts to Minecraft `Entity` from EMF/ETF entity wrappers.
- Restored access to EMF entity position/type methods through the proper EMF/ETF API.
