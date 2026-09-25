package com.paypal.android

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.OnBackPressedDispatcherOwner
import androidx.activity.setViewTreeOnBackPressedDispatcherOwner
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.util.Consumer
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.paypal.android.customenvironment.CustomEnvironmentRepository
import com.paypal.android.customenvironment.SettingsViewModel
import com.paypal.android.ui.CustomTabDemoApp
import com.paypal.android.ui.paypal.PayPalCheckoutViewModel
import com.paypal.android.usecase.CompleteOrderUseCase
import com.paypal.android.usecase.CreateOrderUseCase
import com.paypal.android.utils.NewIntentListenerHost
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/**
 * Hosts the PayPal checkout demo without inheriting from ComponentActivity. This makes the SDK's
 * web fallback use a Chrome Custom Tab while still reusing the Demo app's Compose checkout UI.
 */
@Suppress("TooManyFunctions")
class CustomTabActivity : Activity(),
    LifecycleOwner,
    SavedStateRegistryOwner,
    ViewModelStoreOwner,
    OnBackPressedDispatcherOwner,
    NewIntentListenerHost {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val newIntentListeners = mutableSetOf<Consumer<Intent>>()
    private var retainedViewModelStore: ViewModelStore? = null

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    override val viewModelStore: ViewModelStore
        get() {
            if (retainedViewModelStore == null) {
                retainedViewModelStore =
                    (lastNonConfigurationInstance as? NonConfigurationState)?.viewModelStore
                        ?: ViewModelStore()
            }
            return checkNotNull(retainedViewModelStore)
        }

    override val onBackPressedDispatcher = OnBackPressedDispatcher {
        @Suppress("DEPRECATION")
        super.onBackPressed()
    }

    @ExperimentalComposeUiApi
    @ExperimentalMaterial3Api
    @ExperimentalFoundationApi
    override fun onCreate(savedInstanceState: Bundle?) {
        savedStateRegistryController.performAttach()
        super.onCreate(savedInstanceState)
        savedStateRegistryController.performRestore(savedInstanceState)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)

        if (redirectCheckoutReturnIfNeeded(DemoActivityType.PLAIN_ACTIVITY)) {
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            onBackPressedDispatcher.setOnBackInvokedDispatcher(onBackInvokedDispatcher)
        }

        val dependencies = EntryPointAccessors.fromApplication(
            applicationContext,
            CustomTabActivityDependencies::class.java,
        )
        val viewModelProvider = ViewModelProvider(
            this,
            CustomTabViewModelFactory(applicationContext, dependencies),
        )
        val checkoutViewModel = viewModelProvider[PayPalCheckoutViewModel::class.java]
        val settingsViewModel = viewModelProvider[SettingsViewModel::class.java]

        val composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@CustomTabActivity)
            setViewTreeSavedStateRegistryOwner(this@CustomTabActivity)
            setViewTreeViewModelStoreOwner(this@CustomTabActivity)
            setViewTreeOnBackPressedDispatcherOwner(this@CustomTabActivity)
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
            )
            setContent {
                CustomTabDemoApp(
                    checkoutViewModel = checkoutViewModel,
                    settingsViewModel = settingsViewModel,
                    onSwitchActivityType = {
                        switchDemoActivityType(DemoActivityType.PLAIN_ACTIVITY)
                    },
                )
            }
        }
        setContentView(composeView)
    }

    override fun onStart() {
        super.onStart()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
    }

    override fun onResume() {
        super.onResume()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    override fun onPause() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        super.onPause()
    }

    override fun onStop() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        super.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        savedStateRegistryController.performSave(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        if (!isChangingConfigurations) {
            viewModelStore.clear()
        }
        super.onDestroy()
    }

    override fun onRetainNonConfigurationInstance(): Any =
        NonConfigurationState(viewModelStore)

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        onBackPressedDispatcher.onBackPressed()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        newIntentListeners.toList().forEach { listener -> listener.accept(intent) }
    }

    override fun addOnNewIntentListener(listener: Consumer<Intent>) {
        newIntentListeners += listener
    }

    override fun removeOnNewIntentListener(listener: Consumer<Intent>) {
        newIntentListeners -= listener
    }

    private data class NonConfigurationState(val viewModelStore: ViewModelStore)
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface CustomTabActivityDependencies {
    fun customEnvironmentRepository(): CustomEnvironmentRepository
    fun createOrderUseCase(): CreateOrderUseCase
    fun completeOrderUseCase(): CompleteOrderUseCase
}

private class CustomTabViewModelFactory(
    private val applicationContext: android.content.Context,
    private val dependencies: CustomTabActivityDependencies,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when (modelClass) {
        PayPalCheckoutViewModel::class.java -> PayPalCheckoutViewModel(
            applicationContext = applicationContext,
            createOrderUseCase = dependencies.createOrderUseCase(),
            completeOrderUseCase = dependencies.completeOrderUseCase(),
            customEnvironmentRepository = dependencies.customEnvironmentRepository(),
        ) as T
        SettingsViewModel::class.java -> SettingsViewModel(
            customEnvironmentRepository = dependencies.customEnvironmentRepository(),
        ) as T
        else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
