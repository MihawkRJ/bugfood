package com.bugfood.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.bugfood.database.AppDatabase
import com.bugfood.database.DeliveryEntity
import kotlinx.coroutines.launch

class DeliveryViewModel(app: Application) : AndroidViewModel(app) {

    private val dao = AppDatabase.getInstance(app).deliveryDao()

    val allDeliveries: LiveData<List<DeliveryEntity>> = dao.getAllLive()

    fun delete(entity: DeliveryEntity) {
        viewModelScope.launch { dao.delete(entity) }
    }

    fun deleteAll() {
        viewModelScope.launch { dao.deleteAll() }
    }

    fun updateDeliveryCode(id: Long, code: String) {
        viewModelScope.launch { dao.updateDeliveryCode(id, code) }
    }
}
