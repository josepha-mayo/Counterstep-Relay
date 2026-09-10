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

    fun cannotCheck(practice: Practice): String? = when {
        practice.draft.isBlank() -> "Write an expression first. An empty field is not recorded as an attempt."
        practice.attempts.size >= 100 -> "This task has reached its attempt limit. Start another task."
        else -> null
    }

    /** Each hint is recorded before its text is returned, so later hints can be more specific. */
    fun progressiveHint(practice: Practice): String {
        require(practice.hints > 0) { "Record a hint request before displaying a hint." }
        val a = practice.task.coefficient
        val b = practice.task.offset
        return when (practice.hints) {
            1 -> when (focus(practice.task, practice.draft)) {
                Focus.CONSTANT, Focus.COEFFICIENT ->
                    explain(practice.task, practice.draft) + " Multiply the outside coefficient by each term."
                else -> "Multiply the outside coefficient by x AND by the constant."
            }
            2 -> "There are two products: ($a) times x, and ($a) times ($b). Do both before combining your expression."
            else -> when {
                b == 0 -> "The constant product is zero. Keep the x term."
                a < 0 && b < 0 -> "The constant product is positive because two negatives multiply. The x coefficient stays negative."
                a < 0 -> "A negative outside coefficient changes the sign of both terms."
                b < 0 -> "A positive coefficient times a negative constant stays negative."
                else -> "Both factors of the constant are positive. Check its product, not just the value inside the brackets."
            }
        }
    }
    // A past correct attempt does not make a newer, edited draft complete.
    fun hasUnfinishedDraft(practice: Practice): Boolean =
        practice.draft.isNotBlank() && Expansion.check(practice.task, practice.draft) != Verdict.CORRECT
}
