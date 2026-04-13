package com.moneymanager.di;

import android.content.Context;
import com.moneymanager.data.preferences.PreferencesManager;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
public final class PreferencesModule_ProvidePreferencesManagerFactory implements Factory<PreferencesManager> {
  private final Provider<Context> contextProvider;

  public PreferencesModule_ProvidePreferencesManagerFactory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public PreferencesManager get() {
    return providePreferencesManager(contextProvider.get());
  }

  public static PreferencesModule_ProvidePreferencesManagerFactory create(
      Provider<Context> contextProvider) {
    return new PreferencesModule_ProvidePreferencesManagerFactory(contextProvider);
  }

  public static PreferencesManager providePreferencesManager(Context context) {
    return Preconditions.checkNotNullFromProvides(PreferencesModule.INSTANCE.providePreferencesManager(context));
  }
}
