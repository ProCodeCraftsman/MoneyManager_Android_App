package com.moneymanager.app.ui.screens;

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
public final class CategoriesViewModel_Factory implements Factory<CategoriesViewModel> {
  private final Provider<CategoryRepository> categoryRepositoryProvider;

  public CategoriesViewModel_Factory(Provider<CategoryRepository> categoryRepositoryProvider) {
    this.categoryRepositoryProvider = categoryRepositoryProvider;
  }

  @Override
  public CategoriesViewModel get() {
    return newInstance(categoryRepositoryProvider.get());
  }

  public static CategoriesViewModel_Factory create(
      Provider<CategoryRepository> categoryRepositoryProvider) {
    return new CategoriesViewModel_Factory(categoryRepositoryProvider);
  }

  public static CategoriesViewModel newInstance(CategoryRepository categoryRepository) {
    return new CategoriesViewModel(categoryRepository);
  }
}
