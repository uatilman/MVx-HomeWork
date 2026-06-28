package ru.otus.cryptomvisample.features.favourites.mvi

import com.arkivanov.mvikotlin.core.store.Store
import ru.otus.cryptomvisample.features.favourites.FavouriteCoinState

/**
 * Интерфейс Store для экрана избранных монет.
 */
interface FavoriteStore : Store<FavoriteStore.Intent, FavoriteStore.State, Nothing> {

    /**
     * Интенты для экрана избранных монет.
     */
    sealed class Intent {
        /**
         * Удаление монеты из избранного.
         * @property coinId идентификатор монеты.
         */
        data class RemoveFavourite(val coinId: String) : Intent()
    }

    /**
     * Состояние экрана избранных монет.
     * @property favoriteCoins список избранных монет.
     */
    data class State(
        val favoriteCoins: List<FavouriteCoinState> = emptyList()
    )
}
