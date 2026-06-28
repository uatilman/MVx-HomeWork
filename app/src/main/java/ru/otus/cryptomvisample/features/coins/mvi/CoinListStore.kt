package ru.otus.cryptomvisample.features.coins.mvi

import com.arkivanov.mvikotlin.core.store.Store
import ru.otus.cryptomvisample.features.coins.CoinCategoryState

/**
 * Интерфейс Store для экрана списка монет.
 */
interface CoinListStore : Store<CoinListStore.Intent, CoinListStore.State, Nothing> {

    /**
     * Интенты (намерения) пользователя для экрана списка монет.
     */
    sealed class Intent {
        /**
         * Переключение подсветки монет с сильным изменением цены.
         * @property isChecked true, если подсветка включена.
         */
        data class ToggleHighlightMovers(val isChecked: Boolean) : Intent()

        /**
         * Переключение статуса "избранное" для монеты.
         * @property coinId идентификатор монеты.
         */
        data class ToggleFavourite(val coinId: String) : Intent()
    }

    /**
     * Состояние экрана списка монет.
     *
     * @property categories список категорий монет для отображения.
     * @property highlightMovers флаг подсветки "горячих" монет.
     * @property rawCategories исходный список категорий без применения фильтров/подсветок.
     */
    data class State(
        val categories: List<CoinCategoryState> = emptyList(),
        val highlightMovers: Boolean = false,
        val rawCategories: List<CoinCategoryState> = emptyList()
    )
}
