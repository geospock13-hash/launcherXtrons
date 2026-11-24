package com.artefgo.metro.data

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.os.SystemClock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

data class ObdState(
    val status: String = "OBD2 no configurado",
    val connected: Boolean = false,
    val rpm: Int? = null,
    val speedKmh: Int? = null,
    val voltage: Float? = null,
    val coolantC: Int? = null
)

/**
 * Lector de adaptador OBD2 ELM327 por Bluetooth clásico (SPP).
 * Da RPM (010C), velocidad (010D), temperatura del motor (0105) y voltaje de batería (ATRV).
 */
class ObdManager(private val context: Context) {
    private val _state = MutableStateFlow(ObdState())
    val state: StateFlow<ObdState> = _state
    private var job: Job? = null

    @SuppressLint("MissingPermission")
    fun pairedDevices(): List<Pair<String, String>> = try {
        val adapter = context.getSystemService(BluetoothManager::class.java)?.adapter
        adapter?.bondedDevices?.map { (it.name ?: it.address) to it.address }?.sortedBy { it.first }
            ?: emptyList()
    } catch (_: SecurityException) {
        emptyList()
    }

    fun stop() {
        job?.cancel()
        job = null
    }

    @SuppressLint("MissingPermission")
    fun start(scope: CoroutineScope, address: String?) {
        stop()
        if (address == null) {
            _state.value = ObdState()
            return
        }
        job = scope.launch(Dispatchers.IO) {
            while (isActive) {
                var socket: BluetoothSocket? = null
                try {
                    _state.value = ObdState(status = "Conectando OBD2…")
                    val adapter = context.getSystemService(BluetoothManager::class.java)?.adapter
                        ?: throw IOException("sin Bluetooth")
                    val dev = adapter.getRemoteDevice(address)
                    socket = dev.createRfcommSocketToServiceRecord(SPP)
                    socket.connect()
                    val elm = Elm(socket.inputStream, socket.outputStream)
                    elm.cmd("ATZ", 3000)
                    for (c in listOf("ATE0", "ATL0", "ATS0", "ATH0", "ATSP0")) elm.cmd(c)
                    elm.cmd("0100", 6000) // primera consulta: el ELM busca el protocolo
                    var tick = 0
                    while (isActive) {
                        val rpm = parseRpm(elm.cmd("010C"))
                        val spd = parseByte(elm.cmd("010D"), "410D")
                        val prev = _state.value
                        val volt = if (tick % 5 == 0) parseVolt(elm.cmd("ATRV")) else prev.voltage
                        val temp = if (tick % 10 == 0) parseByte(elm.cmd("0105"), "4105")?.minus(40) else prev.coolantC
                        _state.value = ObdState("OBD2 conectado", true, rpm, spd, volt, temp)
                        tick++
                        delay(250)
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    _state.value = ObdState(status = "OBD2: ${e.message ?: "sin conexión"} (reintentando)")
                } finally {
                    runCatching { socket?.close() }
                }
                delay(5000)
            }
        }
    }

    private class Elm(private val inp: InputStream, private val out: OutputStream) {
        fun cmd(c: String, timeoutMs: Long = 1500): String {
            out.write((c + "\r").toByteArray())
            out.flush()
            val sb = StringBuilder()
            val end = SystemClock.elapsedRealtime() + timeoutMs
            while (SystemClock.elapsedRealtime() < end) {
                if (inp.available() > 0) {
                    val b = inp.read()
                    if (b < 0) throw IOException("conexión cerrada")
                    val ch = b.toChar()
                    if (ch == '>') break
                    sb.append(ch)
                } else {
                    Thread.sleep(10)
                }
            }
            return sb.toString().replace("SEARCHING...", "").trim()
        }
    }

    companion object {
        private val SPP: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

        private fun clean(r: String) = r.uppercase().replace(" ", "").replace("\r", "").replace("\n", "")

        fun parseRpm(r: String): Int? {
            val s = clean(r)
            val i = s.indexOf("410C")
            if (i < 0 || s.length < i + 8) return null
            val a = s.substring(i + 4, i + 6).toIntOrNull(16) ?: return null
            val b = s.substring(i + 6, i + 8).toIntOrNull(16) ?: return null
            return (a * 256 + b) / 4
        }

        fun parseByte(r: String, header: String): Int? {
            val s = clean(r)
            val i = s.indexOf(header)
            if (i < 0 || s.length < i + 6) return null
            return s.substring(i + 4, i + 6).toIntOrNull(16)
        }

        fun parseVolt(r: String): Float? =
            Regex("""(\d+(?:\.\d+)?)\s*V""").find(r.uppercase())?.groupValues?.get(1)?.toFloatOrNull()
    }
}
