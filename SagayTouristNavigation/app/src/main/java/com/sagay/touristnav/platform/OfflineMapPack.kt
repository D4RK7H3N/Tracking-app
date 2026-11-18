package com.sagay.touristnav.platform

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.osmdroid.tileprovider.modules.OfflineTileProvider
import org.osmdroid.tileprovider.util.SimpleRegisterReceiver
import org.osmdroid.tileprovider.modules.SqliteArchiveTileWriter
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.MapView
import java.io.File
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.tan

class OfflineMapPack(private val context: Context) {

    private val packFile: File
        get() = File(context.filesDir, "sagay_offline.sqlite")

    fun isDownloaded(): Boolean = packFile.exists() && packFile.length() > 0L

    fun sizeBytes(): Long = if (packFile.exists()) packFile.length() else 0L

    fun tileCount(): Int {
        var count = 0
        for (zoom in MIN_ZOOM..MAX_ZOOM) {
            val (xMin, xMax, yMin, yMax) = tileRange(zoom)
            count += (xMax - xMin + 1) * (yMax - yMin + 1)
        }
        return count
    }

    suspend fun delete() = withContext(Dispatchers.IO) {
        if (packFile.exists()) packFile.delete()
    }

    suspend fun download(onProgress: (Int, Int) -> Unit) = withContext(Dispatchers.IO) {
        val total = tileCount()
        val writer = SqliteArchiveTileWriter(packFile.absolutePath)
        val client = OkHttpClient()
        var done = 0
        try {
            for (zoom in MIN_ZOOM..MAX_ZOOM) {
                val (xMin, xMax, yMin, yMax) = tileRange(zoom)
                for (x in xMin..xMax) {
                    for (y in yMin..yMax) {
                        val request = Request.Builder()
                            .url("https://tile.openstreetmap.org/$zoom/$x/$y.png")
                            .build()
                        client.newCall(request).execute().use { response ->
                            if (response.isSuccessful) {
                                response.body?.byteStream()?.use { stream ->
                                    writer.saveFile(
                                        TileSourceFactory.MAPNIK,
                                        MapTileIndex.getTileIndex(zoom, x, y),
                                        stream,
                                        null,
                                    )
                                }
                            }
                        }
                        done++
                        onProgress(done, total)
                    }
                }
            }
        } finally {
            writer.onDetach()
        }
    }

    fun activate(mapView: MapView) {
        if (isDownloaded()) {
            mapView.setTileProvider(
                OfflineTileProvider(SimpleRegisterReceiver(context), arrayOf(packFile)),
            )
        }
    }

    private fun tileRange(zoom: Int): IntArray {
        val xMin = longitudeToTile(WEST, zoom)
        val xMax = longitudeToTile(EAST, zoom)
        val yMin = latitudeToTile(NORTH, zoom)
        val yMax = latitudeToTile(SOUTH, zoom)
        return intArrayOf(xMin, xMax, yMin, yMax)
    }

    private fun longitudeToTile(longitude: Double, zoom: Int): Int =
        ((longitude + 180.0) / 360.0 * 2.0.pow(zoom)).toInt()

    private fun latitudeToTile(latitude: Double, zoom: Int): Int {
        val radians = Math.toRadians(latitude)
        return ((1.0 - ln(tan(radians) + 1.0 / cos(radians)) / Math.PI) / 2.0 * 2.0.pow(zoom)).toInt()
    }

    companion object {
        private const val SOUTH = 10.78
        private const val NORTH = 10.99
        private const val WEST = 123.34
        private const val EAST = 123.58
        private const val MIN_ZOOM = 12
        private const val MAX_ZOOM = 14
    }
}
