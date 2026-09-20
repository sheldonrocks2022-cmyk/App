package com.esn.hub

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

private val DeepNavy=Color(0xFF020914)
private val Navy=Color(0xFF061426)
private val Panel=Color(0xFF0A1B30)
private val Raised=Color(0xFF102A45)
private val Cyan=Color(0xFF62E9FF)
private val Teal=Color(0xFF58D9C8)
private val Ice=Color(0xFFD5FAFF)
private val Muted=Color(0xFF8CA8C0)
private val White=Color(0xFFF5FCFF)
private val Green=Color(0xFF6EE7B7)
private val EsnColors=darkColorScheme(primary=Cyan,secondary=Teal,background=DeepNavy,surface=Panel,onPrimary=DeepNavy,onBackground=White,onSurface=White)

private data class UserSession(val memberId:String,val name:String,val email:String,val role:String="member")
private data class AuthResult(val user:UserSession,val token:String)
private data class AdminMember(val memberId:String,val name:String,val email:String,val role:String,val createdAt:String)
private data class HubAction(val title:String,val subtitle:String,val icon:ImageVector,val tab:Int)
private data class Feature(val title:String,val subtitle:String,val icon:ImageVector)
private enum class AuthMode{LOGIN,REGISTER,RECOVER}

private class SessionStore(context:Context){
    private val prefs=context.getSharedPreferences("esn_session",Context.MODE_PRIVATE)
    private val alias="esn_hub_session_key"
    fun loadUser():UserSession?{
        val id=prefs.getString("member_id",null)?:return null
        return UserSession(id,prefs.getString("name","ESN Member")?:"ESN Member",prefs.getString("email","")?:"",prefs.getString("role","member")?:"member")
    }
    fun saveUser(u:UserSession)=prefs.edit().putString("member_id",u.memberId).putString("name",u.name).putString("email",u.email).putString("role",u.role).apply()
    fun save(u:UserSession,token:String){saveUser(u);saveToken(token)}
    fun clear(){prefs.edit().clear().apply()}
    private fun key():SecretKey{
        val ks=KeyStore.getInstance("AndroidKeyStore").apply{load(null)}
        val existing=ks.getKey(alias,null)
        if(existing is SecretKey)return existing
        val generator=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore")
        generator.init(KeyGenParameterSpec.Builder(alias,KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        return generator.generateKey()
    }
    private fun saveToken(token:String){
        val cipher=Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE,key())
        val value=Base64.encodeToString(cipher.iv,Base64.NO_WRAP)+"."+Base64.encodeToString(cipher.doFinal(token.toByteArray()),Base64.NO_WRAP)
        prefs.edit().putString("token",value).apply()
    }
    fun loadToken():String?=runCatching{
        val parts=(prefs.getString("token",null)?:return null).split(".")
        if(parts.size!=2)return null
        val cipher=Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE,key(),GCMParameterSpec(128,Base64.decode(parts[0],Base64.NO_WRAP)))
        String(cipher.doFinal(Base64.decode(parts[1],Base64.NO_WRAP)))
    }.getOrNull()
}

private class EsnApi{
    private fun base():String=BuildConfig.ESN_API_BASE_URL.trim().trimEnd('/').ifBlank{error("ESN account server is not configured yet.")}
    private suspend fun request(method:String,path:String,body:JSONObject?=null,token:String?=null):JSONObject=withContext(Dispatchers.IO){
        val c=(URL(base()+path).openConnection() as HttpURLConnection).apply{
            requestMethod=method;connectTimeout=10000;readTimeout=10000
            setRequestProperty("Accept","application/json")
            if(body!=null){doOutput=true;setRequestProperty("Content-Type","application/json")}
            if(!token.isNullOrBlank())setRequestProperty("Authorization","Bearer "+token)
        }
        try{
            if(body!=null)c.outputStream.use{it.write(body.toString().toByteArray())}
            val status=c.responseCode
            val stream=if(status in 200..299)c.inputStream else c.errorStream
            val text=stream?.bufferedReader()?.use{it.readText()}.orEmpty()
            val json=runCatching{JSONObject(text)}.getOrElse{JSONObject()}
            if(status !in 200..299)error(json.optString("error").ifBlank{"ESN server returned status "+status+"."})
            json
        }finally{c.disconnect()}
    }
    private fun auth(j:JSONObject)=AuthResult(UserSession(j.getString("memberId"),j.optString("name","ESN Member"),j.getString("email"),j.optString("role","member")),j.getString("sessionToken"))
    suspend fun register(name:String,email:String,password:String,question:String,answer:String)=auth(request("POST","/v1/auth/register",JSONObject().put("name",name).put("email",email).put("password",password).put("securityQuestion",question).put("securityAnswer",answer)))
    suspend fun login(email:String,password:String)=auth(request("POST","/v1/auth/login",JSONObject().put("email",email).put("password",password)))
    suspend fun adminLogin(email:String,password:String)=auth(request("POST","/v1/auth/admin-login",JSONObject().put("email",email).put("password",password)))
    suspend fun recoveryQuestion(email:String)=request("POST","/v1/auth/recovery-question",JSONObject().put("email",email)).getString("question")
    suspend fun resetPassword(email:String,answer:String,newPassword:String)=request("POST","/v1/auth/reset-password",JSONObject().put("email",email).put("securityAnswer",answer).put("newPassword",newPassword))
    suspend fun session(token:String):UserSession{val j=request("GET","/v1/auth/session",token=token);return UserSession(j.getString("memberId"),j.optString("name","ESN Member"),j.getString("email"),j.optString("role","member"))}
    suspend fun adminMembers(token:String):List<AdminMember>{val a=request("GET","/v1/admin/members",token=token).getJSONArray("members");return (0 until a.length()).map{val j=a.getJSONObject(it);AdminMember(j.getString("memberId"),j.optString("name","ESN Member"),j.getString("email"),j.optString("role","member"),j.optString("createdAt",""))}}
}

class MainActivity:ComponentActivity(){
    override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);setContent{MaterialTheme(colorScheme=EsnColors){EsnHubApp(this)}}}
}

@Composable
private fun EsnHubApp(context:Context){
    val store=remember{SessionStore(context)}
    val api=remember{EsnApi()}
    var tab by remember{mutableIntStateOf(0)}
    var user by remember{mutableStateOf(store.loadUser())}
    var token by remember{mutableStateOf(store.loadToken())}
    var message by remember{mutableStateOf<String?>(null)}
    LaunchedEffect(Unit){
        val t=token
        if(t!=null)runCatching{api.session(t)}.onSuccess{user=it;store.saveUser(it)}.onFailure{store.clear();user=null;token=null;message="Your session expired. Please sign in again."}
        else if(user!=null){store.clear();user=null}
    }
    fun authenticated(result:AuthResult){store.save(result.user,result.token);user=result.user;token=result.token;message=if(result.user.role=="admin")"Admin access verified." else "Signed in to ESN Hub."}
    fun signOut(){store.clear();user=null;token=null;message="Signed out."}
    Scaffold(containerColor=DeepNavy,bottomBar={
        NavigationBar(containerColor=Color(0xFF04101E)){
            listOf(Triple("Home",Icons.Default.Home,0),Triple("Store",Icons.Default.Storefront,1),Triple("Community",Icons.Default.Groups,2),Triple("Profile",Icons.Default.Person,3)).forEach{(label,icon,index)->
                NavigationBarItem(selected=tab==index,onClick={tab=index},icon={Icon(icon,label)},label={Text(label)},colors=NavigationBarItemDefaults.colors(selectedIconColor=Ice,selectedTextColor=Ice,indicatorColor=Cyan.copy(alpha=.18f),unselectedIconColor=Muted,unselectedTextColor=Muted))
            }
        }
    }){pad->
        when(tab){
            0->Home(Modifier.padding(pad),user,{tab=it},{openWebsite(context)})
            1->Store(Modifier.padding(pad),user,{openWebsite(context)})
            2->Community(Modifier.padding(pad),user,{tab=3})
            else->Profile(Modifier.padding(pad),user,token,message,api,::authenticated,::signOut,{openWebsite(context)})
        }
    }
}

private fun openWebsite(context:Context){runCatching{context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(BuildConfig.ESN_WEBSITE_URL)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))}}

@Composable
private fun BrandHeader(title:String,subtitle:String){
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
        Box(Modifier.size(62.dp).background(Brush.radialGradient(listOf(Cyan.copy(alpha=.35f),Navy)),CircleShape).border(1.dp,Cyan.copy(alpha=.7f),CircleShape),contentAlignment=Alignment.Center){Text("ES",color=Ice,fontWeight=FontWeight.Black,fontSize=22.sp)}
        Spacer(Modifier.width(14.dp));Column{Text(title,color=White,fontWeight=FontWeight.Black,fontSize=25.sp,letterSpacing=1.2.sp);Text(subtitle,color=Muted,fontSize=12.sp)}
    }
}

@Composable
private fun Home(modifier:Modifier,user:UserSession?,selectTab:(Int)->Unit,openWeb:()->Unit){
    val actions=listOf(HubAction("ESN Store","Products, services & rewards",Icons.Default.ShoppingBag,1),HubAction("Invite Rewards","Grow ESN and earn credits",Icons.Default.LocalFireDepartment,2),HubAction("Support","Tickets and member help",Icons.Default.SupportAgent,2),HubAction("My Profile","Account, credits & history",Icons.Default.AccountCircle,3))
    LazyColumn(modifier.fillMaxSize().background(Brush.verticalGradient(listOf(DeepNavy,Navy,DeepNavy))).padding(horizontal=18.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
        item{Spacer(Modifier.height(18.dp));BrandHeader("ESN HUB","ES Network • official mobile hub")}
        item{WelcomeCard(user){selectTab(3)}};item{CreditsCard(user){selectTab(3)}};item{Section("QUICK ACCESS","Everything ESN in one place.")}
        items(actions.chunked(2)){row->Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp)){row.forEach{a->ActionCard(a,Modifier.weight(1f)){selectTab(a.tab)}};if(row.size==1)Spacer(Modifier.weight(1f))}}
        item{WebsiteCard(openWeb)};item{Section("LATEST","News and updates from ES Network.")}
        item{NewsCard("ACCOUNTS","Native ESN accounts are here","Create an ESN account with email and password. Account recovery uses your protected security answer.")}
        item{NewsCard("SECURITY","Admin access is server protected","Admin tools only appear after the server verifies the account's admin role.")}
        item{Spacer(Modifier.height(22.dp))}
    }
}

@Composable
private fun WelcomeCard(user:UserSession?,click:()->Unit){
    Card(Modifier.fillMaxWidth().clickable(onClick=click),shape=RoundedCornerShape(22.dp),colors=CardDefaults.cardColors(containerColor=Panel)){
        Row(Modifier.padding(18.dp),verticalAlignment=Alignment.CenterVertically){
            Box(Modifier.size(48.dp).background(Cyan.copy(alpha=.12f),CircleShape).border(1.dp,Cyan.copy(alpha=.35f),CircleShape),contentAlignment=Alignment.Center){Text(user?.name?.take(1)?.uppercase()?:"E",color=Ice,fontWeight=FontWeight.Black,fontSize=19.sp)}
            Spacer(Modifier.width(13.dp));Column(Modifier.weight(1f)){Text(if(user==null)"Welcome to ES Network" else "Welcome back, "+user.name.substringBefore(" "),color=White,fontWeight=FontWeight.Bold);Text(if(user==null)"Create or sign in to your ESN account." else "ESN account connected • "+user.role.uppercase(),color=Muted,fontSize=12.sp)}
            Icon(if(user==null)Icons.Default.AccountCircle else Icons.Default.VerifiedUser,null,tint=if(user==null)Muted else Green)
        }
    }
}

@Composable
private fun CreditsCard(user:UserSession?,click:()->Unit){
    Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(28.dp),colors=CardDefaults.cardColors(containerColor=Color.Transparent)){
        Column(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Color(0xFF0B456B),Color(0xFF08747A),Color(0xFF113B64)))).border(1.dp,Cyan.copy(alpha=.5f),RoundedCornerShape(28.dp)).padding(22.dp)){
            Row(verticalAlignment=Alignment.CenterVertically){Icon(Icons.Default.CreditCard,null,tint=Ice);Spacer(Modifier.width(8.dp));Text("ESN CREDITS",color=Ice,fontWeight=FontWeight.Black,letterSpacing=1.2.sp)}
            Spacer(Modifier.height(12.dp));Text("0",color=White,fontSize=46.sp,fontWeight=FontWeight.Black);Text(if(user==null)"Sign in to connect your member account." else "Account connected • live balance sync is next.",color=White.copy(alpha=.78f),fontSize=13.sp)
            Spacer(Modifier.height(16.dp));Button(onClick=click,colors=ButtonDefaults.buttonColors(containerColor=Ice,contentColor=DeepNavy),shape=RoundedCornerShape(14.dp)){Text(if(user==null)"CONNECT ACCOUNT" else "VIEW PROFILE",fontWeight=FontWeight.Black)}
        }
    }
}

@Composable
private fun ActionCard(action:HubAction,modifier:Modifier,click:()->Unit){
    Card(modifier.height(150.dp).clickable(onClick=click),shape=RoundedCornerShape(22.dp),colors=CardDefaults.cardColors(containerColor=Panel)){
        Column(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.SpaceBetween){
            Box(Modifier.size(42.dp).background(Cyan.copy(alpha=.1f),RoundedCornerShape(13.dp)).border(1.dp,Cyan.copy(alpha=.25f),RoundedCornerShape(13.dp)),contentAlignment=Alignment.Center){Icon(action.icon,null,tint=Cyan)}
            Column{Text(action.title,color=White,fontWeight=FontWeight.Bold);Text(action.subtitle,color=Muted,fontSize=12.sp,lineHeight=16.sp)}
        }
    }
}

@Composable
private fun Store(modifier:Modifier,user:UserSession?,openWeb:()->Unit){
    val features=listOf(Feature("Live catalog","ESN products, services, drops and rewards.",Icons.Default.Storefront),Feature("Credit redemptions","Redeem eligible items using ESN Credits.",Icons.Default.Redeem),Feature("Order history","Purchases and redemptions tied to your profile.",Icons.Default.ReceiptLong))
    LazyColumn(modifier.fillMaxSize().background(DeepNavy).padding(horizontal=18.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
        item{Spacer(Modifier.height(18.dp));BrandHeader("ESN STORE","The ES Network storefront")}
        item{Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(26.dp),colors=CardDefaults.cardColors(containerColor=Panel)){Column(Modifier.padding(20.dp)){Text("ES Network Store",color=White,fontSize=24.sp,fontWeight=FontWeight.Black);Spacer(Modifier.height(7.dp));Text("Products, rewards and account history in one ESN experience.",color=Muted,lineHeight=20.sp);Spacer(Modifier.height(16.dp));Button(onClick=openWeb,colors=ButtonDefaults.buttonColors(containerColor=Cyan,contentColor=DeepNavy)){Text("OPEN LIVE STORE",fontWeight=FontWeight.Black)}}}}
        items(features){FeatureRow(it)}
        item{StatusCard(if(user==null)"SIGN IN REQUIRED" else "ACCOUNT CONNECTED",if(user==null)"Sign in from Profile so future purchases and rewards can attach to your ESN identity." else "Your ESN account is ready for store syncing.",user!=null)}
        item{Spacer(Modifier.height(22.dp))}
    }
}

@Composable
private fun Community(modifier:Modifier,user:UserSession?,profile:()->Unit){
    val features=listOf(Feature("Invite Rewards","Track successful invites and earned credits.",Icons.Default.LocalFireDepartment),Feature("Support Tickets","Create and follow member support requests.",Icons.Default.SupportAgent),Feature("Announcements","Important ESN updates in one feed.",Icons.Default.Campaign),Feature("Giveaways & Events","Community drops, events and giveaways.",Icons.Default.Celebration))
    LazyColumn(modifier.fillMaxSize().background(DeepNavy).padding(horizontal=18.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
        item{Spacer(Modifier.height(18.dp));BrandHeader("COMMUNITY","Rewards • support • events")};items(features){FeatureRow(it)}
        item{Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(22.dp),colors=CardDefaults.cardColors(containerColor=Raised)){Column(Modifier.padding(18.dp)){Text(if(user==null)"Connect your member identity" else "Member identity connected",color=White,fontWeight=FontWeight.Black,fontSize=18.sp);Spacer(Modifier.height(6.dp));Text(if(user==null)"Your ESN account connects rewards, tickets and history to one profile." else user.name+" is ready for community syncing.",color=Muted,lineHeight=19.sp);if(user==null){Spacer(Modifier.height(14.dp));Button(onClick=profile,colors=ButtonDefaults.buttonColors(containerColor=Cyan,contentColor=DeepNavy)){Text("SIGN IN",fontWeight=FontWeight.Black)}}}}}
        item{Spacer(Modifier.height(22.dp))}
    }
}

@Composable
private fun Profile(modifier:Modifier,user:UserSession?,token:String?,message:String?,api:EsnApi,onAuthenticated:(AuthResult)->Unit,signOut:()->Unit,openWeb:()->Unit){
    LazyColumn(modifier.fillMaxSize().background(DeepNavy).padding(horizontal=18.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
        item{Spacer(Modifier.height(18.dp));BrandHeader("PROFILE","Your ES Network identity")}
        if(user==null)item{AuthPanel(api,onAuthenticated)}
        else{
            item{AccountCard(user,signOut)}
            if(user.role=="admin"&&token!=null)item{AdminDashboard(api,token)}
        }
        if(!message.isNullOrBlank())item{StatusCard("ACCOUNT STATUS",message,user!=null)}
        item{FeatureRow(Feature("Protected session","Your session token is encrypted using Android Keystore before local storage.",Icons.Default.Lock))}
        item{WebsiteCard(openWeb)};item{Spacer(Modifier.height(22.dp))}
    }
}

@Composable
private fun AuthPanel(api:EsnApi,onAuthenticated:(AuthResult)->Unit){
    var mode by remember{mutableStateOf(AuthMode.LOGIN)}
    var email by remember{mutableStateOf("")};var password by remember{mutableStateOf("")};var confirm by remember{mutableStateOf("")};var name by remember{mutableStateOf("")};var question by remember{mutableStateOf("")};var answer by remember{mutableStateOf("")};var recoveryQuestion by remember{mutableStateOf<String?>(null)}
    var busy by remember{mutableStateOf(false)};var status by remember{mutableStateOf<String?>(null)}
    val scope=rememberCoroutineScope()
    fun run(block:suspend()->Unit){if(busy)return;busy=true;status=null;scope.launch{runCatching{block()}.onFailure{status=it.message?:"Request failed."};busy=false}}
    Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(28.dp),colors=CardDefaults.cardColors(containerColor=Panel)){
        Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                listOf(AuthMode.LOGIN to "SIGN IN",AuthMode.REGISTER to "CREATE",AuthMode.RECOVER to "RECOVER").forEach{(m,label)->FilterChip(selected=mode==m,onClick={mode=m;status=null},label={Text(label)})}
            }
            when(mode){
                AuthMode.LOGIN->{
                    Text("ESN ACCOUNT",color=Cyan,fontWeight=FontWeight.Black);EmailField(email){email=it};PasswordField("Password",password){password=it}
                    Button(onClick={run{onAuthenticated(api.login(email,password))}},enabled=!busy,modifier=Modifier.fillMaxWidth(),colors=ButtonDefaults.buttonColors(containerColor=Cyan,contentColor=DeepNavy)){Text(if(busy)"SIGNING IN..." else "SIGN IN",fontWeight=FontWeight.Black)}
                    OutlinedButton(onClick={run{onAuthenticated(api.adminLogin(email,password))}},enabled=!busy,modifier=Modifier.fillMaxWidth(),colors=ButtonDefaults.outlinedButtonColors(contentColor=Ice)){Icon(Icons.Default.AdminPanelSettings,null);Spacer(Modifier.width(8.dp));Text("ADMIN LOGIN",fontWeight=FontWeight.Bold)}
                    TextButton(onClick={mode=AuthMode.RECOVER}){Text("Forgot password?")}
                }
                AuthMode.REGISTER->{
                    Text("CREATE ESN ACCOUNT",color=Cyan,fontWeight=FontWeight.Black);InputField("Display name",name){name=it};EmailField(email){email=it};PasswordField("Password (10+ characters)",password){password=it};PasswordField("Confirm password",confirm){confirm=it};InputField("Secret question",question){question=it};PasswordField("Secret answer",answer){answer=it}
                    Text("Your secret answer is hashed on the server and is never stored as readable text.",color=Muted,fontSize=11.sp,lineHeight=16.sp)
                    Button(onClick={if(password!=confirm){status="Passwords do not match."}else run{onAuthenticated(api.register(name,email,password,question,answer))}},enabled=!busy,modifier=Modifier.fillMaxWidth(),colors=ButtonDefaults.buttonColors(containerColor=Cyan,contentColor=DeepNavy)){Text(if(busy)"CREATING..." else "CREATE ACCOUNT",fontWeight=FontWeight.Black)}
                }
                AuthMode.RECOVER->{
                    Text("RESET PASSWORD",color=Cyan,fontWeight=FontWeight.Black);EmailField(email){email=it;recoveryQuestion=null}
                    if(recoveryQuestion==null){
                        Button(onClick={run{recoveryQuestion=api.recoveryQuestion(email)}},enabled=!busy,modifier=Modifier.fillMaxWidth()){Text(if(busy)"CHECKING..." else "GET SECURITY QUESTION")}
                    }else{
                        Card(colors=CardDefaults.cardColors(containerColor=Raised)){Column(Modifier.padding(14.dp)){Text("SECURITY QUESTION",color=Cyan,fontWeight=FontWeight.Black,fontSize=11.sp);Text(recoveryQuestion!!,color=White)}}
                        PasswordField("Secret answer",answer){answer=it};PasswordField("New password (10+ characters)",password){password=it};PasswordField("Confirm new password",confirm){confirm=it}
                        Button(onClick={if(password!=confirm){status="Passwords do not match."}else run{api.resetPassword(email,answer,password);status="Password reset. You can sign in with your new password.";password="";confirm="";answer="";recoveryQuestion=null}},enabled=!busy,modifier=Modifier.fillMaxWidth(),colors=ButtonDefaults.buttonColors(containerColor=Cyan,contentColor=DeepNavy)){Text(if(busy)"RESETTING..." else "RESET PASSWORD",fontWeight=FontWeight.Black)}
                    }
                }
            }
            if(!status.isNullOrBlank())StatusCard("ACCOUNT",status!!,false)
        }
    }
}

@Composable private fun InputField(label:String,value:String,onChange:(String)->Unit){OutlinedTextField(value=value,onValueChange=onChange,label={Text(label)},singleLine=true,modifier=Modifier.fillMaxWidth())}
@Composable private fun EmailField(value:String,onChange:(String)->Unit){OutlinedTextField(value=value,onValueChange=onChange,label={Text("Email")},singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Email),modifier=Modifier.fillMaxWidth())}
@Composable private fun PasswordField(label:String,value:String,onChange:(String)->Unit){OutlinedTextField(value=value,onValueChange=onChange,label={Text(label)},singleLine=true,visualTransformation=PasswordVisualTransformation(),keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Password),modifier=Modifier.fillMaxWidth())}

@Composable
private fun AccountCard(user:UserSession,signOut:()->Unit){
    Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(28.dp),colors=CardDefaults.cardColors(containerColor=Panel)){
        Column(Modifier.padding(22.dp),horizontalAlignment=Alignment.CenterHorizontally){
            Box(Modifier.size(78.dp).background(Cyan.copy(alpha=.1f),CircleShape).border(1.dp,Cyan.copy(alpha=.4f),CircleShape),contentAlignment=Alignment.Center){Text(user.name.take(1).uppercase(),color=Ice,fontWeight=FontWeight.Black,fontSize=28.sp)}
            Spacer(Modifier.height(15.dp));Text(user.name,color=White,fontWeight=FontWeight.Black,fontSize=23.sp);Text(user.email,color=Muted,fontSize=12.sp);Spacer(Modifier.height(8.dp))
            AssistChip(onClick={},label={Text(if(user.role=="admin")"ESN ADMIN" else "ESN MEMBER")},leadingIcon={Icon(if(user.role=="admin")Icons.Default.AdminPanelSettings else Icons.Default.VerifiedUser,null)})
            Spacer(Modifier.height(8.dp));Text("Member ID: "+user.memberId,color=Muted,fontSize=11.sp);Spacer(Modifier.height(16.dp))
            OutlinedButton(onClick=signOut,modifier=Modifier.fillMaxWidth(),colors=ButtonDefaults.outlinedButtonColors(contentColor=Ice)){Text("SIGN OUT")}
        }
    }
}

@Composable
private fun AdminDashboard(api:EsnApi,token:String){
    var members by remember{mutableStateOf<List<AdminMember>>(emptyList())};var status by remember{mutableStateOf<String?>(null)};var busy by remember{mutableStateOf(false)};val scope=rememberCoroutineScope()
    fun load(){if(busy)return;busy=true;scope.launch{runCatching{api.adminMembers(token)}.onSuccess{members=it;status=null}.onFailure{status=it.message};busy=false}}
    LaunchedEffect(token){load()}
    Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(24.dp),colors=CardDefaults.cardColors(containerColor=Color(0xFF081F30))){
        Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
            Row(verticalAlignment=Alignment.CenterVertically){Icon(Icons.Default.AdminPanelSettings,null,tint=Cyan);Spacer(Modifier.width(8.dp));Column(Modifier.weight(1f)){Text("ADMIN DASHBOARD",color=White,fontWeight=FontWeight.Black);Text("Server-verified owner controls",color=Muted,fontSize=11.sp)};Text(members.size.toString(),color=Cyan,fontWeight=FontWeight.Black,fontSize=22.sp)}
            Button(onClick={load()},enabled=!busy,colors=ButtonDefaults.buttonColors(containerColor=Cyan,contentColor=DeepNavy)){Text(if(busy)"REFRESHING..." else "REFRESH MEMBERS",fontWeight=FontWeight.Black)}
            status?.let{Text(it,color=Muted,fontSize=12.sp)}
            members.take(10).forEach{m->HorizontalDivider(color=Cyan.copy(alpha=.12f));Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(m.name,color=White,fontWeight=FontWeight.Bold);Text(m.email,color=Muted,fontSize=11.sp)};Text(m.role.uppercase(),color=if(m.role=="admin")Cyan else Green,fontSize=10.sp,fontWeight=FontWeight.Black)}}
            if(members.size>10)Text("Showing first 10 of "+members.size+" accounts.",color=Muted,fontSize=11.sp)
        }
    }
}

@Composable
private fun FeatureRow(feature:Feature){Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=Panel)){Row(Modifier.padding(18.dp),verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(44.dp).background(Cyan.copy(alpha=.1f),RoundedCornerShape(14.dp)).border(1.dp,Cyan.copy(alpha=.22f),RoundedCornerShape(14.dp)),contentAlignment=Alignment.Center){Icon(feature.icon,null,tint=Cyan)};Spacer(Modifier.width(13.dp));Column{Text(feature.title,color=White,fontWeight=FontWeight.Bold,fontSize=16.sp);Text(feature.subtitle,color=Muted,fontSize=12.sp,lineHeight=17.sp)}}}}
@Composable
private fun WebsiteCard(openWeb:()->Unit){Card(Modifier.fillMaxWidth().clickable(onClick=openWeb),shape=RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=Raised)){Row(Modifier.padding(18.dp),verticalAlignment=Alignment.CenterVertically){Icon(Icons.Default.Language,null,tint=Cyan);Spacer(Modifier.width(12.dp));Column(Modifier.weight(1f)){Text("ESN Official",color=White,fontWeight=FontWeight.Bold);Text("esnoffical.com",color=Muted,fontSize=12.sp)};Text("OPEN",color=Cyan,fontWeight=FontWeight.Black,fontSize=12.sp)}}}
@Composable
private fun NewsCard(tag:String,title:String,body:String){Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=Panel)){Column(Modifier.padding(18.dp)){Text(tag,color=Cyan,fontWeight=FontWeight.Black,fontSize=11.sp,letterSpacing=1.2.sp);Spacer(Modifier.height(6.dp));Text(title,color=White,fontWeight=FontWeight.Bold,fontSize=17.sp);Spacer(Modifier.height(5.dp));Text(body,color=Muted,fontSize=13.sp,lineHeight=19.sp)}}}
@Composable
private fun StatusCard(title:String,body:String,positive:Boolean){Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=if(positive)Color(0xFF092A2A) else Raised)){Column(Modifier.padding(18.dp)){Text(title,color=if(positive)Green else Cyan,fontWeight=FontWeight.Black,fontSize=12.sp);Spacer(Modifier.height(5.dp));Text(body,color=Muted,fontSize=12.sp,lineHeight=18.sp)}}}
@Composable
private fun Section(title:String,subtitle:String){Column{Text(title,color=White,fontWeight=FontWeight.Black,fontSize=18.sp,letterSpacing=.8.sp);Text(subtitle,color=Muted,fontSize=12.sp)}}
