# Privacy Policy — Kron Drive: Retro Racer

**Effective date:** 14 August 2026
**Last updated:** 14 August 2026

**App:** Kron Drive: Retro Racer
**Package name:** `com.miniappfactory.krondrive`
**Developer:** MiniAppFactory
**Contact:** whatsthisapp@proton.me
**Source / project page:** https://github.com/MiniAppFactory/KronDrive

---

## 1. Short version

Kron Drive is an offline single-player racing game. It has **no user accounts, no login,
no servers of our own, no analytics, no crash-reporting service and no in-app purchases.**

We — the developer — do **not** collect, receive, store or sell any personal data about you.
Everything the game remembers about you (coins, level stars, upgrades, car choice, mission
progress, language and sound preference) is written to storage **on your own device** and
never sent anywhere.

The one exception is advertising. The game shows ads through **Google AdMob**. The Google
Mobile Ads SDK, running inside the app, collects and shares a limited set of data —
including your device's advertising identifier — for advertising, analytics and fraud
prevention. That data goes to Google, not to us. Section 4 describes it in detail.

---

## 2. Data the game itself collects

**None that leaves your device.**

The game stores the following on your device, in the app's private storage area
(Android `DataStore`, file name `kron_drive_progress`):

| Stored value | What it is |
|---|---|
| Coins, XP | In-game currency and experience |
| Highest unlocked level, per-level stars | Career progress |
| Upgrade levels (speed, acceleration, brake, boost) | Garage upgrades |
| Owned boosters | Consumable item counts |
| Selected car body and paint, owned bodies and paints | Vehicle customisation |
| Endless-mode best time and best score | Personal records |
| Daily challenge tier reached, day identifier | Daily mission state |
| Weekly mission progress and claimed rewards, week identifier | Weekly mission state |
| Rewarded-ad counter for the current day | Enforces the daily reward limit |
| Interstitial-ad counters | Controls how often a full-screen ad appears |
| Sound on/off, language (Turkish/English), onboarding-seen flag | Your preferences |

None of these values identify you. There is no name, e-mail address, phone number,
account, contact list, photo, microphone or camera access, and no precise location access.
The app declares only two Android permissions of its own — `INTERNET` and
`ACCESS_NETWORK_STATE` — and both exist solely so that the advertising SDK can reach the
network. **The game itself is fully playable offline.**

### Android backup

The app uses Android's standard Auto Backup feature. If you have device backup enabled in
your Android settings, your progress file may be copied to **your own Google Drive backup
account**, under your control and Google's terms — not to us. You can turn this off in your
device's system settings (Settings → Google → Backup).

---

## 3. What we do **not** do

- We do not create user accounts and we cannot identify individual players.
- We do not operate any server, database or backend for this game.
- We do not use analytics SDKs (no Firebase Analytics, no Google Analytics).
- We do not use a crash-reporting service.
- We do not offer in-app purchases and we never ask for payment details.
- There is no chat, no leaderboard, no friend list and no user-generated content.
- There is no cryptocurrency, token, wallet, real-money prize or "play-to-earn" mechanic.
- We do not knowingly sell or share personal information for cross-context behavioural
  advertising outside what is described in Section 4.

---

## 4. Advertising (Google AdMob)

The app displays banner ads on menu screens, full-screen interstitial ads between runs, and
optional rewarded video ads that you choose to watch in exchange for in-game rewards. These
are served by **Google AdMob** using the Google Mobile Ads SDK.

According to Google's own published disclosure for the Google Mobile Ads SDK, the SDK
collects and shares the following categories of data:

| Data | Purpose | Notes |
|---|---|---|
| Android advertising ID (AAID) | Advertising, analytics, fraud prevention | Optional — you can reset or delete it in your device settings (see below) |
| App set ID | Advertising, analytics, fraud prevention | Identifier scoped to apps from the same developer |
| Account identifiers | Advertising, analytics, fraud prevention | Collected by Google's SDK, not visible to us |
| Approximate location (derived from IP address) | Estimating the general location of the device for ad delivery | Not GPS. The app requests no location permission |
| App interactions (ad impressions, clicks) | Advertising, analytics, fraud prevention | Ad events only |
| Diagnostics | App and SDK performance monitoring | Technical data |

Google states that all user data collected by the Mobile Ads SDK is encrypted in transit
using TLS. This is Google's statement about Google's SDK; we have no server and therefore
no server-side copy of any of it.

**Google acts as an independent controller for the data it collects through AdMob.**
To understand how Google uses it, please read:

- Google Privacy Policy — https://policies.google.com/privacy
- How Google uses information from sites or apps that use our services —
  https://policies.google.com/technologies/partner-sites
- Google Advertising Policies — https://policies.google.com/technologies/ads

Sources for the table above:
https://developers.google.com/admob/android/privacy/play-data-disclosure

### Controlling ad personalisation

- **On your device:** Settings → Privacy → Ads → *Delete advertising ID* or
  *Reset advertising ID*. Deleting the advertising ID stops apps from receiving it; ads
  will still appear, but they will not be personalised.
- **In Google's account settings:** https://myadcenter.google.com

---

## 5. Consent (EEA, UK and Switzerland)

For users in the European Economic Area, the United Kingdom and Switzerland, the app uses
**Google's User Messaging Platform (UMP)** to present a consent form before personalised
advertising data is used. Your choice in that form is stored by the UMP SDK on your device
and is passed to the advertising SDK.

Depending on your choice, you will see either personalised or non-personalised ads.
Non-personalised ads still require basic data (such as an IP address and ad interaction
counts) for delivery, frequency capping and fraud prevention.

You may withdraw or change your consent at any time. You can do this by clearing the app's
storage (Settings → Apps → Kron Drive → Storage → *Clear data*), which resets the stored
consent choice and makes the form appear again on next launch, or by writing to
whatsthisapp@proton.me.

---

## 6. Children's privacy

Kron Drive is **not directed to children under 13**. In the Google Play store listing the
app's target audience is declared as **13 years and older**. We do not knowingly collect
personal data from children.

If you believe a child has used the app in a way that resulted in data being collected,
contact us at whatsthisapp@proton.me. Because the app holds no account and no server-side
record, the practical remedy is to delete the app and reset the device's advertising ID,
which we will explain on request.

---

## 7. Data retention and deletion

- **Game progress:** stored only on your device, for as long as the app is installed.
  Uninstalling the app deletes it. You can also delete it without uninstalling:
  Settings → Apps → Kron Drive → Storage → *Clear data*. There is no copy on our side to
  delete, because we never received one.
- **Advertising data:** held by Google under Google's retention policies. Use
  https://myadcenter.google.com and your device's advertising-ID controls, or contact
  Google directly, to exercise rights over that data.
- **E-mail you send us:** if you write to whatsthisapp@proton.me we will keep your message
  only as long as needed to answer it, then delete it.

Because the app does not support account creation, Google Play's in-app account-deletion
requirement does not apply.

---

## 8. Your rights (GDPR, UK GDPR, KVKK, CCPA/CPRA)

Since we hold no personal data about you, we have nothing to access, correct, export or
erase on your behalf. Where you wish to exercise rights over data collected by Google
through AdMob, those requests must be directed to Google as the controller of that data
(see the links in Section 4).

Subject to applicable law, you generally have the right to: request information about
processing; request access, rectification or erasure; object to or restrict processing;
withdraw consent; request data portability; and lodge a complaint with your supervisory
authority (in the EU, your national data protection authority; in Turkey, the Personal Data
Protection Authority, KVKK).

You may always contact us first at **whatsthisapp@proton.me** and we will respond within
30 days.

---

## 9. Security

We want to be precise rather than reassuring:

- We run no servers for this game, so there is **no central database of player data that
  could be breached**.
- Your progress file is kept in the app's private storage directory and is protected by
  Android's standard application sandbox, which prevents other apps from reading it. It is
  not additionally encrypted by us, and a device on which the user has granted themselves
  root access can read it. It contains no personal or sensitive information.
- Data transmitted by the Google Mobile Ads SDK is encrypted in transit using TLS,
  according to Google's published statement.

We make no other security claims.

---

## 10. Changes to this policy

If this policy changes, the new version will be published at the same URL and the
"Last updated" date at the top will change. Material changes affecting how data is
collected will also be noted in the app's store listing release notes.

---

## 11. Contact

**E-mail:** whatsthisapp@proton.me
**Project:** https://github.com/MiniAppFactory/KronDrive

---

<!--
========================================================================
NOT PART OF THE PUBLISHED POLICY — INTERNAL NOTES, DELETE BEFORE HOSTING
========================================================================

1. THIS FILE IS THE SOURCE TEXT, NOT THE PUBLISHED PAGE.
   The published page is docs/index.html →
   https://miniappfactory.github.io/KronDrive/
   If this text changes, update docs/index.html too. The Turkish pair is
   docs/PRIVACY_POLICY_TR.md → docs/tr/index.html.
   Hosting steps: docs/STORE_SUBMISSION_CHECKLIST.md section 2.

2. Section 5 describes consent withdrawal via "clear app data" because the app
   has no in-app privacy-options entry point yet (audit finding C-2a). When that
   button ships, update section 5 in BOTH the .md and the .html.

3. Section 6 is written for the RECOMMENDED "13+" target-audience decision.
   If the owner instead selects a target audience that includes children,
   replace section 6 with the following text AND set TFCD/TFUA in the app:

   ## 6. Children's privacy
   Kron Drive is available to children. In line with Google Play's Families
   Policy and COPPA, the app tags all ad requests as child-directed
   (tagForChildDirectedTreatment) and as under the age of consent
   (tagForUnderAgeOfConsent). As a result, only NON-PERSONALISED ads are
   shown, the advertising identifier is not used for interest-based
   advertising or remarketing, and ads are served exclusively through a
   Google Play Families Self-Certified Ads SDK. We do not knowingly collect
   personal information from children.

4. If a legal entity name / registered address is required for KVKK
   "veri sorumlusu" identification, add it under the Developer line.
   Whether VERBİS registration is required depends on the entity type and
   turnover thresholds — get legal confirmation; this document does not
   constitute legal advice.
-->
