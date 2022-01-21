package com.jacobtread.mck.logger

import java.io.BufferedOutputStream
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.GZIPOutputStream
import kotlin.io.path.*

class Logger {
    companion object {
        private val ROOT = Logger()
        const val DEFAULT_BUFFER_SIZE = 256 * 1024

        fun setLogLevel(level: Level) {
            ROOT.logLevel = level
        }

        fun get(): Logger {
            return ROOT
        }
    }

    private val printDateFormat = SimpleDateFormat("HH:mm:ss")
    private val loggingPath: Path = Paths.get("logs")
    private val logFile: Path = loggingPath.resolve("latest.log")
    private val file: RandomAccessFile
    private val outputBuffer = ByteBuffer.allocate(DEFAULT_BUFFER_SIZE)
    var logLevel: Level = Level.INFO

    init {
        archiveOld()
        try {
            file = createFile()
        } catch (e: IOException) {
            System.err.println("Failed to open RandomAccessFile to the logging path")
            throw e
        }
        Runtime.getRuntime().addShutdownHook(Thread(this::close))
    }

    @JvmOverloads
    fun info(text: String, throwable: Throwable? = null) = append(Level.INFO, text, throwable)

    @JvmOverloads
    fun warn(text: String, throwable: Throwable? = null) = append(Level.WARN, text, throwable)

    @JvmOverloads
    fun fatal(text: String, throwable: Throwable? = null) = append(Level.FATAL, text, throwable)

    @JvmOverloads
    fun debug(text: String, throwable: Throwable? = null) = append(Level.DEBUG, text, throwable)

    @JvmOverloads
    fun log(level: Level, text: String, throwable: Throwable? = null) = append(level, text, throwable)

    /**
     * close Called when the application is closing. Flushes
     * the contents of the buffer and closes the [file]
     */
    private fun close() {
        try {
            flush()
            file.close()
        } catch (_: Exception) {
        }
    }

    /**
     * flush Flushes the contents of the [outputBuffer] to
     * the file and clears the buffer
     */
    private fun flush() {
        outputBuffer.flip()
        try {
            file.write(
                outputBuffer.array(),
                outputBuffer.arrayOffset() + outputBuffer.position(),
                outputBuffer.remaining()
            )
        } finally {
            outputBuffer.clear()
        }
    }

    /**
     * append Appends a new log entry
     *
     * @param level The level of the log
     * @param message The message of the log
     * @param throwable An optional exception passed to the log
     */
    private fun append(level: Level, message: String, throwable: Throwable?) {
        if (level.index > logLevel.index) return
        val time = printDateFormat.format(Date())
        val threadName = Thread.currentThread().name
        val text = "[$time] [$threadName/${level.levelName}] $message"
        write(text)
        if (throwable != null) {
            write(throwable.stackTraceToString())
        }
    }

    /**
     * write Writes the provided text to the [outputBuffer]
     * but if it cannot fit the output buffer will be flushed
     * to the [file] along with the bytes
     *
     * @param text The text to write to the log
     */
    private fun write(text: String) {
        val bytes = text.toByteArray()
        if (bytes.size > outputBuffer.remaining()) {
            flush()
            file.write(bytes)
        } else {
            outputBuffer.put(bytes)
        }
    }


    /**
     * createFile Creates a new logging file and
     * opens a random access file to that path
     */
    private fun createFile(): RandomAccessFile {
        if (!logFile.exists()) logFile.createFile()
        val randomAccess = RandomAccessFile(logFile.toFile(), "rw")
        val length = randomAccess.length()
        randomAccess.seek(length)
        return randomAccess
    }

    /**
     * archiveOld Archives any existing latest.log files as
     * logs/{yyyy-MM-dd}-{i}.log.gz this is stored using the
     * gzip file format.
     */
    private fun archiveOld() {
        if (logFile.isRegularFile()) {
            val lastModified = logFile.getLastModifiedTime().toMillis()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd")
            val date = dateFormat.format(Date(lastModified))
            var file: Path? = null
            var i = 1
            while (file == null) {
                val path = loggingPath.resolve("$date-$i.log.gz")
                if (path.exists()) {
                    i++
                    continue
                }
                file = path
            }
            logFile.inputStream().use { input ->
                GZIPOutputStream(BufferedOutputStream(file.outputStream(StandardOpenOption.CREATE))).use { output ->
                    var read: Int
                    val buffer = ByteArray(2048)
                    while (true) {
                        read = input.read(buffer)
                        if (read == -1) break
                        output.write(buffer)
                    }
                }
            }
            logFile.deleteIfExists()
        }
    }
}