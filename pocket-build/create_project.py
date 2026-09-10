"""Create the isolated native prototype. No old submission or production source is changed."""
from pathlib import Path
R=Path('pocket');R.mkdir(exist_ok=True)
files={
'settings.gradle.kts':'''pluginManagement { repositories { google(); mavenCentral(); gradlePluginPortal() } }
dependencyResolutionManagement { repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS); repositories { google(); mavenCentral() } }
rootProject.name = "CounterstepPocket"
include(":app")
''',
'build.gradle.kts':'''plugins {
 id("com.android.application") version "8.11.1" apply false
 id("org.jetbrains.kotlin.android") version "2.1.20" apply false
}
''',
'gradle.properties':'''org.gradle.jvmargs=-Xmx3g -Dfile.encoding=UTF-8
android.useAndroidX=true
kotlin.code.style=official
''',
'app/build.gradle.kts':'''plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android {
 namespace = "dev.joseph.countersteppocket"
 compileSdk = 35
 defaultConfig {
  applicationId = "dev.joseph.countersteppocket"
  minSdk = 26
  targetSdk = 35
  versionCode = 1
  versionName = "0.1.0-prototype"
 }
 compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
 kotlinOptions { jvmTarget = "17" }
 testOptions { unitTests.isReturnDefaultValues = true }
}
dependencies {
 implementation("com.revenuecat.purchases:purchases:9.9.0")
 testImplementation("junit:junit:4.13.2")
}
''',
'app/src/main/AndroidManifest.xml':'''<manifest xmlns:android="http://schemas.android.com/apk/res/android">
 <uses-permission android:name="android.permission.INTERNET" />
 <application android:label="Counterstep Pocket" android:theme="@style/AppTheme" android:allowBackup="false" android:supportsRtl="true" android:usesCleartextTraffic="false">
  <activity android:name=".MainActivity" android:exported="true" android:windowSoftInputMode="adjustResize">
   <intent-filter><action android:name="android.intent.action.MAIN"/><category android:name="android.intent.category.LAUNCHER"/></intent-filter>
  </activity>
 </application>
</manifest>
''',
'app/src/main/res/values/styles.xml':'''<resources>
 <style name="AppTheme" parent="android:style/Theme.Material.NoActionBar">
  <item name="android:fontFamily">sans</item><item name="android:windowLightStatusBar">false</item>
  <item name="android:colorAccent">#6CE0CE</item><item name="android:windowActionModeOverlay">true</item>
  <item name="android:navigationBarColor">#0E1522</item><item name="android:windowBackground">#0E1522</item>
 </style>
</resources>
''',
'app/src/main/java/dev/joseph/countersteppocket/Practice.kt':r'''package dev.joseph.countersteppocket

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
''',
'app/src/main/java/dev/joseph/countersteppocket/MainActivity.kt':r'''package dev.joseph.countersteppocket

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.content.Intent
import android.text.InputFilter
import android.text.TextWatcher
import android.text.Editable
import android.view.View
import android.widget.*
import org.json.JSONObject
import org.json.JSONArray
import com.revenuecat.purchases.*

class MainActivity: Activity() {
 private val bg=Color.rgb(14,21,34); private val card=Color.rgb(25,37,55)
 private val fg=Color.rgb(238,243,250); private val muted=Color.rgb(168,186,207); private val accent=Color.rgb(108,224,206)
 private lateinit var root:LinearLayout
 private lateinit var input:EditText
 private lateinit var result:TextView
 private lateinit var audit:TextView
 private var practice=Practice(Task(2,3))
 private var sequence=0
 private val access=AccessState()
 private var billingConfigured=false
 private var billingBusy=false
 private val prefs get()=getSharedPreferences("practice",MODE_PRIVATE)
 override fun onCreate(state:Bundle?) {
  super.onCreate(state)
  restore();render()
 }
 private fun d(n:Int)=(n*resources.displayMetrics.density).toInt()
 private fun text(value:String,size:Float=16f,color:Int=fg,bold:Boolean=false)=TextView(this).apply {
  text=value;textSize=size;setTextColor(color);if(bold)setTypeface(typeface,Typeface.BOLD);setPadding(0,d(5),0,d(7))
 }
 private fun box():LinearLayout=LinearLayout(this).apply {
  orientation=LinearLayout.VERTICAL;setPadding(d(18),d(16),d(18),d(16))
  background=GradientDrawable().apply { setColor(card);cornerRadius=d(18).toFloat() }
  layoutParams=LinearLayout.LayoutParams(-1,-2).apply{setMargins(0,d(10),0,d(12))}
 }
 private fun button(label:String,action:()->Unit)=Button(this).apply {
  text=label;isAllCaps=false;textSize=16f;minHeight=d(50);setTextColor(bg)
  backgroundTintList=android.content.res.ColorStateList.valueOf(accent)
  setOnClickListener{action()}
 }
 private fun render(){
  root=LinearLayout(this).apply {orientation=LinearLayout.VERTICAL;setPadding(d(22),d(28),d(22),d(28));setBackgroundColor(bg)}
  val scroll=ScrollView(this);scroll.addView(root);setContentView(scroll)
  scroll.setOnApplyWindowInsetsListener { v,i -> v.setPadding(0,i.systemWindowInsetTop,0,i.systemWindowInsetBottom);i }
  root.addView(text("COUNTERSTEP / POCKET",12f,accent,true))
  root.addView(text("Fix one line.\nKeep the work.",32f,fg,true))
  root.addView(text("A focused algebra repair, not an answer to copy.",16f,muted))
  val taskBox=box();taskBox.addView(text("EXPAND BOTH TERMS",12f,accent,true));taskBox.addView(text(practice.task.expression,36f,fg,true))
  taskBox.addView(text("Write an equivalent expanded expression. Use x, integer terms, + or -. You can put the constant first.",15f,muted))
  input=EditText(this).apply {hint="Example format: 2x + 6";setHintTextColor(muted);setTextColor(fg);textSize=22f;setSingleLine(true);filters=arrayOf(InputFilter.LengthFilter(120));setText(practice.draft)}
  taskBox.addView(input)
  input.addTextChangedListener(object:TextWatcher{override fun beforeTextChanged(s:CharSequence?,start:Int,count:Int,after:Int){};override fun onTextChanged(s:CharSequence?,start:Int,before:Int,count:Int){practice.draft=s.toString();persist()};override fun afterTextChanged(s:Editable?){} })
  result=text("Your first response and any hints stay in the record.",15f,muted);taskBox.addView(result)
  taskBox.addView(button("Check my step") {
   if(practice.attempts.size>=100){result.text="This task has reached its attempt limit. Start another task.";return@button}
   val v=practice.submit();persist()
   result.text=when(v){Verdict.CORRECT->if(practice.independentlyCorrectFirstTry)"Correct on the first try without a hint. Now try another." else "Correct repair. Earlier attempts and hints are still recorded.";Verdict.DIFFERENT->"Not equivalent yet. Check what happens to the constant inside the brackets.";Verdict.UNSUPPORTED->"This small checker accepts expanded integer-linear terms only. No equations, brackets, decimals, powers or other variables."}
   audit.text=practice.history()
  })
  taskBox.addView(button("Give me a hint"){
   if(practice.hints>=100){result.text="Hint limit reached.";return@button}
   practice.hint();persist();result.text="Multiply the outside coefficient by x AND by the constant. Keep track of the sign. This hint is recorded.";audit.text=practice.history()
  })
  root.addView(taskBox)
  val note=box();note.addView(text("THE WORK RECORD",12f,accent,true));audit=text(practice.history(),14f,muted);note.addView(audit)
  note.addView(text("Personal practice only. Local records can be edited; they are not authenticated grades or proof of mastery.",12f,muted))
  note.addView(button("Share my practice note"){
   persist();val body="Counterstep Pocket personal practice\nTask: ${practice.task.expression}\nDraft: ${practice.draft}\n${practice.history()}\nHints requested: ${practice.hints}\nPersonal self-reported practice, not an authenticated grade."
   startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {type="text/plain";putExtra(Intent.EXTRA_TEXT,body)},"Share practice note"))
  });root.addView(note)
  root.addView(button("Start another free task") { newTask(false) })
  root.addView(button("Mixed-sign practice pack") { if(access.active)newTask(true) else showStore() })
  root.addView(text("Free practice, hints and saving stay free. The optional pack is a RevenueCat Test Store integration under development, not a live paid offer.",13f,muted))
 }
 private fun newTask(mixed:Boolean){
  val action={sequence++; val a=if(mixed && sequence%2==1) -(2+sequence%6) else 2+sequence%6; val b=if(mixed) sequence%15-7 else sequence%8+1;practice=Practice(Task(a,b));persist();render()}
  if(practice.draft.isNotBlank() && !practice.solved) AlertDialog.Builder(this).setTitle("Leave this unfinished task?").setMessage("The current draft will be replaced. Share its note first to keep a copy.").setNegativeButton("Keep working",null).setPositiveButton("Start another"){_,_->action()}.show() else action()
 }
 private fun persist(){
  val j=JSONObject().put("schema",1).put("a",practice.task.coefficient).put("b",practice.task.offset).put("hints",practice.hints).put("draft",practice.draft).put("sequence",sequence)
  val arr=JSONArray();practice.attempts.forEach {arr.put(JSONObject().put("text",it.text).put("hints",it.hintsBefore))};j.put("attempts",arr)
  prefs.edit().putString("state",j.toString()).apply()
 }
 private fun restore(){
  try { val raw=prefs.getString("state",null)?:return;require(raw.length<40000);val j=JSONObject(raw);require(j.getInt("schema")==1)
   val a=j.getJSONArray("attempts");require(a.length()<=100);val attempts=mutableListOf<Attempt>();for(i in 0 until a.length()){val v=a.getJSONObject(i);attempts.add(Attempt(v.getString("text"),v.getInt("hints")))}
   val draft=j.getString("draft");require(draft.length<=120);sequence=j.optInt("sequence",0).coerceIn(0,1000000)
   practice=Practice(Task(j.getInt("a"),j.getInt("b")),attempts,j.getInt("hints"),draft)
  } catch(_:Exception){ practice=Practice(Task(2,3));Toast.makeText(this,"Saved practice was invalid. Started a fresh task.",Toast.LENGTH_LONG).show() }
 }
 private fun message(s:String){ if(!isFinishing)AlertDialog.Builder(this).setMessage(s).setPositiveButton("OK",null).show() }
 private fun showStore(){
  if(billingBusy)return
  if(!billingConfigured){
   val field=EditText(this).apply{hint="Your public Test Store SDK key (test_...)";setSingleLine(true)}
   AlertDialog.Builder(this).setTitle("Connect a Test Store").setMessage("Optional developer setup. Only a RevenueCat public Test Store key is accepted. Connecting sends SDK/device and anonymous purchase data to RevenueCat, not your typed practice. Never paste a secret API key. No real charge is made by Test Store.").setView(field).setNegativeButton("Keep free practice",null).setPositiveButton("Connect"){_,_->
    val key=field.text.toString().trim();if(!Regex("test_[A-Za-z0-9_]{10,200}").matches(key)){message("A valid public test_ SDK key is required. Nothing was connected.");return@setPositiveButton}
    Purchases.logLevel=LogLevel.ERROR
    Purchases.configure(PurchasesConfiguration.Builder(applicationContext,key).build());billingConfigured=true;showStore()
   }.show();return
  }
  billingBusy=true
  Purchases.sharedInstance.getOfferingsWith(onError={billingBusy=false;access.failedRefresh();message("Could not load the Test Store. Free practice and your draft are unchanged.")}) { offerings ->
   billingBusy=false
   val pack=offerings.current?.availablePackages?.firstOrNull()
   if(pack==null){message("No offering is configured. In your RevenueCat project attach a product to the mixed_signs entitlement and a current offering.");return@getOfferingsWith}
   AlertDialog.Builder(this).setTitle("Mixed-sign practice / TEST STORE").setMessage("${pack.storeProduct.title}\n${pack.storeProduct.price.formatted}\nTest purchase only. No real payment. Basic practice and hints remain free.").setNegativeButton("Not now",null).setNeutralButton("Restore test access"){_,_->
    billingBusy=true
    Purchases.sharedInstance.restorePurchasesWith(onError={billingBusy=false;access.failedRefresh();message("Restore failed. Your practice is unchanged.")}) { info->billingBusy=false;access.fromSdkEntitlements(info.entitlements.active.keys);message(if(access.active)"Test access restored. Open Mixed-sign practice pack." else "No active mixed_signs test entitlement was returned.")}
   }.setPositiveButton("Make test purchase"){_,_->
    billingBusy=true
    Purchases.sharedInstance.purchaseWith(PurchaseParams.Builder(this,pack).build(),onError={_,cancelled->billingBusy=false;access.failedRefresh();message(if(cancelled)"Cancelled. Your free practice and draft are unchanged." else "Purchase failed. No access was granted by this app.")}) { _,info->
     billingBusy=false;access.fromSdkEntitlements(info.entitlements.active.keys)
     message(if(access.active)"The SDK returned active test access. Open Mixed-sign practice pack." else "No active mixed_signs entitlement was returned. Core practice remains free.")
    }
   }.show()
  }
 }
}
''',
'app/src/test/java/dev/joseph/countersteppocket/PracticeTest.kt':r'''package dev.joseph.countersteppocket
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
''',
'README.md':'''# Counterstep Pocket: native Android prototype

A separate native-mobile implementation of the Counterstep repair-to-practice idea. It does not replace the existing Amazon or Prom entries. The current slice is deliberately focused: expand an integer multiple of (x + an integer), check it, retain the first response and hint history, persist a draft, reopen and share a practice note.

Core practice and hints are free and work offline. The optional mixed-sign pack has an actual pinned RevenueCat Android SDK adapter for Test Store offerings, purchase and restore callbacks. Public Test Store keys are entered only after the operator accepts the SDK connection. No key is shipped, no provider success is fabricated, and local study files cannot grant an entitlement. The SDK's active mixed_signs entitlement is the only in-process unlock source. This prototype is not configured for real charges or store publication.

## Build

JDK 17, Android SDK 35, Gradle 8.13:

```
gradle :app:testDebugUnitTest :app:assembleDebug
```

The isolated GitHub workflow produces the debug APK and test XML. Open this directory as an Android Studio project. The included exercise checker is new Kotlin code scoped to expanded integer-linear terms, not a full port of the earlier algebra/model stack. No conversational model or photo OCR is claimed.

## RevenueCat setup and evidence still needed

Create an owned RevenueCat Test Store app, a product, the mixed_signs entitlement, and a current offering. On device, open Mixed-sign practice pack and connect its public test_ SDK key. Test cancellation, failure, success, expiry and restore against the actual SDK and confirm the original exercise draft survives. Test Store transactions are sandbox events, not revenue. Never use a secret API key in a mobile client. Do not release this test-key prototype to an app store.

No account/configuration or live Test Store transaction was available during this initial code stage. A compiled dependency and local callback-state tests are not end-to-end payment evidence. Android runtime interaction, accessibility and lifecycle/expiry behavior still require device or emulator checks before a hackathon demo.

## Registration and submission remain pending

Target prospect: RevenueCat Shipaton Next Gen. Registration requires explicit consent to the official rules/Devpost terms. Student-category entry needs active student status and a qualifying academic email on Devpost. Neither registration nor student verification is implied by this source. The required meaningful RevenueCat demonstration, public open-source repository, under-two-minute device video, icon and screenshot remain final-entry tasks.

Sources: https://revenuecat-shipaton-2026.devpost.com/rules ; https://www.revenuecat.com/docs/getting-started/installation/android ; https://www.revenuecat.com/docs/test-and-launch/sandbox/test-store ; https://github.com/RevenueCat/purchases-android/blob/9.9.0/purchases/src/main/kotlin/com/revenuecat/purchases/ListenerConversionsCommon.kt

Original work by Joseph Ayanda, developed with substantial AI assistance. MIT. Dependency licenses are retained by their distributions. Synthetic exercises, no student personal data, no measured learning or revenue claim.
''',
'.gitignore':'.gradle/\n**/build/\nlocal.properties\n*.jks\n*.keystore\n',
'LICENSE':'''MIT License

Copyright (c) 2026 Joseph Ayanda

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
'''
}
for name,content in files.items():
 p=R/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(content)
print(f'Created {len(files)} source files in isolated pocket directory.')
