package com.esn.hub

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Ink = Color(0xFF080B12)
private val Panel = Color(0xFF101621)
private val PanelRaised = Color(0xFF151D2A)
private val ElectricBlue = Color(0xFF4A8DFF)
private val ElectricPurple = Color(0xFF875CFF)
private val Cyan = Color(0xFF55D6FF)
private val Muted = Color(0xFF97A3B6)
private val White = Color(0xFFF7F9FC)

private val EsnColors = darkColorScheme(
    primary = ElectricBlue,
    secondary = ElectricPurple,
    tertiary = Cyan,
    background = Ink,
    surface = Panel,
    onPrimary = White,
    onBackground = White,
    onSurface = White
)

private data class HubAction(
    val title: String,
    val subtitle: String,
    val icon: ImageVector
)

private data class Announcement(
    val title: String,
    val body: String,
    val tag: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = EsnColors) {
                Surface(modifier = Modifier.fillMaxSize(), color = Ink) {
                    EsnHubApp()
                }
            }
        }
    }
}

@Composable
private fun EsnHubApp() {
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        containerColor = Ink,
        bottomBar = {
            NavigationBar(containerColor = Color(0xFF0D121B)) {
                val tabs = listOf(
                    Triple("Home", Icons.Default.Home, 0),
                    Triple("Store", Icons.Default.Storefront, 1),
                    Triple("Community", Icons.Default.Groups, 2),
                    Triple("Profile", Icons.Default.Person, 3)
                )
                tabs.forEach { (label, icon, index) ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = { Icon(icon, contentDescription = label) },
                        label = { Text(label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = White,
                            selectedTextColor = White,
                            indicatorColor = ElectricBlue.copy(alpha = 0.28f),
                            unselectedIconColor = Muted,
                            unselectedTextColor = Muted
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        when (selectedTab) {
            0 -> HomeScreen(Modifier.padding(innerPadding))
            1 -> StoreScreen(Modifier.padding(innerPadding))
            2 -> CommunityScreen(Modifier.padding(innerPadding))
            else -> ProfileScreen(Modifier.padding(innerPadding))
        }
    }
}

@Composable
private fun HomeScreen(modifier: Modifier = Modifier) {
    val actions = listOf(
        HubAction("ESN Store", "Browse drops, perks and rewards", Icons.Default.ShoppingBag),
        HubAction("Invite Rewards", "Earn credits by growing the community", Icons.Default.LocalFireDepartment),
        HubAction("Support", "Open a ticket when you need help", Icons.Default.SupportAgent),
        HubAction("Announcements", "Catch up on the latest ESN news", Icons.Default.Campaign)
    )
    val announcements = listOf(
        Announcement(
            title = "Welcome to ESN Hub",
            body = "The official ESN app is being built from the ground up. More community features are on the way.",
            tag = "APP"
        ),
        Announcement(
            title = "Credits are coming",
            body = "Your ESN credit balance, rewards history and store redemptions will live here.",
            tag = "CREDITS"
        )
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Ink)
            .padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(Modifier.height(16.dp))
            Text(
                text = "ESN HUB",
                color = White,
                fontWeight = FontWeight.Black,
                fontSize = 28.sp,
                letterSpacing = 1.6.sp
            )
            Text(
                text = "Everything ESN. One place.",
                color = Muted,
                fontSize = 14.sp
            )
        }

        item {
            CreditsHero()
        }

        item {
            Text(
                text = "Quick access",
                color = White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        }

        items(actions.chunked(2)) { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowItems.forEach { action ->
                    QuickActionCard(
                        action = action,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (rowItems.size == 1) Spacer(Modifier.weight(1f))
            }
        }

        item {
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Latest",
                color = White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        }

        items(announcements) { announcement ->
            AnnouncementCard(announcement)
        }

        item { Spacer(Modifier.height(18.dp)) }
    }
}

@Composable
private fun CreditsHero() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            ElectricBlue.copy(alpha = 0.95f),
                            ElectricPurple.copy(alpha = 0.95f)
                        )
                    )
                )
                .padding(22.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CreditCard,
                        contentDescription = null,
                        tint = White,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "ESN Credits",
                        color = White.copy(alpha = 0.88f),
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "0",
                    color = White,
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "Connect your ESN account to sync your balance",
                    color = White.copy(alpha = 0.78f),
                    fontSize = 13.sp
                )
                Spacer(Modifier.height(18.dp))
                Button(
                    onClick = { },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = White,
                        contentColor = Ink
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Account connection coming soon", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun QuickActionCard(action: HubAction, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.height(148.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Panel)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(ElectricBlue.copy(alpha = 0.14f), RoundedCornerShape(13.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    action.icon,
                    contentDescription = null,
                    tint = Cyan,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column {
                Text(
                    text = action.title,
                    color = White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Text(
                    text = action.subtitle,
                    color = Muted,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
private fun AnnouncementCard(announcement: Announcement) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Panel)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = announcement.tag,
                color = Cyan,
                fontWeight = FontWeight.Black,
                fontSize = 11.sp,
                letterSpacing = 1.2.sp
            )
            Spacer(Modifier.height(7.dp))
            Text(
                text = announcement.title,
                color = White,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = announcement.body,
                color = Muted,
                fontSize = 13.sp,
                lineHeight = 19.sp
            )
        }
    }
}

@Composable
private fun StoreScreen(modifier: Modifier = Modifier) {
    PlaceholderScreen(
        modifier = modifier,
        icon = Icons.Default.Storefront,
        eyebrow = "ESN STORE",
        title = "The store is getting its own home.",
        description = "Credits, rewards, limited drops and redemptions will appear here when the store backend is connected.",
        buttonText = "Store backend coming soon"
    )
}

@Composable
private fun CommunityScreen(modifier: Modifier = Modifier) {
    PlaceholderScreen(
        modifier = modifier,
        icon = Icons.Default.ConfirmationNumber,
        eyebrow = "COMMUNITY",
        title = "Rewards, tickets and events.",
        description = "This section will connect ESN members with invite rewards, support tickets, giveaways, announcements and community events.",
        buttonText = "Community tools coming soon"
    )
}

@Composable
private fun ProfileScreen(modifier: Modifier = Modifier) {
    PlaceholderScreen(
        modifier = modifier,
        icon = Icons.Default.Person,
        eyebrow = "PROFILE",
        title = "Your ESN identity.",
        description = "Once accounts are connected, this is where your username, credit balance, reward history and membership details will live.",
        buttonText = "Sign-in coming soon"
    )
}

@Composable
private fun PlaceholderScreen(
    modifier: Modifier,
    icon: ImageVector,
    eyebrow: String,
    title: String,
    description: String,
    buttonText: String
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Ink)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .size(62.dp)
                .background(PanelRaised, RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Cyan, modifier = Modifier.size(30.dp))
        }
        Spacer(Modifier.height(24.dp))
        Text(
            text = eyebrow,
            color = Cyan,
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.4.sp
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = title,
            color = White,
            fontSize = 30.sp,
            lineHeight = 34.sp,
            fontWeight = FontWeight.Black
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = description,
            color = Muted,
            fontSize = 15.sp,
            lineHeight = 22.sp
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = { },
            enabled = false,
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(buttonText)
        }
    }
}