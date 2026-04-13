package com.moneymanager.app.ui.screens;

import com.moneymanager.domain.repository.BudgetRepository;
import com.moneymanager.domain.repository.CategoryRepository;
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
public final class BudgetsViewModel_Factory implements Factory<BudgetsViewModel> {
  private final Provider<BudgetRepository> budgetRepositoryProvider;

  private final Provider<CategoryRepository> categoryRepositoryProvider;

  public BudgetsViewModel_Factory(Provider<BudgetRepository> budgetRepositoryProvider,
      Provider<CategoryRepository> categoryRepositoryProvider) {
    this.budgetRepositoryProvider = budgetRepositoryProvider;
    this.categoryRepositoryProvider = categoryRepositoryProvider;
  }

  @Override
  public BudgetsViewModel get() {
    return newInstance(budgetRepositoryProvider.get(), categoryRepositoryProvider.get());
  }

  public static BudgetsViewModel_Factory create(Provider<BudgetRepository> budgetRepositoryProvider,
      Provider<CategoryRepository> categoryRepositoryProvider) {
    return new BudgetsViewModel_Factory(budgetRepositoryProvider, categoryRepositoryProvider);
  }

  public static BudgetsViewModel newInstance(BudgetRepository budgetRepository,
      CategoryRepository categoryRepository) {
    return new BudgetsViewModel(budgetRepository, categoryRepository);
  }
}
