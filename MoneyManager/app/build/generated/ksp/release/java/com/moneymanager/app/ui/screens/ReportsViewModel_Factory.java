package com.moneymanager.app.ui.screens;

import com.moneymanager.domain.repository.AccountRepository;
import com.moneymanager.domain.repository.BudgetRepository;
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
public final class ReportsViewModel_Factory implements Factory<ReportsViewModel> {
  private final Provider<TransactionRepository> transactionRepositoryProvider;

  private final Provider<AccountRepository> accountRepositoryProvider;

  private final Provider<BudgetRepository> budgetRepositoryProvider;

  public ReportsViewModel_Factory(Provider<TransactionRepository> transactionRepositoryProvider,
      Provider<AccountRepository> accountRepositoryProvider,
      Provider<BudgetRepository> budgetRepositoryProvider) {
    this.transactionRepositoryProvider = transactionRepositoryProvider;
    this.accountRepositoryProvider = accountRepositoryProvider;
    this.budgetRepositoryProvider = budgetRepositoryProvider;
  }

  @Override
  public ReportsViewModel get() {
    return newInstance(transactionRepositoryProvider.get(), accountRepositoryProvider.get(), budgetRepositoryProvider.get());
  }

  public static ReportsViewModel_Factory create(
      Provider<TransactionRepository> transactionRepositoryProvider,
      Provider<AccountRepository> accountRepositoryProvider,
      Provider<BudgetRepository> budgetRepositoryProvider) {
    return new ReportsViewModel_Factory(transactionRepositoryProvider, accountRepositoryProvider, budgetRepositoryProvider);
  }

  public static ReportsViewModel newInstance(TransactionRepository transactionRepository,
      AccountRepository accountRepository, BudgetRepository budgetRepository) {
    return new ReportsViewModel(transactionRepository, accountRepository, budgetRepository);
  }
}
