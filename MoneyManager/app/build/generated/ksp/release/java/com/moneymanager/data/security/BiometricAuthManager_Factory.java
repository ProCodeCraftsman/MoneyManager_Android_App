package com.moneymanager.data.security;

import android.content.Context;
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
public final class BiometricAuthManager_Factory implements Factory<BiometricAuthManager> {
  private final Provider<Context> contextProvider;

  public BiometricAuthManager_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public BiometricAuthManager get() {
    return newInstance(contextProvider.get());
  }

  public static BiometricAuthManager_Factory create(Provider<Context> contextProvider) {
    return new BiometricAuthManager_Factory(contextProvider);
  }

  public static BiometricAuthManager newInstance(Context context) {
    return new BiometricAuthManager(context);
  }
}
