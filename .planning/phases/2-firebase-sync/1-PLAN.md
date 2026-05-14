<objective>
Implement Firebase cloud backup with Google Sign-In authentication
</objective>

<context>
User requested Firebase cloud backup for data sync. Need to:
1. Set up Firebase project configuration
2. Implement Google Sign-In
3. Create sync repositories
4. Handle online/offline scenarios
</context>

<tasks>

## 1. Firebase Project Setup
- [x] Create Firebase project in Firebase Console
- [x] Add google-services.json to app module
- [x] Update build.gradle with Firebase dependencies
- [ ] Configure SHA-1 fingerprint for debug signing

## 2. Google Sign-In Implementation
- [x] Add Google Sign-In dependency
- [x] Create AuthManager for authentication state
- [x] Implement sign-in flow in Settings or startup
- [x] Create Google account picker UI
- [x] Handle sign-out functionality

## 3. Sync Repository Interfaces
- [x] Define SyncRepository interface with conflict resolution
- [x] Methods: push, pull, sync, getLastSyncTime

## 4. Firebase Implementation
- [ ] Implement Firestore collections for each entity type
- [x] Create FirebaseSyncManager class
- [x] Implement exponential backoff for failures
- [x] Handle network state changes

## 5. Data Layer Updates
- [ ] Update repository implementations to support sync
- [ ] Add sync timestamps to entities
- [x] Implement offline-first with pending changes queue

## 6. Sync UI
- [x] Add sync status indicator in Settings
- [x] Manual sync button
- [x] Last sync time display
- [x] Sync error notifications

## 7. Testing
- [ ] Test sign-in/sign-out flow
- [ ] Test data syncs between devices
- [ ] Test offline mode
- [ ] Test conflict resolution

</tasks>

<success_criteria>
- User can sign in with Google account
- Data syncs to Firebase when online
- App works offline with queued changes
- Sync status visible in UI
</success_criteria>
