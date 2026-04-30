package com.bugfood.ui

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bugfood.R
import com.bugfood.database.DeliveryEntity
import com.bugfood.databinding.ActivityMainBinding
import com.bugfood.service.BugFoodAccessibilityService
import com.bugfood.service.BugFoodForegroundService
import com.bugfood.utils.NotificationHelper
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: DeliveryViewModel by viewModels()
    private lateinit var adapter: DeliveryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        NotificationHelper.createChannels(this)

        setupRecyclerView()
        setupButtons()
        observeData()
        startForegroundService()
        checkAccessibilityOnResume()
    }

    override fun onResume() {
        super.onResume()
        updateServiceStatus()
    }

    // ── RecyclerView ──────────────────────────────────────────────
    private fun setupRecyclerView() {
        adapter = DeliveryAdapter(
            onDelete = { entity -> confirmDelete(entity) },
            onEditCode = { entity -> showEditCodeDialog(entity) }
        )
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = this@MainActivity.adapter
            setHasFixedSize(false)
        }
    }

    // ── Observers ─────────────────────────────────────────────────
    private fun observeData() {
        viewModel.allDeliveries.observe(this) { list ->
            adapter.submitList(list)
            binding.tvEmpty.isVisible = list.isEmpty()
            binding.recyclerView.isVisible = list.isNotEmpty()
            binding.tvCount.text = "${list.size} cliente(s) armazenado(s)"
        }
    }

    // ── Buttons ───────────────────────────────────────────────────
    private fun setupButtons() {
        binding.fabCheck.setOnClickListener {
            if (!isAccessibilityEnabled()) {
                openAccessibilitySettings()
            } else {
                Toast.makeText(this, "✅ Serviço ativo e monitorando!", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnEnableAccessibility.setOnClickListener {
            openAccessibilitySettings()
        }
    }

    // ── Service Management ────────────────────────────────────────
    private fun startForegroundService() {
        val intent = Intent(this, BugFoodForegroundService::class.java).apply {
            action = BugFoodForegroundService.ACTION_START
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun isAccessibilityEnabled(): Boolean {
        val am = getSystemService(Context.ACCESSIBILITY_SERVICE)
                as android.view.accessibility.AccessibilityManager
        val enabledServices = am.getEnabledAccessibilityServiceList(
            AccessibilityServiceInfo.FEEDBACK_ALL_MASK
        )
        return enabledServices.any {
            it.resolveInfo.serviceInfo.packageName == packageName
        }
    }

    private fun updateServiceStatus() {
        val enabled = isAccessibilityEnabled()
        binding.apply {
            chipStatus.text = if (enabled) "🟢 Ativo" else "🔴 Inativo"
            btnEnableAccessibility.isVisible = !enabled
            bannerAccessibility.isVisible = !enabled
        }
    }

    private fun checkAccessibilityOnResume() {
        updateServiceStatus()
    }

    private fun openAccessibilitySettings() {
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        Toast.makeText(
            this,
            "Ative o BugFood na lista de Acessibilidade",
            Toast.LENGTH_LONG
        ).show()
    }

    // ── Dialogs ───────────────────────────────────────────────────
    private fun confirmDelete(entity: DeliveryEntity) {
        AlertDialog.Builder(this)
            .setTitle("Excluir registro")
            .setMessage("Remover ${entity.customerName}?")
            .setPositiveButton("Excluir") { _, _ -> viewModel.delete(entity) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showEditCodeDialog(entity: DeliveryEntity) {
        val input = EditText(this).apply {
            hint = "Código de entrega"
            setText(entity.deliveryCode)
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            imeOptions = EditorInfo.IME_ACTION_DONE
            setPadding(48, 24, 48, 24)
        }

        AlertDialog.Builder(this)
            .setTitle("Código de entrega\n${entity.customerName}")
            .setView(input)
            .setPositiveButton("Salvar") { _, _ ->
                val code = input.text.toString().trim()
                if (code.isNotBlank()) {
                    viewModel.updateDeliveryCode(entity.id, code)
                    Toast.makeText(this, "Código salvo!", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // ── Menu ──────────────────────────────────────────────────────
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_toggle_theme -> {
                val current = AppCompatDelegate.getDefaultNightMode()
                if (current == AppCompatDelegate.MODE_NIGHT_YES) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                    getSharedPreferences("bugfood_prefs", MODE_PRIVATE)
                        .edit().putInt("theme", AppCompatDelegate.MODE_NIGHT_NO).apply()
                } else {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                    getSharedPreferences("bugfood_prefs", MODE_PRIVATE)
                        .edit().putInt("theme", AppCompatDelegate.MODE_NIGHT_YES).apply()
                }
                true
            }
            R.id.action_clear_all -> {
                AlertDialog.Builder(this)
                    .setTitle("Limpar banco de dados")
                    .setMessage("Isso vai apagar TODOS os ${adapter.itemCount} registros. Confirmar?")
                    .setPositiveButton("Limpar tudo") { _, _ ->
                        viewModel.deleteAll()
                        Toast.makeText(this, "Banco limpo!", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
