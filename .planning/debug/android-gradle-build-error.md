---
status: resolved
trigger: "JAVA_HOME not set"
created: 2026-04-13
updated: 2026-04-13
---

## Status: RESOLVED

## Root Cause
- JAVA_HOME environment variable not set
- `java` command not in PATH

## Resolution Applied
- Set JAVA_HOME = `C:\Program Files\Android\Android Studio\jbr` (User level)
- Added `C:\Program Files\Android\Android Studio\jbr\bin` to PATH (User level)

## Verification
- JAVA_HOME confirmed set: `C:\Program Files\Android\Android Studio\jbr`
- PATH now includes Java bin directory
- Java executable responds to `java -version`: OpenJDK 21.0.10

## Note
Secondary issue detected: Gradle distribution URL points to invalid GitHub release URL. The primary JAVA_HOME fix has been applied. The network issue is separate.

## Files Changed
- Environment variables (User level): JAVA_HOME, PATH
