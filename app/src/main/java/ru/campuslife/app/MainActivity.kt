package ru.campuslife.app

import android.Manifest
import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items as rowItems
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject

private val bg = Color(0xFF111214)
private val card = Color(0xFF202123)
private val line = Color(0xFF343332)
private val muted = Color(0xFFAAA7A1)
private val sand = Color(0xFFE3C4A0)
private val ink = Color(0xFFF4F0E9)
private val locale = Locale.forLanguageTag("ru")
private val fmt = DateTimeFormatter.ofPattern("d MMMM, EEEE", locale)
private data class Event(val id:Int, val title:String, val category:String, val date:LocalDateTime, val place:String, val description:String, val subtitle:String, val tint:Color)
private fun events(day:LocalDate):List<Event> {

 return listOf(
 Event(1,"Ночь науки", "Наука",day.plusDays(2).atTime(16,30),"Актовый зал","Опыты, которые можно не только увидеть, но и повторить. Исследуем свет, звук и неожиданные свойства привычных вещей.","Опыты и открытия рядом",Color(0xFF755E4E)),
 Event(2,"Творческая мастерская", "Творчество",day.plusDays(4).atTime(15,0),"Кабинет 204","Встреча для тех, кто любит придумывать и создавать. Материалы будут на месте, опыт не требуется.","Идеи, которым нужно место",Color(0xFF6E5A55)),
 Event(3,"Школьный турнир", "Спорт",day.plusDays(6).atTime(14,30),"Спортивный зал","Собираем команды для дружеских соревнований. Приходи участвовать или поддержать друзей.","Играем одной командой",Color(0xFF56665F)),
 Event(4,"Клуб создателей", "Технологии",day.plusDays(9).atTime(16,0),"Медиатека","Обсуждаем идеи приложений и пробуем создавать небольшие проекты вместе. Можно прийти без подготовки.","От первой идеи к проекту",Color(0xFF565F71)),
 Event(5,"Открытый микрофон", "Творчество",day.plusDays(12).atTime(17,0),"Библиотека","Музыка, стихи и истории. Выступай или просто приходи послушать в уютной компании.","Вечер, где слышно каждого",Color(0xFF746054)))
}
class MainActivity : ComponentActivity() {
 private var pendingReminder: Pair<Event, Int>? = null
 private var permissionVersion by mutableIntStateOf(0)
 private val permission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
  val pending = pendingReminder; pendingReminder = null
  if (granted && pending != null) { saveReminder(this, pending.first, pending.second); permissionVersion++ } else if (!granted) Toast.makeText(this, "Разреши уведомления, чтобы получать напоминания", Toast.LENGTH_LONG).show()
 }
 override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); window.statusBarColor=android.graphics.Color.rgb(17,18,20); window.navigationBarColor=android.graphics.Color.rgb(17,18,20); setContent { MaterialTheme(colorScheme=darkColorScheme(primary=sand,background=bg,surface=card,onSurface=ink)) { Campus(permissionVersion=permissionVersion,onRemind={ e, minutes ->
   if (minutes == 0) { cancelReminder(this,e); true }
   else if (e.date.minusMinutes(minutes.toLong()).isBefore(LocalDateTime.now())) { Toast.makeText(this, "Это время напоминания уже прошло", Toast.LENGTH_SHORT).show(); false }
   else if (Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED) { pendingReminder=e to minutes; permission.launch(Manifest.permission.POST_NOTIFICATIONS); false }
   else { saveReminder(this,e,minutes); true }
  }) } } }
}
private fun reminderIntent(context:Context,e:Event): PendingIntent {
 val intent=Intent(context,ReminderReceiver::class.java).putExtra("title",e.title).putExtra("place",e.place).putExtra("id",e.id)
 return PendingIntent.getBroadcast(context,e.id,intent,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
}
private fun saveReminder(context:Context,e:Event,minutes:Int) {
 val time=e.date.minusMinutes(minutes.toLong()).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
 (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).set(AlarmManager.RTC_WAKEUP,time,reminderIntent(context,e))
 context.getSharedPreferences("campus",Context.MODE_PRIVATE).edit().putInt("reminder_"+e.id,minutes).apply()
}
private fun cancelReminder(context:Context,e:Event) {
 (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).cancel(reminderIntent(context,e))
 context.getSharedPreferences("campus",Context.MODE_PRIVATE).edit().remove("reminder_"+e.id).apply()
}
private fun loadCustom(context: Context):List<Event> = try {
 val a=JSONArray(context.getSharedPreferences("campus",Context.MODE_PRIVATE).getString("custom_events","[]")); (0 until a.length()).map { i -> val o=a.getJSONObject(i); Event(o.getInt("id"),o.getString("title"),o.getString("category"),LocalDateTime.parse(o.getString("date")),o.getString("place"),o.getString("description"),o.getString("subtitle"),Color(0xFF755E4E)) }
} catch (_:Exception) { emptyList() }
private fun saveCustom(context:Context,e:Event) {
 val prefs=context.getSharedPreferences("campus",Context.MODE_PRIVATE); val a=try { JSONArray(prefs.getString("custom_events","[]")) } catch (_:Exception) { JSONArray() }
 a.put(JSONObject().put("id",e.id).put("title",e.title).put("category",e.category).put("date",e.date.toString()).put("place",e.place).put("description",e.description).put("subtitle",e.subtitle)); prefs.edit().putString("custom_events",a.toString()).apply()
}
class BootReceiver:BroadcastReceiver() {
 override fun onReceive(context:Context,intent:Intent) {
  if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
  val prefs=context.getSharedPreferences("campus",Context.MODE_PRIVATE); val seed=prefs.getLong("seed_day",0L)
  val all=(if(seed!=0L) events(LocalDate.ofEpochDay(seed)) else emptyList())+loadCustom(context)
  all.forEach { event -> val minutes=prefs.getInt("reminder_"+event.id,0); if(minutes>0 && event.date.minusMinutes(minutes.toLong()).isAfter(LocalDateTime.now())) saveReminder(context,event,minutes) }
 }
}
class ReminderReceiver:BroadcastReceiver(){ override fun onReceive(context:Context,intent:Intent){ val manager=context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager; if(Build.VERSION.SDK_INT>=26) manager.createNotificationChannel(NotificationChannel("events","Мероприятия",NotificationManager.IMPORTANCE_DEFAULT)); val launch=PendingIntent.getActivity(context,0,Intent(context,MainActivity::class.java),PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT); val n=NotificationCompat.Builder(context,"events").setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle("Скоро: ${intent.getStringExtra("title")}").setContentText(intent.getStringExtra("place")).setContentIntent(launch).setAutoCancel(true).build(); try { manager.notify(intent.getIntExtra("id",0),n) } catch (_:SecurityException) {} } }
@Composable private fun Campus(permissionVersion:Int,onRemind:(Event,Int)->Boolean) {
 val ctx=LocalContext.current; val prefs=remember { ctx.getSharedPreferences("campus",Context.MODE_PRIVATE) }; val seed=remember { val start=prefs.getLong("seed_day", 0L).let { if(it==0L) LocalDate.now().toEpochDay().also { value -> prefs.edit().putLong("seed_day",value).apply() } else it }; events(LocalDate.ofEpochDay(start)) }; var custom by remember { mutableStateOf(loadCustom(ctx)) }; val all=(seed+custom).sortedBy { it.date }
 var favorites by remember { mutableStateOf(prefs.getStringSet("favorites",emptySet())!!.toSet()) }; var joined by remember { mutableStateOf(prefs.getStringSet("joined",emptySet())!!.toSet()) }; var reminderVersion by remember { mutableIntStateOf(0) }
 var creating by remember { mutableStateOf(false) }; var exactDate by remember { mutableStateOf<LocalDate?>(null) }; var tab by remember { mutableStateOf("Афиша") }; var selected by remember { mutableStateOf<Event?>(null) }; var query by remember { mutableStateOf("") }; var category by remember { mutableStateOf("Все") }; var dateFilter by remember { mutableStateOf("Все даты") }
 fun toggle(set:Set<String>,key:String,id:Int):Set<String> { val s=if(id.toString() in set) set-id.toString() else set+id.toString(); prefs.edit().putStringSet(key,s).apply(); return s }
 Surface(color=bg,modifier=Modifier.fillMaxSize()) { Column(Modifier.fillMaxSize().statusBarsPadding()) {
  val e=selected
  BackHandler(enabled=creating || e!=null) { if(creating) creating=false else selected=null }
  if(creating) { CreateEvent(onBack={creating=false},onSave={ event -> saveCustom(ctx,event); custom=loadCustom(ctx); creating=false; selected=event }) }
  else if(e!=null){ val reminder=remember(e,reminderVersion,permissionVersion) { prefs.getInt("reminder_"+e.id,0) }; Detail(e,e.id.toString() in favorites,e.id.toString() in joined,reminder,onBack={selected=null},onFavorite={favorites=toggle(favorites,"favorites",e.id)},onJoin={joined=toggle(joined,"joined",e.id)},onReminder={ minutes -> if(onRemind(e,minutes)) reminderVersion++ }) }
  else { Column(Modifier.weight(1f)) {
   Row(Modifier.fillMaxWidth().padding(start=22.dp,end=22.dp,top=18.dp,bottom=22.dp),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){ Column { Text("CAMPUSLIFE",color=sand,fontSize=11.sp,letterSpacing=2.5.sp,fontWeight=FontWeight.Bold); Spacer(Modifier.height(6.dp)); Text(when(tab){"Избранное"->"Твоё избранное";"Записи"->"Мои мероприятия";else->"Время быть вместе"},fontSize=26.sp,fontWeight=FontWeight.SemiBold,color=ink) }; Box(Modifier.size(39.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFF3A332D)).clickable { creating=true },contentAlignment=Alignment.Center){Text("+",color=sand,fontSize=26.sp)} }
   if(tab=="Афиша") { OutlinedTextField(value=query,onValueChange={query=it},modifier=Modifier.fillMaxWidth().padding(horizontal=22.dp),singleLine=true,placeholder={Text("Найти событие",color=muted)}, leadingIcon={Text("⌕",color=sand,fontSize=25.sp)},shape=RoundedCornerShape(16.dp),colors=OutlinedTextFieldDefaults.colors(focusedBorderColor=sand,unfocusedBorderColor=line,focusedContainerColor=card,unfocusedContainerColor=card)); Spacer(Modifier.height(16.dp)); LazyRow(contentPadding=PaddingValues(horizontal=22.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)){rowItems(listOf("Все","Наука","Творчество","Спорт","Технологии")){ item-> Chip(item,item==category){category=item} }}; Spacer(Modifier.height(10.dp)); LazyRow(contentPadding=PaddingValues(horizontal=22.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)){rowItems(listOf("Все даты","7 дней","30 дней")){ item-> Chip(item,item==dateFilter && exactDate==null){dateFilter=item; exactDate=null} }}; Spacer(Modifier.height(10.dp)); Row(Modifier.padding(horizontal=22.dp),verticalAlignment=Alignment.CenterVertically) { Text(if(exactDate==null) "Выбрать дату" else exactDate!!.format(DateTimeFormatter.ofPattern("d MMMM yyyy",locale)),Modifier.clip(RoundedCornerShape(50)).background(card).clickable { val d=exactDate?:LocalDate.now(); DatePickerDialog(ctx,{ _,y,m,day -> exactDate=LocalDate.of(y,m+1,day); dateFilter="Все даты" },d.year,d.monthValue-1,d.dayOfMonth).show() }.padding(horizontal=16.dp,vertical=9.dp),color=sand,fontSize=12.sp); if(exactDate!=null) Text("  Сбросить",Modifier.clickable { exactDate=null }.padding(8.dp),color=muted,fontSize=12.sp) }; Spacer(Modifier.height(14.dp)) }
   val filtered=all.filter { ev -> (tab!="Избранное" || ev.id.toString() in favorites) && (tab!="Записи" || ev.id.toString() in joined) && (tab!="Афиша" || ((category=="Все"||ev.category==category) && (query.isBlank()||("${ev.title} ${ev.place} ${ev.category}").contains(query,true)) && (exactDate==null || ev.date.toLocalDate()==exactDate) && (dateFilter=="Все даты" || (!ev.date.toLocalDate().isBefore(LocalDate.now()) && ev.date.toLocalDate()<=LocalDate.now().plusDays(if(dateFilter=="7 дней") 7 else 30))))) }
   Row(Modifier.fillMaxWidth().padding(horizontal=22.dp),horizontalArrangement=Arrangement.SpaceBetween){Text(if(tab=="Афиша") "БЛИЖАЙШИЕ СОБЫТИЯ" else "СОБЫТИЯ",fontSize=10.sp,letterSpacing=1.5.sp,color=muted,fontWeight=FontWeight.Bold);Text("${filtered.size}",fontSize=11.sp,color=muted)}
   if(filtered.isEmpty()) Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){Text(if(tab=="Афиша") "Ничего не найдено" else "Пока здесь пусто",color=muted)} else LazyColumn(contentPadding=PaddingValues(start=22.dp,end=22.dp,top=14.dp,bottom=24.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){items(filtered,key={it.id}){ ev-> EventCard(ev,ev.id.toString() in favorites,{selected=ev},{favorites=toggle(favorites,"favorites",ev.id)}) }}
  }; Row(Modifier.fillMaxWidth().background(Color(0xFF1B1C1E)).navigationBarsPadding().padding(vertical=13.dp),horizontalArrangement=Arrangement.SpaceEvenly){ listOf("Афиша","Избранное","Записи").forEach { item-> Text(item,Modifier.clickable { tab=item }.padding(horizontal=10.dp,vertical=5.dp),color=if(tab==item)sand else muted,fontSize=13.sp,fontWeight=if(tab==item)FontWeight.Bold else FontWeight.Normal) } } }
 } }
}
@Composable private fun Chip(text:String,selected:Boolean,onClick:()->Unit){Box(Modifier.clip(RoundedCornerShape(50)).background(if(selected)sand else card).clickable(onClick=onClick).padding(horizontal=16.dp,vertical=10.dp)){Text(text,color=if(selected)bg else muted,fontSize=12.sp)}}
@Composable private fun EventCard(e:Event,favorite:Boolean,onClick:()->Unit,onFavorite:()->Unit){Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(card).clickable(onClick=onClick)){
 Box(Modifier.fillMaxWidth().height(145.dp).background(e.tint)){Image(painterResource(ru.campuslife.app.R.drawable.event_cover),null,Modifier.fillMaxSize(),contentScale=ContentScale.Crop); Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha=.42f)));Text(e.category.uppercase(locale),Modifier.align(Alignment.TopStart).padding(16.dp).clip(RoundedCornerShape(20.dp)).background(Color.Black.copy(alpha=.28f)).padding(horizontal=10.dp,vertical=6.dp),color=ink,fontSize=10.sp,letterSpacing=1.sp);Text(if(favorite)"♥" else "♡",Modifier.align(Alignment.TopEnd).padding(15.dp).clip(RoundedCornerShape(50)).background(Color.Black.copy(alpha=.3f)).clickable(onClick=onFavorite).padding(horizontal=10.dp,vertical=4.dp),color=sand,fontSize=21.sp);Text(e.subtitle,Modifier.align(Alignment.BottomStart).padding(16.dp),color=ink.copy(alpha=.85f),fontSize=13.sp)}
 Column(Modifier.padding(17.dp)){Text(e.title,color=ink,fontSize=19.sp,fontWeight=FontWeight.SemiBold);Spacer(Modifier.height(9.dp));Text("${e.date.format(fmt)}  ·  ${e.date.format(DateTimeFormatter.ofPattern("HH:mm"))}",color=sand,fontSize=12.sp);Spacer(Modifier.height(5.dp));Text(e.place,color=muted,fontSize=12.sp)} }}
@Composable private fun Detail(e:Event,favorite:Boolean,joined:Boolean,reminded:Int,onBack:()->Unit,onFavorite:()->Unit,onJoin:()->Unit,onReminder:(Int)->Unit){Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(22.dp)){
 Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("‹  Назад",Modifier.clickable(onClick=onBack).padding(vertical=8.dp),color=muted,fontSize=15.sp);Text(if(favorite)"♥" else "♡",Modifier.clickable(onClick=onFavorite).padding(horizontal=8.dp),color=sand,fontSize=27.sp)}; Spacer(Modifier.height(24.dp));Box(Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(22.dp)).background(e.tint)){Image(painterResource(ru.campuslife.app.R.drawable.event_cover),null,Modifier.fillMaxSize(),contentScale=ContentScale.Crop); Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha=.35f)));Text(e.category.uppercase(locale),Modifier.align(Alignment.BottomStart).padding(20.dp),color=sand,fontSize=12.sp,letterSpacing=2.sp)}; Spacer(Modifier.height(25.dp));Text(e.title,color=ink,fontSize=30.sp,fontWeight=FontWeight.SemiBold);Spacer(Modifier.height(8.dp));Text(e.subtitle,color=muted,fontSize=15.sp);Spacer(Modifier.height(25.dp));Info("КОГДА", "${e.date.format(fmt)} · ${e.date.format(DateTimeFormatter.ofPattern("HH:mm"))}");Info("ГДЕ",e.place);Spacer(Modifier.height(12.dp));Text("О событии",color=ink,fontSize=18.sp,fontWeight=FontWeight.Medium);Spacer(Modifier.height(10.dp));Text(e.description,color=muted,lineHeight=23.sp,fontSize=14.sp);Spacer(Modifier.height(24.dp)); Button(onClick=onJoin,modifier=Modifier.fillMaxWidth().height(53.dp),shape=RoundedCornerShape(15.dp),colors=ButtonDefaults.buttonColors(containerColor=if(joined)line else sand,contentColor=if(joined)ink else bg)){Text(if(joined)"Отменить запись" else "Записаться",fontWeight=FontWeight.Bold)};Spacer(Modifier.height(10.dp)); Text("НАПОМИНАНИЕ",color=muted,fontSize=10.sp,letterSpacing=1.3.sp); Spacer(Modifier.height(8.dp)); LazyRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) { rowItems(listOf(0 to "Нет", 10 to "10 мин", 60 to "1 час", 1440 to "1 день")) { (minutes,label) -> Chip(label,reminded==minutes) { onReminder(minutes) } } }; if(reminded>0){ Spacer(Modifier.height(8.dp)); Text("Уведомление может прийти с задержкой из-за настроек энергосбережения телефона.",color=muted,fontSize=11.sp,lineHeight=16.sp) };Spacer(Modifier.height(10.dp));Text("Демонстрационные события · данные сохраняются на устройстве",color=muted,fontSize=11.sp,lineHeight=16.sp)
}}
@Composable private fun Info(label:String,value:String){Column(Modifier.fillMaxWidth().padding(bottom=15.dp).clip(RoundedCornerShape(14.dp)).background(card).padding(15.dp)){Text(label,color=muted,fontSize=10.sp,letterSpacing=1.3.sp);Spacer(Modifier.height(6.dp));Text(value,color=ink,fontSize=14.sp)}}


@Composable private fun CreateEvent(onBack:()->Unit,onSave:(Event)->Unit) {
 val ctx=LocalContext.current; var title by remember { mutableStateOf("") }; var place by remember { mutableStateOf("") }; var desc by remember { mutableStateOf("") }; var category by remember { mutableStateOf("Наука") }; var date by remember { mutableStateOf(LocalDateTime.now().plusDays(1).withHour(16).withMinute(0).withSecond(0).withNano(0)) }; var error by remember { mutableStateOf("") }
 Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(22.dp)) {
  Text("‹  Назад",Modifier.clickable(onClick=onBack).padding(vertical=8.dp),color=muted); Spacer(Modifier.height(18.dp)); Text("Новое событие",fontSize=27.sp,fontWeight=FontWeight.SemiBold,color=ink); Spacer(Modifier.height(8.dp)); Text("Создаётся на этом устройстве",color=muted,fontSize=13.sp); Spacer(Modifier.height(18.dp))
  OutlinedTextField(title,{title=it},label={Text("Название")},modifier=Modifier.fillMaxWidth(),singleLine=true); Spacer(Modifier.height(10.dp)); OutlinedTextField(place,{place=it},label={Text("Место")},modifier=Modifier.fillMaxWidth(),singleLine=true); Spacer(Modifier.height(10.dp)); OutlinedTextField(desc,{desc=it},label={Text("Описание")},modifier=Modifier.fillMaxWidth(),minLines=3); Spacer(Modifier.height(16.dp)); Text("КАТЕГОРИЯ",color=muted,fontSize=10.sp,letterSpacing=1.sp); Spacer(Modifier.height(8.dp)); LazyRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) { rowItems(listOf("Наука","Творчество","Спорт","Технологии")) { c -> Chip(c,c==category){category=c} } }; Spacer(Modifier.height(18.dp))
  Text("ДАТА И ВРЕМЯ",color=muted,fontSize=10.sp,letterSpacing=1.sp); Spacer(Modifier.height(8.dp)); Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){ Chip(date.format(DateTimeFormatter.ofPattern("d MMM yyyy",locale)),false) { DatePickerDialog(ctx,{ _,y,m,d -> date=date.withDayOfMonth(1).withYear(y).withMonth(m+1).withDayOfMonth(d) },date.year,date.monthValue-1,date.dayOfMonth).show() }; Chip(date.format(DateTimeFormatter.ofPattern("HH:mm")),false) { TimePickerDialog(ctx,{ _,h,m -> date=date.withHour(h).withMinute(m) },date.hour,date.minute,true).show() } }; Spacer(Modifier.height(16.dp)); if(error.isNotEmpty()) { Text(error,color=Color(0xFFFFB4AB),fontSize=13.sp); Spacer(Modifier.height(10.dp)) }; Button(onClick={ if(title.isBlank()||place.isBlank()||desc.isBlank()) error="Заполни название, место и описание" else if(date.isBefore(LocalDateTime.now())) error="Выбери дату в будущем" else onSave(Event((System.currentTimeMillis()%Int.MAX_VALUE).toInt(),title.trim(),category,date,place.trim(),desc.trim(),"Событие школьного сообщества",Color(0xFF755E4E))) },modifier=Modifier.fillMaxWidth().height(54.dp),shape=RoundedCornerShape(15.dp),colors=ButtonDefaults.buttonColors(containerColor=sand,contentColor=bg)){Text("Создать событие",fontWeight=FontWeight.Bold)}; Spacer(Modifier.height(24.dp))
 }
}
