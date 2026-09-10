package dev.joseph.countersteppocket

/** Targeted feedback from exact term comparison, not an LLM or a claim about a learner's intent. */
object Coaching {
    enum class Focus { MATCH, CONSTANT, COEFFICIENT, BOTH, FORMAT }
    fun focus(task: Task, raw: String): Focus {
        val got = Expansion.parse(raw) ?: return Focus.FORMAT
        val expected = task.target
        return when {
            got == expected -> Focus.MATCH
            got.first == expected.first -> Focus.CONSTANT
            got.second == expected.second -> Focus.COEFFICIENT
            else -> Focus.BOTH
        }
    }
    fun explain(task: Task, raw: String): String = when (focus(task, raw)) {
        Focus.MATCH -> "Both expanded terms agree."
        Focus.CONSTANT -> "The x term agrees. Recheck the constant: the outside coefficient multiplies it too."
        Focus.COEFFICIENT -> "The constant term agrees. Recheck the coefficient of x."
        Focus.BOTH -> "Neither term agrees yet. Distribute the outside coefficient to each term separately."
        Focus.FORMAT -> "Use an expanded expression with x and integer terms, not an equation or brackets."
    }
    fun hint(task: Task, raw: String): String = when (focus(task, raw)) {
        Focus.MATCH -> "Both terms agree. Explain to yourself why the outside coefficient multiplies each term."
        Focus.CONSTANT, Focus.COEFFICIENT -> explain(task, raw)
        else -> "Multiply the outside coefficient by x AND by the constant. Keep track of the sign."
    }
    // A past correct attempt does not make a newer, edited draft complete.
    fun hasUnfinishedDraft(practice: Practice): Boolean =
        practice.draft.isNotBlank() && Expansion.check(practice.task, practice.draft) != Verdict.CORRECT
}
