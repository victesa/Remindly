# Changelog

All notable changes to this project will be documented in this file.

## [2.0.0] - 2026-08-17

### Added
- **Onboarding Experience**: Implemented a 4-page modern onboarding flow using Jetpack Compose `HorizontalPager` and `DataStore` for persistence.
- **Strict Date Validation**: Added comprehensive checks to prevent captures or edits with past deadlines/event dates across the app.
- **Serverless AI Architecture**: Integrated **Vertex AI for Firebase** for direct cloud-to-device AI extraction.
- **On-Device AI Support**: Prepared groundwork for **Gemini Nano** to enable zero-cost, private extraction on supported hardware.
- **MIME Type Sniffing**: Implemented magic-byte file signature detection for accurate identification of PDF, JPEG, and PNG files.
- **App Check Support**: Added security layer placeholders for Firebase App Check to prevent API misuse.

### Changed
- **Architectural Shift**: Deprecated custom backend (Cloudflare Workers) in favor of a direct-to-cloud serverless model.
- **Robust Share Intent Handling**: Rewrote `ShareIntentHandler` and `MainActivity` transit copy logic to handle restricted URIs from specialized PDF viewers and browsers.
- **Enhanced Capture Feedback**: Updated `CapturingDialog` with distinct visual states for "Success", "Saved Offline", "Capture Rejected", and "Failed".

### Fixed
- **Google Calendar Permission Logic**: Resolved issue where the calendar integration would fail due to unrequested OAuth scopes.
- **Build Metadata Conflicts**: Fixed duplicate `META-INF/DEPENDENCIES` files in Gradle build.
- **Reliable Notification Scheduling**: Fixed alarm collision issues in `ReminderScheduler` by ensuring unique intent actions for each reminder type.
- **Background Sync Reliability**: Updated `SyncWorker` to correctly handle 400 and 402 terminal error codes.
- **Referrer NPE**: Fixed a crash caused by accessing the `referrer` property too early on some devices.

### Removed
- **Legacy Google Calendar Feature**: Completely removed the Google Calendar integration codebase as per user request to simplify the app focus.
- **Custom Backend Dependency**: Removed Retrofit calls to the old ingestion endpoint.
