package dev.joseph.countersteppocket
import org.junit.Assert.*
import org.junit.Test

class CoachingTest {
 @Test fun identifiesConstantOnly(){assertEquals(Coaching.Focus.CONSTANT,Coaching.focus(Task(2,3),"2x+3"))}
 @Test fun identifiesCoefficientOnly(){assertEquals(Coaching.Focus.COEFFICIENT,Coaching.focus(Task(2,3),"3x+6"))}
 @Test fun identifiesBoth(){assertEquals(Coaching.Focus.BOTH,Coaching.focus(Task(2,3),"3x+5"))}
 @Test fun acceptsConstantFirst(){assertEquals(Coaching.Focus.MATCH,Coaching.focus(Task(2,3),"6+2x"))}
 @Test fun rejectsUnsupportedEquation(){assertEquals(Coaching.Focus.FORMAT,Coaching.focus(Task(2,3),"x=3"))}
 @Test fun preservesWhitespaceParserCorrection(){assertEquals(Coaching.Focus.FORMAT,Coaching.focus(Task(2,3),"2x+1 2"))}
 @Test fun negativeCoefficientUsesExactAlgebra(){assertEquals(Coaching.Focus.CONSTANT,Coaching.focus(Task(-3,-2),"-3x-6"))}
 @Test fun zeroOffsetIsSupported(){assertEquals(Coaching.Focus.MATCH,Coaching.focus(Task(2,0),"2x"))}
 @Test fun combinesTermsRatherThanJudgesSurface(){assertEquals(Coaching.Focus.MATCH,Coaching.focus(Task(2,3),"x+x+8-2"))}
 @Test fun feedbackDoesNotSupplySolution(){assertFalse(Coaching.explain(Task(2,3),"2x+3").contains("2x+6"))}
 @Test fun oldSuccessDoesNotCompleteNewDraft(){val p=Practice(Task(2,3));p.draft="2x+6";p.submit();p.draft="2x+3";assertTrue(p.solved);assertTrue(Coaching.hasUnfinishedDraft(p))}
 @Test fun hintDoesNotMutateWork(){val p=Practice(Task(2,3));p.draft="3x+6";val before=p.workStamp(0);Coaching.hint(p.task,p.draft);assertEquals(before,p.workStamp(0))}
}
