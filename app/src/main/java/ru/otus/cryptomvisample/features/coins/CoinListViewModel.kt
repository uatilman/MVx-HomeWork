package ru.otus.cryptomvisample.features.coins

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arkivanov.mvikotlin.extensions.coroutines.states
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import ru.otus.cryptomvisample.features.coins.mvi.CoinListStore

/**
 * ViewModel для экрана списка монет, адаптирующая [CoinListStore] для UI слоя.
 *
 * @property store Экземпляр [CoinListStore], реализующий бизнес-логику и управление состоянием.
 */
class CoinListViewModel(
    private val store: CoinListStore,
) : ViewModel() {

    /**
     * Состояние UI, получаемое из Store.
     */
    val state: StateFlow<CoinsScreenState> = store.states
        .map { mviState ->
            CoinsScreenState(
                categories = mviState.categories,
                highlightMovers = mviState.highlightMovers
            )
        }
        .stateIn(
            // Подписка на поток состояний в рамках жизненного цикла ViewModel
            scope = viewModelScope,
            // Поток остается активным в течение 5 секунд после отписки последнего подписчика,
            // что позволяет пережить смену конфигурации (например, поворот экрана)
            started = SharingStarted.WhileSubscribed(5000),
            // Начальное состояние до получения первого значения из Store
            initialValue = CoinsScreenState()
        )

    /**
     * Обработка события переключения подсветки волатильных монет.
     *
     * @param isChecked Новое состояние переключателя.
     */
    fun onHighlightMoversToggled(isChecked: Boolean) {
        store.accept(CoinListStore.Intent.ToggleHighlightMovers(isChecked))
    }

    /**
     * Обработка события нажатия на иконку избранного.
     *
     * @param coinId Идентификатор выбранной монеты.
     */
    fun onToggleFavourite(coinId: String) {
        store.accept(CoinListStore.Intent.ToggleFavourite(coinId))
    }

    override fun onCleared() {
        store.dispose()
        super.onCleared()
    }
}
