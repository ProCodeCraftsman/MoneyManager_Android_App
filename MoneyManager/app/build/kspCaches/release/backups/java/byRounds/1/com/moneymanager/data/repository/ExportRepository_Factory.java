package com.moneymanager.data.repository;

import android.content.Context;
import com.moneymanager.data.dao.AccountDao;
import com.moneymanager.data.dao.BudgetDao;
import com.moneymanager.data.dao.CategoryDao;
import com.moneymanager.data.dao.GoalDao;
import com.moneymanager.data.dao.TagDao;
import com.moneymanager.data.dao.TransactionDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class ExportRepository_Factory implements Factory<ExportRepository> {
  private final Provider<Context> contextProvider;

  private final Provider<AccountDao> accountDaoProvider;

  private final Provider<TransactionDao> transactionDaoProvider;

  private final Provider<CategoryDao> categoryDaoProvider;

  private final Provider<BudgetDao> budgetDaoProvider;

  private final Provider<GoalDao> goalDaoProvider;

  private final Provider<TagDao> tagDaoProvider;

  public ExportRepository_Factory(Provider<Context> contextProvider,
      Provider<AccountDao> accountDaoProvider, Provider<TransactionDao> transactionDaoProvider,
      Provider<CategoryDao> categoryDaoProvider, Provider<BudgetDao> budgetDaoProvider,
      Provider<GoalDao> goalDaoProvider, Provider<TagDao> tagDaoProvider) {
    this.contextProvider = contextProvider;
    this.accountDaoProvider = accountDaoProvider;
    this.transactionDaoProvider = transactionDaoProvider;
    this.categoryDaoProvider = categoryDaoProvider;
    this.budgetDaoProvider = budgetDaoProvider;
    this.goalDaoProvider = goalDaoProvider;
    this.tagDaoProvider = tagDaoProvider;
  }

  @Override
  public ExportRepository get() {
    return newInstance(contextProvider.get(), accountDaoProvider.get(), transactionDaoProvider.get(), categoryDaoProvider.get(), budgetDaoProvider.get(), goalDaoProvider.get(), tagDaoProvider.get());
  }

  public static ExportRepository_Factory create(Provider<Context> contextProvider,
      Provider<AccountDao> accountDaoProvider, Provider<TransactionDao> transactionDaoProvider,
      Provider<CategoryDao> categoryDaoProvider, Provider<BudgetDao> budgetDaoProvider,
      Provider<GoalDao> goalDaoProvider, Provider<TagDao> tagDaoProvider) {
    return new ExportRepository_Factory(contextProvider, accountDaoProvider, transactionDaoProvider, categoryDaoProvider, budgetDaoProvider, goalDaoProvider, tagDaoProvider);
  }

  public static ExportRepository newInstance(Context context, AccountDao accountDao,
      TransactionDao transactionDao, CategoryDao categoryDao, BudgetDao budgetDao, GoalDao goalDao,
      TagDao tagDao) {
    return new ExportRepository(context, accountDao, transactionDao, categoryDao, budgetDao, goalDao, tagDao);
  }
}
