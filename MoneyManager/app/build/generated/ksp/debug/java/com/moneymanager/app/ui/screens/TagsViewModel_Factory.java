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
public final class TagsViewModel_Factory implements Factory<TagsViewModel> {
  private final Provider<CategoryRepository> categoryRepositoryProvider;

  public TagsViewModel_Factory(Provider<CategoryRepository> categoryRepositoryProvider) {
    this.categoryRepositoryProvider = categoryRepositoryProvider;
  }

  @Override
  public TagsViewModel get() {
    return newInstance(categoryRepositoryProvider.get());
  }

  public static TagsViewModel_Factory create(
      Provider<CategoryRepository> categoryRepositoryProvider) {
    return new TagsViewModel_Factory(categoryRepositoryProvider);
  }

  public static TagsViewModel newInstance(CategoryRepository categoryRepository) {
    return new TagsViewModel(categoryRepository);
  }
}
