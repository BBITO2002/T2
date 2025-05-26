package com.penia.appt2.ui.mascota

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.penia.appt2.data.model.Mascota
import com.penia.appt2.data.repository.AuthRepository
import com.penia.appt2.data.repository.MascotaRepository
import kotlinx.coroutines.launch

class MascotaAddEditViewModel(
    private val petRepository: MascotaRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    // LiveData para observar el resultado de la operación de guardar (añadir o actualizar)
    private val _saveResult = MutableLiveData<Result<Boolean>>()
    val saveResult: LiveData<Result<Boolean>> = _saveResult

    // LiveData para observar el resultado de la operación de eliminar
    private val _deleteResult = MutableLiveData<Result<Boolean>>()
    val deleteResult: LiveData<Result<Boolean>> = _deleteResult

    /**
     * Guarda una mascota. Si la mascota tiene un ID, se actualizará. Si no tiene ID, se añadirá.
     * @param pet La mascota a guardar.
     */
    fun savePet(pet: Mascota) {
        val currentUserId = authRepository.getCurrentUserId()
        if (currentUserId == null) {
            _saveResult.postValue(Result.failure(Exception("User not authenticated. Cannot save pet.")))
            return
        }

        // Asignamos el ID del propietario antes de guardar la mascota
        val petToSave = pet.copy(ownerId = currentUserId)

        viewModelScope.launch {
            val result = if (petToSave.id.isEmpty()) {
                // Si el ID está vacío, es una nueva mascota, la añadimos
                petRepository.addPet(petToSave)
            } else {
                // Si el ID no está vacío, es una mascota existente, la actualizamos
                petRepository.updatePet(petToSave)
            }
            _saveResult.postValue(result) // Publica el resultado en el LiveData
        }
    }

    /**
     * Elimina una mascota de la base de datos.
     * @param petId El ID de la mascota a eliminar.
     */
    fun deletePet(petId: String) {
        viewModelScope.launch {
            val result = petRepository.deletePet(petId)
            _deleteResult.postValue(result) // Publica el resultado de la eliminación
        }
    }
}