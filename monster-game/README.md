# Monster Soul - Your Digital Companion

A mobile-first game that blends **Digimon Tamagotchi** virtual pet care with **Pokémon-style** turn-based battling. Create your unique monster from your face and personality!

## Features

- **Face-Based Monster Creation**: Capture your face with the camera (or skip for random) — your skin tones and features influence your monster's colors and appearance
- **Personality Quiz**: Answer 4 questions to shape your monster's traits, element, and body type
- **Tamagotchi-Style Care**: Feed, play, train, clean, and rest with your monster on the home screen. Stats decay over time — keep your companion happy!
- **3D Turn-Based Battles**: Fight wild foes in a 3D arena. Use Strike, Blast, Defend, and Heal. Battle stats scale with your care level and training
- **Progressive Web App**: Install on your phone for a native app experience. Works offline after first load

## Tech Stack

- **React 19** + **Vite 7**
- **Three.js** (via React Three Fiber) for 3D battles
- **Zustand** for state management with persistence
- **PWA** support for mobile install

## Getting Started

```bash
npm install
npm run dev
```

Open [http://localhost:5173](http://localhost:5173) and allow camera access when prompted.

For mobile testing, use your local network IP or deploy to a hosting service.

## Build

```bash
npm run build
npm run preview
```

## How to Play

1. **Create Monster**: Capture your face (or skip) → Answer 4 personality questions → Confirm your monster
2. **Care**: Use Feed, Play, Train, Clean, and Rest to keep stats high
3. **Battle**: When energy and hunger are sufficient, tap Battle to fight. Win to gain EXP and level up
4. **New Monster**: Tap "New Monster" to start over with a fresh creation

## Project Structure

```
monster-game/
├── src/
│   ├── components/     # UI components
│   ├── store/          # Zustand game state
│   ├── utils/          # Monster generator
│   └── data/           # Questions, etc.
├── public/
└── ...
```

## License

MIT
