package com.mono.launcher.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mono.launcher.FeedState
import com.mono.launcher.Feeds
import com.mono.launcher.Headline
import com.mono.launcher.Prefs
import com.mono.launcher.Quote

private fun openUrl(context: Context, url: String) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

private fun quoteUrl(symbol: String) = "https://finance.yahoo.com/quote/" + Uri.encode(symbol)

/** Compact market line for the home screen: "CAC 40 7 412 +0,84 %   S&P 500 …". */
@Composable
fun MarketStrip(prefs: Prefs, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    if (!prefs.showMarkets) return
    val quotes = FeedState.quotes
    if (quotes.isEmpty()) return
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onOpen)
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        quotes.take(3).forEach { quote ->
            Column {
                Text(quote.name, color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(Feeds.formatPrice(quote), color = White, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    Spacer(Modifier.width(4.dp))
                    ChangeText(quote, 11)
                }
            }
        }
    }
}

/** Latest headline on the home screen; tap opens the "Infos" screen. */
@Composable
fun HeadlineLine(prefs: Prefs, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    if (!prefs.showNews) return
    val headline = FeedState.headlines.firstOrNull() ?: return
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onOpen)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(6.dp).background(Accent, CircleShape))
        Spacer(Modifier.width(8.dp))
        Text(
            headline.title,
            color = White,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(8.dp))
        Text(headline.source, color = Muted, fontSize = 11.sp, maxLines = 1)
    }
}

@Composable
private fun ChangeText(quote: Quote, size: Int) {
    Text(
        Feeds.formatChange(quote),
        color = if (quote.changePercent >= 0) Accent else Muted,
        fontSize = size.sp,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
    )
}

/** Full screen: all market quotes, then the latest headlines. */
@Composable
fun BriefingScreen(prefs: Prefs, onClose: () -> Unit) {
    val context = LocalContext.current
    val quotes = FeedState.quotes
    val headlines = FeedState.headlines

    LazyColumn(
        Modifier
            .fillMaxSize()
            .background(Black)
            .swallowTouches()
            .systemBarsPadding(),
        contentPadding = PaddingValues(24.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Infos", color = White, fontSize = 32.sp, fontWeight = FontWeight.Black, letterSpacing = (-1).sp)
                Box(Modifier.padding(start = 6.dp, top = 12.dp).size(9.dp).background(Accent, CircleShape))
                Spacer(Modifier.weight(1f))
                Text(
                    "FERMER",
                    color = Muted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable(onClick = onClose)
                        .padding(12.dp),
                )
            }
        }

        if (prefs.showMarkets) {
            item {
                Spacer(Modifier.height(20.dp))
                SectionLabel("Bourse")
                Spacer(Modifier.height(8.dp))
                if (quotes.isEmpty()) {
                    Text("Chargement des cours…", color = Muted, fontSize = 14.sp)
                }
            }
            items(quotes, key = { "q:" + it.symbol }) { quote ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { openUrl(context, quoteUrl(quote.symbol)) }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(quote.name, color = White, fontSize = 17.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        Text(quote.symbol, color = Muted, fontSize = 12.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            Feeds.formatPrice(quote) + if (quote.currency.isNotBlank()) " ${quote.currency}" else "",
                            color = White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        ChangeText(quote, 13)
                    }
                }
            }
        }

        if (prefs.showNews) {
            item {
                Spacer(Modifier.height(24.dp))
                SectionLabel("Actualités")
                Spacer(Modifier.height(8.dp))
                if (headlines.isEmpty()) {
                    Text("Chargement des actualités…", color = Muted, fontSize = 14.sp)
                }
            }
            items(headlines, key = { "n:" + it.link }) { headline ->
                HeadlineRow(headline) { openUrl(context, headline.link) }
            }
        }

        item {
            Spacer(Modifier.height(24.dp))
            Text(
                "Cours : Yahoo Finance, avec un léger différé. Actualités : flux RSS des journaux choisis dans les réglages.",
                color = Muted,
                fontSize = 11.sp,
                lineHeight = 15.sp,
            )
        }
    }
}

@Composable
private fun HeadlineRow(headline: Headline, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp)
    ) {
        Text(
            headline.title,
            color = White,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 21.sp,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            listOf(headline.source, Feeds.ago(headline.published)).filter { it.isNotBlank() }.joinToString(" · "),
            color = Muted,
            fontSize = 12.sp,
        )
    }
}
