package com.moneymanager.di;

import com.moneymanager.data.MoneyManagerDatabase;
import com.moneymanager.data.dao.TransactionDao;
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
public final class DatabaseModule_ProvideTransactionDaoFactory implements Factory<TransactionDao> {
  private final Provider<MoneyManagerDatabase> databaseProvider;

  public DatabaseModule_ProvideTransactionDaoFactory(
      Provider<MoneyManagerDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public TransactionDao get() {
    return provideTransactionDao(databaseProvider.get());
  }

  public static DatabaseModule_ProvideTransactionDaoFactory create(
      Provider<MoneyManagerDatabase> databaseProvider) {
    return new DatabaseModule_ProvideTransactionDaoFactory(databaseProvider);
  }

  public static TransactionDao provideTransactionDao(MoneyManagerDatabase database) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideTransactionDao(database));
  }
}
