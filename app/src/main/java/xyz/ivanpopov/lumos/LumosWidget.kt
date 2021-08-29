package xyz.ivanpopov.lumos

import android.Manifest
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.widget.RemoteViews
import android.widget.Toast
import androidx.core.app.ActivityCompat

/**
 * Implementation of App Widget functionality.
 * App Widget Configuration implemented in [LumosWidgetConfigureActivity]
 */

var isTorchEnabled = false

const val ON_CLICK_BULB = "onCLickBulb"
lateinit var widgetIds: MutableSet<Int>

class LumosWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        // There may be multiple widgets active, so update all of them

        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)

            if (!widgetIds.contains(appWidgetId)) {
                widgetIds.add(appWidgetId)
            }
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        context?.let {
            if (ON_CLICK_BULB == intent?.action) {
                for (id in widgetIds) {
                    updateAppWidget(it, AppWidgetManager.getInstance(context), id)
                }
                isTorchEnabled = !isTorchEnabled
            }
        }
        super.onReceive(context, intent)
    }


    override fun onEnabled(context: Context) {
        widgetIds = mutableSetOf()
    }

    override fun onDisabled(context: Context) {
        // Enter relevant functionality for when the last widget is disabled
    }
}


internal fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
    val views = RemoteViews(context.packageName, R.layout.lumos_widget)
    if (isTorchEnabled) {
        views.setImageViewResource(R.id.imageButtonWidget, R.drawable.ic_bulb_on)
        torchEnable(context)
    } else {
        views.setImageViewResource(R.id.imageButtonWidget, R.drawable.ic_bulb_off_transparent)
        torchEnable(context)
    }
    val intent = Intent(context, LumosWidget::class.java)
    intent.action = ON_CLICK_BULB
    val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0)
    views.setOnClickPendingIntent(R.id.imageButtonWidget, pendingIntent)
    appWidgetManager.updateAppWidget(appWidgetId, views)
}

fun torchEnable(context: Context) {
    // Check if the Camera permission has been granted
    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
        PackageManager.PERMISSION_GRANTED
    ) {
        val cameraManager: CameraManager by lazy {
            context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        }
        for (camera in cameraManager.cameraIdList) {
            if (cameraManager.getCameraCharacteristics(camera).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true) {
                cameraManager.setTorchMode(camera, isTorchEnabled)
            }
        }
    } else {
        Toast.makeText(context, "No camera permission", Toast.LENGTH_SHORT).show()
    }
}