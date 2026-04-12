# Phase 1: Project Setup

## Goal
Create Android Studio project with Kotlin + Jetpack Compose and verify shell builds

## Tasks

### 1.1 Check Development Environment
- [ ] Verify Android Studio is installed
- [ ] Verify JDK (Java 17+)
- [ ] Verify Gradle and Android SDK

### 1.2 Create Project Structure
- [ ] Create new project in Android Studio
- [ ] Select Kotlin + Jetpack Compose template
- [ ] Configure app name: MoneyManager
- [ ] Min SDK: 26, Target SDK: 34

### 1.3 Configure Dependencies
- [ ] Add Room database
- [ ] Add Hilt for DI
- [ ] Add Firebase (Auth + Firestore)
- [ ] Add MPAndroidChart
- [ ] Add other Compose dependencies

### 1.4 Set Up Project Structure
- [ ] Create Clean Architecture packages:
  - data/ (entities, dao, repository)
  - domain/ (models, use cases)
  - ui/ (screens, components, viewmodels)
  - di/ (modules)
- [ ] Create basic Application class

### 1.5 Verify Build
- [ ] Build debug APK
- [ ] Verify APK generates successfully

## Acceptance Criteria
- [ ] Android Studio project opens without errors
- [ ] Clean Architecture folder structure created
- [ ] Debug APK builds successfully