package com.penia.appt2.ui.mascota

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.penia.appt2.R
import com.penia.appt2.data.model.Mascota
import com.penia.appt2.databinding.ItemMascotaBinding
import com.penia.appt2.utils.ImageUtils

class MascotaAdapter(
    private val onItemClick: (Mascota) -> Unit,
    private val onEditClick: (Mascota) -> Unit,
    private val onDeleteClick: (String) -> Unit
) : ListAdapter<Mascota, MascotaAdapter.MascotaViewHolder>(PetDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MascotaViewHolder {
        // Infla el layout de cada elemento de la lista
        val binding = ItemMascotaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MascotaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MascotaViewHolder, position: Int) {
        // Vincula los datos de la mascota en la posición actual al ViewHolder
        val mascota = getItem(position)
        holder.bind(mascota)
    }

    inner class MascotaViewHolder(private val binding: ItemMascotaBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            // Click en toda la tarjeta para editar
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(position))
                }
            }

            // Configura los listeners para los botones de editar y eliminar
            binding.btnEditPet.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onEditClick(getItem(position))
                }
            }

            binding.btnDeletePet.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onDeleteClick(getItem(position).id)
                }
            }
        }

        fun bind(pet: Mascota) {
            // Asigna los datos de la mascota a las vistas correspondientes
            binding.tvPetName.text = pet.name
            binding.tvPetType.text = "Tipo: ${pet.type}"
            binding.tvPetAge.text = "Edad: ${pet.age} años"

            // Manejo de la imagen de la mascota
            loadPetImage(pet)
        }

        private fun loadPetImage(pet: Mascota) {
            if (pet.imageBase64.isNotEmpty() && ImageUtils.isValidBase64Image(pet.imageBase64)) {
                // Si hay una imagen válida en Base64, la convertimos a Bitmap y la mostramos
                val bitmap = ImageUtils.base64ToBitmap(pet.imageBase64)
                if (bitmap != null) {
                    binding.ivPetImage.setImageBitmap(bitmap)
                } else {
                    // Si hay error al convertir, mostrar imagen por defecto
                    setDefaultImage(pet.type)
                }
            } else {
                // Si no hay imagen, mostrar imagen por defecto basada en el tipo
                setDefaultImage(pet.type)
            }
        }

        private fun setDefaultImage(petType: String) {
            // Establecer imagen por defecto basada en el tipo de mascota
           /* val defaultImageResource = when (petType.lowercase()) {
                "perro", "dog" -> R.drawable.ic_default_dog
                "gato", "cat" -> R.drawable.ic_default_cat
                "pájaro", "bird", "ave" -> R.drawable.ic_default_bird
                "pez", "fish" -> R.drawable.ic_default_fish
                "conejo", "rabbit" -> R.drawable.ic_default_rabbit
                "hámster", "hamster" -> R.drawable.ic_default_hamster
                else -> R.drawable.ic_default_pet // Imagen genérica para mascotas
            }

            // Si no tienes iconos específicos, puedes usar un icono genérico
            try {
                binding.ivPetImage.setImageResource(defaultImageResource)
            } catch (e: Exception) {
                // Si no existe el recurso, usar un icono por defecto del sistema
                binding.ivPetImage.setImageResource(android.R.drawable.ic_menu_camera)
            }*/
        }
    }

    /**
     * Callback para calcular las diferencias entre dos listas de mascotas.
     * Mejora el rendimiento del RecyclerView al solo actualizar los elementos que han cambiado.
     */
    class PetDiffCallback : DiffUtil.ItemCallback<Mascota>() {
        override fun areItemsTheSame(oldItem: Mascota, newItem: Mascota): Boolean {
            // Compara si los IDs de los elementos son los mismos (mismo elemento)
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Mascota, newItem: Mascota): Boolean {
            // Compara si el contenido de los elementos es el mismo (no hay cambios visuales)
            return oldItem == newItem
        }
    }
}