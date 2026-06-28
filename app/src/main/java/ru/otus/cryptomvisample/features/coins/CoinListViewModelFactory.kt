package ru.otus.cryptomvisample.features.coins

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import ru.otus.common.di.FeatureScope
import ru.otus.cryptomvisample.common.domain_api.ConsumeCoinsUseCase
import ru.otus.cryptomvisample.common.domain_api.SetFavouriteCoinUseCase
import ru.otus.cryptomvisample.common.domain_api.UnsetFavouriteCoinUseCase
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import ru.otus.cryptomvisample.features.coins.mvi.CoinListStoreFactory
import javax.inject.Inject

@FeatureScope
class CoinListViewModelFactory @Inject constructor(
    private val consumeCoinsUseCase: ConsumeCoinsUseCase,
    private val coinsStateFactory: CoinsStateFactory,
    private val setFavouriteCoinUseCase: SetFavouriteCoinUseCase,
    private val unsetFavouriteCoinUseCase: UnsetFavouriteCoinUseCase,
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(
        modelClass: Class<T>,
        extras: CreationExtras,
    ): T {
        when {
            modelClass.isAssignableFrom(CoinListViewModel::class.java) -> {
                val store = CoinListStoreFactory(
                    storeFactory = DefaultStoreFactory(),
                    consumeCoinsUseCase = consumeCoinsUseCase,
                    coinsStateFactory = coinsStateFactory,
                    setFavouriteCoinUseCase = setFavouriteCoinUseCase,
                    unsetFavouriteCoinUseCase = unsetFavouriteCoinUseCase
                ).create()

                @Suppress("UNCHECKED_CAST")
                return CoinListViewModel(store = store) as T
            }
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
