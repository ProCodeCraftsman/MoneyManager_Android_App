package com.moneymanager.app.ui.screens;

import com.moneymanager.domain.repository.CategoryRepository;
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
public final class TransactionsViewModel_Factory implements Factory<TransactionsViewModel> {
  private final Provider<TransactionRepository> transactionRepositoryProvider;

  private final Provider<CategoryRepository> categoryRepositoryProvider;

  public TransactionsViewModel_Factory(
      Provider<TransactionRepository> transactionRepositoryProvider,
      Provider<CategoryRepository> categoryRepositoryProvider) {
    this.transactionRepositoryProvider = transactionRepositoryProvider;
    this.categoryRepositoryProvider = categoryRepositoryProvider;
  }

  @Override
  public TransactionsViewModel get() {
    return newInstance(transactionRepositoryProvider.get(), categoryRepositoryProvider.get());
  }

  public static TransactionsViewModel_Factory create(
      Provider<TransactionRepository> transactionRepositoryProvider,
      Provider<CategoryRepository> categoryRepositoryProvider) {
    return new TransactionsViewModel_Factory(transactionRepositoryProvider, categoryRepositoryProvider);
  }

  public static TransactionsViewModel newInstance(TransactionRepository transactionRepository,
      CategoryRepository categoryRepository) {
    return new TransactionsViewModel(transactionRepository, categoryRepository);
  }
}
