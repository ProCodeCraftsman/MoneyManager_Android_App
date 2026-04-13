package com.moneymanager.di;

import com.moneymanager.data.MoneyManagerDatabase;
import com.moneymanager.data.dao.RecurringDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
public final class DatabaseModule_ProvideRecurringDaoFactory implements Factory<RecurringDao> {
  private final Provider<MoneyManagerDatabase> databaseProvider;

  public DatabaseModule_ProvideRecurringDaoFactory(
      Provider<MoneyManagerDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public RecurringDao get() {
    return provideRecurringDao(databaseProvider.get());
  }

  public static DatabaseModule_ProvideRecurringDaoFactory create(
      Provider<MoneyManagerDatabase> databaseProvider) {
    return new DatabaseModule_ProvideRecurringDaoFactory(databaseProvider);
  }

  public static RecurringDao provideRecurringDao(MoneyManagerDatabase database) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideRecurringDao(database));
  }
}
