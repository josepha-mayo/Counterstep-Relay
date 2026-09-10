package dev.joseph.countersteppocket
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/** JUnit bridge for the same scenarios used by the offline Kotlin runner. */
@RunWith(Parameterized::class)
class BillingGateTest(private val label: String, private val exercise: () -> Unit) {
    companion object {
        @JvmStatic @Parameterized.Parameters(name="{0}")
        fun cases(): Collection<Array<Any>> = BillingChecks.scenarios().map { (name, body) ->
            arrayOf<Any>(name, body)
        }
    }
    @Test fun executesScenario() = exercise()
}
