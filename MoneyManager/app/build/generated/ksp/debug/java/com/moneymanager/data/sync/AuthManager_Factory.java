package com.moneymanager.data.sync;

import com.google.firebase.auth.FirebaseAuth;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class AuthManager_Factory implements Factory<AuthManager> {
  private final Provider<FirebaseAuth> firebaseAuthProvider;

  public AuthManager_Factory(Provider<FirebaseAuth> firebaseAuthProvider) {
    this.firebaseAuthProvider = firebaseAuthProvider;
  }

  @Override
  public AuthManager get() {
    return newInstance(firebaseAuthProvider.get());
  }

  public static AuthManager_Factory create(Provider<FirebaseAuth> firebaseAuthProvider) {
    return new AuthManager_Factory(firebaseAuthProvider);
  }

  public static AuthManager newInstance(FirebaseAuth firebaseAuth) {
    return new AuthManager(firebaseAuth);
  }
}
