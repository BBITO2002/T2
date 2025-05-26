package com.penia.appt2.ui.mascota

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.penia.appt2.data.model.Mascota
import com.penia.appt2.data.repository.AuthRepository
import com.penia.appt2.data.repository.MascotaRepository
import com.penia.appt2.ui.auth.LoginActivity
import com.penia.appt2.databinding.ActivityMascotaListBinding

class MascotaListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMascotaListBinding
    private lateinit var petListViewModel: MascotaListViewModel
    private lateinit var petAdapter: MascotaAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMascotaListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupFirebaseAndRepositories()
        setupRecyclerView()
        setupObservers()
        setupClickListeners()
    }

    private fun setupFirebaseAndRepositories() {
        // Inicializa Firebase Auth y Firestore
        val firebaseAuth = FirebaseAuth.getInstance()
        val firestore = FirebaseFirestore.getInstance()

        // Inicializa los repositorios
        val authRepository = AuthRepository(firebaseAuth, firestore)
        val petRepository = MascotaRepository(firestore)

        // Inicializa el ViewModel con una Factory
        petListViewModel = ViewModelProvider(this, PetListViewModelFactory(petRepository, authRepository))
            .get(MascotaListViewModel::class.java)
    }

    private fun setupRecyclerView() {
        // Configura el adaptador del RecyclerView
        petAdapter = MascotaAdapter(
            onItemClick = { mascota ->
                // Cuando se hace clic en una mascota, navega a la pantalla de edición
                navigateToEditPet(mascota)
            },
            onEditClick = { mascota ->
                // Navega directamente a editar
                navigateToEditPet(mascota)
            },
            onDeleteClick = { petId ->
                // Muestra un diálogo de confirmación antes de eliminar
                showDeleteConfirmationDialog(petId)
            }
        )

        // Configura el RecyclerView
        binding.recyclerViewPets.apply {
            layoutManager = LinearLayoutManager(this@MascotaListActivity)
            adapter = petAdapter
        }
    }

    private fun setupObservers() {
        // Observa la lista de mascotas
        petListViewModel.pets.observe(this) { result ->
            result.onSuccess { pets ->
                petAdapter.submitList(pets)
                // Muestra/oculta el mensaje de lista vacía
                if (pets.isEmpty()) {
                    binding.textViewEmptyList.visibility = android.view.View.VISIBLE
                    binding.recyclerViewPets.visibility = android.view.View.GONE
                } else {
                    binding.textViewEmptyList.visibility = android.view.View.GONE
                    binding.recyclerViewPets.visibility = android.view.View.VISIBLE
                }
            }.onFailure { exception ->
                Toast.makeText(this, "Error al cargar mascotas: ${exception.message}", Toast.LENGTH_LONG).show()
            }
        }

        // Observa el resultado de eliminar una mascota
        petListViewModel.deleteResult.observe(this) { result ->
            result.onSuccess {
                Toast.makeText(this, "Mascota eliminada exitosamente", Toast.LENGTH_SHORT).show()
            }.onFailure { exception ->
                Toast.makeText(this, "Error al eliminar mascota: ${exception.message}", Toast.LENGTH_LONG).show()
            }
        }

        // Observa si el usuario no está autenticado
        petListViewModel.isUserNotAuthenticated.observe(this) { isNotAuthenticated ->
            if (isNotAuthenticated) {
                Toast.makeText(this, "Debes iniciar sesión para ver tus mascotas", Toast.LENGTH_LONG).show()
                navigateToLogin()
            }
        }
    }

    private fun setupClickListeners() {
        // Botón para cerrar sesión
        binding.btnCerrarSesion.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            Toast.makeText(this, "Sesión cerrada", Toast.LENGTH_SHORT).show()
            navigateToLogin()
        }

        // Botón flotante para agregar nueva mascota
        binding.fabAddPet.setOnClickListener {
            navigateToAddPet()
        }
    }

    private fun navigateToAddPet() {
        val intent = Intent(this, MascotaAddEditActivity::class.java)
        startActivity(intent)
    }

    private fun navigateToEditPet(mascota: Mascota) {
        val intent = Intent(this, MascotaAddEditActivity::class.java).apply {
            putExtra("PET_ID", mascota.id)
            putExtra("PET_NAME", mascota.name)
            putExtra("PET_TYPE", mascota.type)
            putExtra("PET_AGE", mascota.age)
            putExtra("PET_IMAGE", mascota.imageBase64) // Pasar la imagen en base64
        }
        startActivity(intent)
    }

    private fun showDeleteConfirmationDialog(petId: String) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar Mascota")
            .setMessage("¿Estás seguro de que deseas eliminar esta mascota? Esta acción no se puede deshacer.")
            .setPositiveButton("Eliminar") { _, _ ->
                petListViewModel.deletePet(petId)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun onResume() {
        super.onResume()
        // Recarga las mascotas cuando la actividad vuelve al primer plano
        petListViewModel.loadPets()
    }
}

/**
 * Factory para crear instancias de MascotaListViewModel.
 */
class PetListViewModelFactory(
    private val petRepository: MascotaRepository,
    private val authRepository: AuthRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MascotaListViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MascotaListViewModel(petRepository, authRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}