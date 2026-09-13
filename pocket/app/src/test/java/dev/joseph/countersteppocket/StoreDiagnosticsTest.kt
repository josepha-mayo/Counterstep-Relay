package dev.joseph.countersteppocket
import org.junit.Assert.*
import org.junit.Test

class StoreDiagnosticsTest {
 @Test fun emptyTraceDoesNotInventPurchase(){val r=StoreDiagnostics{0}.report();assertTrue(r.contains("No store events recorded"));assertFalse(r.contains("ACTIVE_RESPONSE"))}
 @Test fun sessionOpenIsNotSdkResponse(){val d=StoreDiagnostics{0};d.record(StoreDiagnostics.Operation.SESSION,StoreDiagnostics.Outcome.OPENED);assertFalse(d.report().contains("ACTIVE_RESPONSE"))}
 @Test fun cancellationStaysCancellation(){val d=StoreDiagnostics{0};d.record(StoreDiagnostics.Operation.PURCHASE,StoreDiagnostics.Outcome.CANCELLED);assertEquals(StoreDiagnostics.Outcome.CANCELLED,d.snapshot().single().outcome);assertFalse(d.report().contains("ACTIVE_RESPONSE"))}
 @Test fun failureStaysFailure(){val d=StoreDiagnostics{0};d.record(StoreDiagnostics.Operation.RESTORE,StoreDiagnostics.Outcome.ERROR);assertTrue(d.report().contains("RESTORE | ERROR"))}
 @Test fun onlyLatest64AreRetained(){val d=StoreDiagnostics{0};repeat(70){d.record(StoreDiagnostics.Operation.REFRESH,StoreDiagnostics.Outcome.STARTED)};assertEquals(64,d.snapshot().size);assertEquals(7L,d.snapshot().first().sequence);assertEquals(70L,d.snapshot().last().sequence)}
 @Test fun snapshotIsDetached(){val d=StoreDiagnostics{0};d.record(StoreDiagnostics.Operation.SESSION,StoreDiagnostics.Outcome.OPENED);val old=d.snapshot();d.record(StoreDiagnostics.Operation.CONNECTION,StoreDiagnostics.Outcome.CONNECTED);assertEquals(1,old.size);assertEquals(2,d.snapshot().size)}
 @Test fun elapsedTimeIsRelative(){var time=500L;val d=StoreDiagnostics{time};time=750;d.record(StoreDiagnostics.Operation.REFRESH,StoreDiagnostics.Outcome.STARTED);assertEquals(250L,d.snapshot().single().elapsedMillis)}
 @Test fun backwardsClockIsBounded(){var time=500L;val d=StoreDiagnostics{time};time=1;d.record(StoreDiagnostics.Operation.REFRESH,StoreDiagnostics.Outcome.TIMEOUT);assertEquals(0L,d.snapshot().single().elapsedMillis)}
 @Test fun lateResultIsNotRecordedAsAccess(){val d=StoreDiagnostics{0};d.record(StoreDiagnostics.Operation.PURCHASE,StoreDiagnostics.Outcome.STALE_IGNORED);assertFalse(d.report().contains("ACTIVE_RESPONSE"));assertTrue(d.report().contains("STALE_IGNORED"))}
 @Test fun reportHasExplicitEvidenceAndRestoreScope(){val r=StoreDiagnostics{0}.report();assertTrue(r.contains("not authenticated receipts"));assertTrue(r.contains("not validation of Google Play"));assertTrue(r.contains("practice text"));assertTrue(r.contains("this app process only"))}
}
