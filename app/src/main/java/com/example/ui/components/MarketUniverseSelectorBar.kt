package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MarketAsset
import com.example.data.MarketCategory
import com.example.data.MarketUniverse

@Composable
fun MarketUniverseSelectorBar(
    selectedCategory: MarketCategory,
    selectedAsset: MarketAsset,
    isScanning: Boolean,
    onCategorySelected: (MarketCategory) -> Unit,
    onAssetSelected: (MarketAsset) -> Unit,
    onScanAllMarkets: () -> Unit,
    modifier: Modifier = Modifier
) {
    val greenColor = Color(0xFF00E676)
    val cyanColor = Color(0xFF00E5FF)
    val goldColor = Color(0xFFFFD700)
    val purpleColor = Color(0xFFB388FF)
    val blueColor = Color(0xFF448AFF)

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0D121D)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFF1E293B))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Header: Confirmation badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ShowChart,
                        contentDescription = null,
                        tint = greenColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "CONFIRMED TRADE ASSETS",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = Color.White
                    )
                }

                Surface(
                    color = greenColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, greenColor.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(greenColor)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "4 MARKETS • 8 ASSETS READY",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = greenColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Category Chips Row (5 ALL OF THEM, 1 FOREX, 2 GOLD, 3 CRYPTO, 4 STOCKS)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                MarketCategory.values().forEach { category ->
                    val isSelected = category == selectedCategory
                    val chipColor = when (category) {
                        MarketCategory.ALL_MARKETS -> greenColor
                        MarketCategory.FOREX -> blueColor
                        MarketCategory.GOLD -> goldColor
                        MarketCategory.CRYPTO -> cyanColor
                        MarketCategory.STOCKS -> purpleColor
                    }

                    Surface(
                        color = if (isSelected) chipColor.copy(alpha = 0.22f) else Color(0xFF131822),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) chipColor else Color(0xFF263238)
                        ),
                        modifier = Modifier
                            .clickable { onCategorySelected(category) }
                            .testTag("market_cat_${category.name.lowercase()}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(chipColor)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text(
                                text = category.badgeLabel,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = if (isSelected) Color.White else Color(0xFFB0BEC5)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Instruments Carousel for the active category (or all 8)
            val currentAssets = MarketUniverse.getAssetsForCategory(selectedCategory)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                currentAssets.forEach { asset ->
                    val isAssetSelected = asset.symbol == selectedAsset.symbol
                    val catColor = when (asset.category) {
                        MarketCategory.FOREX -> blueColor
                        MarketCategory.GOLD -> goldColor
                        MarketCategory.CRYPTO -> cyanColor
                        MarketCategory.STOCKS -> purpleColor
                        MarketCategory.ALL_MARKETS -> greenColor
                    }

                    Surface(
                        color = if (isAssetSelected) catColor.copy(alpha = 0.18f) else Color(0xFF131A27),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(
                            width = if (isAssetSelected) 1.5.dp else 1.dp,
                            color = if (isAssetSelected) catColor else Color(0xFF232D3F)
                        ),
                        modifier = Modifier
                            .clickable { onAssetSelected(asset) }
                            .testTag("asset_chip_${asset.symbol.replace("/", "_")}")
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = asset.symbol,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = if (isAssetSelected) catColor else Color.White
                                )
                                Surface(
                                    color = catColor.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = asset.category.shortName,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = catColor,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(3.dp))

                            Text(
                                text = if (asset.decimalDigits == 4) {
                                    String.format(java.util.Locale.US, "%.4f", asset.basePrice)
                                } else {
                                    String.format(java.util.Locale.US, "$%.2f", asset.basePrice)
                                },
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = Color(0xFFE0E0E0)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action: Scan All Markets with AI Button
            Button(
                onClick = onScanAllMarkets,
                enabled = !isScanning,
                colors = ButtonDefaults.buttonColors(
                    containerColor = greenColor,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("btn_scan_all_confirmed_markets")
            ) {
                if (isScanning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color.Black,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "AI SCANNING ${selectedCategory.shortName.uppercase()}...",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (selectedCategory == MarketCategory.ALL_MARKETS) {
                            "AI MULTI-SCAN ALL 8 CONFIRMED MARKETS"
                        } else {
                            "AI SCAN ALL ${selectedCategory.shortName.uppercase()} PAIRS"
                        },
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    )
                }
            }
        }
    }
}
