# M-Hike Android Application

A simple, reliable Android app to plan hikes, record observations, and share read-only views with others.

## Features

- **Hike Planning**: Create and manage hikes with name, location, date, length, difficulty level, and parking availability
- **Photo Management**: Attach multiple images to hikes and observations
- **Observations**: Record real-time observations during your hikes with timestamps and location data
- **Location Services**: Auto-capture GPS coordinates or manually enter location details
- **User Authentication**: Secure signup and login with email and password
- **Social Sharing**: Share hikes with other users for read-only viewing
- **User Discovery**: Search and connect with other hikers
- **Real-Time Updates**: Shared hikes automatically sync across all viewers
- **Offline Storage**: Local data persistence with cloud synchronization

## Getting Started

### Prerequisites

- Android device or emulator running Android 7.0 (API 24) or higher
- Active internet connection for cloud features

### Installation

1. Download the APK from the releases page
2. Install the APK on your Android device
3. Open M-Hike and create an account

### First Steps

1. **Create Account**: Sign up with your email, password, and username (handle)
2. **Create Your First Hike**: Tap the "+" button to add a new hike
3. **Add Photos**: Attach images to help remember your hike
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

### Sharing Hikes

1. Open any hike you created
2. Tap the share icon in the top-right corner
3. Search for users by name or handle
4. Tap "Add" next to users you want to share with
5. Shared users can view but not edit your hike

**Note**: Only the hike owner can edit or delete the hike and its observations. Shared users have read-only access.

### Managing Your Hikes

- **View All Hikes**: See your created hikes and those shared with you
- **Edit Hike**: Tap a hike to view details, then use the edit button
- **Delete Hike**: Open a hike and tap the delete icon (owner only)
- **Search**: Use the search bar to find hikes by name

## Privacy & Security

- Your data is encrypted and stored securely in the cloud
- Only you can edit your hikes
- You control who can view your hikes through sharing
- Location data is only collected when you use location features
- Your password is never stored in plain text

## Support

For questions, issues, or feature requests:
- Check the [Product Requirements Document](docs/prd.md) for detailed feature information
- Review the [Implementation Guide](IMPLEMENTATION_GUIDE.md) for technical details and Firebase setup

## Technology

M-Hike is built with modern Android development tools:
- Native Android with Kotlin
- Jetpack Compose for beautiful, responsive UI
- Material Design 3 for consistent design
- Firebase for secure backend services
- Real-time synchronization across devices

## Version

**Current Version**: 1.0.0
**Release Date**: 2025-10-29
**Status**: Production Ready

## License

[Add your license here]

## Acknowledgments

Built with modern Android development best practices and powered by Firebase cloud services.
