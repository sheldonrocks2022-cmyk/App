package com.esn.hub

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch

private val DeepNavy = Color(0xFF020914)
private val Navy = Color(0xFF061426)
private val Panel = Color(0xFF0A1B30)
private val Raised = Color(0xFF102A45)
private val Cyan = Color(0xFF62E9FF)
private val Teal = Color(0xFF58D9C8)
private val Ice = Color(0xFFD5FAFF)
private val Muted = Color(0xFF8CA8C0)
private val White = Color(0xFFF5FCFF)
private val Green = Color(0xFF6EE7B7)

private val EsnColors = darkColorScheme(
    primary = Cyan, secondary = Teal, background = DeepNavy,
    surface = Panel, onPrimary = DeepNavy, onBackground = White, onSurface = White
)

private data class UserSession(val id: String, val name: String, val email: String)
private data class HubAction(val title: String, val subtitle: String, val icon: ImageVector, val tab: Int)
private data class Feature(val title: String, val subtitle: String, val icon: ImageVector)

private class SessionStore(context: Context) {
    private val prefs = context.getSharedPreferences("esn_session", Context.MODE_PRIVATE)
    fun load(): UserSession? {
        val id = prefs.getString("id", null) ?: return null
        return UserSession(id, prefs.getString("name", "ESN Member") ?: "ESN Member", prefs.getString("email", "") ?: "")
    }
    fun save(user: UserSession) = prefs.edit().putString("id", user.id).putString("name", user.name).putString("email", user.email).apply()
    fun clear() = prefs.edit().clear().apply()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme(colorScheme = EsnColors) { EsnHubApp(this) } }
    }
}

@Composable
private fun EsnHubApp(activity: Activity) {
    val store = remember { SessionStore(activity) }
    var tab by remember { mutableIntStateOf(0) }
    var user by remember { mutableStateOf(store.load()) }
    var authMessage by remember { mutableStateOf<String?>(null) }
    var authBusy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun signIn() {
        if (BuildConfig.GOOGLE_WEB_CLIENT_ID.isBlank()) {
            authMessage = "Google sign-in is ready. Add GOOGLE_WEB_CLIENT_ID to the repository configuration to activate live login."
            return
        }
        if (authBusy) return
        authBusy = true
        authMessage = null
        scope.launch {
            runCatching { googleSignIn(activity) }
                .onSuccess { store.save(it); user = it; authMessage = "Google account connected." }
                .onFailure { authMessage = it.message ?: "Google sign-in could not be completed." }
            authBusy = false
        }
    }

    Scaffold(
        containerColor = DeepNavy,
        bottomBar = {
            NavigationBar(containerColor = Color(0xFF04101E)) {
                listOf(
                    Triple("Home", Icons.Default.Home, 0),
                    Triple("Store", Icons.Default.Storefront, 1),
                    Triple("Community", Icons.Default.Groups, 2),
                    Triple("Profile", Icons.Default.Person, 3)
                ).forEach { (label, icon, index) ->
                    NavigationBarItem(
                        selected = tab == index,
                        onClick = { tab = index },
                        icon = { Icon(icon, label) },
                        label = { Text(label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Ice, selectedTextColor = Ice,
                            indicatorColor = Cyan.copy(alpha = .18f),
                            unselectedIconColor = Muted, unselectedTextColor = Muted
                        )
                    )
                }
            }
        }
    ) { pad ->
        when (tab) {
            0 -> Home(Modifier.padding(pad), user, { tab = it }, { openWebsite(activity) })
            1 -> Store(Modifier.padding(pad), user, { openWebsite(activity) })
            2 -> Community(Modifier.padding(pad), user, { tab = 3 })
            else -> Profile(
                Modifier.padding(pad), user, authBusy, authMessage,
                ::signIn,
                { store.clear(); user = null; authMessage = "Signed out." },
                { openWebsite(activity) }
            )
        }
    }
}

private suspend fun googleSignIn(activity: Activity): UserSession {
    val option = GetSignInWithGoogleOption.Builder(BuildConfig.GOOGLE_WEB_CLIENT_ID).build()
    val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
    val credential = CredentialManager.create(activity).getCredential(activity, request).credential
    require(credential is CustomCredential) { "Unsupported Google credential." }
    val google = GoogleIdTokenCredential.createFrom(credential.data)
    val email = google.email.orEmpty()
    val name = google.displayName ?: google.givenName ?: email.substringBefore("@").ifBlank { "ESN Member" }
    return UserSession(google.id, name, email)
}

private fun openWebsite(context: Context) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(BuildConfig.ESN_WEBSITE_URL)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

@Composable
private fun BrandHeader(title: String, subtitle: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(62.dp).background(Brush.radialGradient(listOf(Cyan.copy(alpha=.35f), Navy)), CircleShape)
                .border(1.dp, Cyan.copy(alpha=.7f), CircleShape),
            contentAlignment = Alignment.Center
        ) { Text("ES", color = Ice, fontWeight = FontWeight.Black, fontSize = 22.sp) }
        Spacer(Modifier.width(14.dp))
        Column {
            Text(title, color = White, fontWeight = FontWeight.Black, fontSize = 25.sp, letterSpacing = 1.2.sp)
            Text(subtitle, color = Muted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun Home(modifier: Modifier, user: UserSession?, selectTab: (Int)->Unit, openWeb: ()->Unit) {
    val actions = listOf(
        HubAction("ESN Store", "Products, services & rewards", Icons.Default.ShoppingBag, 1),
        HubAction("Invite Rewards", "Grow ESN and earn credits", Icons.Default.LocalFireDepartment, 2),
        HubAction("Support", "Tickets and member help", Icons.Default.SupportAgent, 2),
        HubAction("My Profile", "Account, credits & history", Icons.Default.AccountCircle, 3)
    )
    LazyColumn(
        modifier.fillMaxSize().background(Brush.verticalGradient(listOf(DeepNavy, Navy, DeepNavy))).padding(horizontal=18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { Spacer(Modifier.height(18.dp)); BrandHeader("ESN HUB", "ES Network • official mobile hub") }
        item { WelcomeCard(user) { selectTab(3) } }
        item { CreditsCard(user) { selectTab(3) } }
        item { Section("QUICK ACCESS", "Everything ESN in one place.") }
        items(actions.chunked(2)) { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { action -> ActionCard(action, Modifier.weight(1f)) { selectTab(action.tab) } }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        item { WebsiteCard(openWeb) }
        item { Section("LATEST", "News and updates from ES Network.") }
        item { NewsCard("APP UPDATE", "Welcome to the new ESN Hub", "A native ES Network experience built around the new cyan, teal and dark-tech brand system.") }
        item { NewsCard("MEMBERS", "One account across the hub", "Google sign-in is the identity foundation for credits, rewards, purchases and support history.") }
        item { Spacer(Modifier.height(22.dp)) }
    }
}

@Composable
private fun WelcomeCard(user: UserSession?, click: ()->Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick=click), shape=RoundedCornerShape(22.dp), colors=CardDefaults.cardColors(containerColor=Panel)) {
        Row(Modifier.padding(18.dp), verticalAlignment=Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).background(Cyan.copy(alpha=.12f), CircleShape).border(1.dp,Cyan.copy(alpha=.35f),CircleShape), contentAlignment=Alignment.Center) {
                Text(user?.name?.take(1)?.uppercase() ?: "E", color=Ice, fontWeight=FontWeight.Black, fontSize=19.sp)
            }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text(if(user==null) "Welcome to ES Network" else "Welcome back, " + user.name.substringBefore(" "), color=White, fontWeight=FontWeight.Bold)
                Text(if(user==null) "Connect Google to create your ESN Hub identity." else "Google account connected • member profile ready", color=Muted, fontSize=12.sp)
            }
            Icon(Icons.Default.VerifiedUser, null, tint=if(user==null) Muted else Green)
        }
    }
}

@Composable
private fun CreditsCard(user: UserSession?, click: ()->Unit) {
    Card(Modifier.fillMaxWidth(), shape=RoundedCornerShape(28.dp), colors=CardDefaults.cardColors(containerColor=Color.Transparent)) {
        Column(
            Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Color(0xFF0B456B),Color(0xFF08747A),Color(0xFF113B64))))
                .border(1.dp,Cyan.copy(alpha=.5f),RoundedCornerShape(28.dp)).padding(22.dp)
        ) {
            Row(verticalAlignment=Alignment.CenterVertically) {
                Icon(Icons.Default.CreditCard,null,tint=Ice); Spacer(Modifier.width(8.dp))
                Text("ESN CREDITS",color=Ice,fontWeight=FontWeight.Black,letterSpacing=1.2.sp)
            }
            Spacer(Modifier.height(12.dp))
            Text("0",color=White,fontSize=46.sp,fontWeight=FontWeight.Black)
            Text(if(user==null) "Sign in to connect your member account." else "Account connected • live balance sync is next.", color=White.copy(alpha=.78f),fontSize=13.sp)
            Spacer(Modifier.height(16.dp))
            Button(onClick=click, colors=ButtonDefaults.buttonColors(containerColor=Ice,contentColor=DeepNavy),shape=RoundedCornerShape(14.dp)) {
                Text(if(user==null) "CONNECT ACCOUNT" else "VIEW PROFILE",fontWeight=FontWeight.Black)
            }
        }
    }
}

@Composable
private fun ActionCard(action: HubAction, modifier: Modifier, click: ()->Unit) {
    Card(modifier.height(150.dp).clickable(onClick=click), shape=RoundedCornerShape(22.dp), colors=CardDefaults.cardColors(containerColor=Panel)) {
        Column(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.SpaceBetween) {
            Box(Modifier.size(42.dp).background(Cyan.copy(alpha=.1f),RoundedCornerShape(13.dp)).border(1.dp,Cyan.copy(alpha=.25f),RoundedCornerShape(13.dp)),contentAlignment=Alignment.Center) {
                Icon(action.icon,null,tint=Cyan)
            }
            Column { Text(action.title,color=White,fontWeight=FontWeight.Bold); Text(action.subtitle,color=Muted,fontSize=12.sp,lineHeight=16.sp) }
        }
    }
}

@Composable
private fun Store(modifier: Modifier, user: UserSession?, openWeb: ()->Unit) {
    val features=listOf(
        Feature("Live catalog","ESN products, services, drops and rewards.",Icons.Default.Storefront),
        Feature("Credit redemptions","Redeem eligible items using ESN Credits.",Icons.Default.Redeem),
        Feature("Order history","Purchases and redemptions tied to your profile.",Icons.Default.ReceiptLong)
    )
    LazyColumn(modifier.fillMaxSize().background(DeepNavy).padding(horizontal=18.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
        item { Spacer(Modifier.height(18.dp)); BrandHeader("ESN STORE","The ES Network storefront") }
        item {
            Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(26.dp),colors=CardDefaults.cardColors(containerColor=Panel)) {
                Column(Modifier.padding(20.dp)) {
                    Text("ES Network Store",color=White,fontSize=24.sp,fontWeight=FontWeight.Black)
                    Spacer(Modifier.height(7.dp))
                    Text("A native storefront built to mirror the ESN web experience while keeping credits, rewards and account history in one app.",color=Muted,lineHeight=20.sp)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick=openWeb,colors=ButtonDefaults.buttonColors(containerColor=Cyan,contentColor=DeepNavy)) { Text("OPEN LIVE STORE",fontWeight=FontWeight.Black) }
                }
            }
        }
        items(features) { FeatureRow(it) }
        item { StatusCard(if(user==null) "SIGN IN REQUIRED" else "ACCOUNT CONNECTED", if(user==null) "Connect Google from Profile so future purchases and rewards can attach to your ESN identity." else "Your Google identity is ready for ESN store syncing.", user!=null) }
        item { Spacer(Modifier.height(22.dp)) }
    }
}

@Composable
private fun Community(modifier: Modifier, user: UserSession?, profile: ()->Unit) {
    val features=listOf(
        Feature("Invite Rewards","Track successful invites and earned credits.",Icons.Default.LocalFireDepartment),
        Feature("Support Tickets","Create and follow member support requests.",Icons.Default.SupportAgent),
        Feature("Announcements","Important ESN updates in one feed.",Icons.Default.Campaign),
        Feature("Giveaways & Events","Community drops, events and giveaways.",Icons.Default.Celebration)
    )
    LazyColumn(modifier.fillMaxSize().background(DeepNavy).padding(horizontal=18.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
        item { Spacer(Modifier.height(18.dp)); BrandHeader("COMMUNITY","Rewards • support • events") }
        items(features) { FeatureRow(it) }
        item {
            Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(22.dp),colors=CardDefaults.cardColors(containerColor=Raised)) {
                Column(Modifier.padding(18.dp)) {
                    Text(if(user==null) "Connect your member identity" else "Member identity connected",color=White,fontWeight=FontWeight.Black,fontSize=18.sp)
                    Spacer(Modifier.height(6.dp))
                    Text(if(user==null) "Google sign-in will connect rewards, tickets and history to one ESN profile." else user.name + " is ready for community syncing.",color=Muted,lineHeight=19.sp)
                    if(user==null) { Spacer(Modifier.height(14.dp)); Button(onClick=profile,colors=ButtonDefaults.buttonColors(containerColor=Cyan,contentColor=DeepNavy)){Text("SIGN IN",fontWeight=FontWeight.Black)} }
                }
            }
        }
        item { Spacer(Modifier.height(22.dp)) }
    }
}

@Composable
private fun Profile(modifier: Modifier,user: UserSession?,busy:Boolean,message:String?,signIn:()->Unit,signOut:()->Unit,openWeb:()->Unit) {
    LazyColumn(modifier.fillMaxSize().background(DeepNavy).padding(horizontal=18.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
        item { Spacer(Modifier.height(18.dp)); BrandHeader("PROFILE","Your ES Network identity") }
        item {
            Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(28.dp),colors=CardDefaults.cardColors(containerColor=Panel)) {
                Column(Modifier.padding(22.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                    Box(Modifier.size(78.dp).background(Cyan.copy(alpha=.1f),CircleShape).border(1.dp,Cyan.copy(alpha=.4f),CircleShape),contentAlignment=Alignment.Center) {
                        if(user==null) Icon(Icons.Default.AccountCircle,null,tint=Cyan,modifier=Modifier.size(46.dp))
                        else Text(user.name.take(1).uppercase(),color=Ice,fontWeight=FontWeight.Black,fontSize=28.sp)
                    }
                    Spacer(Modifier.height(15.dp))
                    Text(user?.name ?: "Sign in to ESN Hub",color=White,fontWeight=FontWeight.Black,fontSize=23.sp)
                    if(user!=null && user.email.isNotBlank()) Text(user.email,color=Muted,fontSize=12.sp)
                    Spacer(Modifier.height(9.dp))
                    Text(if(user==null) "Use Google as your ESN Hub identity for credits, rewards, store history and support." else "Google connected • ESN member profile ready",color=if(user==null) Muted else Green,lineHeight=19.sp)
                    Spacer(Modifier.height(18.dp))
                    if(user==null) Button(onClick=signIn,enabled=!busy,modifier=Modifier.fillMaxWidth(),colors=ButtonDefaults.buttonColors(containerColor=White,contentColor=Color(0xFF202124)),shape=RoundedCornerShape(14.dp)) {
                        Icon(Icons.Default.AccountCircle,null); Spacer(Modifier.width(9.dp)); Text(if(busy) "CONNECTING..." else "CONTINUE WITH GOOGLE",fontWeight=FontWeight.Bold)
                    } else OutlinedButton(onClick=signOut,modifier=Modifier.fillMaxWidth(),colors=ButtonDefaults.outlinedButtonColors(contentColor=Ice)){Text("SIGN OUT")}
                }
            }
        }
        if(!message.isNullOrBlank()) item { StatusCard("ACCOUNT STATUS",message,user!=null) }
        item { FeatureRow(Feature("Privacy-first session","Google ID tokens are not stored in local preferences.",Icons.Default.Lock)) }
        item { WebsiteCard(openWeb) }
        item { Spacer(Modifier.height(22.dp)) }
    }
}

@Composable
private fun FeatureRow(feature: Feature) {
    Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=Panel)) {
        Row(Modifier.padding(18.dp),verticalAlignment=Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).background(Cyan.copy(alpha=.1f),RoundedCornerShape(14.dp)).border(1.dp,Cyan.copy(alpha=.22f),RoundedCornerShape(14.dp)),contentAlignment=Alignment.Center) { Icon(feature.icon,null,tint=Cyan) }
            Spacer(Modifier.width(13.dp))
            Column { Text(feature.title,color=White,fontWeight=FontWeight.Bold,fontSize=16.sp); Text(feature.subtitle,color=Muted,fontSize=12.sp,lineHeight=17.sp) }
        }
    }
}

@Composable
private fun WebsiteCard(openWeb:()->Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick=openWeb),shape=RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=Raised)) {
        Row(Modifier.padding(18.dp),verticalAlignment=Alignment.CenterVertically) {
            Icon(Icons.Default.Language,null,tint=Cyan); Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)){Text("ESN Official",color=White,fontWeight=FontWeight.Bold);Text("esnoffical.com",color=Muted,fontSize=12.sp)}
            Text("OPEN",color=Cyan,fontWeight=FontWeight.Black,fontSize=12.sp)
        }
    }
}

@Composable
private fun NewsCard(tag:String,title:String,body:String) {
    Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=Panel)) {
        Column(Modifier.padding(18.dp)) {
            Text(tag,color=Cyan,fontWeight=FontWeight.Black,fontSize=11.sp,letterSpacing=1.2.sp); Spacer(Modifier.height(6.dp))
            Text(title,color=White,fontWeight=FontWeight.Bold,fontSize=17.sp); Spacer(Modifier.height(5.dp))
            Text(body,color=Muted,fontSize=13.sp,lineHeight=19.sp)
        }
    }
}

@Composable
private fun StatusCard(title:String,body:String,positive:Boolean) {
    Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=if(positive) Color(0xFF092A2A) else Raised)) {
        Column(Modifier.padding(18.dp)){Text(title,color=if(positive) Green else Cyan,fontWeight=FontWeight.Black,fontSize=12.sp);Spacer(Modifier.height(5.dp));Text(body,color=Muted,fontSize=12.sp,lineHeight=18.sp)}
    }
}

@Composable
private fun Section(title:String,subtitle:String) {
    Column { Text(title,color=White,fontWeight=FontWeight.Black,fontSize=18.sp,letterSpacing=.8.sp);Text(subtitle,color=Muted,fontSize=12.sp) }
}
