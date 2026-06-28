package ru.otus.cryptomvisample.features.favourites.mvi

import com.arkivanov.mvikotlin.core.store.Reducer
import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineBootstrapper
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutor
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import ru.otus.cryptomvisample.common.domain_api.ConsumeFavoriteCoinsUseCase
import ru.otus.cryptomvisample.common.domain_api.UnsetFavouriteCoinUseCase
import ru.otus.cryptomvisample.features.favourites.FavoriteStateMapper
import ru.otus.cryptomvisample.features.favourites.FavouriteCoinState

/**
 * Фабрика для создания [FavoriteStore].
 *
 * @property storeFactory базовая фабрика MVIKotlin.
 * @property consumeFavoriteCoinsUseCase сценарий получения потока избранных монет.
 * @property mapper маппер для преобразования доменных моделей в стейт.
 * @property unsetFavouriteCoinUseCase сценарий удаления из избранного.
 */
class FavoriteStoreFactory(
    private val storeFactory: StoreFactory,
    private val consumeFavoriteCoinsUseCase: ConsumeFavoriteCoinsUseCase,
    private val mapper: FavoriteStateMapper,
    private val unsetFavouriteCoinUseCase: UnsetFavouriteCoinUseCase,
) {

    /**
     * Создает новый экземпляр [FavoriteStore].
     *
     * @return [FavoriteStore].
     */
    fun create(): FavoriteStore =
        object : FavoriteStore, Store<FavoriteStore.Intent, FavoriteStore.State, Nothing> by storeFactory.create(
            name = "FavoriteStore",
            initialState = FavoriteStore.State(),
            bootstrapper = BootstrapperImpl(),
            executorFactory = ::ExecutorImpl,
            reducer = ReducerImpl
        ) {}

    /**
     * Внутренние действия Store для избранного.
     */
    private sealed class Action {
        /**
         * Действие инициализации загрузки избранных монет.
         */
        object Init : Action()
    }

    /**
     * Класс для инициализации Store избранного.
     */
    private class BootstrapperImpl : CoroutineBootstrapper<Action>() {
        override fun invoke() {
            dispatch(Action.Init)
        }
    }

    /**
     * Результаты выполнения операций в Executor для избранного.
     */
    private sealed class Result {
        /**
         * Список избранных монет загружен.
         * @property coins список монет в состоянии для отображения.
         */
        data class FavoritesLoaded(val coins: List<FavouriteCoinState>) : Result()
    }

    /**
     * Обработчик интентов и действий для экрана избранного.
     */
    private inner class ExecutorImpl : CoroutineExecutor<FavoriteStore.Intent, Action, FavoriteStore.State, Result, Nothing>() {
        override fun executeIntent(intent: FavoriteStore.Intent) {
            when (intent) {
                is FavoriteStore.Intent.RemoveFavourite -> {
                    unsetFavouriteCoinUseCase(intent.coinId)
                }
            }
        }

        override fun executeAction(action: Action) {
            when (action) {
                Action.Init -> loadFavorites()
            }
        }

        /**
         * Загружает поток избранных монет.
         */
        private fun loadFavorites() {
            consumeFavoriteCoinsUseCase()
                .map { favoriteCoins ->
                    favoriteCoins.map { coin ->
                        mapper.mapToState(coin)
                    }
                }
                .onEach { favoriteCoinsState ->
                    dispatch(Result.FavoritesLoaded(favoriteCoinsState))
                }
                .launchIn(scope)
        }
    }

    /**
     * Редьюсер для обновления состояния избранных монет.
     */
    private object ReducerImpl : Reducer<FavoriteStore.State, Result> {
        override fun FavoriteStore.State.reduce(msg: Result): FavoriteStore.State =
            when (msg) {
                is Result.FavoritesLoaded -> copy(favoriteCoins = msg.coins)
            }
    }
}
