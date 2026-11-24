package com.mono.launcher

import android.util.Xml
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.NumberFormat
import java.time.Duration
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

data class Quote(
    val symbol: String,
    val name: String,
    val price: Double,
    val changePercent: Double,
    val currency: String,
)

data class Headline(
    val title: String,
    val source: String,
    val link: String,
    val published: ZonedDateTime?,
)

/** A news source offered in the settings (RSS feed of the front page). */
data class NewsSource(val id: String, val name: String, val url: String)

/** Stock quotes and news headlines, refreshed by MainActivity, read by the UI. */
object FeedState {
    var quotes by mutableStateOf<List<Quote>>(emptyList())
    var headlines by mutableStateOf<List<Headline>>(emptyList())
}

object Feeds {
    val NEWS_SOURCES = listOf(
        NewsSource("lemonde", "Le Monde", "https://www.lemonde.fr/rss/une.xml"),
        NewsSource("franceinfo", "franceinfo", "https://www.francetvinfo.fr/titres.rss"),
        NewsSource("figaro", "Le Figaro", "https://www.lefigaro.fr/rss/figaro_actualites.xml"),
        NewsSource("20minutes", "20 Minutes", "https://www.20minutes.fr/feeds/rss-une.xml"),
        NewsSource("lemonde_eco", "Le Monde Économie", "https://www.lemonde.fr/economie/rss_full.xml"),
    )

    /** Quick picks for the watchlist: Yahoo Finance symbol -> short name. */
    val MARKET_PRESETS = linkedMapOf(
        "^FCHI" to "CAC 40",
        "^GSPC" to "S&P 500",
        "^IXIC" to "Nasdaq",
        "^STOXX50E" to "Euro Stoxx 50",
        "BTC-EUR" to "Bitcoin",
        "EURUSD=X" to "EUR/USD",
    )

    fun displayName(symbol: String): String = MARKET_PRESETS[symbol] ?: symbol

    suspend fun refresh(prefs: Prefs) = coroutineScope {
        val quotesJob = async(Dispatchers.IO) { if (prefs.showMarkets) loadQuotes(prefs.tickers) else emptyList() }
        val newsJob = async(Dispatchers.IO) { if (prefs.showNews) loadNews(prefs.newsSources) else emptyList() }
        val quotes = quotesJob.await()
        val news = newsJob.await()
        withContext(Dispatchers.Main) {
            // Keep the previous values if the network failed this time.
            if (quotes.isNotEmpty() || !prefs.showMarkets) FeedState.quotes = quotes
            if (news.isNotEmpty() || !prefs.showNews) FeedState.headlines = news
        }
    }

    // ---- Markets (Yahoo Finance public chart endpoint, no account needed) ----

    private suspend fun loadQuotes(symbols: List<String>): List<Quote> = coroutineScope {
        symbols.map { symbol -> async { runCatching { loadQuote(symbol) }.getOrNull() } }
            .awaitAll()
            .filterNotNull()
    }

    private fun loadQuote(symbol: String): Quote? {
        val encoded = URLEncoder.encode(symbol, "UTF-8")
        val json = get("https://query1.finance.yahoo.com/v8/finance/chart/$encoded?range=1d&interval=1d") ?: return null
        val meta = JSONObject(json).getJSONObject("chart").getJSONArray("result").getJSONObject(0).getJSONObject("meta")
        val price = meta.getDouble("regularMarketPrice")
        val previous = meta.optDouble("chartPreviousClose", meta.optDouble("previousClose", price))
        val change = if (previous != 0.0) (price - previous) / previous * 100 else 0.0
        val name = MARKET_PRESETS[symbol] ?: meta.optString("shortName").ifBlank { symbol }
        return Quote(symbol, name, price, change, meta.optString("currency"))
    }

    // ---- News (RSS) ----

    private suspend fun loadNews(sourceIds: Set<String>): List<Headline> = coroutineScope {
        NEWS_SOURCES.filter { it.id in sourceIds }
            .map { source -> async { runCatching { loadFeed(source) }.getOrDefault(emptyList()) } }
            .awaitAll()
            .flatten()
            .sortedByDescending { it.published }
            .distinctBy { it.title }
            .take(30)
    }

    private fun loadFeed(source: NewsSource): List<Headline> {
        val xml = get(source.url) ?: return emptyList()
        val parser = Xml.newPullParser().apply { setInput(xml.reader()) }
        val items = mutableListOf<Headline>()
        var inItem = false
        var title = ""
        var link = ""
        var date: ZonedDateTime? = null
        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            when (parser.eventType) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "item", "entry" -> {
                        inItem = true; title = ""; link = ""; date = null
                    }
                    "title" -> if (inItem) title = parser.nextText().trim()
                    "link" -> if (inItem) {
                        val href = parser.getAttributeValue(null, "href")
                        link = href ?: parser.nextText().trim()
                    }
                    "pubDate", "published", "updated" -> if (inItem && date == null) date = parseDate(parser.nextText())
                }
                XmlPullParser.END_TAG -> if (parser.name == "item" || parser.name == "entry") {
                    if (title.isNotBlank() && link.isNotBlank()) items += Headline(title, source.name, link, date)
                    inItem = false
                }
            }
        }
        return items.take(15)
    }

    private fun parseDate(raw: String): ZonedDateTime? {
        val text = raw.trim()
        return runCatching { ZonedDateTime.parse(text, DateTimeFormatter.RFC_1123_DATE_TIME) }.getOrNull()
            ?: runCatching { ZonedDateTime.parse(text) }.getOrNull()
    }

    private fun get(url: String): String? {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 8_000
            readTimeout = 8_000
            setRequestProperty("User-Agent", "Mozilla/5.0 (Android) Lumen")
            instanceFollowRedirects = true
        }
        return try {
            if (connection.responseCode in 200..299) connection.inputStream.bufferedReader().use { it.readText() } else null
        } finally {
            connection.disconnect()
        }
    }

    // ---- Formatting ----

    fun formatPrice(quote: Quote): String {
        val format = NumberFormat.getNumberInstance(Locale.FRANCE).apply {
            maximumFractionDigits = if (quote.price >= 1_000) 0 else if (quote.price >= 10) 2 else 4
            minimumFractionDigits = if (quote.price >= 1_000) 0 else 2
        }
        return format.format(quote.price)
    }

    fun formatChange(quote: Quote): String {
        val sign = if (quote.changePercent >= 0) "+" else "−"
        return sign + "%.2f".format(Locale.FRANCE, kotlin.math.abs(quote.changePercent)) + " %"
    }

    /** "il y a 25 min", "il y a 3 h". */
    fun ago(time: ZonedDateTime?): String {
        if (time == null) return ""
        val minutes = Duration.between(time, ZonedDateTime.now()).toMinutes().coerceAtLeast(0)
        return when {
            minutes < 1 -> "à l'instant"
            minutes < 60 -> "il y a $minutes min"
            minutes < 24 * 60 -> "il y a ${minutes / 60} h"
            else -> "il y a ${minutes / (24 * 60)} j"
        }
    }
}
