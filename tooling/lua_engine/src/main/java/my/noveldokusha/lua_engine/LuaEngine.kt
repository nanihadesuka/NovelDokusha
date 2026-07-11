package my.noveldokusha.lua_engine

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonParser
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import my.noveldokusha.network.NetworkClient
import my.noveldokusha.network.getRequest
import my.noveldokusha.network.postRequest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.jsoup.Jsoup
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.Varargs
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.TwoArgFunction
import org.luaj.vm2.lib.VarArgFunction
import org.luaj.vm2.lib.jse.JsePlatform
import timber.log.Timber
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Lua scripting engine for external source plugins.
 *
 * Loads .lua plugin files from HnDK0's external-sources GitHub repo and
 * executes them via luaj-jse (a pure-Java Lua 5.1 interpreter). All API
 * functions (http_get, html_select, string_clean, etc.) are implemented
 * as Java functions exposed to the Lua global scope.
 *
 * The engine is thread-safe — each [loadPlugin] call creates an isolated
 * Lua globals environment, so multiple plugins can run concurrently
 * without interfering with each other.
 */
@Singleton
class LuaEngine @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val networkClient: NetworkClient,
) {
    private val gson = Gson()

    /**
     * Load and execute a Lua plugin script, returning the globals
     * environment containing all the plugin's functions and metadata.
     *
     * @param scriptText the full .lua file content
     * @param pluginId   the plugin's unique ID (for logging)
     * @return the Lua globals, or null if the script failed to load
     */
    fun loadPlugin(scriptText: String, pluginId: String): LuaValue? {
        return try {
            val globals = JsePlatform.standardGlobals()
            registerApiFunctions(globals)
            globals.load(scriptText, pluginId).call()
            globals
        } catch (e: Exception) {
            Timber.e(e, "LuaEngine: failed to load plugin '$pluginId': ${e.message}")
            null
        }
    }

    // ─── API function registration ──────────────────────────────────

    private fun registerApiFunctions(globals: LuaValue) {
        // HTTP
        globals.set("http_get", object : VarArgFunction() {
            override fun invoke(args: Varargs): LuaValue = luaHttpGet(args)
        })
        globals.set("http_post", object : VarArgFunction() {
            override fun invoke(args: Varargs): LuaValue = luaHttpPost(args)
        })
        globals.set("get_cookies", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue = luaGetCookies(arg)
        })
        globals.set("set_cookies", object : TwoArgFunction() {
            override fun call(arg1: LuaValue, arg2: LuaValue): LuaValue = luaSetCookies(arg1, arg2)
        })

        // HTML / DOM
        globals.set("html_select", object : TwoArgFunction() {
            override fun call(html: LuaValue, selector: LuaValue): LuaValue = luaHtmlSelect(html, selector)
        })
        globals.set("html_select_first", object : TwoArgFunction() {
            override fun call(html: LuaValue, selector: LuaValue): LuaValue = luaHtmlSelectFirst(html, selector)
        })
        globals.set("html_text", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue = luaHtmlText(arg)
        })
        globals.set("html_remove", object : VarArgFunction() {
            override fun invoke(args: Varargs): LuaValue = luaHtmlRemove(args)
        })

        // Strings
        globals.set("string_clean", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue = LuaValue.valueOf(cleanString(arg.optjstring("")))
        })
        globals.set("string_trim", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue = LuaValue.valueOf(arg.optjstring("").trim())
        })
        globals.set("string_split", object : TwoArgFunction() {
            override fun call(str: LuaValue, sep: LuaValue): LuaValue = luaStringSplit(str, sep)
        })
        globals.set("string_starts_with", object : TwoArgFunction() {
            override fun call(str: LuaValue, prefix: LuaValue): LuaValue = LuaValue.valueOf(str.optjstring("").startsWith(prefix.optjstring("")))
        })
        globals.set("string_ends_with", object : TwoArgFunction() {
            override fun call(str: LuaValue, suffix: LuaValue): LuaValue = LuaValue.valueOf(str.optjstring("").endsWith(suffix.optjstring("")))
        })

        // Regex
        globals.set("regex_replace", object : ThreeArgFunction() {
            override fun call(input: LuaValue, pattern: LuaValue, replacement: LuaValue): LuaValue =
                LuaValue.valueOf(Regex(pattern.optjstring(""), RegexOption.DOT_MATCHES_ALL).replace(input.optjstring(""), replacement.optjstring("")))
        })
        globals.set("regex_match", object : TwoArgFunction() {
            override fun call(input: LuaValue, pattern: LuaValue): LuaValue = luaRegexMatch(input, pattern)
        })

        // URL
        globals.set("url_encode", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue = LuaValue.valueOf(URLEncoder.encode(arg.optjstring(""), "UTF-8"))
        })
        globals.set("url_resolve", object : TwoArgFunction() {
            override fun call(base: LuaValue, href: LuaValue): LuaValue = LuaValue.valueOf(java.net.URI(base.optjstring("")).resolve(href.optjstring("")).toString())
        })

        // JSON
        globals.set("json_parse", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue = luaJsonParse(arg)
        })
        globals.set("json_stringify", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue = LuaValue.valueOf(gson.toJson(luaToJava(arg)))
        })

        // Crypto / Encoding
        globals.set("base64_encode", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue = LuaValue.valueOf(android.util.Base64.encodeToString(arg.optjstring("").toByteArray(), android.util.Base64.NO_WRAP))
        })
        globals.set("base64_decode", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue = LuaValue.valueOf(String(android.util.Base64.decode(arg.optjstring(""), android.util.Base64.DEFAULT)))
        })

        // Utilities
        globals.set("sleep", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue {
                Thread.sleep(arg.optint(0).toLong())
                return LuaValue.NIL
            }
        })
        globals.set("log_info", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue { Timber.i("[Lua] ${arg.optjstring("")}"); return LuaValue.NIL }
        })
        globals.set("log_error", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue { Timber.e("[Lua] ${arg.optjstring("")}"); return LuaValue.NIL }
        })

        // Storage (SharedPreferences "lua_preferences")
        globals.set("get_preference", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue {
                val prefs = appContext.getSharedPreferences("lua_preferences", Context.MODE_PRIVATE)
                return LuaValue.valueOf(prefs.getString(arg.optjstring(""), "") ?: "")
            }
        })
        globals.set("set_preference", object : TwoArgFunction() {
            override fun call(key: LuaValue, value: LuaValue): LuaValue {
                val prefs = appContext.getSharedPreferences("lua_preferences", Context.MODE_PRIVATE)
                prefs.edit().putString(key.optjstring(""), value.optjstring("")).apply()
                return LuaValue.NIL
            }
        })
    }

    // ─── HTTP functions ─────────────────────────────────────────────

    private fun luaHttpGet(args: Varargs): LuaValue {
        val url = args.arg1().optjstring("")
        val config = args.optvalue(2, LuaValue.NIL)

        return try {
            val request = getRequest(url)
            // Apply config headers if provided
            val cfgTable = if (config.isnil()) null else config as? LuaTable
            cfgTable?.get("headers")?.takeIf { it.istable() }?.let { headersT ->
                headersT as LuaTable
                var k: LuaValue = LuaValue.NIL
                while (true) {
                    val n = headersT.next(k)
                    if (n.arg1().isnil()) break
                    request.addHeader(n.arg1().tojstring(), n.arg(2).tojstring())
                    k = n.arg1()
                }
            }
            val response = runBlocking(Dispatchers.IO) { networkClient.call(request) }
            val body = response.body?.string() ?: ""
            response.close()
            val result = LuaTable()
            result.set("success", LuaValue.valueOf(true))
            result.set("body", LuaValue.valueOf(body))
            result.set("code", LuaValue.valueOf(response.code))
            result
        } catch (e: Exception) {
            Timber.e(e, "[Lua] http_get failed: $url")
            val result = LuaTable()
            result.set("success", LuaValue.valueOf(false))
            result.set("body", LuaValue.valueOf(""))
            result.set("code", LuaValue.valueOf(0))
            result
        }
    }

    private fun luaHttpPost(args: Varargs): LuaValue {
        val url = args.arg1().optjstring("")
        val body = args.optjstring(2, "")
        val config = args.optvalue(3, LuaValue.NIL)

        return try {
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = body.toRequestBody(mediaType)
            val request = postRequest(url, body = requestBody)

            // Apply config headers
            val cfgTable = if (config.isnil()) null else config as? LuaTable
            cfgTable?.get("headers")?.takeIf { it.istable() }?.let { headersT ->
                headersT as LuaTable
                var k: LuaValue = LuaValue.NIL
                while (true) {
                    val n = headersT.next(k)
                    if (n.arg1().isnil()) break
                    request.addHeader(n.arg1().tojstring(), n.arg(2).tojstring())
                    k = n.arg1()
                }
            }
            // Override Content-Type and body if specified in config
            cfgTable?.get("headers")?.takeIf { it.istable() }?.let { ht ->
                ht as LuaTable
                val ct = ht.get("Content-Type")
                if (!ct.isnil()) {
                    val ctBody = body.toRequestBody(ct.tojstring().toMediaType())
                    request.post(ctBody)
                }
            }

            val response = runBlocking(Dispatchers.IO) { networkClient.call(request) }
            val respBody = response.body?.string() ?: ""
            response.close()
            val result = LuaTable()
            result.set("success", LuaValue.valueOf(true))
            result.set("body", LuaValue.valueOf(respBody))
            result.set("code", LuaValue.valueOf(response.code))
            result
        } catch (e: Exception) {
            Timber.e(e, "[Lua] http_post failed: $url")
            val result = LuaTable()
            result.set("success", LuaValue.valueOf(false))
            result.set("body", LuaValue.valueOf(""))
            result.set("code", LuaValue.valueOf(0))
            result
        }
    }

    private fun luaGetCookies(urlArg: LuaValue): LuaValue {
        return try {
            val url = urlArg.optjstring("")
            val cookieManager = android.webkit.CookieManager.getInstance()
            val cookies = cookieManager.getCookie(url) ?: ""
            val result = LuaTable()
            for (pair in cookies.split(";")) {
                val parts = pair.trim().split("=", limit = 2)
                if (parts.size == 2) {
                    result.set(parts[0].trim(), LuaValue.valueOf(parts[1].trim()))
                }
            }
            result
        } catch (e: Exception) {
            LuaTable()
        }
    }

    private fun luaSetCookies(urlArg: LuaValue, cookiesArg: LuaValue): LuaValue {
        try {
            val url = urlArg.optjstring("")
            val cookieManager = android.webkit.CookieManager.getInstance()
            if (cookiesArg.istable()) {
                cookiesArg as LuaTable
                var k: LuaValue = LuaValue.NIL
                while (true) {
                    val n = cookiesArg.next(k)
                    if (n.arg1().isnil()) break
                    cookieManager.setCookie(url, "${n.arg1().tojstring()}=${n.arg(2).tojstring()}")
                    k = n.arg1()
                }
            }
        } catch (_: Exception) {}
        return LuaValue.NIL
    }

    // ─── HTML functions ─────────────────────────────────────────────

    private fun luaHtmlSelect(htmlArg: LuaValue, selectorArg: LuaValue): LuaValue {
        val html = htmlArg.optjstring("")
        val selector = selectorArg.optjstring("")
        val doc = Jsoup.parse(html)
        val elements = doc.select(selector)
        val result = LuaTable()
        for ((i, el) in elements.withIndex()) {
            val item = LuaTable()
            item.set("text", LuaValue.valueOf(el.text()))
            item.set("html", LuaValue.valueOf(el.html()))
            item.set("href", LuaValue.valueOf(el.attr("href")))
            item.set("title", LuaValue.valueOf(el.attr("title")))
            result.set(i + 1, item)
        }
        return result
    }

    private fun luaHtmlSelectFirst(htmlArg: LuaValue, selectorArg: LuaValue): LuaValue {
        val html = htmlArg.optjstring("")
        val selector = selectorArg.optjstring("")
        val doc = Jsoup.parse(html)
        val el = doc.selectFirst(selector) ?: return LuaValue.NIL
        val result = LuaTable()
        result.set("text", LuaValue.valueOf(el.text()))
        result.set("html", LuaValue.valueOf(el.html()))
        result.set("href", LuaValue.valueOf(el.attr("href")))
        result.set("title", LuaValue.valueOf(el.attr("title")))
        return result
    }

    private fun luaHtmlText(htmlArg: LuaValue): LuaValue {
        val html = htmlArg.optjstring("")
        val doc = Jsoup.parse(html)
        // Preserve paragraph structure: <p> → double newline
        doc.select("p").append("\n\n")
        doc.select("br").append("\n")
        return LuaValue.valueOf(doc.text().replace(" \n\n".toRegex(), "\n\n").trim())
    }

    private fun luaHtmlRemove(args: Varargs): LuaValue {
        val html = args.arg1().optjstring("")
        val doc = Jsoup.parse(html)
        for (i in 2..args.narg()) {
            val selector = args.optjstring(i, "")
            if (selector.isNotEmpty()) doc.select(selector).remove()
        }
        return LuaValue.valueOf(doc.html())
    }

    // ─── String functions ───────────────────────────────────────────

    private fun cleanString(s: String): String {
        return java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFKC)
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun luaStringSplit(str: LuaValue, sep: LuaValue): LuaValue {
        val s = str.optjstring("")
        val separator = sep.optjstring("\n")
        val parts = s.split(separator)
        val result = LuaTable()
        for ((i, part) in parts.withIndex()) {
            result.set(i + 1, LuaValue.valueOf(part))
        }
        return result
    }

    private fun luaRegexMatch(input: LuaValue, pattern: LuaValue): LuaValue {
        val s = input.optjstring("")
        val p = pattern.optjstring("")
        val regex = Regex(p, RegexOption.DOT_MATCHES_ALL)
        val matches = regex.findAll(s).map { it.value }.toList()
        val result = LuaTable()
        for ((i, m) in matches.withIndex()) {
            result.set(i + 1, LuaValue.valueOf(m))
        }
        return result
    }

    // ─── JSON functions ─────────────────────────────────────────────

    private fun luaJsonParse(arg: LuaValue): LuaValue {
        return try {
            val json = arg.optjstring("")
            val element = JsonParser.parseString(json)
            javaObjectToLua(element)
        } catch (e: Exception) {
            LuaValue.NIL
        }
    }

    private fun javaObjectToLua(obj: Any?): LuaValue {
        return when (obj) {
            null -> LuaValue.NIL
            is Boolean -> LuaValue.valueOf(obj)
            is Number -> LuaValue.valueOf(obj.toDouble())
            is String -> LuaValue.valueOf(obj)
            is com.google.gson.JsonObject -> {
                val table = LuaTable()
                for ((k, v) in obj.entrySet()) {
                    table.set(k, javaObjectToLua(v))
                }
                table
            }
            is com.google.gson.JsonArray -> {
                val table = LuaTable()
                for ((i, v) in obj.withIndex()) {
                    table.set(i + 1, javaObjectToLua(v))
                }
                table
            }
            is com.google.gson.JsonPrimitive -> {
                if (obj.isBoolean) LuaValue.valueOf(obj.asBoolean)
                else if (obj.isNumber) LuaValue.valueOf(obj.asDouble)
                else LuaValue.valueOf(obj.asString)
            }
            else -> LuaValue.valueOf(obj.toString())
        }
    }

    // ─── Three-arg function helper class ────────────────────────────

    private abstract class ThreeArgFunction : VarArgFunction() {
        abstract override fun call(a: LuaValue, b: LuaValue, c: LuaValue): LuaValue
        override fun invoke(args: Varargs): LuaValue {
            return call(args.arg1(), args.arg(2), args.arg(3))
        }
    }

    /**
     * Convert a LuaValue to a Java object for JSON serialization.
     */
    private fun luaToJava(value: LuaValue): Any? {
        return when {
            value.isnil() -> null
            value.isboolean() -> value.toboolean()
            value.isint() -> value.toint()
            value.isnumber() -> value.todouble()
            value.isstring() -> value.tojstring()
            value.istable() -> {
                value as LuaTable
                // Check if it's an array (all keys are integers starting from 1)
                val isArray = !value.get(1).isnil()
                if (isArray) {
                    val list = mutableListOf<Any?>()
                    var i = 1
                    while (!value.get(i).isnil()) {
                        list.add(luaToJava(value.get(i)))
                        i++
                    }
                    list
                } else {
                    val map = mutableMapOf<String, Any?>()
                    var k: LuaValue = LuaValue.NIL
                    while (true) {
                        val n = value.next(k)
                        if (n.arg1().isnil()) break
                        map[n.arg1().tojstring()] = luaToJava(n.arg(2))
                        k = n.arg1()
                    }
                    map
                }
            }
            else -> value.tojstring()
        }
    }
}
