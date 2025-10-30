# Product Requirements Document: M-Hike Application

**Version**: 2.0.0
**Last Updated**: 2025-10-30
**Architecture**: Hybrid Guest/Authenticated Mode

---

## 1. Introduction & Vision

M-Hike is a simple, reliable Android app to plan hikes, record on-trail observations, and optionally share read-only views with others. The vision is to make hike planning and note-taking fast and trustworthy, with **flexible user onboarding** (guest or authenticated), clear validation, easy media attachments, and seamless cloud features for authenticated users.

### Key Innovation

**Hybrid Architecture**: Users can start immediately as guests (no signup required) with full offline functionality, then seamlessly upgrade to authenticated mode to unlock cloud backup and social features—without losing any data.

---

## 2. Goals & Objectives

### Product Goals
- **Flexibility**: Support both guest mode (offline-first) and authenticated mode (cloud-enabled)
- **Seamless Transition**: Enable frictionless guest-to-authenticated upgrades with automatic data migration
- **Feature Completeness**: Provide full hike/observation CRUD regardless of mode
- **Social Features**: Enable sharing and collaboration for authenticated users

### User Goals
- **Immediate Access**: Start using the app without signup friction
- **Data Safety**: Choose between local-only or cloud backup
- **Easy Upgrade**: Transition to authenticated mode when ready, keeping all data
- **Social Sharing**: Share hikes with friends (authenticated users only)

### Business Goals
- **User Acquisition**: Lower entry barrier with guest mode
- **Conversion Optimization**: Convert guests to authenticated users through value demonstration
- **Technical Excellence**: Showcase clean architecture, data migration, and hybrid storage strategies

---

## 3. User Personas

### Persona 1: Sarah (Guest User → Authenticated)
**Demographics**: 28, casual hiker, privacy-conscious
**Behavior**: Wants to try the app before committing, values local data control
**Journey**:
1. Downloads app, chooses "Continue as Guest"
2. Creates 3 hikes with photos over 2 weeks
3. Wants to share hike with friend
4. Signs up (data automatically migrates to cloud)
5. Now enjoys cloud backup + sharing features

### Persona 2: Mike (Immediate Authenticated User)
**Demographics**: 35, avid hiker, tech-savvy
**Behavior**: Comfortable with accounts, wants cloud features immediately
**Journey**:
1. Downloads app, chooses "Sign Up"
2. Creates account with email/password
3. Immediately uses sharing and cloud backup
4. Invites friends to view hikes
5. Accesses hikes from multiple devices

### Persona 3: Emma (Permanent Guest User)
**Demographics**: 45, privacy advocate, occasional hiker
**Behavior**: Prefers local-only data, no cloud dependency
**Journey**:
1. Downloads app, chooses "Continue as Guest"
2. Uses app for personal hike tracking
3. Values that data stays on her device
4. Never signs up (by choice)
5. Continues using full offline functionality

---

## 4. User Stories

### Authentication & Onboarding
* **As a new user**, I want to start using the app immediately as a guest **so that** I don't have to create an account upfront
* **As a guest user**, I want to see what features require authentication **so that** I can make an informed decision about signing up
* **As a guest user**, I want to sign up at any time **so that** I can unlock cloud features when I'm ready
* **As a guest who signs up**, I want my existing data to migrate automatically **so that** I don't lose my hikes and observations

### Core Features (All Modes)
* **As a user**, I want to create hikes with required details **so that** they're stored consistently
* **As a user**, I want to add observations during or after hike creation **so that** I can capture field notes over time
* **As a user**, I want to attach images to hikes and observations **so that** I can keep visual records
* **As a user**, I want automatic location capture with manual override **so that** entries are geotagged without extra effort
* **As a user**, I want to search and filter my hikes **so that** I can find specific trips easily

### Cloud Features (Authenticated Only)
* **As an authenticated user**, I want to share hikes with other users **so that** they can view my adventures
* **As an authenticated user**, I want to search for other users **so that** I can find friends to share with
* **As an authenticated user**, I want my data backed up to the cloud **so that** I don't lose it if I switch devices
* **As an authenticated user**, I want invited users to see updates automatically **so that** they always have the latest information

### Guest Mode Experience
* **As a guest user**, I want clear indicators of my storage mode **so that** I understand my data is local-only
* **As a guest user**, I want to see promotional messages about authentication benefits **so that** I understand what I'm missing
* **As a guest user**, I want full functionality for personal use **so that** I can use the app effectively without signing up

---

## 5. Features & Requirements

### 5.1. Functional Requirements

| ID | Requirement | Guest Mode | Authenticated Mode | Details |
|:---|:---|:---:|:---:|:---|
| **FR-01** | **Onboarding & Mode Selection** | ✅ | ✅ | First launch presents choice: "Continue as Guest", "Sign Up", "Sign In". Selection persists. Guest users can upgrade anytime. |
| **FR-02** | **Hike Entry & Validation** | ✅ | ✅ | Required: Name, Location, Date, Length, Difficulty, Parking. Optional: Description. Inline validation blocks save until valid. |
| **FR-03** | **Hike Persistence & Management** | ✅ Local (SQLite) | ✅ Cloud (Firestore) | Create, list, view, edit, delete hikes. Timestamps (created/updated). Changes persist across restarts. |
| **FR-04** | **Observations (During/After Creation)** | ✅ | ✅ | CRUD operations. Fields: Text (required), Timestamp (auto), Comments. Multiple per hike. |
| **FR-05** | **Image Attachments** | ✅ Device storage | ✅ Cloud storage | Attach/remove 0..N images. Guest: local files. Authenticated: Firebase Storage. Progress indicators. |
| **FR-06** | **Auto Location with Override** | ✅ | ✅ | Auto-fill GPS coords with permission. Manual edit allowed. |
| **FR-07** | **Accounts: Signup, Login** | ❌ | ✅ | Email/password auth. Session persistence. |
| **FR-08** | **Guest-to-Auth Migration** | N/A | ✅ | Automatic on signup. Uploads local images to Firebase, creates Firestore docs, preserves all data. Progress dialog with retry. |
| **FR-09** | **User Discovery** | ❌ | ✅ | Search users by handle/email/name. Show profiles for selection. |
| **FR-10** | **Hike Sharing** | ❌ Banner shown | ✅ | Share with users for read-only access. Owner can revoke. Real-time updates. |
| **FR-11** | **Multi-Device Sync** | ❌ | ✅ | Hikes sync across devices automatically for authenticated users. |
| **FR-12** | **Search & Filters** | ✅ | ✅ | Name search. Filters: location, length, date, difficulty. |
| **FR-13** | **Guest Mode Indicators** | ✅ | N/A | Banners promoting authentication. Storage indicators (📱 vs ☁️). Clear messaging about limitations. |
| **FR-14** | **Error & Permission Handling** | ✅ | ✅ | Graceful messages for invalid forms, denied permissions, upload failures, missing network. Never crash. |

### 5.2. Non-Functional Requirements

| ID | Requirement | Details |
|:---|:---|:---|
| **NFR-01** | **Hybrid Architecture** | Strategy pattern for repository selection. Dynamic switching based on auth state. Clean separation of local vs remote implementations. |
| **NFR-02** | **Data Migration Reliability** | Automatic, transparent, with progress tracking. Retry on failures. Validates success before cleanup. |
| **NFR-03** | **Offline Functionality** | Guest mode fully functional offline. Authenticated mode uses Firebase offline persistence. |
| **NFR-04** | **Performance** | Lists load <300ms. Image uploads non-blocking. UI interactions <100ms response. |
| **NFR-05** | **State Preservation** | Form inputs survive rotations. Navigation state preserved. No data loss on app switch. |
| **NFR-06** | **Security & Privacy** | Guest data in app private storage. Authenticated data protected by Firebase rules. Owner-only edit. |
| **NFR-07** | **Styling & Theming** | Material Design 3. Consistent tokens. Outdoor theme (forest green primary, trail orange accent). |
| **NFR-08** | **Accessibility** | Contrast ≥4.5:1. Touch targets ≥48px. Icon + text (not color-only). Screen reader support. |
| **NFR-09** | **Scalability** | Pagination for large lists. Lazy loading. Handles thousands of hikes per user. |

---

## 6. Architecture & Technical Design

### 6.1. Hybrid Storage Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                  REPOSITORY PROVIDER                         │
│              (Strategy Pattern Selector)                     │
│                                                              │
│        getCurrentAuthState() → Dynamic Selection             │
│                                                              │
│  ┌──────────────────────┬──────────────────────────┐       │
│  │   Guest/Unauth       │    Authenticated         │       │
│  │   ↓                  │    ↓                     │       │
│  │ Local Repository     │  Remote Repository       │       │
│  │ (Room + Files)       │  (Firestore + Storage)   │       │
│  └──────────────────────┴──────────────────────────┘       │
└─────────────────────────────────────────────────────────────┘
```

### 6.2. Authentication States

```kotlin
sealed class AuthenticationState {
    object Unauthenticated    // First launch, not chosen
    data class Guest(val guestId: String)  // Offline mode
    data class Authenticated(val user: User)  // Cloud mode
}
```

### 6.3. Data Migration Flow

```
1. Guest user signs up
   ↓
2. Check local database for unsynced data
   ↓
3. Show migration progress dialog
   ↓
4. Upload local images to Firebase Storage
   ↓
5. Create Firestore documents with cloud URLs
   ↓
6. Mark local data as synced
   ↓
7. Clean up local data after success
   ↓
8. Switch to authenticated repository mode
```

### 6.4. Technology Stack

**Local Storage (Guest Mode)**:
- Room (SQLite) for structured data
- Internal file storage for images
- SharedPreferences for session

**Remote Storage (Authenticated Mode)**:
- Cloud Firestore for structured data
- Firebase Storage for images
- Firebase Auth for user management

**Architecture**:
- Clean Architecture (3 layers)
- MVVM pattern
- Hilt for dependency injection
- Kotlin Coroutines + Flow

---

## 7. UI/UX Design Guidelines

### 7.1. Design Principles

* **Frictionless Onboarding**: Guest option removes signup barrier
* **Value Demonstration**: Show benefits before requiring commitment
* **Clarity over Clutter**: One primary action per screen
* **Honest Indicators**: Clear about storage location and mode
* **Smooth Transitions**: Seamless guest-to-auth upgrades

### 7.2. Visual Identity

**Palette**:
* Primary (Forest): #256B4A
* Accent (Trail Orange): #E77D43
* Info (Sky): #4F9DD9
* Success (Moss): #2F8F46
* Danger (Berry): #C23C4B
* Background: #0F1210 (Dark) / #F7F8F7 (Light)

**Typography**:
* Display: 28/32px
* Title: 20/28px
* Body: 14/20px
* Caption: 12/16px

**Icons**: Rounded stroke icons with consistent weight

### 7.3. Key Screens

#### 1. Onboarding Screen (New)
- **Hero Section**: App branding + tagline
- **Feature Comparison Card**:
  - Guest: ✅ Create hikes, ✅ Observations, ✅ Local images, ❌ Share
  - Authenticated: ✅ All guest features, ✅ Cloud backup, ✅ Share, ✅ Multi-device
- **Three CTAs**: "Continue as Guest", "Sign Up", "Sign In"

#### 2. Home Screen
- **Dynamic Greeting**:
  - Guest: "Hello, Guest!" + "Using offline mode"
  - Authenticated: "Hello, [Name]!" + "@[handle]"
- **Guest Mode Banner** (conditional):
  - Info icon + "Guest Mode" title
  - "Sign up to unlock cloud backup and sharing"
  - "Sign Up" button
- **Primary Actions**: "My Hikes", "Log Out"

#### 3. Hike Detail Screen
- **Conditional Share Button**: Only visible to authenticated users
- **Guest Mode Banner** (conditional):
  - "Sign up to share this hike with friends and back up to cloud"
- **Storage Indicators**: 📱 Device only / ☁️ Cloud backup
- **View-Only Banner**: For guests viewing shared hikes (future)

#### 4. Migration Progress Dialog (New)
- **Initializing**: Shows stats (X hikes, Y observations, Z images, N MB)
- **Progress**: Real-time updates per stage
  - "Migrating Hikes (3/5)"
  - "Uploading Images (45%)"
  - "Migrating Observations (12/25)"
- **Complete**: Success summary with counts
- **Error**: Error message + "Retry" button

#### 5. Create/Edit Hike (Existing)
- **Storage Indicator**: Shows where images will be saved
- **Guest Note**: "Images stored on device only" (for guests)
- **Auth Note**: "Images backed up to cloud" (for authenticated)

### 7.4. Guest Mode UI Patterns

**Promotional Banners**:
- Informational (not annoying)
- Strategic placement (Home, Hike Detail)
- Clear value proposition
- One-tap signup navigation

**Feature Locked States**:
- Share button hidden for guests
- User search disabled for guests
- Helpful messages: "Sign up to [feature]" (not "You can't...")

**Storage Indicators**:
- Badge: 📱 Device / ☁️ Cloud
- Text: "Images stored on device only"
- Card: Full-width with icon and count

### 7.5. Copy & Messaging

**Positive Framing**:
- ✅ "Sign up to unlock cloud backup"
- ❌ "You can't back up to cloud"

**Clear Benefits**:
- "Never lose your hikes"
- "Share with friends"
- "Access from any device"

**Honest Communication**:
- "Guest mode - data stays on your device"
- "Upgrade anytime without losing data"

---

## 8. User Flows

### 8.1. Guest User Flow

```
1. App Launch
   ↓
2. Onboarding Screen
   - Tap "Continue as Guest"
   ↓
3. Home Screen (Guest Mode)
   - See guest banner
   - Tap "My Hikes"
   ↓
4. Create Hike
   - Fill form
   - Add images (stored locally)
   - Save
   ↓
5. Hike Detail
   - No share button
   - Guest banner visible
   - 📱 Device storage indicator
   ↓
6. Tap "Sign Up" on Banner
   ↓
7. Sign Up Screen
   - Enter credentials
   - Tap "Sign Up"
   ↓
8. Migration Dialog Appears
   - Shows progress
   - Uploads complete
   - "Migration Complete!"
   ↓
9. Now Authenticated
   - Share button visible
   - No guest banners
   - ☁️ Cloud storage indicators
```

### 8.2. Authenticated User Flow

```
1. App Launch
   ↓
2. Onboarding Screen
   - Tap "Sign Up"
   ↓
3. Sign Up Screen
   - Enter credentials
   - Account created
   ↓
4. Home Screen (Authenticated)
   - No guest banners
   - Full features
   ↓
5. Create Hike
   - Fill form
   - Add images (uploaded to cloud)
   - Search and invite users
   - Save
   ↓
6. Hike Detail
   - Share button visible
   - ☁️ Cloud storage indicator
   - "Shared with" list
   ↓
7. Share Hike
   - Search users
   - Add to share list
   - Invited users get read-only access
```

---

## 9. Success Metrics

### Conversion Metrics
* **Guest Adoption Rate**: % of users choosing guest mode
* **Guest-to-Auth Conversion**: % of guests who eventually sign up
* **Time to Conversion**: Average days before guests sign up
* **Banner Click Rate**: % of banner views that lead to signup

### Functional Metrics
* **Migration Success Rate**: >95% successful data migrations
* **Migration Time**: <2 minutes for average dataset (10 hikes, 50 images)
* **Data Loss**: 0% data loss during migration
* **Repository Switching**: Seamless with <100ms latency

### Performance Metrics
* **App Launch**: <2 seconds to onboarding/home
* **Hike List Load**: <300ms for 100 hikes
* **Image Upload**: Progress visible, <5s for 5MB image
* **Migration Progress**: Updates every 1 second

### User Experience Metrics
* **Feature Discoverability**: Users understand guest vs auth benefits
* **Upgrade Friction**: Low perceived effort to sign up
* **Data Trust**: Users confident their data is safe during migration

---

## 10. Edge Cases & Error Handling

### Migration Scenarios

| Scenario | Handling |
|----------|----------|
| Network loss during migration | Pause, show message, auto-resume when connected |
| Partial migration failure | Complete what's possible, report errors, allow retry |
| Large dataset migration | Show progress, don't block UI, allow backgrounding |
| User closes app during migration | Resume on next launch |
| Storage quota exceeded | Show error, offer to delete local images |

### Guest Mode Edge Cases

| Scenario | Handling |
|----------|----------|
| Guest tries to share | Show banner: "Sign up to share hikes" |
| Guest tries to search users | Show banner: "Sign up to find users" |
| Guest device storage full | Show error, suggest deleting old hikes |
| Guest uninstalls app | Data lost (expected behavior for local storage) |

### Authentication Edge Cases

| Scenario | Handling |
|----------|----------|
| Authenticated user goes offline | Use Firebase offline persistence |
| Session expires | Auto re-authenticate or show login |
| Account deleted | Remove local cache, show reactivation option |
| Multiple devices | Sync via Firestore real-time listeners |

---

## 11. Privacy & Security

### Guest Mode Privacy
* **Data Location**: App private directory, not accessible to other apps
* **No Cloud Upload**: Data never leaves device unless user signs up
* **No Tracking**: Guest ID is local UUID, not sent to servers
* **Uninstall Behavior**: All data removed (expected)

### Authenticated Mode Security
* **Firebase Auth**: Industry-standard authentication
* **Firestore Rules**: Owner-only edit, invited-user read
* **Storage Rules**: Authenticated uploads only, read requires auth
* **Data Encryption**: Firebase handles encryption at rest and in transit

### Migration Security
* **Ownership Preservation**: Guest data transferred to new user account only
* **Secure Upload**: Images uploaded via authenticated Firebase SDK
* **Data Validation**: Verify all data migrated before cleanup
* **Rollback Capability**: Keep local data until migration confirmed successful

---

## 12. Future Enhancements

### Phase 1 (Current)
- ✅ Guest mode with local storage
- ✅ Authenticated mode with cloud storage
- ✅ Automatic data migration
- ✅ Conditional UI for both modes

### Phase 2 (Next 3 Months)
- Background sync for authenticated users
- Incremental guest data sync (optional background upload)
- Export functionality (GPX, PDF)
- Advanced search (full-text)

### Phase 3 (Next 6 Months)
- Map integration (display routes)
- Offline maps for guest mode
- Collaborative editing (for authenticated users)
- Social feed (friends' recent hikes)

### Phase 4 (Next 12 Months)
- Web version (React/Vue + Firebase)
- iOS app (SwiftUI + Firebase)
- Apple Watch companion
- Hike recommendations

---

## 13. Open Questions

1. **Guest Data Retention**: How long should we keep local data after migration? (Current: immediate deletion)
2. **Migration Interruption**: Should we allow users to use the app during migration? (Current: blocking dialog)
3. **Partial Authentication**: Should we support social login (Google, Apple)? (Current: email/password only)
4. **Guest Feature Limits**: Should we limit guests (e.g., max 10 hikes)? (Current: no limits)
5. **Migration Retry**: How many automatic retries before giving up? (Current: manual retry only)

---

## 14. Appendix

### A. Glossary

* **Guest Mode**: Offline-first mode using local storage (Room + files)
* **Authenticated Mode**: Cloud-enabled mode using Firebase services
* **Migration**: Automated process of moving guest data to cloud
* **Repository Provider**: Strategy pattern implementation for dynamic storage selection
* **Storage Indicator**: UI element showing where data is stored (📱 local vs ☁️ cloud)
* **Guest Banner**: Promotional UI component encouraging authentication

### B. References

* [Firebase Documentation](https://firebase.google.com/docs)
* [Room Database Guide](https://developer.android.com/training/data-storage/room)
* [Material Design 3](https://m3.material.io/)
* [Android Architecture Guide](https://developer.android.com/topic/architecture)

### C. Related Documents

* `CODEBASE_ANALYSIS.md` - Technical architecture deep dive
* `HYBRID_WORKFLOW.md` - Implementation workflow and phases
* `README.md` - User-facing documentation

---

**Document Status**: Complete
**Approved By**: Product Team
**Last Review**: 2025-10-30
**Next Review**: 2026-01-30
