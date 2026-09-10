package dev.joseph.countersteppocket
import org.junit.Assert.*
import org.junit.Test
class PracticeTest {
 @Test fun correctExpansion(){assertEquals(Verdict.CORRECT,Expansion.check(Task(2,3),"2x+6"))}
 @Test fun constantFirst(){assertEquals(Verdict.CORRECT,Expansion.check(Task(2,3),"6+2*x"))}
 @Test fun detectsCommonError(){assertEquals(Verdict.DIFFERENT,Expansion.check(Task(2,3),"2x+3"))}
 @Test fun negativeOutside(){assertEquals(Verdict.CORRECT,Expansion.check(Task(-3,-2),"6-3x"))}
 @Test fun noEquation(){assertEquals(Verdict.UNSUPPORTED,Expansion.check(Task(2,3),"x=2"))}
 @Test fun unexpandedNotAccepted(){assertEquals(Verdict.UNSUPPORTED,Expansion.check(Task(2,3),"2(x+3)"))}
 @Test fun doubleOperator(){assertNull(Expansion.parse("2x++6"))}
 @Test fun missingOperator(){assertNull(Expansion.parse("x2"))}
 @Test fun unicodeDigits(){assertNull(Expansion.parse("２x+6"))}
 @Test fun otherVariable(){assertNull(Expansion.parse("2y+6"))}
 @Test fun noCodeEvaluation(){assertNull(Expansion.parse("System.exit(0)"))}
 @Test fun overflowRejected(){assertNull(Expansion.parse("999999999999999999999999999x"))}
 @Test fun multiplicationNotCollapsed(){assertNull(Expansion.parse("2**x+6"))}
 @Test fun excessiveLength(){assertNull(Expansion.parse("x".repeat(121)))}
 @Test fun excessiveTerms(){assertNull(Expansion.parse("x"+"+1".repeat(20)))}
 @Test fun emptyRejected(){assertNull(Expansion.parse("   "))}
 @Test fun minusX(){assertEquals(-1L to 3L,Expansion.parse("-x+3"))}
 @Test fun generatedGrid(){for(a in -9..9)if(a!=0)for(b in -9..9){val c=a*b;val text="${a}x${if(c<0)"" else "+"}$c";assertEquals(Verdict.CORRECT,Expansion.check(Task(a,b),text))}}
 @Test fun firstResponseIsPreserved(){val p=Practice(Task(2,3));p.draft="2x+3";p.submit();p.draft="2x+6";p.submit();assertTrue(p.solved);assertFalse(p.independentlyCorrectFirstTry);assertEquals("2x+3",p.attempts.first().text)}
 @Test fun hintTaintsFirstTry(){val p=Practice(Task(2,3));p.hint();p.draft="2x+6";p.submit();assertTrue(p.solved);assertFalse(p.independentlyCorrectFirstTry)}
 @Test fun independentFirstTry(){val p=Practice(Task(2,3));p.draft="2x+6";p.submit();assertTrue(p.independentlyCorrectFirstTry)}
 @Test fun restoredVerdictRecomputed(){val p=Practice(Task(2,3),mutableListOf(Attempt("2x+3",0)));assertFalse(p.solved)}
 @Test fun unrelatedEntitlementDoesNotUnlock(){val a=AccessState();a.fromSdkEntitlements(setOf("other"));assertFalse(a.active)}
 @Test fun intendedEntitlementUnlocks(){val a=AccessState();a.fromSdkEntitlements(setOf("mixed_signs"));assertTrue(a.active)}
 @Test fun failedRefreshDoesNotInventAccess(){val a=AccessState();a.fromSdkEntitlements(setOf("mixed_signs"));a.failedRefresh();assertFalse(a.active);assertFalse(a.checked)}
 @Test fun noAccessByDefault(){assertFalse(AccessState().active)}
 @Test(expected=IllegalArgumentException::class) fun invalidTask(){Task(0,3)}
 @Test(expected=IllegalArgumentException::class) fun invalidHintHistory(){Practice(Task(2,3),mutableListOf(Attempt("2x+6",2)),0)}
}
