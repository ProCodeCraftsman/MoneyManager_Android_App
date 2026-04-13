package com.moneymanager.data.repository;

import com.moneymanager.data.dao.TemplateDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
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
public final class TemplateRepositoryImpl_Factory implements Factory<TemplateRepositoryImpl> {
  private final Provider<TemplateDao> templateDaoProvider;

  public TemplateRepositoryImpl_Factory(Provider<TemplateDao> templateDaoProvider) {
    this.templateDaoProvider = templateDaoProvider;
  }

  @Override
  public TemplateRepositoryImpl get() {
    return newInstance(templateDaoProvider.get());
  }

  public static TemplateRepositoryImpl_Factory create(Provider<TemplateDao> templateDaoProvider) {
    return new TemplateRepositoryImpl_Factory(templateDaoProvider);
  }

  public static TemplateRepositoryImpl newInstance(TemplateDao templateDao) {
    return new TemplateRepositoryImpl(templateDao);
  }
}
