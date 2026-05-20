package com.moneymanager.data.http

import android.util.Log
import com.moneymanager.data.entity.TransactionEntity
import com.moneymanager.domain.repository.AccountRepository
import com.moneymanager.domain.repository.CategoryRepository
import com.moneymanager.domain.repository.PeerContactRepository
import com.moneymanager.domain.repository.TransactionRepository
import fi.iki.elonen.NanoHTTPD
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "TransactionApiServer"
private const val PORT = 18889

@Serializable
data class CreateTransactionRequest(
    val amount: Double,
    val type: String,
    val description: String = "",
    val date: Long? = null,
    val accountName: String? = null,
    val categoryName: String? = null,
    val subCategoryName: String? = null,
    val note: String = "",
    val peerName: String? = null,
)

@Serializable
data class CreateTransactionResponse(val status: String, val id: Long = 0, val error: String = "")

@Singleton
class TransactionApiServer @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val peerContactRepository: PeerContactRepository,
) : NanoHTTPD(PORT) {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    fun startServer() {
        try {
            start(NanoHTTPD.SOCKET_READ_TIMEOUT, false)
            Log.i(TAG, "Transaction API server started on port $PORT")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start server", e)
        }
    }

    fun stopServer() {
        stop()
        Log.i(TAG, "Transaction API server stopped")
    }

    override fun serve(session: IHTTPSession): Response {
        return when {
            session.method == Method.POST && session.uri == "/api/transactions" -> {
                handleCreateTransaction(session)
            }
            session.method == Method.GET && session.uri == "/api/health" -> {
                newFixedLengthResponse(Response.Status.OK, "application/json", """{"status":"ok"}""")
            }
            session.method == Method.OPTIONS -> {
                corsResponse(newFixedLengthResponse(Response.Status.OK, "application/json", ""))
            }
            else -> {
                corsResponse(newFixedLengthResponse(
                    Response.Status.NOT_FOUND, "application/json",
                    """{"error":"Not found"}"""
                ))
            }
        }
    }

    private fun handleCreateTransaction(session: IHTTPSession): Response {
        return try {
            val body = parseBody(session)
            val req = json.decodeFromString<CreateTransactionRequest>(body)

            if (req.amount <= 0) {
                return corsResponse(newFixedLengthResponse(
                    Response.Status.BAD_REQUEST, "application/json",
                    """{"status":"error","error":"Amount must be positive"}"""
                ))
            }
            if (req.type !in TransactionEntity.VALID_TYPES) {
                return corsResponse(newFixedLengthResponse(
                    Response.Status.BAD_REQUEST, "application/json",
                    """{"status":"error","error":"Invalid type '${req.type}'. Valid: ${TransactionEntity.VALID_TYPES}"}"""
                ))
            }

            val id = runBlocking(Dispatchers.IO) {
                val accounts = accountRepository.getAllAccounts().first()
                val account = if (req.accountName != null) {
                    accounts.firstOrNull { it.name.equals(req.accountName, ignoreCase = true) }
                        ?: return@runBlocking -1L
                } else {
                    accounts.firstOrNull { it.type != "peer" } ?: accounts.firstOrNull()
                }

                if (account == null) return@runBlocking -2L

                val category = if (req.categoryName != null) {
                    val cats = categoryRepository.getAllCategories().first()
                    cats.firstOrNull { it.name.equals(req.categoryName, ignoreCase = true) }
                } else null

                val subCategory = if (req.subCategoryName != null && category != null) {
                    val cats = categoryRepository.getAllCategories().first()
                    cats.firstOrNull {
                        it.name.equals(req.subCategoryName, ignoreCase = true) &&
                            it.parentId == category.id
                    }
                } else null

                val peerId = if (req.peerName != null) {
                    val peers = peerContactRepository.getAllPeers().first()
                    peers.firstOrNull { it.displayName.equals(req.peerName, ignoreCase = true) }?.id
                } else null

                val transaction = TransactionEntity(
                    accountId = account.id,
                    type = req.type,
                    amount = req.amount,
                    categoryId = category?.id,
                    subCategoryId = subCategory?.id,
                    peerContactId = peerId,
                    date = req.date ?: System.currentTimeMillis(),
                    description = req.description,
                    note = req.note,
                )

                val txId = transactionRepository.insertTransaction(transaction)

                val sign = when (req.type) {
                    "income", "borrow" -> 1.0
                    "expense", "savings", "lend" -> -1.0
                    else -> 0.0
                }
                if (sign != 0.0) {
                    accountRepository.updateAccountBalance(account.id, sign * req.amount)
                }

                if (peerId != null && req.type in listOf("lend", "borrow")) {
                    val peer = peerContactRepository.getPeerByIdSync(peerId)
                    if (peer != null) {
                        peerContactRepository.updatePeer(
                            when (req.type) {
                                "lend" -> peer.copy(totalGiven = peer.totalGiven + req.amount, updatedAt = System.currentTimeMillis())
                                "borrow" -> peer.copy(totalReceived = peer.totalReceived + req.amount, updatedAt = System.currentTimeMillis())
                                else -> peer
                            }
                        )
                    }
                }

                txId
            }

            when {
                id == -1L -> corsResponse(newFixedLengthResponse(
                    Response.Status.BAD_REQUEST, "application/json",
                    """{"status":"error","error":"Account '${req.accountName}' not found"}"""
                ))
                id == -2L -> corsResponse(newFixedLengthResponse(
                    Response.Status.BAD_REQUEST, "application/json",
                    """{"status":"error","error":"No accounts configured in the app"}"""
                ))
                else -> corsResponse(newFixedLengthResponse(
                    Response.Status.OK, "application/json",
                    """{"status":"created","id":$id}"""
                ))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating transaction", e)
            corsResponse(newFixedLengthResponse(
                Response.Status.INTERNAL_ERROR, "application/json",
                """{"status":"error","error":"${e.message?.replace("\"", "\\\"") ?: "Unknown error"} }"""
            ))
        }
    }

    private fun parseBody(session: IHTTPSession): String {
        return try {
            session.inputStream?.bufferedReader()?.readText() ?: ""
        } catch (e: Exception) {
            val files = HashMap<String, String>()
            try { session.parseBody(files) } catch (_: Exception) {}
            files["postData"] ?: ""
        }
    }

    private fun corsResponse(resp: Response): Response {
        resp.addHeader("Access-Control-Allow-Origin", "*")
        resp.addHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
        resp.addHeader("Access-Control-Allow-Headers", "Content-Type")
        return resp
    }
}
