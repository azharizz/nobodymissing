package org.nobodymissing.adapters

import org.nobodymissing.ports.*

class FakeInference(var status: String = "available", val reply: suspend (InferenceRequest) -> InferenceResult = { InferenceResult("{}", if (it.mode.endsWith("cloud")) "cloud" else "device") }) : InferencePort {
    val calls = mutableListOf<InferenceRequest>()
    override suspend fun onDeviceStatus() = status
    override suspend fun generate(r: InferenceRequest): InferenceResult { calls += r; return reply(r) }
}
class MemorySync : SyncPort {
    val docs = linkedMapOf<String, Map<String, Any?>>()
    override suspend fun put(path: String, data: Map<String, Any?>) { docs[path] = data }
}
class FakeConnectivity(var online: Boolean = true) : ConnectivityPort { override suspend fun check() = online }
