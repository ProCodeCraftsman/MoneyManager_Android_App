package com.moneymanager.data.sync;

import android.content.Context;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.moneymanager.data.preferences.PreferencesManager;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast",
    "deprecation"
})
public final class FirebaseSyncManager_Factory implements Factory<FirebaseSyncManager> {
  private final Provider<Context> contextProvider;

  private final Provider<FirebaseFirestore> firestoreProvider;

  private final Provider<FirebaseAuth> authProvider;

  private final Provider<PreferencesManager> preferencesManagerProvider;

  public FirebaseSyncManager_Factory(Provider<Context> contextProvider,
      Provider<FirebaseFirestore> firestoreProvider, Provider<FirebaseAuth> authProvider,
      Provider<PreferencesManager> preferencesManagerProvider) {
    this.contextProvider = contextProvider;
    this.firestoreProvider = firestoreProvider;
    this.authProvider = authProvider;
    this.preferencesManagerProvider = preferencesManagerProvider;
  }

  @Override
  public FirebaseSyncManager get() {
    return newInstance(contextProvider.get(), firestoreProvider.get(), authProvider.get(), preferencesManagerProvider.get());
  }

  public static FirebaseSyncManager_Factory create(Provider<Context> contextProvider,
      Provider<FirebaseFirestore> firestoreProvider, Provider<FirebaseAuth> authProvider,
      Provider<PreferencesManager> preferencesManagerProvider) {
    return new FirebaseSyncManager_Factory(contextProvider, firestoreProvider, authProvider, preferencesManagerProvider);
  }

  public static FirebaseSyncManager newInstance(Context context, FirebaseFirestore firestore,
      FirebaseAuth auth, PreferencesManager preferencesManager) {
    return new FirebaseSyncManager(context, firestore, auth, preferencesManager);
  }
}
