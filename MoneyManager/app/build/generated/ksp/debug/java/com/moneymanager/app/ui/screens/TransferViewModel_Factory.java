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
public final class TransferViewModel_Factory implements Factory<TransferViewModel> {
  private final Provider<AccountRepository> accountRepositoryProvider;

  private final Provider<TransactionRepository> transactionRepositoryProvider;

  public TransferViewModel_Factory(Provider<AccountRepository> accountRepositoryProvider,
      Provider<TransactionRepository> transactionRepositoryProvider) {
    this.accountRepositoryProvider = accountRepositoryProvider;
    this.transactionRepositoryProvider = transactionRepositoryProvider;
  }

  @Override
  public TransferViewModel get() {
    return newInstance(accountRepositoryProvider.get(), transactionRepositoryProvider.get());
  }

  public static TransferViewModel_Factory create(
      Provider<AccountRepository> accountRepositoryProvider,
      Provider<TransactionRepository> transactionRepositoryProvider) {
    return new TransferViewModel_Factory(accountRepositoryProvider, transactionRepositoryProvider);
  }

  public static TransferViewModel newInstance(AccountRepository accountRepository,
      TransactionRepository transactionRepository) {
    return new TransferViewModel(accountRepository, transactionRepository);
  }
}
