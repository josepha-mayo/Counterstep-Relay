package dev.joseph.countersteppocket

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.*
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers.*
import org.hamcrest.Matchers.*
import org.junit.*
import org.junit.runner.RunWith

/** No valid public key or provider response is fabricated in these device checks. */
@RunWith(AndroidJUnit4::class)
class StoreReadinessTest {
 private lateinit var scenario:ActivityScenario<MainActivity>
 private val context get()=InstrumentationRegistry.getInstrumentation().targetContext
 @Before fun openFresh(){
  Assert.assertTrue(context.getSharedPreferences("practice",Context.MODE_PRIVATE).edit().clear().commit())
  Assert.assertTrue(context.getSharedPreferences("test_store_setup",Context.MODE_PRIVATE).edit().clear().commit())
  scenario=ActivityScenario.launch(MainActivity::class.java)
 }
 @After fun close(){scenario.close()}
 private fun tap(s:String)=onView(allOf(withText(s),isAssignableFrom(Button::class.java))).perform(scrollTo(),click())
 @Test fun noConnectionIsDistinguishedFromVerifiedPurchase(){
  tap("Test Store checks and setup")
  onView(withText("No Test Store connected. Free practice works without it.")).check(matches(isDisplayed()))
  Assert.assertFalse(TestStoreConnection.configured)
 }
 @Test fun keyPersistenceRequiresSeparateOptIn(){
  tap("Mixed-sign practice pack")
  onView(isAssignableFrom(CheckBox::class.java)).check(matches(isNotChecked()))
  onView(withText("Keep free practice")).perform(click())
  Assert.assertFalse(TestStoreConnection.hasRememberedKey(context))
 }
 @Test fun invalidRememberedSecretIsDiscardedWithoutConnecting(){
  scenario.close()
  context.getSharedPreferences("test_store_setup",Context.MODE_PRIVATE).edit().putString("public_test_key","sk_not_a_public_key").commit()
  scenario=ActivityScenario.launch(MainActivity::class.java)
  Assert.assertFalse(TestStoreConnection.configured)
  Assert.assertFalse(TestStoreConnection.hasRememberedKey(context))
  onView(withText("2(x+3)")).check(matches(isDisplayed()))
 }
 @Test fun checkAccessStillRequiresConsent(){
  tap("Test Store checks and setup");tap("Check current access")
  onView(withText("Connect a Test Store")).check(matches(isDisplayed()))
  onView(withText("Keep free practice")).perform(click())
  Assert.assertFalse(TestStoreConnection.configured)
 }
 @Test fun shareDiagnosticExcludesTypedPracticeAndIdentifiers(){
  onView(isAssignableFrom(EditText::class.java)).perform(replaceText("7x+12345"),closeSoftKeyboard())
  Intents.init()
  try{
   Intents.intending(hasAction(Intent.ACTION_CHOOSER)).respondWith(android.app.Instrumentation.ActivityResult(Activity.RESULT_CANCELED,null))
   tap("Test Store checks and setup");tap("Share store diagnostic")
   Intents.intended(allOf(hasAction(Intent.ACTION_CHOOSER),hasExtra(`is`(Intent.EXTRA_INTENT),allOf(hasAction(Intent.ACTION_SEND),hasExtra(Intent.EXTRA_TEXT,allOf(containsString("local Test Store diagnostic"),not(containsString("7x+12345")),not(containsString("RCAnonymousID:")),not(containsString("sk_not_a_public_key")),containsString("not authenticated receipts")))))))
  }finally{Intents.release()}
 }
 @Test fun forgetSavedSetupPreservesPracticeAndExplainsMemoryScope(){
  onView(isAssignableFrom(EditText::class.java)).perform(replaceText("2x+"),closeSoftKeyboard())
  context.getSharedPreferences("test_store_setup",Context.MODE_PRIVATE).edit().putString("public_test_key","not-configured-test").commit()
  tap("Test Store checks and setup");tap("Forget remembered key")
  onView(withText(containsString("This process may remain connected"))).check(matches(isDisplayed()))
  onView(withText("OK")).perform(click())
  Assert.assertFalse(TestStoreConnection.hasRememberedKey(context))
  onView(isAssignableFrom(EditText::class.java)).check(matches(withText("2x+")))
 }
}
