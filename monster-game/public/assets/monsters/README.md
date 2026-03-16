# Monster Sprite Assets

Place your monster sprite images here. Use **transparent PNG** or **WebP** format.

SVG placeholders are included for testing. Replace with PNG/WebP for production art.

## Folder structure

```
/public/assets/monsters/
  flare/
    stage0.png   # Baby (levels 1–5) — or stage0.svg as fallback
    stage1.png   # Stage 1 (levels 6–14)
    stage2.png   # Stage 2 (levels 15–24)
    stage3.png   # Final (levels 25+)
```

The renderer tries PNG first, then falls back to SVG if PNG is missing.

## Stage mapping

| Stage | Level range | File        |
|-------|-------------|-------------|
| 0     | 1–5         | stage0.png  |
| 1     | 6–14        | stage1.png  |
| 2     | 15–24       | stage2.png  |
| 3     | 25+         | stage3.png  |

## Adding your art

1. Replace the placeholder files in `flare/` with your actual sprite art.
2. Recommended: 256×256 or 512×512 px, transparent background.
3. For new monster types, create a folder (e.g. `water/`, `shadow/`) with stage0–stage3.png.
