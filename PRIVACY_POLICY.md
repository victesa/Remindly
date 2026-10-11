# Privacy Policy for Remindly

**Effective Date:** September 23, 2026  
**Last Updated:** September 23, 2026  

Welcome to **Remindly** ("we," "our," or "us"). We are committed to protecting your privacy and ensuring you have a positive experience when using our mobile application ("App"). 

This Privacy Policy explains how we collect, use, disclose, and safeguard your information when you use Remindly on Android devices. Please read this policy carefully. If you do not agree with the terms of this Privacy Policy, please do not access or use the application.

---

## 1. Information We Collect

We collect information to provide, improve, and personalize our services, process AI extractions, and synchronize your reminders across devices.

### A. Information You Provide to Us
* **Account Information:** When you create an account or sign in using Email/Password or Google Sign-In, we collect your name, email address, and unique user ID (Firebase UID).
* **Shared Content & Captures:** When you share text, URLs, screenshots, images, or PDF documents to Remindly via the Android Share menu or in-app capture tools, we process this content to extract metadata (titles, deadlines, summaries, organizations, and actionable checklists).
* **Support & Communications:** Information you provide when contacting us for support or feedback.

### B. Information Automatically Collected
* **Device & Technical Data:** We collect your device's IANA timezone, system timestamp, device model, operating system version, and device locale to accurately calculate and schedule time-sensitive reminders.
* **App Usage & Analytics:** We collect anonymous usage statistics and app performance metrics (such as feature usage and session duration) via Firebase Analytics.
* **Crash & Diagnostic Data:** Crash logs and performance diagnostics collected via Firebase Crashlytics to fix bugs and improve app stability.

### C. Purchase & Subscription Data
* **In-App Billing Information:** When you subscribe to **Remindly Pro**, payment processing is handled directly by Google Play Billing. We do not store or process full credit card numbers or financial account details on our servers. We only receive purchase tokens, product IDs, expiration timestamps, and subscription entitlement status.

---

## 2. How We Use Your Information

We use the collected information for the following purposes:

1. **AI Extraction & Metadata Generation:** To process unstructured text, images, and documents using artificial intelligence (Google Gemini API via secure Cloudflare Workers backend) to extract actionable reminders, deadlines, and task summaries.
2. **Scheduling Local Reminders & Daily Briefings:** To set exact alarms and send push notifications for approaching deadlines, task alerts, and daily morning briefings using Android's `AlarmManager` and `WorkManager`.
3. **Calendar Integration:** To automatically add extracted event dates and deadlines directly to your device's Google Calendar (when enabled by you).
4. **Cloud Backup & Cross-Device Syncing:** To securely back up and synchronize your captures and reminders across your logged-in devices via Firebase and Cloudflare.
5. **App Functions & Voice Assistant Integration:** To enable hands-free voice capture and reminder search via Android AppFunctions and Google Assistant / Gemini AI.
6. **Subscription & Feature Access:** To manage your Remindly Pro subscription status and enforce plan usage quotas.

---

## 3. Third-Party Services & Data Sharing

We **do not sell, rent, trade, or monetize** your personal information or shared content to third parties for advertising or marketing purposes. 

We share data only with trusted third-party service providers necessary to operate the App, as detailed below:

* **Google Firebase (Authentication, Firestore, Analytics & Crashlytics):** Used for secure account management, encrypted database storage, analytics, and crash reporting.  
  * [Google Privacy Policy](https://policies.google.com/privacy)
* **Google Gemini API & Cloudflare Workers:** Used as our cloud AI extraction pipeline to process shared text, images, and documents. Data submitted to the AI pipeline is processed securely and is used solely to generate task metadata for your account.  
  * [Cloudflare Privacy Policy](https://www.cloudflare.com/privacypolicy/)
* **Google Play Billing:** Processes all in-app purchases and Pro subscription transactions.

---

## 4. Android Device Permissions

To deliver core features, Remindly requests the following Android permissions:

* `INTERNET` & `ACCESS_NETWORK_STATE`: Required for cloud AI processing, authentication, and cross-device sync.
* `POST_NOTIFICATIONS`: Required on Android 13+ to send reminder alerts, deadline notifications, and daily morning briefings.
* `SCHEDULE_EXACT_ALARM`: Required to schedule exact, timely notifications for task deadlines using Android's `AlarmManager`.
* `RECEIVE_BOOT_COMPLETED`: Required to automatically restore and reschedule your active reminders after your device reboots.
* `READ_CALENDAR` & `WRITE_CALENDAR`: Required to sync extracted event dates and deadlines directly to your selected Google Calendar.
* `com.android.vending.BILLING`: Required to process Remindly Pro subscriptions via Google Play.

---

## 5. Data Retention & Account Deletion

* **Data Retention:** We retain your account data and captured items as long as your account is active or as needed to provide you services.
* **Item Deletion:** You can delete individual captures at any time directly within the App.
* **Complete Account & Data Deletion:** You have the right to request full deletion of your account and all associated data. You can delete your account data directly from the **Profile > Settings** section in the App, or by emailing us at `victorkirui.dev@gmail.com`. Upon account deletion, all your stored items, cloud backups, and user records are permanently purged from our servers.

---

## 6. Children's Privacy

Remindly is not intended for use by children under the age of 13 (or under 16 in the European Union). We do not knowingly collect personal information from children. If you believe we have inadvertently collected information from a child, please contact us immediately, and we will take steps to permanently delete such information.

---

## 7. Security

We implement industry-standard security measures, including HTTPS/TLS encryption in transit and Firebase ID-token authorization, to protect your data against unauthorized access, alteration, or destruction. However, please note that no method of transmission over the internet or electronic storage is 100% secure.

---

## 8. Changes to This Privacy Policy

We may update our Privacy Policy from time to time. We will notify you of any changes by posting the new Privacy Policy within the App or on our documentation pages and updating the "Effective Date" at the top of this policy. You are advised to review this Privacy Policy periodically for any changes.

---

## 9. Contact Us

If you have any questions, concerns, or requests regarding this Privacy Policy or our data practices, please contact us at:

**Developer Contact:** Victor Kirui  
**Email:** `victorkirui.dev@gmail.com`  
**Application:** Remindly
