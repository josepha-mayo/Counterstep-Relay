package dev.joseph.countersteppocket
import kotlin.test.*

private class Clock(var elapsed:Long=1000,var wall:Long=1_000_000){
 fun tick(ms:Long){elapsed+=ms;wall+=ms}
 fun gate()=BillingGate({elapsed},{wall}).apply{onForeground()}
}
object BillingChecks {
 fun scenarios():Map<String,()->Unit> = linkedMapOf(
  "default is not premium" to {val c=Clock();assertFalse(c.gate().active)},
  "no request starts before foreground" to {val c=Clock();assertNull(BillingGate({c.elapsed},{c.wall}).begin(BillingGate.Kind.REFRESH))},
  "only one operation in flight" to {val g=Clock().gate();assertNotNull(g.begin(BillingGate.Kind.REFRESH));assertNull(g.begin(BillingGate.Kind.PURCHASE))},
  "fresh SDK entitlement grants temporary access" to {val g=Clock().gate();val t=g.begin(BillingGate.Kind.REFRESH)!!;assertTrue(g.finishAccess(t,true,null));assertTrue(g.active)},
  "missing entitlement never grants access" to {val g=Clock().gate();val t=g.begin(BillingGate.Kind.REFRESH)!!;assertTrue(g.finishAccess(t,false,null));assertFalse(g.active)},
  "expiry in the past refuses access" to {val c=Clock();val g=c.gate();g.finishAccess(g.begin(BillingGate.Kind.REFRESH)!!,true,c.wall-1);assertFalse(g.active)},
  "expiry at the current instant refuses access" to {val c=Clock();val g=c.gate();g.finishAccess(g.begin(BillingGate.Kind.REFRESH)!!,true,c.wall);assertFalse(g.active)},
  "expiry is enforced without another callback" to {val c=Clock();val g=c.gate();g.finishAccess(g.begin(BillingGate.Kind.REFRESH)!!,true,c.wall+2000);assertTrue(g.active);c.tick(2000);assertFalse(g.active)},
  "non-expiring SDK snapshot still has local freshness limit" to {val c=Clock();val g=c.gate();g.finishAccess(g.begin(BillingGate.Kind.REFRESH)!!,true,null);c.tick(59_999);assertTrue(g.active);c.tick(1);assertFalse(g.active)},
  "new refresh suspends stale access while waiting" to {val g=Clock().gate();g.finishAccess(g.begin(BillingGate.Kind.REFRESH)!!,true,null);g.begin(BillingGate.Kind.REFRESH);assertFalse(g.active)},
  "fresh inactive response removes previous access" to {val g=Clock().gate();g.finishAccess(g.begin(BillingGate.Kind.REFRESH)!!,true,null);g.finishAccess(g.begin(BillingGate.Kind.REFRESH)!!,false,null);assertFalse(g.active)},
  "failed refresh never unlocks" to {val g=Clock().gate();val t=g.begin(BillingGate.Kind.REFRESH)!!;assertTrue(g.fail(t));assertFalse(g.active);assertFalse(g.busy)},
  "background immediately invalidates access" to {val g=Clock().gate();g.finishAccess(g.begin(BillingGate.Kind.REFRESH)!!,true,null);g.onBackground();assertFalse(g.active)},
  "resume requires another access result" to {val g=Clock().gate();g.finishAccess(g.begin(BillingGate.Kind.REFRESH)!!,true,null);g.onBackground();g.onForeground();assertFalse(g.active)},
  "background callback is ignored" to {val g=Clock().gate();val t=g.begin(BillingGate.Kind.REFRESH)!!;g.onBackground();assertFalse(g.finishAccess(t,true,null))},
  "old callback after resume is ignored" to {val g=Clock().gate();val t=g.begin(BillingGate.Kind.REFRESH)!!;g.onBackground();g.onForeground();g.begin(BillingGate.Kind.REFRESH);assertFalse(g.finishAccess(t,true,null));assertTrue(g.busy)},
  "destroyed owner cannot restart" to {val g=Clock().gate();g.destroy();g.onForeground();assertNull(g.begin(BillingGate.Kind.REFRESH));assertFalse(g.active)},
  "success callback is single use" to {val g=Clock().gate();val t=g.begin(BillingGate.Kind.REFRESH)!!;assertTrue(g.finishAccess(t,true,null));assertFalse(g.finishAccess(t,false,null));assertTrue(g.active)},
  "late error cannot overwrite accepted success" to {val g=Clock().gate();val t=g.begin(BillingGate.Kind.REFRESH)!!;g.finishAccess(t,true,null);assertFalse(g.fail(t));assertTrue(g.active)},
  "late success cannot overwrite accepted failure" to {val g=Clock().gate();val t=g.begin(BillingGate.Kind.REFRESH)!!;g.fail(t);assertFalse(g.finishAccess(t,true,null));assertFalse(g.active)},
  "ticket from a different owner is rejected" to {val c=Clock();val a=c.gate();val b=c.gate();val x=a.begin(BillingGate.Kind.REFRESH)!!;val y=b.begin(BillingGate.Kind.REFRESH)!!;assertFalse(b.finishAccess(x,true,null));assertTrue(b.finishAccess(y,true,null))},
  "fabricated matching ticket is rejected" to {val c=Clock();val g=c.gate();g.begin(BillingGate.Kind.REFRESH);assertFalse(g.finishAccess(BillingGate.Ticket(BillingGate.Kind.REFRESH,c.elapsed),true,null))},
  "offerings cannot manufacture entitlement" to {val g=Clock().gate();val t=g.begin(BillingGate.Kind.OFFERINGS)!!;assertFalse(g.finishAccess(t,true,null));assertFalse(g.active);assertTrue(g.finishOfferings(t))},
  "access reply cannot be consumed as offerings" to {val g=Clock().gate();val t=g.begin(BillingGate.Kind.RESTORE)!!;assertFalse(g.finishOfferings(t));assertTrue(g.finishAccess(t,true,null))},
  "restore starts without fetching offerings" to {val g=Clock().gate();val t=g.begin(BillingGate.Kind.RESTORE)!!;assertTrue(g.finishAccess(t,true,null));assertTrue(g.active)},
  "restore remains possible after offering error" to {val g=Clock().gate();g.fail(g.begin(BillingGate.Kind.OFFERINGS)!!);assertNotNull(g.begin(BillingGate.Kind.RESTORE))},
  "offering error does not erase recent SDK access" to {val g=Clock().gate();g.finishAccess(g.begin(BillingGate.Kind.RESTORE)!!,true,null);g.fail(g.begin(BillingGate.Kind.OFFERINGS)!!);assertTrue(g.active)},
  "test purchase starts only on explicit operation" to {val g=Clock().gate();assertFalse(g.busy);val t=g.begin(BillingGate.Kind.PURCHASE)!!;g.finishAccess(t,true,null);assertFalse(g.busy);assertTrue(g.active)},
  "cancelled purchase produces no access" to {val g=Clock().gate();g.fail(g.begin(BillingGate.Kind.PURCHASE)!!);assertFalse(g.active);assertFalse(g.busy)},
  "request timeout is not an automatic retry" to {val c=Clock();val g=c.gate();val t=g.begin(BillingGate.Kind.PURCHASE)!!;c.tick(30_000);assertTrue(g.expireOutstanding(t));assertFalse(g.busy);assertFalse(g.active)},
  "late purchase callback after timeout is rejected" to {val c=Clock();val g=c.gate();val t=g.begin(BillingGate.Kind.PURCHASE)!!;c.tick(30_000);g.expireOutstanding(t);assertFalse(g.finishAccess(t,true,null))},
  "timeout cannot clear a later operation" to {val c=Clock();val g=c.gate();val t=g.begin(BillingGate.Kind.REFRESH)!!;g.fail(t);val n=g.begin(BillingGate.Kind.RESTORE)!!;c.tick(30_000);assertFalse(g.expireOutstanding(t));assertTrue(g.expireOutstanding(n))},
  "timeout is not early" to {val c=Clock();val g=c.gate();val t=g.begin(BillingGate.Kind.REFRESH)!!;c.tick(29_999);assertFalse(g.expireOutstanding(t));assertTrue(g.finishAccess(t,true,null))},
  "elapsed callback timeout works without UI timer" to {val c=Clock();val g=c.gate();val t=g.begin(BillingGate.Kind.REFRESH)!!;c.tick(30_000);assertFalse(g.finishAccess(t,true,null));assertFalse(g.busy)},
  "wall clock rollback invalidates access" to {val c=Clock();val g=c.gate();g.finishAccess(g.begin(BillingGate.Kind.REFRESH)!!,true,null);c.wall--;assertFalse(g.active)},
  "monotonic clock rollback invalidates access" to {val c=Clock();val g=c.gate();g.finishAccess(g.begin(BillingGate.Kind.REFRESH)!!,true,null);c.elapsed--;assertFalse(g.active)},
  "monotonic rollback while waiting invalidates callback" to {val c=Clock();val g=c.gate();val t=g.begin(BillingGate.Kind.REFRESH)!!;c.elapsed--;assertFalse(g.finishAccess(t,true,null))},
  "new work stamp reflects a draft edit" to {val p=Practice(Task(2,3));val old=p.workStamp(0);p.draft="2x+3";assertNotEquals(old,p.workStamp(0))},
  "new work stamp reflects a hint" to {val p=Practice(Task(2,3));val old=p.workStamp(0);p.hint();assertNotEquals(old,p.workStamp(0))},
  "new work stamp reflects attempts" to {val p=Practice(Task(2,3));p.draft="2x+3";val old=p.workStamp(0);p.submit();assertNotEquals(old,p.workStamp(0));assertTrue(old.attempts.isEmpty())},
  "new work stamp reflects task sequence" to {val p=Practice(Task(2,3));assertNotEquals(p.workStamp(1),p.workStamp(2))},
  "unchanged work stamp is stable" to {val p=Practice(Task(-3,2));assertEquals(p.workStamp(1),p.workStamp(1))},
  "free practice works through a failed store check" to {val p=Practice(Task(2,3));val g=Clock().gate();g.fail(g.begin(BillingGate.Kind.REFRESH)!!);p.hint();p.draft="2x+6";assertEquals(Verdict.CORRECT,p.submit());assertFalse(p.independentlyCorrectFirstTry)},
  "destroyed callback cannot change a newer gate" to {val c=Clock();val old=c.gate();val t=old.begin(BillingGate.Kind.PURCHASE)!!;old.destroy();val fresh=c.gate();assertFalse(old.finishAccess(t,true,null));assertFalse(fresh.active)},
  "secret API key is never accepted for client setup" to {assertFalse(TestStoreKey.accepts("sk_test_not_a_public_sdk_key"))},
  "platform production key is refused by test-only client" to {assertFalse(TestStoreKey.accepts("goog_abcdefghijklmno"))},
  "non ASCII and padded keys are refused by validator" to {assertFalse(TestStoreKey.accepts("test_abcdefghijé"));assertFalse(TestStoreKey.accepts(" test_abcdefghijk"))},
  "public test key shape is accepted but not authenticated" to {assertTrue(TestStoreKey.accepts("test_abcdefghijk"))},
  "invalid freshness is rejected" to {assertFailsWith<IllegalArgumentException>{BillingGate({0},{0},0)}},
  "invalid timeout is rejected" to {assertFailsWith<IllegalArgumentException>{BillingGate({0},{0},requestTimeoutMillis=0)}}
 )
}
