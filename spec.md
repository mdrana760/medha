# Specification

## Summary
**Goal:** Build a single-page, mobile-responsive romantic interactive gift-box experience with cute animations and in-browser voice playback.

**Planned changes:**
- Create a centered scene on a soft pink/purple gradient background showing only a cute animated gift box on first load.
- Add Web Speech API (SpeechSynthesis) voice playback to speak “Medha, open your gift” on initial load, with a tap/click prompt fallback when autoplay is blocked and an unsupported-message fallback when SpeechSynthesis is unavailable.
- Implement a one-time click/tap animation sequence: gift opens, boy and girl appear, boy walks forward and kneels, hearts float in the background, voice speaks “I love you”, and on-screen text shows “I love you ❤️”.
- Implement the scene in clean, well-commented React + TypeScript with CSS/Tailwind styling; keep visuals cute/romantic and UI text in English.
- Keep backend unchanged (no Python/Flask; no new backend functionality) and load any art as static frontend assets.

**User-visible outcome:** Users land on a romantic gift-box screen that speaks a prompt (or asks to enable sound), and on tapping the gift, see a cute romantic animated reveal with hearts and an “I love you ❤️” message plus voice.
