package com.moneymanager.data.repository;

import com.moneymanager.data.dao.CategoryDao;
import com.moneymanager.data.dao.TagDao;
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
public final class CategoryRepositoryImpl_Factory implements Factory<CategoryRepositoryImpl> {
  private final Provider<CategoryDao> categoryDaoProvider;

  private final Provider<TagDao> tagDaoProvider;

  public CategoryRepositoryImpl_Factory(Provider<CategoryDao> categoryDaoProvider,
      Provider<TagDao> tagDaoProvider) {
    this.categoryDaoProvider = categoryDaoProvider;
    this.tagDaoProvider = tagDaoProvider;
  }

  @Override
  public CategoryRepositoryImpl get() {
    return newInstance(categoryDaoProvider.get(), tagDaoProvider.get());
  }

  public static CategoryRepositoryImpl_Factory create(Provider<CategoryDao> categoryDaoProvider,
      Provider<TagDao> tagDaoProvider) {
    return new CategoryRepositoryImpl_Factory(categoryDaoProvider, tagDaoProvider);
  }

  public static CategoryRepositoryImpl newInstance(CategoryDao categoryDao, TagDao tagDao) {
    return new CategoryRepositoryImpl(categoryDao, tagDao);
  }
}
