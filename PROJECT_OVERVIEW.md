# Remindly - Project Overview & Technical Documentation

This document provides a comprehensive breakdown of the Remindly application for use in portfolios, privacy policies, and technical documentation.

---

## 1. App Concept & Purpose
Remindly is an AI-powered productivity assistant designed to eliminate manual data entry. It allows users to "capture" information from any source (text, links, images) via Android's share menu. The app then uses AI to intelligently extract key details like titles, deadlines, organizations, and categories to automatically schedule reminders.

---

## 2. Technical Stack
- **Language:** Kotlin 2.0 (High-performance, modern syntax)
- **UI Framework:** Jetpack Compose with Material 3 (Declarative, reactive UI)
- **Architecture:** Clean Architecture with Multi-module setup:
    - `:app`: Main entry point and navigation.
    - `:core`: Shared models, utilities, and common logic.
    - `:module-features`: Feature-specific UI and ViewModels (e.g., Capturing).
    - `:module-sources`: Data layer implementations (Room for local, Retrofit for remote).
- **Dependency Injection:** Koin (Lightweight and Kotlin-first)
- **Local Persistence:** Room Database with Flow and Coroutines.
- **Networking:** Retrofit & OkHttp with Bearer Token authentication.
- **Background Tasks:** WorkManager for offline synchroniza6tion.
- **Performance:** Native code integration with debug symbol mapping for advanced crash reporting.

---

## 3. Key Features
- **AI-Driven Intelligent Capture:** Automatic extraction of task metadata from unstructured shared content.
- **Seamless Share Integration:** Works as a system-wide share target for text, URLs, and files.
- **Offline-First Reliability:** A `PendingSync` system ensures that captures are saved locally and synced automatically when connectivity is restored.
- **Proactive Notifications:** Background notification helper that ensures reminders are delivered even when the app is inactive.
- **Cross-Platform Sync:** Firebase Authentication (Google/Email) to keep tasks synchronized across devices.

---

## 4. Privacy & Data Handling
- **Data Collection:** The app processes shared content (text, images, URLs), device locale, and timezone to accurately extract and set reminders.
- **Authentication:** Managed via Firebase Auth; user profiles are protected and private.
- **Cloud Processing:** Data is securely transmitted to a custom API endpoint (`v1/extract-data`) for AI analysis. Content is used solely for extraction and not for third-party marketing.
- **Analytics:** Firebase Analytics and Crashlytics are used to monitor app health. No personal task content is included in these logs.
- **Data Control:** Users can delete their captured items and account data at any time through the application settings.

---

## 5. Technical Specifications
- **Min SDK:** 24 (Android 7.0)
- **Target SDK:** 37 (Android 15)
- **Build System:** Gradle Kotlin DSL
- **Key Dependencies:** 
    - `androidx.compose`
    - `androidx.room`
    - `io.insert-koin`
    - `com.google.firebase`
    - `com.squareup.retrofit2`
