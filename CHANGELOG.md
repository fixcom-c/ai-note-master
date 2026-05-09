# Changelog

All notable changes to this project will be documented in this file.

## [0.2.0] - 2026-05-09

### Added

- Added a personal profile system so the app can store long-term context such as identity, goals, work style, life areas, and AI preference.
- Added AI personal insight output based on notes, tasks, knowledge, and profile context.
- Added a dedicated `每日节奏` page with template-based `今日计划` and `晚间复盘`.
- Added support for syncing daily plan content into tasks.
- Added richer note fields including `title`, `category`, and `sourceType`.
- Added personal note import support for `txt`, `md`, and `markdown` files.
- Added backend tests for note create and update behavior.

### Changed

- Repositioned the product from a generic AI note tool toward a personal second-brain style workspace.
- Refreshed the dashboard to surface personal insight and faster entry points into daily use flows.
- Updated navigation to include `个人空间` and `每日节奏`.
- Improved note management so planning and review records can live alongside general inbox notes.
- Updated README to reflect the new product direction and daily usage rhythm.

### Verified

- Backend unit tests pass with `mvn test`.
- Frontend production build passes with `npm run build`.
- Real API flow verified for login, profile update, note create/update, and personal insight retrieval.

## [0.1.0] - 2026-05-09

### Added

- Initialized the full-stack AI note workspace with Spring Boot backend and Vue frontend.
- Added note capture, AI analysis, tasks, knowledge, reports, login, and Docker-based local development setup.

### Changed

- Fixed build issues, API mismatches, and core frontend workflow problems during initial stabilization.
