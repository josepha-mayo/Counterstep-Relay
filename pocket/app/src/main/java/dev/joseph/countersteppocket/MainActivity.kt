package dev.joseph.countersteppocket

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
  input=EditText(this).apply {hint="Write the expanded terms";setHintTextColor(muted);setTextColor(fg);textSize=22f;setSingleLine(true);filters=arrayOf(InputFilter.LengthFilter(120));setText(practice.draft)}
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
   AlertDialog.Builder(this).setTitle("Mixed-sign practice / TEST STORE").setMessage("${pack.product.title}\n${pack.product.price.formatted}\nTest purchase only. No real payment. Basic practice and hints remain free.").setNegativeButton("Not now",null).setNeutralButton("Restore test access"){_,_->
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
