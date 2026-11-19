package com.example.cyclingcheckin.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.IOException
import java.io.StringReader
import java.net.SocketTimeoutException
import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

/**
 * 最小 WebDAV 客户端：PROPFIND / MKCOL / PUT / GET，仅支持 HTTPS。
 * 不打印任何请求头，避免密码进入日志。
 */
class WebDavClient(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()
) {

    suspend fun testConnection(config: WebDavConfig): Result<Unit> =
        ensureDirectory(config)

    suspend fun ensureDirectory(config: WebDavConfig): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val dirUrl = directoryUrl(config)
            val response = propfind(dirUrl, "0", config)
            response.use {
                when (it.code) {
                    200, 207 -> Unit
                    404 -> createDirectory(dirUrl, config)
                    401 -> throw WebDavException(WebDavError.UNAUTHORIZED, 401)
                    403 -> throw WebDavException(WebDavError.FORBIDDEN, 403)
                    in 500..599 -> throw WebDavException(WebDavError.SERVER, it.code)
                    else -> throw WebDavException(WebDavError.HTTP, it.code)
                }
            }
        }.mapError()
    }

    suspend fun upload(config: WebDavConfig, fileName: String, json: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                ensureDirectory(config).getOrThrow()
                val url = directoryUrl(config) + encodeSegment(fileName)
                val request = authorized(config)
                    .url(url)
                    .put(json.toRequestBody("application/json; charset=utf-8".toMediaType()))
                    .build()
                execute(request).use { response ->
                    when (response.code) {
                        200, 201, 204 -> Unit
                        401 -> throw WebDavException(WebDavError.UNAUTHORIZED, 401)
                        403 -> throw WebDavException(WebDavError.FORBIDDEN, 403)
                        in 500..599 -> throw WebDavException(WebDavError.SERVER, response.code)
                        else -> throw WebDavException(WebDavError.HTTP, response.code)
                    }
                }
            }.mapError()
        }

    suspend fun listBackups(config: WebDavConfig): Result<List<RemoteBackupFile>> =
        withContext(Dispatchers.IO) {
            runCatching {
                ensureDirectory(config).getOrThrow()
                val response = propfind(directoryUrl(config), "1", config)
                response.use {
                    when (it.code) {
                        200, 207 -> parseList(it.body?.string().orEmpty(), config)
                        404 -> throw WebDavException(WebDavError.NOT_FOUND, 404)
                        401 -> throw WebDavException(WebDavError.UNAUTHORIZED, 401)
                        403 -> throw WebDavException(WebDavError.FORBIDDEN, 403)
                        in 500..599 -> throw WebDavException(WebDavError.SERVER, it.code)
                        else -> throw WebDavException(WebDavError.HTTP, it.code)
                    }
                }
            }.mapError()
        }

    suspend fun download(config: WebDavConfig, file: RemoteBackupFile): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val url = resolveHref(directoryUrl(config), file.href)
                val request = authorized(config).url(url).get().build()
                execute(request).use { response ->
                    when (response.code) {
                        200 -> response.body?.string()
                            ?: throw WebDavException(WebDavError.INVALID_RESPONSE)
                        401 -> throw WebDavException(WebDavError.UNAUTHORIZED, 401)
                        403 -> throw WebDavException(WebDavError.FORBIDDEN, 403)
                        404 -> throw WebDavException(WebDavError.NOT_FOUND, 404)
                        in 500..599 -> throw WebDavException(WebDavError.SERVER, response.code)
                        else -> throw WebDavException(WebDavError.HTTP, response.code)
                    }
                }
            }.mapError()
        }

    private fun createDirectory(dirUrl: String, config: WebDavConfig) {
        val request = authorized(config)
            .url(dirUrl)
            .method("MKCOL", ByteArray(0).toRequestBody(null))
            .build()
        execute(request).use { response ->
            when (response.code) {
                200, 201, 405 -> Unit // 405：目录已存在
                401 -> throw WebDavException(WebDavError.UNAUTHORIZED, 401)
                403 -> throw WebDavException(WebDavError.FORBIDDEN, 403)
                in 500..599 -> throw WebDavException(WebDavError.SERVER, response.code)
                else -> throw WebDavException(WebDavError.HTTP, response.code)
            }
        }
    }

    private fun propfind(url: String, depth: String, config: WebDavConfig) =
        execute(
            authorized(config)
                .url(url)
                .method(
                    "PROPFIND",
                    PROPFIND_BODY.toRequestBody("application/xml; charset=utf-8".toMediaType())
                )
                .header("Depth", depth)
                .build()
        )

    private fun execute(request: Request) = try {
        client.newCall(request).execute()
    } catch (e: SocketTimeoutException) {
        throw WebDavException(WebDavError.TIMEOUT, cause = e)
    } catch (e: IOException) {
        throw WebDavException(WebDavError.NETWORK, cause = e)
    }

    private fun authorized(config: WebDavConfig): Request.Builder = Request.Builder()
        .header(
            "Authorization",
            Credentials.basic(config.username, config.password, Charsets.UTF_8)
        )

    private fun directoryUrl(config: WebDavConfig): String {
        val server = config.serverUrl.trim().trimEnd('/')
        if (!server.startsWith("https://", ignoreCase = true)) {
            throw WebDavException(WebDavError.INSECURE)
        }
        if (config.username.isBlank() || config.password.isBlank()) {
            throw WebDavException(WebDavError.NOT_CONFIGURED)
        }
        val segments = config.remoteDir.trim().trim('/')
            .split('/')
            .filter { it.isNotBlank() }
            .joinToString("/") { encodeSegment(it) }
        return if (segments.isEmpty()) "$server/" else "$server/$segments/"
    }

    private fun encodeSegment(segment: String): String =
        URLEncoder.encode(segment, "UTF-8").replace("+", "%20")

    private fun resolveHref(baseUrl: String, href: String): String {
        if (href.startsWith("http://") || href.startsWith("https://")) return href
        return runCatching { URI(baseUrl).resolve(href).toString() }.getOrDefault(baseUrl + href)
    }

    private fun parseList(xml: String, config: WebDavConfig): List<RemoteBackupFile> {
        if (xml.isBlank()) return emptyList()
        val parser = XmlPullParserFactory.newInstance().newPullParser()
        parser.setInput(StringReader(xml))
        val result = mutableListOf<RemoteBackupFile>()
        var event = parser.eventType
        var href: String? = null
        var size = 0L
        var modified = 0L
        var collection = false
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> when (parser.name.lowercase()) {
                    "response" -> {
                        href = null
                        size = 0L
                        modified = 0L
                        collection = false
                    }
                    "href" -> href = parser.nextText().trim()
                    "getcontentlength" -> size = parser.nextText().trim().toLongOrNull() ?: 0L
                    "getlastmodified" -> modified = parseHttpDate(parser.nextText().trim())
                    "collection" -> collection = true
                }
                XmlPullParser.END_TAG -> if (parser.name.equals("response", ignoreCase = true)) {
                    val currentHref = href
                    if (!collection && currentHref != null) {
                        val rawName = currentHref.substringBefore('?')
                            .trimEnd('/')
                            .substringAfterLast('/')
                        val name = runCatching {
                            URLDecoder.decode(rawName, "UTF-8")
                        }.getOrDefault(rawName)
                        if (name.startsWith(BACKUP_PREFIX) && name.endsWith(".json")) {
                            result.add(
                                RemoteBackupFile(
                                    name = name,
                                    href = currentHref,
                                    sizeBytes = size,
                                    lastModified = modified
                                )
                            )
                        }
                    }
                    href = null
                }
            }
            event = parser.next()
        }
        return result.sortedByDescending { it.lastModified }
    }

    private fun parseHttpDate(value: String): Long = runCatching {
        ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toEpochMilli()
    }.getOrDefault(0L)

    private fun <T> Result<T>.mapError(): Result<T> = fold(
        onSuccess = { Result.success(it) },
        onFailure = { e ->
            when (e) {
                is WebDavException -> Result.failure(e)
                is SocketTimeoutException -> Result.failure(WebDavException(WebDavError.TIMEOUT, cause = e))
                is IOException -> Result.failure(WebDavException(WebDavError.NETWORK, cause = e))
                else -> Result.failure(WebDavException(WebDavError.INVALID_RESPONSE, cause = e))
            }
        }
    )

    private companion object {
        const val BACKUP_PREFIX = "cycling-backup-"

        val PROPFIND_BODY = """
            <?xml version="1.0" encoding="utf-8" ?>
            <d:propfind xmlns:d="DAV:">
              <d:prop>
                <d:getcontentlength/>
                <d:getlastmodified/>
                <d:resourcetype/>
              </d:prop>
            </d:propfind>
        """.trimIndent()
    }
}
