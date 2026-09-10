package dev.joseph.countersteppocket

import android.app.Activity
import android.app.AlertDialog
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.content.Intent
import android.text.InputFilter
import android.text.InputType
import android.text.TextWatcher
import android.text.Editable
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
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
 private lateinit var scroll:ScrollView
 private var practice=Practice(Task(2,3))
 private var sequence=0
 private val billing=BillingGate({SystemClock.elapsedRealtime()},{System.currentTimeMillis()})
 private val uiHandler=Handler(Looper.getMainLooper())
 private var requestTimeout:Runnable?=null
 private var storeDialog:AlertDialog?=null
 private lateinit var billingStatus:TextView
 private val prefs get()=getSharedPreferences("practice",MODE_PRIVATE)
 override fun onCreate(state:Bundle?) {
  super.onCreate(state)
  restore();render()
 }
 override fun onStart() {
  super.onStart(); billing.onForeground()
  if(TestStoreConnection.configured) refreshAccess(false)
 }
 override fun onStop() {
  billing.onBackground(); clearTimeout(); storeDialog?.dismiss(); storeDialog=null
  super.onStop()
 }
 override fun onDestroy() { billing.destroy(); clearTimeout(); super.onDestroy() }
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
  root=LinearLayout(this).apply {orientation=LinearLayout.VERTICAL;setPadding(d(20),d(18),d(20),d(22));setBackgroundColor(bg)}
  root.isFocusableInTouchMode=true
  scroll=ScrollView(this).apply {
   isFillViewport=true;isSmoothScrollingEnabled=false;setBackgroundColor(bg)
   addView(root,FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT))
  }
  scroll.setOnApplyWindowInsetsListener { v,i ->
   if(Build.VERSION.SDK_INT>=30){
    val bars=i.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
    val keyboard=i.getInsets(WindowInsets.Type.ime())
    v.setPadding(bars.left,bars.top,bars.right,maxOf(bars.bottom,keyboard.bottom))
   } else {
    @Suppress("DEPRECATION")
    v.setPadding(i.systemWindowInsetLeft,i.systemWindowInsetTop,i.systemWindowInsetRight,i.systemWindowInsetBottom)
   }
   i
  }
  setContentView(scroll)
  root.addView(text("COUNTERSTEP / POCKET",12f,accent,true))
  root.addView(text("Fix one line.",28f,fg,true))
  root.addView(text("Work offline. Keep your attempts and hints.",14f,muted))
  val taskBox=box();taskBox.addView(text("EXPAND BOTH TERMS",12f,accent,true));taskBox.addView(text(practice.task.expression,34f,fg,true))
  taskBox.addView(text("Write expanded integer terms using x, + and -. The constant can come first.",14f,muted))
  input=EditText(this).apply {hint="Write the expanded terms";contentDescription="Expanded expression";setHintTextColor(muted);setTextColor(fg);textSize=22f;setSingleLine(true);inputType=InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS;imeOptions=EditorInfo.IME_ACTION_DONE or EditorInfo.IME_FLAG_NO_EXTRACT_UI;filters=arrayOf(InputFilter.LengthFilter(120));setText(practice.draft)}
  taskBox.addView(input)
  input.addTextChangedListener(object:TextWatcher{override fun beforeTextChanged(s:CharSequence?,start:Int,count:Int,after:Int){};override fun onTextChanged(s:CharSequence?,start:Int,before:Int,count:Int){practice.draft=s.toString();persist();if(::result.isInitialized){result.text="Draft changed. Check this version; earlier responses stay in the record.";result.setTextColor(muted)}};override fun afterTextChanged(s:Editable?){} })
  input.setOnEditorActionListener { _,action,event ->
   if(action==EditorInfo.IME_ACTION_DONE || (event?.keyCode==android.view.KeyEvent.KEYCODE_ENTER && event.action==android.view.KeyEvent.ACTION_UP)){checkStep();true}else false
  }
  result=text("Your first response and any hints stay in the record.",15f,muted);taskBox.addView(result)
  val symbols=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL}
  for((label,token) in listOf("x" to "x","+" to "+","−" to "-","⌫" to ""))symbols.addView(button(label){editMathToken(token)}.apply{layoutParams=LinearLayout.LayoutParams(0,d(48),1f).apply{setMargins(0,0,d(4),0)}})
  taskBox.addView(symbols)
  taskBox.addView(button("Check my step"){checkStep()})
  taskBox.addView(button("Give me a hint"){
   if(practice.hints>=100){result.text="Hint limit reached.";return@button}
   dismissKeyboard();practice.hint();persist()
   result.text=Coaching.progressiveHint(practice)+" This hint is recorded.";result.setTextColor(fg);audit.text=practice.history();reveal(result)
  })
  root.addView(taskBox)
  audit=text(practice.history(),14f,muted)
  root.addView(audit)
  val actions=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL}
  actions.addView(button("Restore test access") { ensureStore { restoreTestAccess() } }.apply{layoutParams=LinearLayout.LayoutParams(0,d(50),1f).apply{setMargins(0,0,d(4),0)}})
  actions.addView(button("Start another free task") { newTask(false) }.apply{layoutParams=LinearLayout.LayoutParams(0,d(50),1f)})
  root.addView(actions)
  root.addView(button("Mixed-sign practice pack") { ensureStore { refreshAccess(true) } })
  root.addView(button("Share my practice note"){
   persist();val body="Counterstep Pocket personal practice\nTask: ${practice.task.expression}\nDraft: ${practice.draft}\n${practice.history()}\nHints requested: ${practice.hints}\nPersonal self-reported practice, not an authenticated grade."
   startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {type="text/plain";putExtra(Intent.EXTRA_TEXT,body)},"Share practice note"))
  })
  billingStatus=text(if(billing.active)"Recent test access is available; each new pack task is checked again." else "Optional test access has not been checked.",13f,muted)
  root.addView(billingStatus)
  val note=box();note.addView(text("THE WORK RECORD",12f,accent,true))
  note.addView(text("Personal practice only. Local records can be edited; they are not authenticated grades or proof of mastery.",12f,muted))
  root.addView(note)
  root.requestFocus()
  root.addView(text("Free practice, hints and saving stay free. The optional pack is a RevenueCat Test Store integration under development, not a live paid offer.",13f,muted))
  scroll.requestApplyInsets()
 }
 private fun dismissKeyboard(){
  (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(input.windowToken,0)
  input.clearFocus();root.requestFocus()
 }
 private fun reveal(view:View){view.post { if(!isDestroyed)scroll.smoothScrollTo(0,(view.top-d(12)).coerceAtLeast(0)) }}
 private fun checkStep(){
  val problem=Coaching.cannotCheck(practice)
  if(problem!=null){result.text=problem;result.setTextColor(fg);return}
  dismissKeyboard();val v=practice.submit();persist()
  result.text=when(v){Verdict.CORRECT->if(practice.independentlyCorrectFirstTry)"Correct on the first try without a hint. Now try another." else "Correct repair. Earlier attempts and hints are still recorded.";Verdict.DIFFERENT->"Not equivalent yet. "+Coaching.explain(practice.task,practice.draft);Verdict.UNSUPPORTED->"This small checker accepts expanded integer-linear terms only. No equations, brackets, decimals, powers or other variables."}
  result.setTextColor(if(v==Verdict.CORRECT)accent else fg);audit.text=practice.history();reveal(result)
 }
 private fun editMathToken(token:String){
  val start=minOf(input.selectionStart.coerceAtLeast(0),input.selectionEnd.coerceAtLeast(0));val end=maxOf(input.selectionStart.coerceAtLeast(0),input.selectionEnd.coerceAtLeast(0))
  if(token.isEmpty()){if(start!=end)input.text.delete(start,end) else if(start>0)input.text.delete(start-1,start)} else input.text.replace(start,end,token)
 }
 private fun newTask(mixed:Boolean){
  val requestedWork=practice.workStamp(sequence)
  val action={
   if(practice.workStamp(sequence)!=requestedWork){message("Your work changed. It was not replaced.")}
   else if(mixed && !billing.active){message("Test access needs a fresh check. Your current draft is safe.")}
   else {sequence++; val a=if(mixed && sequence%2==1) -(2+sequence%6) else 2+sequence%6; val b=if(mixed) sequence%15-7 else sequence%8+1;practice=Practice(Task(a,b));persist();render()}}
  if(Coaching.hasUnfinishedDraft(practice)) AlertDialog.Builder(this).setTitle("Leave this unfinished task?").setMessage("The current draft will be replaced. Share its note first to keep a copy.").setNegativeButton("Keep working",null).setPositiveButton("Start another"){_,_->action()}.show() else action()
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
 private fun canUseUi()=billing.isForeground && !isFinishing && !isDestroyed
 private fun message(s:String){ if(canUseUi())AlertDialog.Builder(this).setMessage(s).setPositiveButton("OK",null).show() }
 private fun status(s:String){ if(canUseUi() && ::billingStatus.isInitialized)billingStatus.text=s }
 private fun clearTimeout(){requestTimeout?.let{uiHandler.removeCallbacks(it)};requestTimeout=null}
 private fun begin(kind:BillingGate.Kind):BillingGate.Ticket? {
  val ticket=billing.begin(kind)
  if(ticket==null){status("A store check is already running, or this screen is inactive.");return null}
  clearTimeout()
  requestTimeout=Runnable {
   if(billing.expireOutstanding(ticket))status("The request timed out locally. Nothing was retried; free practice is unchanged.")
  }.also{uiHandler.postDelayed(it,30_050)}
  return ticket
 }
 private fun ensureStore(action:()->Unit){
  if(!canUseUi())return
  if(!BuildConfig.DEBUG){message("This prototype has no production billing. Free practice remains available.");return}
  if(TestStoreConnection.configured){action();return}
  if(storeDialog?.isShowing==true)return
  val field=EditText(this).apply{hint="Your public Test Store SDK key (test_...)";setSingleLine(true);filters=arrayOf(InputFilter.LengthFilter(205))}
  storeDialog=AlertDialog.Builder(this).setTitle("Connect a Test Store")
   .setMessage("Optional developer setup. Connecting sends SDK/device and anonymous purchase data to RevenueCat, not typed practice. Use only a public test_ SDK key. The key stays in memory until this process closes. Test Store makes no real charge.")
   .setView(field).setNegativeButton("Keep free practice",null).setPositiveButton("Connect"){_,_->
    if(!canUseUi())return@setPositiveButton
    val key=field.text.toString().trim()
    if(!TestStoreKey.accepts(key)){message("A valid public test_ SDK key is required. Nothing was connected.");return@setPositiveButton}
    try {TestStoreConnection.connect(applicationContext,key);action()}
    catch(_:Exception){message("The Test Store could not be connected. Use a public test_ key, not a secret key. Your draft is unchanged.")}
   }.create()
  storeDialog?.show()
 }
 private fun finishAccess(ticket:BillingGate.Ticket,info:CustomerInfo):Boolean {
  val e=info.entitlements.active["mixed_signs"]
  return billing.finishAccess(ticket,e?.isActive==true,e?.expirationDate?.time)
 }
 private fun refreshAccess(openAfter:Boolean){
  if(!TestStoreConnection.configured)return
  val work=practice.workStamp(sequence)
  val ticket=begin(BillingGate.Kind.REFRESH)?:return
  status("Checking current Test Store access...")
  try {
   Purchases.sharedInstance.getCustomerInfoWith(CacheFetchPolicy.FETCH_CURRENT,onError={
    runOnUiThread{if(billing.fail(ticket)){clearTimeout();status("Access could not be checked. Core practice is still free.")}}
   }) { info -> runOnUiThread {
    if(finishAccess(ticket,info)){
     clearTimeout();status(if(billing.active)"Test access checked. It will be checked again before a new pack task." else "No active mixed_signs test access was returned.")
     if(openAfter){
      if(work!=practice.workStamp(sequence))message("Your work changed during the check. It was not replaced. Open the pack again when ready.")
      else if(billing.active)newTask(true) else showStore()
     }
    }
   }}
  } catch(_:Exception){if(billing.fail(ticket)){clearTimeout();status("The access request could not start. Nothing was retried.")}}
 }
 private fun restoreTestAccess(){
  // Restore does not depend on a current offering or on loading product prices.
  val ticket=begin(BillingGate.Kind.RESTORE)?:return
  status("Restoring Test Store access...")
  try {
   Purchases.sharedInstance.restorePurchasesWith(onError={
    runOnUiThread{if(billing.fail(ticket)){clearTimeout();status("Restore failed. Your practice and draft are unchanged.")}}
   }) { info -> runOnUiThread{
    if(finishAccess(ticket,info)){
     clearTimeout();status(if(billing.active)"Test access restored. Open the mixed-sign pack to continue." else "Restore returned no active mixed_signs test access.")
    }
   }}
  } catch(_:Exception){if(billing.fail(ticket)){clearTimeout();status("Restore could not start. Nothing was retried.")}}
 }
 private fun showStore(){
  val ticket=begin(BillingGate.Kind.OFFERINGS)?:return
  status("Loading the Test Store pack...")
  try {
   Purchases.sharedInstance.getOfferingsWith(onError={
    runOnUiThread{if(billing.fail(ticket)){clearTimeout();status("The offering is unavailable. Restore remains available separately.")}}
   }) { offerings -> runOnUiThread{
    if(billing.finishOfferings(ticket)){
     clearTimeout()
     val pack=offerings.current?.availablePackages?.singleOrNull{it.identifier=="mixed_signs"}
     if(pack==null){status("Configure exactly one package named mixed_signs in the current offering. Restore does not require an offering.")}
     else if(canUseUi()){
      storeDialog?.dismiss()
      storeDialog=AlertDialog.Builder(this).setTitle("Mixed-sign practice / TEST STORE")
       .setMessage("${pack.product.title}\n${pack.product.price.formatted}\nTest purchase only; no real payment. Free practice, hints and saving remain free.")
       .setNegativeButton("Not now",null).setNeutralButton("Restore test access"){_,_->restoreTestAccess()}
       .setPositiveButton("Make test purchase"){_,_->purchaseTestPack(pack)}.create()
      storeDialog?.show()
     }
    }
   }}
  }catch(_:Exception){if(billing.fail(ticket)){clearTimeout();status("The offering request could not start. Nothing was retried.")}}
 }
 private fun purchaseTestPack(pack:com.revenuecat.purchases.Package){
  val ticket=begin(BillingGate.Kind.PURCHASE)?:return
  status("Waiting for the Test Store result...")
  try {
   Purchases.sharedInstance.purchaseWith(PurchaseParams.Builder(this,pack).build(),onError={_,cancelled->
    runOnUiThread{if(billing.fail(ticket)){clearTimeout();status(if(cancelled)"Test purchase cancelled. Your draft is unchanged." else "Purchase failed. No access was granted by this app.")}}
   }) { _,info -> runOnUiThread{
    if(finishAccess(ticket,info)){
     clearTimeout();status(if(billing.active)"The SDK returned active test access. Open the pack when ready." else "No active mixed_signs entitlement was returned. Core practice stays free.")
    }
   }}
  }catch(_:Exception){if(billing.fail(ticket)){clearTimeout();status("The test purchase could not start. Nothing was retried.")}}
 }
}
