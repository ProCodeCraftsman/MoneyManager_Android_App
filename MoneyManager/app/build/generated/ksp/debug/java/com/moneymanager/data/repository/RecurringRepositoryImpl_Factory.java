package com.moneymanager.data.repository;

import com.moneymanager.data.dao.RecurringDao;
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
public final class RecurringRepositoryImpl_Factory implements Factory<RecurringRepositoryImpl> {
  private final Provider<RecurringDao> recurringDaoProvider;

  public RecurringRepositoryImpl_Factory(Provider<RecurringDao> recurringDaoProvider) {
    this.recurringDaoProvider = recurringDaoProvider;
  }

  @Override
  public RecurringRepositoryImpl get() {
    return newInstance(recurringDaoProvider.get());
  }

  public static RecurringRepositoryImpl_Factory create(
      Provider<RecurringDao> recurringDaoProvider) {
    return new RecurringRepositoryImpl_Factory(recurringDaoProvider);
  }

  public static RecurringRepositoryImpl newInstance(RecurringDao recurringDao) {
    return new RecurringRepositoryImpl(recurringDao);
  }
}
