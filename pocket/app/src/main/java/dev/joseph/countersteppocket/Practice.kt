package dev.joseph.countersteppocket

/** A deliberately narrow integer-linear expansion checker, not a general CAS. */
data class Task(val coefficient: Int, val offset: Int) {
 init { require(coefficient in -9..9 && coefficient != 0 && offset in -9..9) }
 val expression: String get() = "$coefficient(x${if (offset < 0) "" else "+"}$offset)"
 val target: Pair<Long,Long> get() = coefficient.toLong() to coefficient.toLong()*offset
}
enum class Verdict { CORRECT, DIFFERENT, UNSUPPORTED }
object Expansion {
 private val terms = Regex("[+-]?(?:[0-9]+\\*?x|x|[0-9]+)")
 fun parse(raw: String): Pair<Long,Long>? {
  if (raw.length > 120 || raw.any { it !in "0123456789x*+- \t\r\n" }) return null
  val s=raw.filterNot { it.isWhitespace() }
  if (s.isEmpty()) return null
  var position=0; var a=0L; var b=0L; var count=0
  for (m in terms.findAll(s)) {
   if (m.range.first!=position || (position>0 && m.value[0]!='+' && m.value[0]!='-') || ++count>16) return null
   val t=m.value; val variable=t.endsWith('x')
   val n=if(variable) t.dropLast(1).removeSuffix("*") else t
   val value=when(n){"","+"->1L;"-"->-1L;else->n.toLongOrNull()?:return null}
   if(value !in -1000000L..1000000L) return null
   if(variable)a+=value else b+=value
   position=m.range.last+1
  }
  return if(position==s.length) a to b else null
 }
 fun check(task: Task, raw: String): Verdict = parse(raw)?.let { if(it==task.target) Verdict.CORRECT else Verdict.DIFFERENT }?:Verdict.UNSUPPORTED
}
data class Attempt(val text: String, val hintsBefore: Int)
class Practice(val task: Task, val attempts: MutableList<Attempt> = mutableListOf(), var hints: Int=0, var draft: String="") {
 init { require(hints in 0..100 && attempts.size<=100); require(attempts.all { it.text.length<=120 && it.hintsBefore in 0..hints }) }
 fun hint(){ require(hints<100); hints++ }
 fun submit(): Verdict { require(draft.length<=120 && attempts.size<100); attempts.add(Attempt(draft,hints)); return Expansion.check(task,draft) }
 val solved: Boolean get() = attempts.any { Expansion.check(task,it.text)==Verdict.CORRECT }
 val independentlyCorrectFirstTry: Boolean get() = attempts.firstOrNull()?.let { it.hintsBefore==0 && Expansion.check(task,it.text)==Verdict.CORRECT }?:false
 fun history(): String = if(attempts.isEmpty()) "No response yet." else attempts.mapIndexed { i,a -> "${i+1}. ${a.text} | ${Expansion.check(task,a.text).name.lowercase()} | ${a.hintsBefore} hint(s) before response" }.joinToString("\n")
}
/** Entitlement is never restored from a study-session file or a local 'purchase succeeded' flag. */
class AccessState {
 var active: Boolean=false; private set
 var checked: Boolean=false; private set
 fun fromSdkEntitlements(ids: Set<String>) { checked=true; active="mixed_signs" in ids }
 fun failedRefresh(){checked=false;active=false}
}
