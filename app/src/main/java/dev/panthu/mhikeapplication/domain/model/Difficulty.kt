package dev.panthu.mhikeapplication.domain.model

enum class Difficulty {
    EASY,
    MEDIUM,
    HARD;

    companion object {
        fun fromString(value: String): Difficulty {
            return when (value.uppercase()) {
                "EASY" -> EASY
                "MEDIUM" -> MEDIUM
                "HARD" -> HARD
                else -> MEDIUM
            }
        }
    }

    override fun toString(): String {
        return when (this) {
            EASY -> "Easy"
            MEDIUM -> "Medium"
            HARD -> "Hard"
        }
    }
}
