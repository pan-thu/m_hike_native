### **Product Requirements Document: M-Hike Application**

**1. Introduction & Vision**

This document outlines the product requirements for “M-Hike,” a simple, reliable app to plan hikes, record on-trail observations, and share read-only views with others. The vision is to make hike planning and note-taking fast and trustworthy, with clear validation, easy media attachments, and seamless sharing.

**2. Goals & Objectives**

* **Product Goal:** Enable users to create and manage hikes and observations (with images and locations) and share them with read-only guests.
* **User Goal:** Provide a quick, error-resistant way to log hike details before/during/after a trip, attach photos, and keep invited or shared users up-to-date automatically.
* **Business Goal:** Demonstrate end-to-end app proficiency (data modeling, validation, media handling, permissions, and access control) in a polished, user-centered deliverable.

**3. User Stories**

* **As a user, I want** to create a hike with required details **so that** it’s stored consistently and can be shared later.
* **As a user, I want** to add observations during hike creation or afterward **so that** I can capture field notes over time.
* **As a user, I want** to attach images to hikes and observations (stored in Firebase Storage) **so that** I can keep visual records.
* **As a user, I want** the app to pick up my location automatically (with manual override) **so that** entries are geotagged without extra effort.
* **As a user, I want** to sign up, log in, and deactivate my account **so that** my data is secure and under my control.
* **As a user, I want** to find other users and invite them during hike creation **so that** they can view my hike.
* **As a user, I want** to share an existing hike after creation **so that** additional users can view it read-only.
* **As an invited/shared user, I want** the hike/observation details to update automatically **so that** I always see the latest without a new invite.

**4. Features & Requirements**

**4.1. Functional Requirements**

| ID        | Requirement Description                               | Details                                                                                                                                                                                                                                  |
| :-------- | :---------------------------------------------------- | :--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **FR-01** | **Hike Entry & Validation**                           | Required: Name, Location, Date, Length, Difficulty, Parking (Y/N). Optional: Description, custom fields (e.g., Terrain, Group Size). Inline validation blocks save until required fields are valid. Show confirmation before final save. |
| **FR-02** | **Hike Persistence & Management**                     | Create, list, view, edit, delete hikes. Persist timestamps (created/updated). Changes survive app restarts.                                                                                                                              |
| **FR-03** | **Observations (Create During/After Hike Creation)**  | Observations belong to a hike. Fields: Text (required), Time (defaults to “now”), optional comments. Full CRUD; multiple observations per hike.                                                                                          |
| **FR-04** | **Images on Hikes & Observations (Firebase Storage)** | Attach/remove 0..N images to hikes or observations. Show upload progress, generate thumbnails, enforce size/type limits, and retry on transient errors. Secure via storage rules tied to permissions.                                    |
| **FR-05** | **Auto Location Capture with Override**               | On creating a hike/observation, auto-fill current coordinates (with permission). Allow manual edit of coords and place name. Persist chosen value.                                                                                       |
| **FR-06** | **Accounts: Signup, Login, Deactivate**               | Create account with verified identifier. Login persists session; logout ends it. Deactivate prevents future logins while preserving data; allow reactivation.                                                                            |
| **FR-07** | **User Discovery**                                    | Search users by handle/email/name (exact/prefix). Show minimal profile info for selection.                                                                                                                                               |
| **FR-08** | **Invites During Hike Creation**                      | While creating a hike, add invitees. On save, invited users gain read-only access to the hike, its observations, and images. Owner manages invite list before first save.                                                                |
| **FR-09** | **Post-Creation Sharing**                             | From hike detail, share with additional users at any time. Access level is read-only. Owner can revoke access; revocation is immediate.                                                                                                  |
| **FR-10** | **Auto-Update Propagation**                           | When the owner updates hike/observations/images, invited/shared users automatically see the latest content without needing a new invite/share.                                                                                           |
| **FR-11** | **Search & Filters**                                  | Basic name (prefix) search. Advanced filters by name, location, length range, date range, and difficulty. Tapping a result opens the hike.                                                                                               |
| **FR-12** | **Error & Permission Handling**                       | Graceful messages for invalid forms, denied location permission, upload failures (with retry), and missing network. Never crash on user errors.                                                                                          |

**4.2. Non-Functional Requirements**

| ID         | Requirement Description          | Details                                                                                                                                                                          |
| :--------- | :------------------------------- | :------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **NFR-01** | **Styling and Theming**          | Consistent design tokens (colors, type, spacing). Avoid hard-coded styles in views. Provide clear empty states and helpful messages.                                             |
| **NFR-02** | **Performance & Responsiveness** | Lists open quickly; image uploads show non-blocking progress; interactions feel immediate (<100ms tap feedback).                                                                 |
| **NFR-03** | **State Preservation**           | Preserve reasonable form inputs during transient interruptions. Avoid data loss on rotation/app switch.                                                                          |
| **NFR-04** | **Security & Privacy**           | Authentication required for data access. Enforce least-privilege rules for app data and Firebase Storage; protect media with rules/expiring links. Respect location permissions. |
| **NFR-05** | **Reliability & Integrity**      | Atomic saves (no partial records). Defensive error handling. Timestamps on create/update for auditability.                                                                       |
| **NFR-06** | **Scalability & Accessibility**  | Pagination/lazy loading for large lists/media. Meet accessibility guidelines (contrast, readable type, ≥48px touch targets).                                                     |

**5. Success Metrics**

* Core CRUD flows for hikes and observations function reliably, including creating observations both during and after hike creation.
* Images attach and display correctly on both hikes and observations; >95% successful upload rate with clear progress and retries.
* Auto location capture succeeds when permitted and is easily overridden.
* Accounts work end-to-end (signup/login/deactivate/reactivate) without access leaks.
* Invites at creation and post-creation sharing provide read-only access; revocation is immediate; owner edits propagate automatically without re-invites.
* Search/filters return expected results quickly; UI is responsive and consistent with the defined styling.

---

**6. UI/UX Design Guidelines (Visually Appealing Outdoors Theme)**

**6.1 Design Principles**

* **Clarity over clutter:** one primary action per screen, obvious hierarchy, generous whitespace.
* **Familiar patterns:** consistent placement of “Add” and “Save”; predictable navigation.
* **One-hand friendly:** critical actions within thumb reach; tap targets ≥48px.
* **Stateful & honest:** clear “view-only” state; visible upload and permission statuses.

**6.2 Visual Identity (Tokens)**

* **Palette**
  `--bg`: #0F1210 (Dark) / #F7F8F7 (Light)
  `--surface`: #161A18 / #FFFFFF
  `--primary (Forest)`: #256B4A
  `--accent (Trail Orange)`: #E77D43
  `--info (Sky)`: #4F9DD9
  `--success (Moss)`: #2F8F46
  `--danger (Berry)`: #C23C4B
  `--muted`: #889392
  *Use primary for main CTAs; accent for highlights (badges, FAB).*
* **Type Scale**
  Display 28/32, Title 20/28, Subtitle 16/24, Body 14/20, Caption 12/16 (px/line).
  Humanist sans (Inter/Roboto-like); medium for titles, regular for body.
* **Iconography**
  Rounded stroke icons (map, image, share, lock, pin, clock) with consistent line weight.

**6.3 Components & Patterns**

* **Top bar:** large title + optional cover image preview; contextual actions on the right (Share, Edit).
* **Primary CTA:** filled button (primary). Secondary: outline. Destructive: ghost/danger.
* **Hike cards:** hero thumbnail (first image), title, chips (location/date/difficulty), soft shadow, 12–16px radius.
* **Chips/Tags:** Difficulty (Easy/Medium/Hard), Parking (Yes/No), “Shared” badge.
* **Filters sheet:** bottom sheet with sliders (length), date picker, difficulty chips, location text field.
* **Image grid:** 3-column grid; tap to full-screen slider.
* **Empty states:** illustration + sentence + primary CTA.
* **Skeletons:** grey blocks for list cards and thumbnails while loading.

**6.4 Key Screens**

1. **Home / All Hikes**
   Search at top (“Search hikes…”); horizontal filter chips (Date • Length • Difficulty • Location); card list; floating **+ New hike** button.

2. **Create / Edit Hike**
   Sections:
   (a) Details (Name*, Date*, Difficulty*, Length*, Parking*)
   (b) Location (auto-fill + small map snap + “Edit location”)
   (c) Images (grid with upload progress)
   (d) Invite (search users; selected show as face-pile chips)
   Inline validation; sticky **Save** bar; confirmation sheet after save (“View hike” / “Add observation”).

3. **Hike Detail**
   Collapsing hero image with title overlay; info grid (length, date, parking); location card with map snapshot; observations list (time, excerpt, thumbnail, pin if geotagged); actions row (Add Observation, Add Images, Share); **view-only banner** for guests.

4. **Add Observation**
   Minimal form: Text*, Time (defaults to now), “Use current location” toggle, Images grid; fixed Save at bottom.

5. **Share / Invite**
   Search users; results list with avatar/name/handle and Add button; current access list with remove icons; explicit “Guests can view only.”

6. **Image Viewer**
   Edge-to-edge slider, pinch-zoom, swipe to dismiss. Owner overflow: Set Cover, Remove. Viewers: no edit.

**6.5 States, Feedback & Permissions**

* **Uploads:** per-thumbnail progress ring + %; failures show retry chip.
* **Saving:** optimistic UI + toast “Saved”; undo where safe.
* **Location permission:** inline explainer; “Allow once / Don’t allow”; if denied, keep manual entry visible.
* **Read-only:** disabled edit affordances; dimmed icons; tooltip “Owner only.”
* **Errors:** friendly, specific (“Image too large. Max 10MB”) with next steps.

**6.6 Interaction & Motion**

* **Micro-animations:** invite chip slides into face-pile; filter chip scale 0.95→1 on select; header parallax on scroll.
* **Transitions:** bottom sheets spring in; image grid cross-fades to viewer.
* **Haptics:** on Save, successful upload, and key toggles.

**6.7 Accessibility & Inclusivity**

* Contrast ≥ 4.5:1; don’t rely on color alone—pair icons/labels for difficulty.
* Touch targets ≥ 48px; visible focus ring; support dynamic type.
* Alt text/captions for images; readable content width (≈65–75 chars).

**6.8 Copy & Micro-UX**

* Short, action-first labels: “Add image”, “Invite people”, “Share hike”.
* Empty state guidance: “No observations yet. Add your first note.”
* Confirmation dialogs only for destructive actions; otherwise toasts/snackbars.

**6.9 Quick Visual Polish Checklist**

* Consistent 8-pt spacing (4/8/12/16/24).
* Unified shadow/elevation scale; avoid heavy drops.
* First attached image becomes **cover** automatically; “Set as cover” available.
* Distinct badges: “Shared”, “Invited”, “View-only”.
