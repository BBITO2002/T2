package com.penia.appt2.ui.mascota

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.penia.appt2.data.model.Mascota
import com.penia.appt2.data.repository.AuthRepository
import com.penia.appt2.data.repository.MascotaRepository
import com.penia.appt2.databinding.ActivityMascotaAddEditBinding
import com.penia.appt2.utils.ImageUtils

class MascotaAddEditActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMascotaAddEditBinding
    private lateinit var addEditPetViewModel: MascotaAddEditViewModel
    private var petId: String? = null // Para almacenar el ID de la mascota si estamos editando
    private var isEditMode = false
    private var selectedImageBase64: String = "" // Para almacenar la imagen seleccionada en base64

    // Launcher para seleccionar imagen desde galería
    private val selectImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val imageUri: Uri? = result.data?.data
            imageUri?.let { uri ->
                processSelectedImage(uri)
            }
        }
    }

    // Launcher para tomar foto con cámara
    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val imageBitmap = result.data?.extras?.get("data") as? Bitmap
            imageBitmap?.let { bitmap ->
                selectedImageBase64 = ImageUtils.bitmapToBase64(bitmap)
                displaySelectedImage()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMascotaAddEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupFirebaseAndViewModel()
        setupUI()
        setupObservers()
        setupClickListeners()
    }

    private fun setupFirebaseAndViewModel() {
        // Inicializa Firebase Auth y Firestore
        val firebaseAuth = FirebaseAuth.getInstance()
        val firestore = FirebaseFirestore.getInstance()

        // Inicializa los repositorios
        val authRepository = AuthRepository(firebaseAuth, firestore)
        val petRepository = MascotaRepository(firestore)

        // Inicializa el ViewModel con una Factory
        addEditPetViewModel = ViewModelProvider(this, AddEditPetViewModelFactory(petRepository, authRepository))
            .get(MascotaAddEditViewModel::class.java)
    }

    private fun setupUI() {
        // Comprueba si estamos en modo edición (si se pasa un ID de mascota)
        petId = intent.getStringExtra("PET_ID")
        isEditMode = petId != null

        if (isEditMode) {
            // Si hay un ID, estamos editando una mascota existente
            binding.tvTitle.text = "Editar Mascota"
            binding.btnSavePet.text = "Actualizar Mascota"

            // Rellena los campos con los datos existentes
            binding.etPetName.setText(intent.getStringExtra("PET_NAME"))
            binding.etPetType.setText(intent.getStringExtra("PET_TYPE"))
            binding.etPetAge.setText(intent.getIntExtra("PET_AGE", 0).toString())

            // Carga la imagen existente si la hay
            val existingImageBase64 = intent.getStringExtra("PET_IMAGE")
            if (!existingImageBase64.isNullOrEmpty()) {
                selectedImageBase64 = existingImageBase64
                displaySelectedImage()
            }

            // Muestra el botón de eliminar solo en modo edición
            binding.btnDeletePet.visibility = android.view.View.VISIBLE
        } else {
            // Si no hay ID, estamos añadiendo una nueva mascota
            binding.tvTitle.text = "Añadir Nueva Mascota"
            binding.btnSavePet.text = "Guardar Mascota"
            binding.btnDeletePet.visibility = android.view.View.GONE
        }

        // Configurar imagen por defecto
        if (selectedImageBase64.isEmpty()) {
            binding.ivPetImage.setImageResource(android.R.drawable.ic_menu_camera)
        }
    }

    private fun setupObservers() {
        // Observa el resultado de la operación de guardar (añadir/actualizar)
        addEditPetViewModel.saveResult.observe(this) { result ->
            result.onSuccess {
                val message = if (isEditMode) "Mascota actualizada exitosamente" else "Mascota guardada exitosamente"
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                finish() // Cierra la actividad y vuelve a la lista de mascotas
            }.onFailure { exception ->
                Toast.makeText(this, "Error al guardar mascota: ${exception.message}", Toast.LENGTH_LONG).show()
            }
        }

        // Observa el resultado de eliminar (si se implementa en el ViewModel)
        addEditPetViewModel.deleteResult.observe(this) { result ->
            result.onSuccess {
                Toast.makeText(this, "Mascota eliminada exitosamente", Toast.LENGTH_SHORT).show()
                finish()
            }.onFailure { exception ->
                Toast.makeText(this, "Error al eliminar mascota: ${exception.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setupClickListeners() {
        // Configura el listener para el botón de guardar mascota
        binding.btnSavePet.setOnClickListener {
            savePet()
        }

        // Configura el listener para el botón de eliminar mascota
        binding.btnDeletePet.setOnClickListener {
            showDeleteConfirmationDialog()
        }

        // Botón de cancelar/volver
        binding.btnCancel.setOnClickListener {
            finish()
        }

        // Click en la imagen para seleccionar/cambiar imagen
        binding.ivPetImage.setOnClickListener {
            showImageSelectionDialog()
        }

        // Botón para seleccionar imagen
        binding.btnSelectImage.setOnClickListener {
            showImageSelectionDialog()
        }

        // Botón para eliminar imagen
        binding.btnRemoveImage.setOnClickListener {
            removeSelectedImage()
        }
    }

    /**
     * Muestra un diálogo para seleccionar el origen de la imagen (galería o cámara)
     */
    private fun showImageSelectionDialog() {
        val options = arrayOf("Galería", "Cámara")
        AlertDialog.Builder(this)
            .setTitle("Seleccionar imagen")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> selectImageFromGallery()
                    1 -> takePictureWithCamera()
                }
            }
            .show()
    }

    /**
     * Abre la galería para seleccionar una imagen
     */
    private fun selectImageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        selectImageLauncher.launch(intent)
    }

    /**
     * Abre la cámara para tomar una foto
     */
    private fun takePictureWithCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        takePictureLauncher.launch(intent)
    }

    /**
     * Procesa la imagen seleccionada desde la galería
     */
    private fun processSelectedImage(imageUri: Uri) {
        val base64Image = ImageUtils.uriToBase64(this, imageUri)
        if (base64Image != null) {
            selectedImageBase64 = base64Image
            displaySelectedImage()
        } else {
            Toast.makeText(this, "Error al procesar la imagen", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Muestra la imagen seleccionada en el ImageView
     */
    private fun displaySelectedImage() {
        if (selectedImageBase64.isNotEmpty()) {
            val bitmap = ImageUtils.base64ToBitmap(selectedImageBase64)
            if (bitmap != null) {
                binding.ivPetImage.setImageBitmap(bitmap)
                binding.btnRemoveImage.visibility = android.view.View.VISIBLE
            }
        }
    }

    /**
     * Elimina la imagen seleccionada
     */
    private fun removeSelectedImage() {
        selectedImageBase64 = ""
        binding.ivPetImage.setImageResource(android.R.drawable.ic_menu_camera)
        binding.btnRemoveImage.visibility = android.view.View.GONE
        Toast.makeText(this, "Imagen eliminada", Toast.LENGTH_SHORT).show()
    }

    /**
     * Recopila los datos de los campos de entrada y llama al ViewModel para guardar la mascota.
     */
    private fun savePet() {
        val name = binding.etPetName.text.toString().trim()
        val type = binding.etPetType.text.toString().trim()
        val ageString = binding.etPetAge.text.toString().trim()

        if (name.isEmpty() || type.isEmpty() || ageString.isEmpty()) {
            Toast.makeText(this, "Por favor, completa todos los campos", Toast.LENGTH_SHORT).show()
            return
        }

        val age = ageString.toIntOrNull()
        if (age == null || age <= 0) {
            Toast.makeText(this, "La edad debe ser un número válido y positivo", Toast.LENGTH_SHORT).show()
            return
        }

        // Crea un objeto Pet. Si petId es nulo, se añadirá una nueva mascota.
        // Si petId no es nulo, se actualizará la mascota existente.
        val pet = Mascota(
            id = petId ?: "", // Si petId es nulo, se usa una cadena vacía (para nueva mascota)
            name = name,
            type = type,
            age = age,
            ownerId = "", // El ViewModel se encargará de asignar el ownerId
            imageBase64 = selectedImageBase64 // Incluir la imagen en base64
        )

        addEditPetViewModel.savePet(pet)
    }

    private fun showDeleteConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Eliminar Mascota")
            .setMessage("¿Estás seguro de que deseas eliminar esta mascota? Esta acción no se puede deshacer.")
            .setPositiveButton("Eliminar") { _, _ ->
                petId?.let { id ->
                    addEditPetViewModel.deletePet(id)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}

/**
 * Factory para crear instancias de AddEditPetViewModel.
 * Necesario para inyectar los repositorios en el ViewModel.
 */
class AddEditPetViewModelFactory(
    private val petRepository: MascotaRepository,
    private val authRepository: AuthRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MascotaAddEditViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MascotaAddEditViewModel(petRepository, authRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}