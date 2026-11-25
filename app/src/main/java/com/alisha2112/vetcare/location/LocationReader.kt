package com.alisha2112.vetcare.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat

class LocationReader(
    private val context: Context
) {
    private fun hasPermission(permission: String): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    fun getCurrentLocation(
        onSuccess: (Location) -> Unit,
        onError: (String) -> Unit
    ): CancellationSignal? {
        val hasFine = hasPermission(
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        val hasCoarse = hasPermission(
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        if (!hasFine && !hasCoarse) {
            onError("Не надано дозвіл на геолокацію")
            return null
        }

        val manager = context.getSystemService(
            Context.LOCATION_SERVICE
        ) as? LocationManager

        if (manager == null) {
            onError("Служба геолокації недоступна")
            return null
        }

        try {
            if (!LocationManagerCompat.isLocationEnabled(manager)) {
                onError("Увімкніть геолокацію в налаштуваннях Android")
                return null
            }

            val provider = when {
                hasFine && manager.isProviderEnabled(
                    LocationManager.GPS_PROVIDER
                ) -> LocationManager.GPS_PROVIDER

                manager.isProviderEnabled(
                    LocationManager.NETWORK_PROVIDER
                ) -> LocationManager.NETWORK_PROVIDER

                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                        manager.isProviderEnabled(
                            LocationManager.FUSED_PROVIDER
                        ) -> LocationManager.FUSED_PROVIDER

                else -> null
            }

            if (provider == null) {
                onError(
                    "Немає доступного джерела координат. " +
                            "Перевірте GPS або мережеву геолокацію."
                )
                return null
            }

            val cancellationSignal = CancellationSignal()

            LocationManagerCompat.getCurrentLocation(
                manager,
                provider,
                cancellationSignal,
                ContextCompat.getMainExecutor(context)
            ) { location ->
                if (location == null) {
                    onError(
                        "Не вдалося визначити координати. " +
                                "Спробуйте ще раз."
                    )
                } else {
                    onSuccess(Location(location))
                }
            }

            return cancellationSignal
        } catch (exception: SecurityException) {
            onError("Доступ до геолокації заборонений")
        } catch (exception: Exception) {
            onError(
                "Помилка геолокації: " +
                        (exception.message ?: "невідома")
            )
        }

        return null
    }
}
