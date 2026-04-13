package com.moneymanager.app.ui.screens;

import com.moneymanager.data.preferences.PreferencesManager;
import com.moneymanager.data.sync.AuthManager;
import com.moneymanager.data.sync.FirebaseSyncManager;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
@QualifierMetadata
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
public final class SettingsViewModel_Factory implements Factory<SettingsViewModel> {
  private final Provider<PreferencesManager> preferencesManagerProvider;

  private final Provider<AuthManager> authManagerProvider;

  private final Provider<FirebaseSyncManager> syncManagerProvider;

  public SettingsViewModel_Factory(Provider<PreferencesManager> preferencesManagerProvider,
      Provider<AuthManager> authManagerProvider,
      Provider<FirebaseSyncManager> syncManagerProvider) {
    this.preferencesManagerProvider = preferencesManagerProvider;
    this.authManagerProvider = authManagerProvider;
    this.syncManagerProvider = syncManagerProvider;
  }

  @Override
  public SettingsViewModel get() {
    return newInstance(preferencesManagerProvider.get(), authManagerProvider.get(), syncManagerProvider.get());
  }

  public static SettingsViewModel_Factory create(
      Provider<PreferencesManager> preferencesManagerProvider,
      Provider<AuthManager> authManagerProvider,
      Provider<FirebaseSyncManager> syncManagerProvider) {
    return new SettingsViewModel_Factory(preferencesManagerProvider, authManagerProvider, syncManagerProvider);
  }

  public static SettingsViewModel newInstance(PreferencesManager preferencesManager,
      AuthManager authManager, FirebaseSyncManager syncManager) {
    return new SettingsViewModel(preferencesManager, authManager, syncManager);
  }
}
