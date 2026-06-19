package com.example.data

data class SpeechProfile(
    val name: String,
    val greenSeconds: Int,
    val yellowSeconds: Int,
    val redSeconds: Int,
    val maxSeconds: Int,
    val isCustom: Boolean = false
) {
    companion object {
        val TableTopics = SpeechProfile(
            name = "Table Topics",
            greenSeconds = 60,       // 1:00
            yellowSeconds = 90,      // 1:30
            redSeconds = 120,        // 2:00
            maxSeconds = 150         // 2:30 (disqualification)
        )

        val Evaluation = SpeechProfile(
            name = "Speech Evaluation",
            greenSeconds = 120,      // 2:00
            yellowSeconds = 150,      // 2:30
            redSeconds = 180,        // 3:00
            maxSeconds = 210         // 3:30 (disqualification)
        )

        val IceBreaker = SpeechProfile(
            name = "Ice Breaker (4-6 min)",
            greenSeconds = 240,      // 4:00
            yellowSeconds = 300,      // 5:00
            redSeconds = 360,        // 6:00
            maxSeconds = 390         // 6:30 (disqualification)
        )

        val PreparedSpeech = SpeechProfile(
            name = "Prepared Speech (5-7 min)",
            greenSeconds = 300,      // 5:00
            yellowSeconds = 360,      // 6:00
            redSeconds = 420,        // 7:00
            maxSeconds = 450         // 7:30 (disqualification)
        )

        val defaultProfiles = listOf(
            TableTopics,
            Evaluation,
            IceBreaker,
            PreparedSpeech
        )
    }
}
