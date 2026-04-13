package com.moneymanager.di;

import com.moneymanager.data.MoneyManagerDatabase;
import com.moneymanager.data.dao.CategoryDao;
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
public final class DatabaseModule_ProvideCategoryDaoFactory implements Factory<CategoryDao> {
  private final Provider<MoneyManagerDatabase> databaseProvider;

  public DatabaseModule_ProvideCategoryDaoFactory(Provider<MoneyManagerDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public CategoryDao get() {
    return provideCategoryDao(databaseProvider.get());
  }

  public static DatabaseModule_ProvideCategoryDaoFactory create(
      Provider<MoneyManagerDatabase> databaseProvider) {
    return new DatabaseModule_ProvideCategoryDaoFactory(databaseProvider);
  }

  public static CategoryDao provideCategoryDao(MoneyManagerDatabase database) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideCategoryDao(database));
  }
}
