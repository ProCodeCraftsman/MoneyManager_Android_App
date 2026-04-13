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
- [ ] Create Firebase project in Firebase Console
- [ ] Add google-services.json to app module
- [ ] Update build.gradle with Firebase dependencies
- [ ] Configure SHA-1 fingerprint for debug signing

## 2. Google Sign-In Implementation
- [ ] Add Google Sign-In dependency
- [ ] Create AuthManager for authentication state
- [ ] Implement sign-in flow in Settings or startup
- [ ] Create Google account picker UI
- [ ] Handle sign-out functionality

## 3. Sync Repository Interfaces
- [ ] Define SyncRepository interface with conflict resolution
- [ ] Methods: push, pull, sync, getLastSyncTime

## 4. Firebase Implementation
- [ ] Implement Firestore collections for each entity type
- [ ] Create FirebaseSyncManager class
- [ ] Implement exponential backoff for failures
- [ ] Handle network state changes

## 5. Data Layer Updates
- [ ] Update repository implementations to support sync
- [ ] Add sync timestamps to entities
- [ ] Implement offline-first with pending changes queue

## 6. Sync UI
- [ ] Add sync status indicator in Settings
- [ ] Manual sync button
- [ ] Last sync time display
- [ ] Sync error notifications

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
