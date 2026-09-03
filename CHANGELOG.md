# Changelog

All notable changes to this project will be documented in this file.

## [2.1.0] - 2026-09-03

### Fixed
- **Network Deserialization & Response Unwrapping**: Resolved `com.google.gson.JsonIOException: JSON document was not fully consumed` by removing global lenient Gson mode and replacing it with safe, full-length response buffering via OkHttp `source.request(Long.MAX_VALUE)`.
- **Cloudflare Access HTML Interception Guard**: Added HTML detection in `RemoteModule` and `RemindlyBackendExtractor` to catch Cloudflare Access sign-in pages (`<!DOCTYPE html>`) and map them to clean `401 Unauthorized` HTTP responses, preventing Gson from attempting to parse HTML into JSON models.
- **Top-Level & Nested Stringified JSON Handling**: Updated `RemoteModule` and `StringifiedTypeAdapterFactory` to use lenient `JsonReader` string streams when unwrapping top-level and nested stringified JSON strings.
- **Morning Briefing Scheduling & Time Drift**: Unified daily morning briefing delivery around exact `AlarmManager` alarms (`ReminderScheduler`). Automatically cancels stale `MorningBriefingWork` jobs in `WorkManager` on startup to prevent unintended 10 PM background notifications caused by WorkManager drift.
- **Profile Screen Time Formatting**: Updated `ProfileScreen` to display preferred notification times dynamically formatted in 12-hour AM/PM format (e.g., `08:00 AM`) and made setting subtitles update dynamically when notification times are modified.

## [1.0.0] - 2026-03-01

### Added
- **AI-Powered Capture**: Automatic detail extraction and reminder creation from shared content.
- **Offline Mode**: Reliable local storage with `PendingSync` for background synchronization.
- **Background Notifications**: Full integration for timely alerts even when the app is closed.
- **Native Support**: Added **debug symbol logs** for optimized native code troubleshooting and crash reporting.
- **Secure Authentication**: Integrated Google and Email-based sign-in.
- **Modern UI**: First release of the Material 3 based Jetpack Compose interface.

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
