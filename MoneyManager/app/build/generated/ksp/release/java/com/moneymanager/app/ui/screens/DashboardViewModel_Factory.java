package com.moneymanager.app.ui.screens;

import com.moneymanager.domain.repository.AccountRepository;
import com.moneymanager.domain.repository.TransactionRepository;
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
public final class DashboardViewModel_Factory implements Factory<DashboardViewModel> {
  private final Provider<AccountRepository> accountRepositoryProvider;

  private final Provider<TransactionRepository> transactionRepositoryProvider;

  public DashboardViewModel_Factory(Provider<AccountRepository> accountRepositoryProvider,
      Provider<TransactionRepository> transactionRepositoryProvider) {
    this.accountRepositoryProvider = accountRepositoryProvider;
    this.transactionRepositoryProvider = transactionRepositoryProvider;
  }

  @Override
  public DashboardViewModel get() {
    return newInstance(accountRepositoryProvider.get(), transactionRepositoryProvider.get());
  }

  public static DashboardViewModel_Factory create(
      Provider<AccountRepository> accountRepositoryProvider,
      Provider<TransactionRepository> transactionRepositoryProvider) {
    return new DashboardViewModel_Factory(accountRepositoryProvider, transactionRepositoryProvider);
  }

  public static DashboardViewModel newInstance(AccountRepository accountRepository,
      TransactionRepository transactionRepository) {
    return new DashboardViewModel(accountRepository, transactionRepository);
  }
}
