package com.alisha2112.vetcare.ui.weather

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alisha2112.vetcare.data.cache.WeatherCacheRepository
import com.alisha2112.vetcare.data.model.WeatherReport
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
        val report: WeatherReport,
        val isStale: Boolean = false,
        val isRefreshing: Boolean = false,
        val warning: String? = null
    ) : WeatherUiState

    data class Error(
        val message: String
    ) : WeatherUiState
}

@HiltViewModel
class WeatherViewModel @Inject constructor(
    private val repository: WeatherRepository,
    private val cacheRepository: WeatherCacheRepository
) : ViewModel() {

    private val _uiState =
        MutableStateFlow<WeatherUiState>(
            WeatherUiState.Loading
        )

    val uiState: StateFlow<WeatherUiState> =
        _uiState.asStateFlow()

    private var requestInProgress = false

    init {
        refresh()
    }

    fun refresh() {
        if (requestInProgress) return

        requestInProgress = true

        viewModelScope.launch {
            try {
                val cached = try {
                    cacheRepository.getCachedWeather()
                } catch (exception: CancellationException) {
                    throw exception
                } catch (exception: Exception) {
                    null
                }

                if (cached != null) {
                    _uiState.value = WeatherUiState.Success(
                        report = cached,
                        isStale = true,
                        isRefreshing = true
                    )
                } else {
                    _uiState.value = WeatherUiState.Loading
                }

                try {
                    val freshReport =
                        repository.getWeatherReport()

                    val cacheSaved = try {
                        cacheRepository.saveWeather(freshReport)
                        true
                    } catch (exception: CancellationException) {
                        throw exception
                    } catch (exception: Exception) {
                        false
                    }

                    _uiState.value = WeatherUiState.Success(
                        report = freshReport,
                        isStale = false,
                        warning = if (cacheSaved) {
                            null
                        } else {
                            "Не вдалося зберегти офлайн-копію."
                        }
                    )

                } catch (exception: CancellationException) {
                    throw exception

                } catch (exception: Exception) {
                    val message = errorMessage(exception)

                    _uiState.value =
                        if (cached != null) {
                            WeatherUiState.Success(
                                report = cached,
                                isStale = true,
                                warning = "Не вдалося оновити: $message"
                            )
                        } else {
                            WeatherUiState.Error(message)
                        }
                }
            } finally {
                requestInProgress = false
            }
        }
    }

    private fun errorMessage(exception: Exception): String =
        when (exception) {
            is HttpException ->
                "Помилка сервера: HTTP ${exception.code()}"

            is IOException ->
                "Перевірте підключення до інтернету."

            is SerializationException ->
                "Не вдалося обробити відповідь сервера."

            else ->
                "Сталася помилка завантаження погоди."
        }
}
