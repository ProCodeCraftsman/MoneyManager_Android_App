package com.moneymanager.data.security;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata("javax.inject.Singleton")
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
public final class SecurityManager_Factory implements Factory<SecurityManager> {
  @Override
  public SecurityManager get() {
    return newInstance();
  }

  public static SecurityManager_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static SecurityManager newInstance() {
    return new SecurityManager();
  }

  private static final class InstanceHolder {
    private static final SecurityManager_Factory INSTANCE = new SecurityManager_Factory();
  }
}
