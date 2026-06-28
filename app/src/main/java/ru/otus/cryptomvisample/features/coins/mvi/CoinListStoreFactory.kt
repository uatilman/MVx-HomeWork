package ru.otus.cryptomvisample.features.coins.mvi

import com.arkivanov.mvikotlin.core.store.Reducer
import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineBootstrapper
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutor
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import ru.otus.cryptomvisample.common.domain_api.ConsumeCoinsUseCase
import ru.otus.cryptomvisample.common.domain_api.SetFavouriteCoinUseCase
import ru.otus.cryptomvisample.common.domain_api.UnsetFavouriteCoinUseCase
import ru.otus.cryptomvisample.features.coins.CoinCategoryState
import ru.otus.cryptomvisample.features.coins.CoinsStateFactory

/**
 * Фабрика для создания [CoinListStore].
 *
 * @property storeFactory базовая фабрика MVIKotlin.
 * @property consumeCoinsUseCase сценарий получения потока монет.
 * @property coinsStateFactory маппер для преобразования доменных моделей в стейт.
 * @property setFavouriteCoinUseCase сценарий добавления в избранное.
 * @property unsetFavouriteCoinUseCase сценарий удаления из избранного.
 */
class CoinListStoreFactory(
    private val storeFactory: StoreFactory,
    private val consumeCoinsUseCase: ConsumeCoinsUseCase,
    private val coinsStateFactory: CoinsStateFactory,
    private val setFavouriteCoinUseCase: SetFavouriteCoinUseCase,
    private val unsetFavouriteCoinUseCase: UnsetFavouriteCoinUseCase,
) {

    /**
     * Создает новый экземпляр [CoinListStore].
     *
     * @return [CoinListStore].
     */
    fun create(): CoinListStore =
        object : CoinListStore, Store<CoinListStore.Intent, CoinListStore.State, Nothing> by storeFactory.create(
            name = "CoinListStore",
            initialState = CoinListStore.State(),
            bootstrapper = BootstrapperImpl(),
            executorFactory = ::ExecutorImpl,
            reducer = ReducerImpl
        ) {}

    /**
     * Внутренние действия Store, не доступные извне.
     */
    private sealed class Action {
        /**
         * Действие инициализации загрузки данных.
         */
        object Init : Action()
    }

    /**
     * Класс для инициализации Store и отправки начальных действий.
     */
    private class BootstrapperImpl : CoroutineBootstrapper<Action>() {
        override fun invoke() {
            dispatch(Action.Init)
        }
    }

    /**
     * Результаты выполнения операций в Executor, передаваемые в Reducer.
     */
    private sealed class Result {
        /**
         * Данные успешно загружены.
         * @property categories список категорий монет.
         */
        data class CategoriesLoaded(val categories: List<CoinCategoryState>) : Result()

        /**
         * Состояние подсветки волатильных монет изменено.
         * @property isChecked true, если подсветка включена.
         */
        data class HighlightMoversToggled(val isChecked: Boolean) : Result()

        /**
         * Ошибка при загрузке данных.
         */
        object ErrorLoading : Result()
    }

    /**
     * Обработчик интентов пользователя и внутренних действий.
     */
    private inner class ExecutorImpl : CoroutineExecutor<CoinListStore.Intent, Action, CoinListStore.State, Result, Nothing>() {
        override fun executeIntent(intent: CoinListStore.Intent) {
            when (intent) {
                is CoinListStore.Intent.ToggleHighlightMovers -> {
                    dispatch(Result.HighlightMoversToggled(intent.isChecked))
                }
                is CoinListStore.Intent.ToggleFavourite -> {
                    toggleFavourite(intent.coinId, state())
                }
            }
        }

        override fun executeAction(action: Action) {
            when (action) {
                Action.Init -> loadCoins()
            }
        }

        /**
         * Загружает поток монет из use case.
         */
        private fun loadCoins() {
            consumeCoinsUseCase()
                .map { categories ->
                    categories.map { category -> coinsStateFactory.create(category) }
                }
                .onEach { categoryListState ->
                    dispatch(Result.CategoriesLoaded(categoryListState))
                }
                .catch {
                    dispatch(Result.ErrorLoading)
                }
                .launchIn(scope)
        }

        /**
         * Переключает статус избранного для монеты.
         *
         * @param coinId идентификатор монеты.
         * @param state текущее состояние.
         */
        private fun toggleFavourite(coinId: String, state: CoinListStore.State) {
            val isCurrentlyFavorite = state.rawCategories.any { category ->
                category.coins.any { coin -> coin.id == coinId && (coin.isFavourite) }
            }

            if (isCurrentlyFavorite) {
                unsetFavouriteCoinUseCase(coinId)
            } else {
                setFavouriteCoinUseCase(coinId)
            }
        }
    }

    /**
     * Объект, отвечающий за создание нового состояния на основе текущего состояния и результата.
     */
    private object ReducerImpl : Reducer<CoinListStore.State, Result> {
        override fun CoinListStore.State.reduce(msg: Result): CoinListStore.State =
            when (msg) {
                is Result.CategoriesLoaded -> copy(
                    rawCategories = msg.categories,
                    categories = applyHighlight(msg.categories, highlightMovers)
                )
                is Result.HighlightMoversToggled -> copy(
                    highlightMovers = msg.isChecked,
                    categories = applyHighlight(rawCategories, msg.isChecked)
                )
                is Result.ErrorLoading -> copy(
                    rawCategories = emptyList(),
                    categories = emptyList()
                )
            }

        /**
         * Применяет подсветку к списку категорий монет.
         *
         * @param categories исходный список категорий.
         * @param highlightMovers нужно ли подсвечивать волатильные монеты.
         * @return модифицированный список категорий.
         */
        private fun applyHighlight(
            categories: List<CoinCategoryState>,
            highlightMovers: Boolean
        ): List<CoinCategoryState> {
            return categories.map { category ->
                category.copy(coins = category.coins.map { coin ->
                    coin.copy(
                        highlight = highlightMovers && coin.isHotMover
                    )
                })
            }
        }
    }
}
