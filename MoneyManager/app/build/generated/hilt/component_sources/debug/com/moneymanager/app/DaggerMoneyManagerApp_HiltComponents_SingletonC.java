package com.moneymanager.app;

import android.app.Activity;
import android.app.Service;
import android.view.View;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.moneymanager.app.ui.screens.AccountsViewModel;
import com.moneymanager.app.ui.screens.AccountsViewModel_HiltModules;
import com.moneymanager.app.ui.screens.BudgetsViewModel;
import com.moneymanager.app.ui.screens.BudgetsViewModel_HiltModules;
import com.moneymanager.app.ui.screens.CategoriesViewModel;
import com.moneymanager.app.ui.screens.CategoriesViewModel_HiltModules;
import com.moneymanager.app.ui.screens.DashboardViewModel;
import com.moneymanager.app.ui.screens.DashboardViewModel_HiltModules;
import com.moneymanager.app.ui.screens.GoalsViewModel;
import com.moneymanager.app.ui.screens.GoalsViewModel_HiltModules;
import com.moneymanager.app.ui.screens.ReportsViewModel;
import com.moneymanager.app.ui.screens.ReportsViewModel_HiltModules;
import com.moneymanager.app.ui.screens.SettingsViewModel;
import com.moneymanager.app.ui.screens.SettingsViewModel_HiltModules;
import com.moneymanager.app.ui.screens.TagsViewModel;
import com.moneymanager.app.ui.screens.TagsViewModel_HiltModules;
import com.moneymanager.app.ui.screens.TransactionsViewModel;
import com.moneymanager.app.ui.screens.TransactionsViewModel_HiltModules;
import com.moneymanager.app.ui.screens.TransferViewModel;
import com.moneymanager.app.ui.screens.TransferViewModel_HiltModules;
import com.moneymanager.data.MoneyManagerDatabase;
import com.moneymanager.data.dao.AccountDao;
import com.moneymanager.data.dao.BudgetDao;
import com.moneymanager.data.dao.CategoryDao;
import com.moneymanager.data.dao.GoalDao;
import com.moneymanager.data.dao.TagDao;
import com.moneymanager.data.dao.TransactionDao;
import com.moneymanager.data.preferences.PreferencesManager;
import com.moneymanager.data.repository.AccountRepositoryImpl;
import com.moneymanager.data.repository.BudgetRepositoryImpl;
import com.moneymanager.data.repository.CategoryRepositoryImpl;
import com.moneymanager.data.repository.GoalRepositoryImpl;
import com.moneymanager.data.repository.TransactionRepositoryImpl;
import com.moneymanager.data.sync.AuthManager;
import com.moneymanager.data.sync.FirebaseSyncManager;
import com.moneymanager.di.DatabaseModule_ProvideAccountDaoFactory;
import com.moneymanager.di.DatabaseModule_ProvideBudgetDaoFactory;
import com.moneymanager.di.DatabaseModule_ProvideCategoryDaoFactory;
import com.moneymanager.di.DatabaseModule_ProvideDatabaseFactory;
import com.moneymanager.di.DatabaseModule_ProvideGoalDaoFactory;
import com.moneymanager.di.DatabaseModule_ProvideTagDaoFactory;
import com.moneymanager.di.DatabaseModule_ProvideTransactionDaoFactory;
import com.moneymanager.di.FirebaseModule_ProvideFirebaseAuthFactory;
import com.moneymanager.di.FirebaseModule_ProvideFirebaseFirestoreFactory;
import com.moneymanager.di.PreferencesModule_ProvidePreferencesManagerFactory;
import dagger.hilt.android.ActivityRetainedLifecycle;
import dagger.hilt.android.ViewModelLifecycle;
import dagger.hilt.android.internal.builders.ActivityComponentBuilder;
import dagger.hilt.android.internal.builders.ActivityRetainedComponentBuilder;
import dagger.hilt.android.internal.builders.FragmentComponentBuilder;
import dagger.hilt.android.internal.builders.ServiceComponentBuilder;
import dagger.hilt.android.internal.builders.ViewComponentBuilder;
import dagger.hilt.android.internal.builders.ViewModelComponentBuilder;
import dagger.hilt.android.internal.builders.ViewWithFragmentComponentBuilder;
import dagger.hilt.android.internal.lifecycle.DefaultViewModelFactories;
import dagger.hilt.android.internal.lifecycle.DefaultViewModelFactories_InternalFactoryFactory_Factory;
import dagger.hilt.android.internal.managers.ActivityRetainedComponentManager_LifecycleModule_ProvideActivityRetainedLifecycleFactory;
import dagger.hilt.android.internal.managers.SavedStateHandleHolder;
import dagger.hilt.android.internal.modules.ApplicationContextModule;
import dagger.hilt.android.internal.modules.ApplicationContextModule_ProvideContextFactory;
import dagger.internal.DaggerGenerated;
import dagger.internal.DoubleCheck;
import dagger.internal.IdentifierNameString;
import dagger.internal.KeepFieldType;
import dagger.internal.LazyClassKeyMap;
import dagger.internal.Preconditions;
import dagger.internal.Provider;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

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
public final class DaggerMoneyManagerApp_HiltComponents_SingletonC {
  private DaggerMoneyManagerApp_HiltComponents_SingletonC() {
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private ApplicationContextModule applicationContextModule;

    private Builder() {
    }

    public Builder applicationContextModule(ApplicationContextModule applicationContextModule) {
      this.applicationContextModule = Preconditions.checkNotNull(applicationContextModule);
      return this;
    }

    public MoneyManagerApp_HiltComponents.SingletonC build() {
      Preconditions.checkBuilderRequirement(applicationContextModule, ApplicationContextModule.class);
      return new SingletonCImpl(applicationContextModule);
    }
  }

  private static final class ActivityRetainedCBuilder implements MoneyManagerApp_HiltComponents.ActivityRetainedC.Builder {
    private final SingletonCImpl singletonCImpl;

    private SavedStateHandleHolder savedStateHandleHolder;

    private ActivityRetainedCBuilder(SingletonCImpl singletonCImpl) {
      this.singletonCImpl = singletonCImpl;
    }

    @Override
    public ActivityRetainedCBuilder savedStateHandleHolder(
        SavedStateHandleHolder savedStateHandleHolder) {
      this.savedStateHandleHolder = Preconditions.checkNotNull(savedStateHandleHolder);
      return this;
    }

    @Override
    public MoneyManagerApp_HiltComponents.ActivityRetainedC build() {
      Preconditions.checkBuilderRequirement(savedStateHandleHolder, SavedStateHandleHolder.class);
      return new ActivityRetainedCImpl(singletonCImpl, savedStateHandleHolder);
    }
  }

  private static final class ActivityCBuilder implements MoneyManagerApp_HiltComponents.ActivityC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private Activity activity;

    private ActivityCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
    }

    @Override
    public ActivityCBuilder activity(Activity activity) {
      this.activity = Preconditions.checkNotNull(activity);
      return this;
    }

    @Override
    public MoneyManagerApp_HiltComponents.ActivityC build() {
      Preconditions.checkBuilderRequirement(activity, Activity.class);
      return new ActivityCImpl(singletonCImpl, activityRetainedCImpl, activity);
    }
  }

  private static final class FragmentCBuilder implements MoneyManagerApp_HiltComponents.FragmentC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private Fragment fragment;

    private FragmentCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
    }

    @Override
    public FragmentCBuilder fragment(Fragment fragment) {
      this.fragment = Preconditions.checkNotNull(fragment);
      return this;
    }

    @Override
    public MoneyManagerApp_HiltComponents.FragmentC build() {
      Preconditions.checkBuilderRequirement(fragment, Fragment.class);
      return new FragmentCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, fragment);
    }
  }

  private static final class ViewWithFragmentCBuilder implements MoneyManagerApp_HiltComponents.ViewWithFragmentC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl;

    private View view;

    private ViewWithFragmentCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        FragmentCImpl fragmentCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
      this.fragmentCImpl = fragmentCImpl;
    }

    @Override
    public ViewWithFragmentCBuilder view(View view) {
      this.view = Preconditions.checkNotNull(view);
      return this;
    }

    @Override
    public MoneyManagerApp_HiltComponents.ViewWithFragmentC build() {
      Preconditions.checkBuilderRequirement(view, View.class);
      return new ViewWithFragmentCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, fragmentCImpl, view);
    }
  }

  private static final class ViewCBuilder implements MoneyManagerApp_HiltComponents.ViewC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private View view;

    private ViewCBuilder(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
        ActivityCImpl activityCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
    }

    @Override
    public ViewCBuilder view(View view) {
      this.view = Preconditions.checkNotNull(view);
      return this;
    }

    @Override
    public MoneyManagerApp_HiltComponents.ViewC build() {
      Preconditions.checkBuilderRequirement(view, View.class);
      return new ViewCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, view);
    }
  }

  private static final class ViewModelCBuilder implements MoneyManagerApp_HiltComponents.ViewModelC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private SavedStateHandle savedStateHandle;

    private ViewModelLifecycle viewModelLifecycle;

    private ViewModelCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
    }

    @Override
    public ViewModelCBuilder savedStateHandle(SavedStateHandle handle) {
      this.savedStateHandle = Preconditions.checkNotNull(handle);
      return this;
    }

    @Override
    public ViewModelCBuilder viewModelLifecycle(ViewModelLifecycle viewModelLifecycle) {
      this.viewModelLifecycle = Preconditions.checkNotNull(viewModelLifecycle);
      return this;
    }

    @Override
    public MoneyManagerApp_HiltComponents.ViewModelC build() {
      Preconditions.checkBuilderRequirement(savedStateHandle, SavedStateHandle.class);
      Preconditions.checkBuilderRequirement(viewModelLifecycle, ViewModelLifecycle.class);
      return new ViewModelCImpl(singletonCImpl, activityRetainedCImpl, savedStateHandle, viewModelLifecycle);
    }
  }

  private static final class ServiceCBuilder implements MoneyManagerApp_HiltComponents.ServiceC.Builder {
    private final SingletonCImpl singletonCImpl;

    private Service service;

    private ServiceCBuilder(SingletonCImpl singletonCImpl) {
      this.singletonCImpl = singletonCImpl;
    }

    @Override
    public ServiceCBuilder service(Service service) {
      this.service = Preconditions.checkNotNull(service);
      return this;
    }

    @Override
    public MoneyManagerApp_HiltComponents.ServiceC build() {
      Preconditions.checkBuilderRequirement(service, Service.class);
      return new ServiceCImpl(singletonCImpl, service);
    }
  }

  private static final class ViewWithFragmentCImpl extends MoneyManagerApp_HiltComponents.ViewWithFragmentC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl;

    private final ViewWithFragmentCImpl viewWithFragmentCImpl = this;

    private ViewWithFragmentCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        FragmentCImpl fragmentCImpl, View viewParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
      this.fragmentCImpl = fragmentCImpl;


    }
  }

  private static final class FragmentCImpl extends MoneyManagerApp_HiltComponents.FragmentC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl = this;

    private FragmentCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        Fragment fragmentParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;


    }

    @Override
    public DefaultViewModelFactories.InternalFactoryFactory getHiltInternalFactoryFactory() {
      return activityCImpl.getHiltInternalFactoryFactory();
    }

    @Override
    public ViewWithFragmentComponentBuilder viewWithFragmentComponentBuilder() {
      return new ViewWithFragmentCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl, fragmentCImpl);
    }
  }

  private static final class ViewCImpl extends MoneyManagerApp_HiltComponents.ViewC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final ViewCImpl viewCImpl = this;

    private ViewCImpl(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
        ActivityCImpl activityCImpl, View viewParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;


    }
  }

  private static final class ActivityCImpl extends MoneyManagerApp_HiltComponents.ActivityC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl = this;

    private ActivityCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, Activity activityParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;


    }

    @Override
    public void injectMainActivity(MainActivity mainActivity) {
    }

    @Override
    public DefaultViewModelFactories.InternalFactoryFactory getHiltInternalFactoryFactory() {
      return DefaultViewModelFactories_InternalFactoryFactory_Factory.newInstance(getViewModelKeys(), new ViewModelCBuilder(singletonCImpl, activityRetainedCImpl));
    }

    @Override
    public Map<Class<?>, Boolean> getViewModelKeys() {
      return LazyClassKeyMap.<Boolean>of(ImmutableMap.<String, Boolean>builderWithExpectedSize(10).put(LazyClassKeyProvider.com_moneymanager_app_ui_screens_AccountsViewModel, AccountsViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_moneymanager_app_ui_screens_BudgetsViewModel, BudgetsViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_moneymanager_app_ui_screens_CategoriesViewModel, CategoriesViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_moneymanager_app_ui_screens_DashboardViewModel, DashboardViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_moneymanager_app_ui_screens_GoalsViewModel, GoalsViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_moneymanager_app_ui_screens_ReportsViewModel, ReportsViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_moneymanager_app_ui_screens_SettingsViewModel, SettingsViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_moneymanager_app_ui_screens_TagsViewModel, TagsViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_moneymanager_app_ui_screens_TransactionsViewModel, TransactionsViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_moneymanager_app_ui_screens_TransferViewModel, TransferViewModel_HiltModules.KeyModule.provide()).build());
    }

    @Override
    public ViewModelComponentBuilder getViewModelComponentBuilder() {
      return new ViewModelCBuilder(singletonCImpl, activityRetainedCImpl);
    }

    @Override
    public FragmentComponentBuilder fragmentComponentBuilder() {
      return new FragmentCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl);
    }

    @Override
    public ViewComponentBuilder viewComponentBuilder() {
      return new ViewCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl);
    }

    @IdentifierNameString
    private static final class LazyClassKeyProvider {
      static String com_moneymanager_app_ui_screens_BudgetsViewModel = "com.moneymanager.app.ui.screens.BudgetsViewModel";

      static String com_moneymanager_app_ui_screens_TransactionsViewModel = "com.moneymanager.app.ui.screens.TransactionsViewModel";

      static String com_moneymanager_app_ui_screens_AccountsViewModel = "com.moneymanager.app.ui.screens.AccountsViewModel";

      static String com_moneymanager_app_ui_screens_TagsViewModel = "com.moneymanager.app.ui.screens.TagsViewModel";

      static String com_moneymanager_app_ui_screens_CategoriesViewModel = "com.moneymanager.app.ui.screens.CategoriesViewModel";

      static String com_moneymanager_app_ui_screens_TransferViewModel = "com.moneymanager.app.ui.screens.TransferViewModel";

      static String com_moneymanager_app_ui_screens_DashboardViewModel = "com.moneymanager.app.ui.screens.DashboardViewModel";

      static String com_moneymanager_app_ui_screens_GoalsViewModel = "com.moneymanager.app.ui.screens.GoalsViewModel";

      static String com_moneymanager_app_ui_screens_SettingsViewModel = "com.moneymanager.app.ui.screens.SettingsViewModel";

      static String com_moneymanager_app_ui_screens_ReportsViewModel = "com.moneymanager.app.ui.screens.ReportsViewModel";

      @KeepFieldType
      BudgetsViewModel com_moneymanager_app_ui_screens_BudgetsViewModel2;

      @KeepFieldType
      TransactionsViewModel com_moneymanager_app_ui_screens_TransactionsViewModel2;

      @KeepFieldType
      AccountsViewModel com_moneymanager_app_ui_screens_AccountsViewModel2;

      @KeepFieldType
      TagsViewModel com_moneymanager_app_ui_screens_TagsViewModel2;

      @KeepFieldType
      CategoriesViewModel com_moneymanager_app_ui_screens_CategoriesViewModel2;

      @KeepFieldType
      TransferViewModel com_moneymanager_app_ui_screens_TransferViewModel2;

      @KeepFieldType
      DashboardViewModel com_moneymanager_app_ui_screens_DashboardViewModel2;

      @KeepFieldType
      GoalsViewModel com_moneymanager_app_ui_screens_GoalsViewModel2;

      @KeepFieldType
      SettingsViewModel com_moneymanager_app_ui_screens_SettingsViewModel2;

      @KeepFieldType
      ReportsViewModel com_moneymanager_app_ui_screens_ReportsViewModel2;
    }
  }

  private static final class ViewModelCImpl extends MoneyManagerApp_HiltComponents.ViewModelC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ViewModelCImpl viewModelCImpl = this;

    private Provider<AccountsViewModel> accountsViewModelProvider;

    private Provider<BudgetsViewModel> budgetsViewModelProvider;

    private Provider<CategoriesViewModel> categoriesViewModelProvider;

    private Provider<DashboardViewModel> dashboardViewModelProvider;

    private Provider<GoalsViewModel> goalsViewModelProvider;

    private Provider<ReportsViewModel> reportsViewModelProvider;

    private Provider<SettingsViewModel> settingsViewModelProvider;

    private Provider<TagsViewModel> tagsViewModelProvider;

    private Provider<TransactionsViewModel> transactionsViewModelProvider;

    private Provider<TransferViewModel> transferViewModelProvider;

    private ViewModelCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, SavedStateHandle savedStateHandleParam,
        ViewModelLifecycle viewModelLifecycleParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;

      initialize(savedStateHandleParam, viewModelLifecycleParam);

    }

    @SuppressWarnings("unchecked")
    private void initialize(final SavedStateHandle savedStateHandleParam,
        final ViewModelLifecycle viewModelLifecycleParam) {
      this.accountsViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 0);
      this.budgetsViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 1);
      this.categoriesViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 2);
      this.dashboardViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 3);
      this.goalsViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 4);
      this.reportsViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 5);
      this.settingsViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 6);
      this.tagsViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 7);
      this.transactionsViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 8);
      this.transferViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 9);
    }

    @Override
    public Map<Class<?>, javax.inject.Provider<ViewModel>> getHiltViewModelMap() {
      return LazyClassKeyMap.<javax.inject.Provider<ViewModel>>of(ImmutableMap.<String, javax.inject.Provider<ViewModel>>builderWithExpectedSize(10).put(LazyClassKeyProvider.com_moneymanager_app_ui_screens_AccountsViewModel, ((Provider) accountsViewModelProvider)).put(LazyClassKeyProvider.com_moneymanager_app_ui_screens_BudgetsViewModel, ((Provider) budgetsViewModelProvider)).put(LazyClassKeyProvider.com_moneymanager_app_ui_screens_CategoriesViewModel, ((Provider) categoriesViewModelProvider)).put(LazyClassKeyProvider.com_moneymanager_app_ui_screens_DashboardViewModel, ((Provider) dashboardViewModelProvider)).put(LazyClassKeyProvider.com_moneymanager_app_ui_screens_GoalsViewModel, ((Provider) goalsViewModelProvider)).put(LazyClassKeyProvider.com_moneymanager_app_ui_screens_ReportsViewModel, ((Provider) reportsViewModelProvider)).put(LazyClassKeyProvider.com_moneymanager_app_ui_screens_SettingsViewModel, ((Provider) settingsViewModelProvider)).put(LazyClassKeyProvider.com_moneymanager_app_ui_screens_TagsViewModel, ((Provider) tagsViewModelProvider)).put(LazyClassKeyProvider.com_moneymanager_app_ui_screens_TransactionsViewModel, ((Provider) transactionsViewModelProvider)).put(LazyClassKeyProvider.com_moneymanager_app_ui_screens_TransferViewModel, ((Provider) transferViewModelProvider)).build());
    }

    @Override
    public Map<Class<?>, Object> getHiltViewModelAssistedMap() {
      return ImmutableMap.<Class<?>, Object>of();
    }

    @IdentifierNameString
    private static final class LazyClassKeyProvider {
      static String com_moneymanager_app_ui_screens_DashboardViewModel = "com.moneymanager.app.ui.screens.DashboardViewModel";

      static String com_moneymanager_app_ui_screens_TransferViewModel = "com.moneymanager.app.ui.screens.TransferViewModel";

      static String com_moneymanager_app_ui_screens_SettingsViewModel = "com.moneymanager.app.ui.screens.SettingsViewModel";

      static String com_moneymanager_app_ui_screens_GoalsViewModel = "com.moneymanager.app.ui.screens.GoalsViewModel";

      static String com_moneymanager_app_ui_screens_CategoriesViewModel = "com.moneymanager.app.ui.screens.CategoriesViewModel";

      static String com_moneymanager_app_ui_screens_AccountsViewModel = "com.moneymanager.app.ui.screens.AccountsViewModel";

      static String com_moneymanager_app_ui_screens_BudgetsViewModel = "com.moneymanager.app.ui.screens.BudgetsViewModel";

      static String com_moneymanager_app_ui_screens_ReportsViewModel = "com.moneymanager.app.ui.screens.ReportsViewModel";

      static String com_moneymanager_app_ui_screens_TagsViewModel = "com.moneymanager.app.ui.screens.TagsViewModel";

      static String com_moneymanager_app_ui_screens_TransactionsViewModel = "com.moneymanager.app.ui.screens.TransactionsViewModel";

      @KeepFieldType
      DashboardViewModel com_moneymanager_app_ui_screens_DashboardViewModel2;

      @KeepFieldType
      TransferViewModel com_moneymanager_app_ui_screens_TransferViewModel2;

      @KeepFieldType
      SettingsViewModel com_moneymanager_app_ui_screens_SettingsViewModel2;

      @KeepFieldType
      GoalsViewModel com_moneymanager_app_ui_screens_GoalsViewModel2;

      @KeepFieldType
      CategoriesViewModel com_moneymanager_app_ui_screens_CategoriesViewModel2;

      @KeepFieldType
      AccountsViewModel com_moneymanager_app_ui_screens_AccountsViewModel2;

      @KeepFieldType
      BudgetsViewModel com_moneymanager_app_ui_screens_BudgetsViewModel2;

      @KeepFieldType
      ReportsViewModel com_moneymanager_app_ui_screens_ReportsViewModel2;

      @KeepFieldType
      TagsViewModel com_moneymanager_app_ui_screens_TagsViewModel2;

      @KeepFieldType
      TransactionsViewModel com_moneymanager_app_ui_screens_TransactionsViewModel2;
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final ActivityRetainedCImpl activityRetainedCImpl;

      private final ViewModelCImpl viewModelCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
          ViewModelCImpl viewModelCImpl, int id) {
        this.singletonCImpl = singletonCImpl;
        this.activityRetainedCImpl = activityRetainedCImpl;
        this.viewModelCImpl = viewModelCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // com.moneymanager.app.ui.screens.AccountsViewModel 
          return (T) new AccountsViewModel(singletonCImpl.accountRepositoryImplProvider.get());

          case 1: // com.moneymanager.app.ui.screens.BudgetsViewModel 
          return (T) new BudgetsViewModel(singletonCImpl.budgetRepositoryImplProvider.get(), singletonCImpl.categoryRepositoryImplProvider.get());

          case 2: // com.moneymanager.app.ui.screens.CategoriesViewModel 
          return (T) new CategoriesViewModel(singletonCImpl.categoryRepositoryImplProvider.get());

          case 3: // com.moneymanager.app.ui.screens.DashboardViewModel 
          return (T) new DashboardViewModel(singletonCImpl.accountRepositoryImplProvider.get(), singletonCImpl.transactionRepositoryImplProvider.get());

          case 4: // com.moneymanager.app.ui.screens.GoalsViewModel 
          return (T) new GoalsViewModel(singletonCImpl.goalRepositoryImplProvider.get());

          case 5: // com.moneymanager.app.ui.screens.ReportsViewModel 
          return (T) new ReportsViewModel(singletonCImpl.transactionRepositoryImplProvider.get(), singletonCImpl.accountRepositoryImplProvider.get(), singletonCImpl.budgetRepositoryImplProvider.get());

          case 6: // com.moneymanager.app.ui.screens.SettingsViewModel 
          return (T) new SettingsViewModel(singletonCImpl.providePreferencesManagerProvider.get(), singletonCImpl.authManagerProvider.get(), singletonCImpl.firebaseSyncManagerProvider.get());

          case 7: // com.moneymanager.app.ui.screens.TagsViewModel 
          return (T) new TagsViewModel(singletonCImpl.categoryRepositoryImplProvider.get());

          case 8: // com.moneymanager.app.ui.screens.TransactionsViewModel 
          return (T) new TransactionsViewModel(singletonCImpl.transactionRepositoryImplProvider.get(), singletonCImpl.categoryRepositoryImplProvider.get());

          case 9: // com.moneymanager.app.ui.screens.TransferViewModel 
          return (T) new TransferViewModel(singletonCImpl.accountRepositoryImplProvider.get(), singletonCImpl.transactionRepositoryImplProvider.get());

          default: throw new AssertionError(id);
        }
      }
    }
  }

  private static final class ActivityRetainedCImpl extends MoneyManagerApp_HiltComponents.ActivityRetainedC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl = this;

    private Provider<ActivityRetainedLifecycle> provideActivityRetainedLifecycleProvider;

    private ActivityRetainedCImpl(SingletonCImpl singletonCImpl,
        SavedStateHandleHolder savedStateHandleHolderParam) {
      this.singletonCImpl = singletonCImpl;

      initialize(savedStateHandleHolderParam);

    }

    @SuppressWarnings("unchecked")
    private void initialize(final SavedStateHandleHolder savedStateHandleHolderParam) {
      this.provideActivityRetainedLifecycleProvider = DoubleCheck.provider(new SwitchingProvider<ActivityRetainedLifecycle>(singletonCImpl, activityRetainedCImpl, 0));
    }

    @Override
    public ActivityComponentBuilder activityComponentBuilder() {
      return new ActivityCBuilder(singletonCImpl, activityRetainedCImpl);
    }

    @Override
    public ActivityRetainedLifecycle getActivityRetainedLifecycle() {
      return provideActivityRetainedLifecycleProvider.get();
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final ActivityRetainedCImpl activityRetainedCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
          int id) {
        this.singletonCImpl = singletonCImpl;
        this.activityRetainedCImpl = activityRetainedCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // dagger.hilt.android.ActivityRetainedLifecycle 
          return (T) ActivityRetainedComponentManager_LifecycleModule_ProvideActivityRetainedLifecycleFactory.provideActivityRetainedLifecycle();

          default: throw new AssertionError(id);
        }
      }
    }
  }

  private static final class ServiceCImpl extends MoneyManagerApp_HiltComponents.ServiceC {
    private final SingletonCImpl singletonCImpl;

    private final ServiceCImpl serviceCImpl = this;

    private ServiceCImpl(SingletonCImpl singletonCImpl, Service serviceParam) {
      this.singletonCImpl = singletonCImpl;


    }
  }

  private static final class SingletonCImpl extends MoneyManagerApp_HiltComponents.SingletonC {
    private final ApplicationContextModule applicationContextModule;

    private final SingletonCImpl singletonCImpl = this;

    private Provider<MoneyManagerDatabase> provideDatabaseProvider;

    private Provider<AccountRepositoryImpl> accountRepositoryImplProvider;

    private Provider<BudgetRepositoryImpl> budgetRepositoryImplProvider;

    private Provider<CategoryRepositoryImpl> categoryRepositoryImplProvider;

    private Provider<TransactionRepositoryImpl> transactionRepositoryImplProvider;

    private Provider<GoalRepositoryImpl> goalRepositoryImplProvider;

    private Provider<PreferencesManager> providePreferencesManagerProvider;

    private Provider<FirebaseAuth> provideFirebaseAuthProvider;

    private Provider<AuthManager> authManagerProvider;

    private Provider<FirebaseFirestore> provideFirebaseFirestoreProvider;

    private Provider<FirebaseSyncManager> firebaseSyncManagerProvider;

    private SingletonCImpl(ApplicationContextModule applicationContextModuleParam) {
      this.applicationContextModule = applicationContextModuleParam;
      initialize(applicationContextModuleParam);

    }

    private AccountDao accountDao() {
      return DatabaseModule_ProvideAccountDaoFactory.provideAccountDao(provideDatabaseProvider.get());
    }

    private BudgetDao budgetDao() {
      return DatabaseModule_ProvideBudgetDaoFactory.provideBudgetDao(provideDatabaseProvider.get());
    }

    private CategoryDao categoryDao() {
      return DatabaseModule_ProvideCategoryDaoFactory.provideCategoryDao(provideDatabaseProvider.get());
    }

    private TagDao tagDao() {
      return DatabaseModule_ProvideTagDaoFactory.provideTagDao(provideDatabaseProvider.get());
    }

    private TransactionDao transactionDao() {
      return DatabaseModule_ProvideTransactionDaoFactory.provideTransactionDao(provideDatabaseProvider.get());
    }

    private GoalDao goalDao() {
      return DatabaseModule_ProvideGoalDaoFactory.provideGoalDao(provideDatabaseProvider.get());
    }

    @SuppressWarnings("unchecked")
    private void initialize(final ApplicationContextModule applicationContextModuleParam) {
      this.provideDatabaseProvider = DoubleCheck.provider(new SwitchingProvider<MoneyManagerDatabase>(singletonCImpl, 1));
      this.accountRepositoryImplProvider = DoubleCheck.provider(new SwitchingProvider<AccountRepositoryImpl>(singletonCImpl, 0));
      this.budgetRepositoryImplProvider = DoubleCheck.provider(new SwitchingProvider<BudgetRepositoryImpl>(singletonCImpl, 2));
      this.categoryRepositoryImplProvider = DoubleCheck.provider(new SwitchingProvider<CategoryRepositoryImpl>(singletonCImpl, 3));
      this.transactionRepositoryImplProvider = DoubleCheck.provider(new SwitchingProvider<TransactionRepositoryImpl>(singletonCImpl, 4));
      this.goalRepositoryImplProvider = DoubleCheck.provider(new SwitchingProvider<GoalRepositoryImpl>(singletonCImpl, 5));
      this.providePreferencesManagerProvider = DoubleCheck.provider(new SwitchingProvider<PreferencesManager>(singletonCImpl, 6));
      this.provideFirebaseAuthProvider = DoubleCheck.provider(new SwitchingProvider<FirebaseAuth>(singletonCImpl, 8));
      this.authManagerProvider = DoubleCheck.provider(new SwitchingProvider<AuthManager>(singletonCImpl, 7));
      this.provideFirebaseFirestoreProvider = DoubleCheck.provider(new SwitchingProvider<FirebaseFirestore>(singletonCImpl, 10));
      this.firebaseSyncManagerProvider = DoubleCheck.provider(new SwitchingProvider<FirebaseSyncManager>(singletonCImpl, 9));
    }

    @Override
    public void injectMoneyManagerApp(MoneyManagerApp moneyManagerApp) {
    }

    @Override
    public Set<Boolean> getDisableFragmentGetContextFix() {
      return ImmutableSet.<Boolean>of();
    }

    @Override
    public ActivityRetainedComponentBuilder retainedComponentBuilder() {
      return new ActivityRetainedCBuilder(singletonCImpl);
    }

    @Override
    public ServiceComponentBuilder serviceComponentBuilder() {
      return new ServiceCBuilder(singletonCImpl);
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, int id) {
        this.singletonCImpl = singletonCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // com.moneymanager.data.repository.AccountRepositoryImpl 
          return (T) new AccountRepositoryImpl(singletonCImpl.accountDao());

          case 1: // com.moneymanager.data.MoneyManagerDatabase 
          return (T) DatabaseModule_ProvideDatabaseFactory.provideDatabase(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 2: // com.moneymanager.data.repository.BudgetRepositoryImpl 
          return (T) new BudgetRepositoryImpl(singletonCImpl.budgetDao());

          case 3: // com.moneymanager.data.repository.CategoryRepositoryImpl 
          return (T) new CategoryRepositoryImpl(singletonCImpl.categoryDao(), singletonCImpl.tagDao());

          case 4: // com.moneymanager.data.repository.TransactionRepositoryImpl 
          return (T) new TransactionRepositoryImpl(singletonCImpl.transactionDao());

          case 5: // com.moneymanager.data.repository.GoalRepositoryImpl 
          return (T) new GoalRepositoryImpl(singletonCImpl.goalDao());

          case 6: // com.moneymanager.data.preferences.PreferencesManager 
          return (T) PreferencesModule_ProvidePreferencesManagerFactory.providePreferencesManager(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 7: // com.moneymanager.data.sync.AuthManager 
          return (T) new AuthManager(singletonCImpl.provideFirebaseAuthProvider.get());

          case 8: // com.google.firebase.auth.FirebaseAuth 
          return (T) FirebaseModule_ProvideFirebaseAuthFactory.provideFirebaseAuth();

          case 9: // com.moneymanager.data.sync.FirebaseSyncManager 
          return (T) new FirebaseSyncManager(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule), singletonCImpl.provideFirebaseFirestoreProvider.get(), singletonCImpl.provideFirebaseAuthProvider.get(), singletonCImpl.providePreferencesManagerProvider.get());

          case 10: // com.google.firebase.firestore.FirebaseFirestore 
          return (T) FirebaseModule_ProvideFirebaseFirestoreFactory.provideFirebaseFirestore();

          default: throw new AssertionError(id);
        }
      }
    }
  }
}
