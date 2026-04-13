package com.moneymanager.app.ui.screens;

import com.moneymanager.domain.repository.GoalRepository;
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
public final class GoalsViewModel_Factory implements Factory<GoalsViewModel> {
  private final Provider<GoalRepository> goalRepositoryProvider;

  public GoalsViewModel_Factory(Provider<GoalRepository> goalRepositoryProvider) {
    this.goalRepositoryProvider = goalRepositoryProvider;
  }

  @Override
  public GoalsViewModel get() {
    return newInstance(goalRepositoryProvider.get());
  }

  public static GoalsViewModel_Factory create(Provider<GoalRepository> goalRepositoryProvider) {
    return new GoalsViewModel_Factory(goalRepositoryProvider);
  }

  public static GoalsViewModel newInstance(GoalRepository goalRepository) {
    return new GoalsViewModel(goalRepository);
  }
}
