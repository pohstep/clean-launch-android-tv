package app.cleanlaunch.tv.sentence

data class ClauseBit(val id: String, val label: String, val priority: Int)

data class FittedSentence(
    val lines: List<String>,
    val dropped: List<String>,
)

/**
 * Fits the sentence to [maxLines]. A line never holds a single word when the line
 * above can spare one. If the paragraph is still too long, clauses leave in
 * priority order, lowest first.
 */
object SentenceLayout {
    fun fitClauses(
        bits: List<ClauseBit>,
        data: SentenceData,
        maxWidth: Float,
        measure: (String) -> Float,
        maxLines: Int = 7,
    ): FittedSentence {
        val alive = bits.toMutableList()
        val dropped = mutableListOf<String>()
        while (true) {
            val text = SentenceComposer.prose(alive.map { it.id }, data)
            val wrapped = wrap(text, maxWidth, measure)
            if (wrapped.size <= maxLines || alive.size <= 1) {
                val lines = if (wrapped.size > maxLines) wrapped.take(maxLines) else wrapped
                return FittedSentence(lines.ifEmpty { emptyList() }, dropped.toList())
            }
            val victim = alive.minBy { it.priority }
            alive.removeAll { it.id == victim.id }
            dropped += victim.label
        }
    }

    fun fitTemplate(
        template: String,
        values: Map<String, String>,
        maxWidth: Float,
        measure: (String) -> Float,
        maxLines: Int = 7,
    ): FittedSentence {
        val active = values.toMutableMap()
        val dropped = mutableListOf<String>()
        while (true) {
            val text = SentenceComposer.applyTemplate(template, active)
            val wrapped = wrap(text, maxWidth, measure)
            if (wrapped.size <= maxLines) {
                return FittedSentence(wrapped.ifEmpty { emptyList() }, dropped.toList())
            }
            val victim = Clauses.tokenDropOrder.firstOrNull { token ->
                template.contains("{$token}") && active[token].orEmpty().isNotEmpty()
            }
            if (victim == null) {
                return FittedSentence(wrapped.take(maxLines), dropped.toList())
            }
            active[victim] = ""
            dropped += Clauses.tokenLabel(victim)
        }
    }

    fun wrap(text: String, maxWidth: Float, measure: (String) -> Float): List<String> {
        val words = text.split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (words.isEmpty()) return emptyList()
        val lines = mutableListOf<String>()
        var current = mutableListOf<String>()
        for (word in words) {
            val trial = if (current.isEmpty()) listOf(word) else current + word
            if (current.isEmpty() || measure(trial.joinToString(" ")) <= maxWidth) {
                current = trial.toMutableList()
            } else {
                lines += current.joinToString(" ")
                current = mutableListOf(word)
            }
        }
        if (current.isNotEmpty()) lines += current.joinToString(" ")
        return fixOrphans(lines, maxWidth, measure)
    }

    private fun fixOrphans(
        lines: List<String>,
        maxWidth: Float,
        measure: (String) -> Float,
    ): List<String> {
        if (lines.size < 2) return lines
        val out = lines.toMutableList()
        var index = 1
        var guard = 0
        while (index < out.size && guard < 64) {
            guard++
            val words = out[index].split(" ")
            if (words.size != 1) {
                index++
                continue
            }
            val previous = out[index - 1].split(" ").toMutableList()
            if (previous.size < 2) {
                index++
                continue
            }
            val moved = previous.removeAt(previous.lastIndex)
            val newPrevious = previous.joinToString(" ")
            val newCurrent = "$moved ${out[index]}"
            if (measure(newCurrent) <= maxWidth && measure(newPrevious) <= maxWidth) {
                out[index - 1] = newPrevious
                out[index] = newCurrent
                if (index > 1) index--
                continue
            }
            index++
        }
        return out
    }
}
