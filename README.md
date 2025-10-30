# M-Hike Android Application

A simple, reliable Android app to plan hikes, record observations, and share read-only views with others.

**Try it instantly as a guest, or sign up for cloud features** - your choice!

## How It Works

M-Hike offers two ways to use the app:

### 🚶 Guest Mode (No Account Required)
- Start using the app immediately without signing up
- All your data stays on your device
- Full access to core hiking features
- Perfect for trying out the app or casual use

### ☁️ Full Account (Cloud Features)
- Everything from Guest Mode, plus:
- Cloud backup of all your hikes
- Share hikes with other users
- Access your hikes from any device
- Search and connect with other hikers

**You can upgrade from Guest to Full Account anytime** - all your data moves to the cloud automatically!

## Features

### Available to Everyone (Guest & Authenticated)
- **Hike Planning**: Create and manage hikes with name, location, date, length, difficulty level, and parking availability
- **Photo Management**: Attach multiple images to hikes and observations
- **Observations**: Record real-time observations during your hikes with timestamps and location data
- **Location Services**: Auto-capture GPS coordinates or manually enter location details
- **Edit & Delete**: Full control over your hikes and observations

### Cloud Features (Authenticated Users Only)
- **Cloud Backup**: All hikes automatically backed up to the cloud
- **Social Sharing**: Share hikes with other users for read-only viewing
- **User Discovery**: Search and connect with other hikers
- **Real-Time Updates**: Shared hikes automatically sync across all viewers
- **Multi-Device Access**: View your hikes from any device

## Getting Started

### Prerequisites

- Android device or emulator running Android 7.0 (API 24) or higher
- Internet connection (optional for Guest Mode, required for Cloud Features)

### Installation

1. Download the APK from the releases page
2. Install the APK on your Android device
3. Open M-Hike and choose your path:
   - **Continue as Guest** - Start immediately, no signup required
   - **Sign Up** - Create an account for cloud features

### First Steps

#### Option 1: Guest Mode (Quickest Start)
1. **Open M-Hike** and tap "Continue as Guest"
2. **Create Your First Hike**: Tap the "+" button to add a new hike
3. **Add Photos**: Attach images to help remember your hike
4. **Record Observations**: During or after your hike, add observations with photos and notes
5. **Upgrade Anytime**: Tap "Sign Up" from the home screen to unlock cloud features

#### Option 2: Full Account (Cloud Features)
1. **Create Account**: Sign up with your email, password, and username (handle)
2. **Create Your First Hike**: Tap the "+" button to add a new hike
3. **Add Photos**: Images are automatically backed up to the cloud
4. **Record Observations**: During or after your hike, add observations with photos and notes
5. **Share with Friends**: Use the share button to invite other users to view your hike

## Usage Guide

### Creating a Hike

1. Tap the "Create Hike" button on the home screen
2. Fill in required details:
   - **Name**: Give your hike a memorable name
   - **Location**: Use auto-location or enter manually
   - **Date**: Select when you hiked
   - **Length**: Enter distance in kilometers
   - **Difficulty**: Choose Easy, Medium, or Hard
   - **Parking**: Indicate if parking is available
3. Add a description (optional)
4. Attach photos (optional)
5. Tap "Save" to create your hike

### Adding Observations

1. Open a hike from your list
2. Tap "Add Observation"
3. Write your observation (3-500 characters)
4. Location and timestamp are captured automatically
5. Add photos if desired
6. Tap "Save"

### Sharing Hikes (Cloud Feature - Authenticated Users Only)

1. Open any hike you created
2. Tap the share icon in the top-right corner
3. Search for users by name or handle
4. Tap "Add" next to users you want to share with
5. Shared users can view but not edit your hike

**Note**:
- Sharing requires a full account (not available in Guest Mode)
- Only the hike owner can edit or delete the hike and its observations
- Shared users have read-only access
- Guest users will see a prompt to sign up when tapping the share button

### Managing Your Hikes

- **View All Hikes**: See your created hikes and those shared with you (authenticated users only for shared hikes)
- **Edit Hike**: Tap a hike to view details, then use the edit button
- **Delete Hike**: Open a hike and tap the delete icon (owner only)
- **Search**: Use the search bar to find hikes by name

## Data Storage & Upgrading

### Where Your Data Lives

**Guest Mode**:
- All hikes and photos are stored locally on your device
- Data is private and secure on your device
- No cloud backup (data loss if you uninstall or lose device)
- Look for the 📱 "Device only" indicator on photos

**Full Account**:
- All hikes and photos are backed up to the cloud
- Access your data from any device
- Automatic synchronization
- Look for the ☁️ "Cloud backup" indicator on photos

### Upgrading from Guest to Full Account

Ready to unlock cloud features? Here's what happens when you sign up:

1. **Tap "Sign Up"** from the home screen guest banner
2. **Create Your Account** with email, password, and username
3. **Automatic Migration**: All your guest hikes move to the cloud
4. **Keep Everything**: No data loss - all hikes, photos, and observations transfer automatically
5. **Cloud Backup Active**: From now on, everything syncs to the cloud

The upgrade process is seamless and takes just a few seconds!

## Privacy & Security

### Guest Mode
- Your data is stored locally on your device
- No account information collected
- Location data only collected when you use location features
- Data is private to your device

### Full Account
- Your data is encrypted and stored securely in the cloud
- Only you can edit your hikes
- You control who can view your hikes through sharing
- Location data is only collected when you use location features
- Your password is never stored in plain text
- Email is only used for authentication

## Support

For questions, issues, or feature requests:
- Check the [Product Requirements Document](docs/prd.md) for detailed feature information and user flows
- Review the [Codebase Analysis](docs/CODEBASE_ANALYSIS.md) for technical architecture and implementation details

## Technology

M-Hike is built with modern Android development tools and a hybrid architecture:

### Core Technologies
- **Native Android with Kotlin**: Modern, safe, and efficient
- **Jetpack Compose**: Beautiful, responsive Material Design 3 UI
- **Clean Architecture**: Separation of concerns with MVVM pattern
- **Hilt/Dagger**: Dependency injection for maintainable code

### Storage Strategy
- **Room Database**: Local SQLite storage for guest mode and offline access
- **Firebase Firestore**: Cloud document storage for authenticated users
- **Firebase Storage**: Cloud image storage with automatic optimization
- **Firebase Auth**: Secure user authentication
- **Hybrid Repository Pattern**: Intelligent switching between local and cloud storage

### Key Features
- Automatic data migration from guest to cloud
- Offline-first architecture with cloud sync
- Real-time synchronization for shared hikes
- Strategic retry logic for network resilience

For detailed architecture documentation, see [CODEBASE_ANALYSIS.md](docs/CODEBASE_ANALYSIS.md)

## Version

**Current Version**: 2.0.0
**Architecture**: Hybrid (Guest Mode + Cloud)
**Release Date**: 2025-10-30
**Status**: Production Ready with Guest Mode Support

## Acknowledgments

Built with modern Android development best practices and powered by Firebase cloud services.
