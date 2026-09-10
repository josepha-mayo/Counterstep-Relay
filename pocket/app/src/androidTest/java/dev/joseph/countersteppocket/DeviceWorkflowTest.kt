package dev.joseph.countersteppocket

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.espresso.Espresso.*
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.*
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers.*
import org.hamcrest.Matchers.*
import org.junit.*
import org.junit.runner.RunWith
import java.io.File

/** Native emulator tests. No RevenueCat key, provider success, or purchase is invented. */
@RunWith(AndroidJUnit4::class)
class DeviceWorkflowTest {
 private lateinit var scenario: ActivityScenario<MainActivity>
 private val instrumentation get()=InstrumentationRegistry.getInstrumentation()
 private val context get()=instrumentation.targetContext
 @Before fun openFresh(){
  Assert.assertTrue(context.getSharedPreferences("practice",Context.MODE_PRIVATE).edit().clear().commit())
  scenario=ActivityScenario.launch(MainActivity::class.java)
 }
 @After fun close(){scenario.close()}
 private fun enter(s:String){onView(isAssignableFrom(EditText::class.java)).perform(scrollTo(),replaceText(s),closeSoftKeyboard())}
 private fun tap(s:String){onView(allOf(withText(s),isAssignableFrom(Button::class.java))).perform(scrollTo(),click())}
 private fun visible(s:String){onView(withText(containsString(s))).perform(scrollTo()).check(matches(isDisplayed()))}
 private fun shot(name:String){
  instrumentation.waitForIdleSync()
  val file=File(context.getExternalFilesDir(null),"device-evidence/$name.png");file.parentFile!!.mkdirs()
  val bitmap=instrumentation.uiAutomation.takeScreenshot();Assert.assertNotNull(bitmap)
  file.outputStream().use { Assert.assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG,100,it)) }
  bitmap.recycle()
 }
 @Test fun initialScreenDoesNotLeakSolution(){
  onView(withText("2(x+3)")).check(matches(isDisplayed()))
  onView(isAssignableFrom(EditText::class.java)).check(matches(withHint("Write the expanded terms")))
  onView(withText("2x + 6")).check(doesNotExist());shot("01-first-task")
 }
 @Test fun firstCorrectAttemptIsSeparate(){
  enter("2x+6");tap("Check my step");visible("Correct on the first try without a hint.")
  visible("1. 2x+6 | correct | 0 hint(s) before response");shot("02-independent-response")
 }
 @Test fun wrongThenHintThenRepairKeepsHistory(){
  enter("2x+3");tap("Check my step");visible("Not equivalent yet.")
  tap("Give me a hint");visible("This hint is recorded.")
  enter("2x+6");tap("Check my step");visible("Correct repair. Earlier attempts and hints are still recorded.")
  visible("1. 2x+3 | different | 0 hint(s) before response\n2. 2x+6 | correct | 1 hint(s) before response")
  shot("03-repaired-history")
 }
 @Test fun unsupportedWorkIsNotCalledCorrect(){
  enter("x=3");tap("Check my step");visible("expanded integer-linear terms only")
  visible("1. x=3 | unsupported | 0 hint(s) before response")
 }
 @Test fun constantFirstIsAccepted(){enter("6+2x");tap("Check my step");visible("Correct on the first try without a hint.")}
 @Test fun recreationPreservesUnfinishedDraft(){
  enter("2x+");scenario.recreate()
  onView(isAssignableFrom(EditText::class.java)).check(matches(withText("2x+")))
  visible("No response yet.")
 }
 @Test fun backgroundResumePreservesHistory(){
  tap("Give me a hint");enter("2x+6");tap("Check my step")
  scenario.moveToState(Lifecycle.State.CREATED);scenario.moveToState(Lifecycle.State.RESUMED)
  visible("1. 2x+6 | correct | 1 hint(s) before response")
 }
 @Test fun unfinishedTaskRequiresConfirmationAndCancelKeepsDraft(){
  enter("2x+");tap("Start another free task")
  onView(withText("Leave this unfinished task?")).check(matches(isDisplayed()))
  onView(withText("Keep working")).perform(click())
  onView(isAssignableFrom(EditText::class.java)).check(matches(withText("2x+")))
  tap("Start another free task");onView(withText("Start another")).perform(click())
  onView(withText("3(x+2)")).check(matches(isDisplayed()))
 }
 @Test fun shareContainsActualResponsesWithoutSending(){
  enter("2x+3");tap("Check my step");tap("Give me a hint")
  Intents.init()
  try {
   Intents.intending(hasAction(Intent.ACTION_CHOOSER)).respondWith(android.app.Instrumentation.ActivityResult(Activity.RESULT_CANCELED,null))
   tap("Share my practice note")
   Intents.intended(allOf(hasAction(Intent.ACTION_CHOOSER),hasExtra(`is`(Intent.EXTRA_INTENT),allOf(hasAction(Intent.ACTION_SEND),hasType("text/plain"),hasExtra(Intent.EXTRA_TEXT,containsString("2x+3 | different | 0 hint(s)")),hasExtra(Intent.EXTRA_TEXT,containsString("Hints requested: 1"))))))
  } finally {Intents.release()}
 }
 @Test fun closingAndRelaunchingRecomputesPractice(){
  enter("2x+3");tap("Check my step");enter("2x+")
  scenario.close();scenario=ActivityScenario.launch(MainActivity::class.java)
  onView(isAssignableFrom(EditText::class.java)).check(matches(withText("2x+")))
  visible("1. 2x+3 | different | 0 hint(s) before response")
 }
 @Test fun invalidSavedStateResetsRatherThanApproves(){
  scenario.close()
  Assert.assertTrue(context.getSharedPreferences("practice",Context.MODE_PRIVATE).edit().putString("state","{broken}").commit())
  scenario=ActivityScenario.launch(MainActivity::class.java)
  onView(withText("2(x+3)")).check(matches(isDisplayed()));visible("No response yet.")
 }
 @Test fun unconfiguredStoreCanBeCancelledWithoutLosingWork(){
  enter("2x+");tap("Mixed-sign practice pack")
  onView(withText("Connect a Test Store")).check(matches(isDisplayed()));shot("04-explicit-store-consent")
  onView(withText("Keep free practice")).perform(click())
  onView(isAssignableFrom(EditText::class.java)).check(matches(withText("2x+")))
 }
 @Test fun secretKeyIsRejectedBeforeProviderConfiguration(){
  tap("Mixed-sign practice pack")
  onView(withHint("Your public Test Store SDK key (test_...)")).perform(replaceText("sk_test_not_a_public_sdk_key"),closeSoftKeyboard())
  onView(withText("Connect")).perform(click())
  onView(withText("A valid public test_ SDK key is required. Nothing was connected.")).check(matches(isDisplayed()))
 }
 @Test fun buttonsHaveMinimumTouchHeight(){
  scenario.onActivity { activity ->
   val minimum=48*activity.resources.displayMetrics.density
   var count=0
   fun walk(view:View){
    if(view is Button){count++;Assert.assertTrue("Small touch target: "+view.text,view.height>=minimum)}
    if(view is ViewGroup)for(i in 0 until view.childCount)walk(view.getChildAt(i))
   }
   walk(activity.window.decorView);Assert.assertTrue(count>=5)
  }
 }
}
