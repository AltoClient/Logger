package com.jacobtread.mck.logger

/**
 * Level Represents a level of logging. Setting the loggers
 * level to a level with a lower index prevents the logging
 * of levels with a higher index
 *
 * @property levelName The name of this level
 * @property index The index / height of this level
 * @constructor Create empty Level
 */
enum class Level(val levelName: String, val index: Byte) {
    INFO("INFO", 3),
    WARN("WARN", 2),
    FATAL("FATAL", 1),
    DEBUG("DEBUG", 4)
}