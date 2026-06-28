package ru.otus.cryptomvisample.features.favourites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arkivanov.mvikotlin.extensions.coroutines.states
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import ru.otus.cryptomvisample.features.favourites.mvi.FavoriteStore

/**
 * ViewModel для экрана избранного, адаптирующая [FavoriteStore] для UI слоя.
 *
 * @property store Экземпляр [FavoriteStore], управляющий состоянием избранных монет.
 */
class FavoriteViewModel(
    private val store: FavoriteStore,
) : ViewModel() {

    /**
     * Состояние UI со списком избранных монет.
     */
    val state: StateFlow<FavoriteCoinsScreenState> = store.states
        .map { mviState ->
            FavoriteCoinsScreenState(
                favoriteCoins = mviState.favoriteCoins
            )
        }
        .stateIn(
            // Подписка на поток состояний в рамках жизненного цикла ViewModel
            scope = viewModelScope,
            // Поток остается активным в течение 5 секунд после отписки последнего подписчика,
            // что позволяет пережить смену конфигурации (например, поворот экрана)
            started = SharingStarted.WhileSubscribed(5000),
            // Начальное состояние до получения первого значения из Store
            initialValue = FavoriteCoinsScreenState()
        )

    /**
     * Удаление монеты из списка избранных.
     *
     * @param coinId Идентификатор монеты.
     */
    fun removeFavourite(coinId: String) {
        store.accept(FavoriteStore.Intent.RemoveFavourite(coinId))
    }

    override fun onCleared() {
        store.dispose()
        super.onCleared()
    }
}
