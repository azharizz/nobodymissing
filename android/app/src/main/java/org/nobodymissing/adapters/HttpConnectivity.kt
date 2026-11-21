package org.nobodymissing.adapters

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.nobodymissing.ports.ConnectivityPort
import java.net.HttpURLConnection
import java.net.URL

/** Real internet check with a short timeout; never trusts the OS "connected" flag (6.10). */
class HttpConnectivity(private val timeoutMs: Int = 4000) : ConnectivityPort {
    override suspend fun check(): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            (URL("https://firestore.googleapis.com/").openConnection() as HttpURLConnection).run {
                connectTimeout = timeoutMs; readTimeout = timeoutMs; requestMethod = "HEAD"; responseCode; disconnect(); true
            }
        }.getOrDefault(false)
    }
}
