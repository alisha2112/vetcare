package com.alisha2112.vetcare.ui.weather

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alisha2112.vetcare.data.model.CurrentWeather
import com.alisha2112.vetcare.data.repository.WeatherRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException
import retrofit2.HttpException

sealed interface WeatherUiState {

    data object Loading : WeatherUiState

    data class Success(
        val weather: CurrentWeather
    ) : WeatherUiState

    data class Error(
        val message: String
    ) : WeatherUiState
}

@HiltViewModel
class WeatherViewModel @Inject constructor(
    private val repository: WeatherRepository
) : ViewModel() {

    private val _uiState =
        MutableStateFlow<WeatherUiState>(
            WeatherUiState.Loading
        )

    val uiState: StateFlow<WeatherUiState> =
        _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        if (_uiState.value is WeatherUiState.Loading &&
            requestInProgress
        ) return

        viewModelScope.launch {
            requestInProgress = true
            _uiState.value = WeatherUiState.Loading

            try {
                val weather = repository.getCurrentWeather()

                _uiState.value = WeatherUiState.Success(
                    weather
                )

            } catch (exception: CancellationException) {
                throw exception

            } catch (exception: HttpException) {
                _uiState.value = WeatherUiState.Error(
                    "Помилка сервера: HTTP ${exception.code()}"
                )

            } catch (exception: IOException) {
                _uiState.value = WeatherUiState.Error(
                    "Не вдалося отримати погоду. " +
                            "Перевірте підключення до інтернету."
                )

            } catch (exception: SerializationException) {
                _uiState.value = WeatherUiState.Error(
                    "Не вдалося обробити відповідь сервера."
                )

            } catch (exception: Exception) {
                _uiState.value = WeatherUiState.Error(
                    "Сталася помилка завантаження погоди."
                )

            } finally {
                requestInProgress = false
            }
        }
    }

    private var requestInProgress = false
}
