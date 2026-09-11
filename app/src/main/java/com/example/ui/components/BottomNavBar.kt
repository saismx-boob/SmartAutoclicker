package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary

@Composable
fun BottomNavBar(
    currentIndex: Int,
    onSelectTab: (Int) -> Unit
) {
    NavigationBar(
        containerColor = SlateSurface,
        modifier = Modifier
            .height(68.dp)
            .shadow(12.dp)
            .border(1.dp, CyberBorder.copy(alpha = 0.5f))
    ) {
        val items = listOf(
            Triple(0, "Accueil", Icons.Default.Dashboard),
            Triple(1, "Scénarios", Icons.Default.ListAlt),
            Triple(2, "Détection", Icons.Default.Radar),
            Triple(3, "Studio IA", Icons.Default.AutoAwesome),
            Triple(4, "Journal", Icons.Default.Article)
        )

        items.forEach { (index, label, icon) ->
            val isSelected = currentIndex == index
            NavigationBarItem(
                selected = isSelected,
                onClick = { onSelectTab(index) },
                icon = {
                    Icon(
                        icon,
                        contentDescription = label,
                        modifier = Modifier.size(20.dp)
                    )
                },
                label = {
                    Text(
                        text = label,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = CyberCyan,
                    selectedTextColor = CyberCyan,
                    unselectedIconColor = TextMuted,
                    unselectedTextColor = TextMuted,
                    indicatorColor = CyberCyan.copy(alpha = 0.15f)
                ),
                modifier = Modifier.testTag("nav_tab_$index")
            )
        }
    }
}
