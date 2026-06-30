package com.rendox.routineblocker.feature.shortsblocker.di

import com.rendox.routineblocker.feature.shortsblocker.ui.viewmodels.BlockerViewModel
import com.rendox.routineblocker.feature.shortsblocker.utils.UserPreferencesProvider
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val shortsBlockerModule = module {
    single { UserPreferencesProvider(androidContext()) }
    viewModel { BlockerViewModel(get(), get()) }
}
