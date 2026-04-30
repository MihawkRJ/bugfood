package com.bugfood.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.bugfood.database.AppDatabase
import com.bugfood.database.DeliveryEntity
import com.bugfood.utils.NotificationHelper
import kotlinx.coroutines.*

class BugFoodAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "BugFoodA11y"
        const val IFOOD_PACKAGE = "br.com.iFood.deliveryApp"

        // Palavras-chave para identificar campos relevantes no iFood Entregadores
        private val PICKUP_CODE_KEYWORDS = listOf(
            "código de coleta", "código coleta", "coleta:", "pickup code",
            "código:", "retirada:", "retire com o código", "mostrar código"
        )
        private val CUSTOMER_NAME_KEYWORDS = listOf(
            "pedido de", "cliente:", "para:", "nome:"
        )
        private val DELIVERY_CODE_KEYWORDS = listOf(
            "código de entrega", "código entrega", "delivery code",
            "confirmar entrega", "inserir código"
        )

        var instance: BugFoodAccessibilityService? = null
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var db: AppDatabase
    private var lastProcessedEvent = 0L
    private var lastDetectedCustomer: String? = null
    private var lastDetectedPickupCode: String? = null
    private var isAutoFillPending = false

    override fun onCreate() {
        super.onCreate()
        instance = this
        db = AppDatabase.getInstance(this)
        NotificationHelper.createChannels(this)
        Log.d(TAG, "BugFood Accessibility Service iniciado")
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                    AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED or
                    AccessibilityEvent.TYPE_VIEW_FOCUSED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                    AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
            packageNames = arrayOf(IFOOD_PACKAGE)
            notificationTimeout = 100
        }
        serviceInfo = info
        Log.d(TAG, "Service conectado ao iFood Entregadores")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return

        // Throttle: processa no máximo 1 vez por 500ms
        val now = System.currentTimeMillis()
        if (now - lastProcessedEvent < 500) return
        lastProcessedEvent = now

        val pkg = event.packageName?.toString() ?: return
        if (pkg != IFOOD_PACKAGE) return

        val root = rootInActiveWindow ?: return

        serviceScope.launch {
            try {
                processScreen(root)
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao processar tela: ${e.message}")
            }
        }
    }

    private suspend fun processScreen(root: AccessibilityNodeInfo) {
        val allTexts = mutableListOf<String>()
        extractAllText(root, allTexts)
        val fullText = allTexts.joinToString("\n").lowercase()

        Log.d(TAG, "Textos capturados: ${allTexts.take(10)}")

        // ── TELA DE COLETA: detecta nome + código de coleta ──
        val isPickupScreen = PICKUP_CODE_KEYWORDS.any { fullText.contains(it) }
        if (isPickupScreen) {
            val customerName = extractCustomerName(allTexts)
            val pickupCode = extractPickupCode(allTexts)

            if (!customerName.isNullOrBlank() && !pickupCode.isNullOrBlank()) {
                Log.d(TAG, "Coleta detectada: $customerName / $pickupCode")
                saveOrUpdateDelivery(customerName, pickupCode)
                lastDetectedCustomer = customerName
                lastDetectedPickupCode = pickupCode
            }
        }

        // ── TELA DE ENTREGA: auto-preenche código de entrega ──
        val isDeliveryScreen = DELIVERY_CODE_KEYWORDS.any { fullText.contains(it) }
        if (isDeliveryScreen && !isAutoFillPending) {
            val customerName = lastDetectedCustomer ?: extractCustomerName(allTexts)
            if (!customerName.isNullOrBlank()) {
                val existing = db.deliveryDao().findByName(customerName)
                if (existing != null && existing.deliveryCode.isNotBlank()) {
                    Log.d(TAG, "Auto-preenchendo código para $customerName: ${existing.deliveryCode}")
                    autoFillDeliveryCode(root, existing.deliveryCode, existing.id)
                    isAutoFillPending = true
                    delay(3000)
                    isAutoFillPending = false
                }
            }
        }
    }

    // ── Extrai todos os textos visíveis da hierarquia de views ──
    private fun extractAllText(node: AccessibilityNodeInfo, result: MutableList<String>) {
        val text = node.text?.toString()?.trim()
        if (!text.isNullOrBlank()) result.add(text)

        val desc = node.contentDescription?.toString()?.trim()
        if (!desc.isNullOrBlank() && desc != text) result.add(desc)

        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { extractAllText(it, result) }
        }
    }

    // ── Heurística para extrair nome do cliente ──
    private fun extractCustomerName(texts: List<String>): String? {
        for (i in texts.indices) {
            val lower = texts[i].lowercase()
            CUSTOMER_NAME_KEYWORDS.forEach { keyword ->
                if (lower.contains(keyword)) {
                    // Tenta pegar o texto após o keyword
                    val after = texts[i].substringAfter(keyword, "").trim()
                    if (after.isNotBlank() && after.length in 2..60) return after

                    // Ou o texto na próxima linha
                    if (i + 1 < texts.size && texts[i + 1].length in 2..60) {
                        return texts[i + 1].trim()
                    }
                }
            }
        }
        // Fallback: procura texto que parece nome próprio (capitalizado, 2+ palavras)
        return texts.firstOrNull { text ->
            val words = text.trim().split(" ")
            words.size in 2..4 && words.all { it.isNotBlank() && it[0].isUpperCase() } && text.length < 50
        }
    }

    // ── Heurística para extrair código de coleta (geralmente 4-6 dígitos) ──
    private fun extractPickupCode(texts: List<String>): String? {
        for (i in texts.indices) {
            val lower = texts[i].lowercase()
            if (PICKUP_CODE_KEYWORDS.any { lower.contains(it) }) {
                // Código geralmente está na mesma linha após ':' ou na próxima
                val sameLineCode = texts[i].filter { it.isDigit() }
                if (sameLineCode.length in 4..8) return sameLineCode

                if (i + 1 < texts.size) {
                    val nextCode = texts[i + 1].filter { it.isDigit() }
                    if (nextCode.length in 4..8) return nextCode
                }
            }
        }
        // Fallback: número isolado de 4-6 dígitos
        return texts.firstOrNull { text ->
            text.trim().matches(Regex("\\d{4,8}"))
        }?.trim()
    }

    // ── Salva novo registro ou atualiza existente no banco ──
    private suspend fun saveOrUpdateDelivery(name: String, pickupCode: String) {
        val existing = db.deliveryDao().findByName(name)
        if (existing == null) {
            val entity = DeliveryEntity(
                customerName = name,
                pickupCode = pickupCode
            )
            db.deliveryDao().insert(entity)
            Log.d(TAG, "Novo cliente salvo: $name")
            NotificationHelper.showCaptureNotification(this, name, pickupCode)
        } else {
            // Atualiza código se mudou
            if (existing.pickupCode != pickupCode) {
                db.deliveryDao().update(existing.copy(
                    pickupCode = pickupCode,
                    lastUsedAt = System.currentTimeMillis()
                ))
            }
        }
    }

    // ── Auto-preenche o campo de código de entrega via Accessibility ──
    private suspend fun autoFillDeliveryCode(
        root: AccessibilityNodeInfo,
        code: String,
        entityId: Long
    ) {
        withContext(Dispatchers.Main) {
            // Procura campo de input editável
            val inputNode = findEditableField(root)
            if (inputNode != null) {
                val args = Bundle()
                args.putCharSequence(
                    AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                    code
                )
                inputNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
                Log.d(TAG, "Código auto-preenchido: $code")

                serviceScope.launch {
                    db.deliveryDao().incrementUsage(entityId)
                }
                NotificationHelper.showAutoFillNotification(this@BugFoodAccessibilityService, code)
            } else {
                Log.w(TAG, "Campo de input não encontrado para auto-fill")
            }
        }
    }

    private fun findEditableField(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isEditable && node.isEnabled) return node
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findEditableField(child)
            if (found != null) return found
        }
        return null
    }

    override fun onInterrupt() {
        Log.w(TAG, "Accessibility Service interrompido")
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        serviceScope.cancel()
        Log.d(TAG, "BugFood Accessibility Service destruído")
    }

    // ── Método público para trigger manual via MainActivity ──
    fun triggerManualScan(): Boolean {
        val root = rootInActiveWindow ?: return false
        serviceScope.launch {
            try {
                processScreen(root)
            } catch (e: Exception) {
                Log.e(TAG, "Erro no scan manual: ${e.message}")
            }
        }
        return true
    }
}
