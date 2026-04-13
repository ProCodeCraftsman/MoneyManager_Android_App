package com.moneymanager.di;

import com.moneymanager.data.MoneyManagerDatabase;
import com.moneymanager.data.dao.TemplateDao;
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
public final class DatabaseModule_ProvideTemplateDaoFactory implements Factory<TemplateDao> {
  private final Provider<MoneyManagerDatabase> databaseProvider;

  public DatabaseModule_ProvideTemplateDaoFactory(Provider<MoneyManagerDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public TemplateDao get() {
    return provideTemplateDao(databaseProvider.get());
  }

  public static DatabaseModule_ProvideTemplateDaoFactory create(
      Provider<MoneyManagerDatabase> databaseProvider) {
    return new DatabaseModule_ProvideTemplateDaoFactory(databaseProvider);
  }

  public static TemplateDao provideTemplateDao(MoneyManagerDatabase database) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideTemplateDao(database));
  }
}
