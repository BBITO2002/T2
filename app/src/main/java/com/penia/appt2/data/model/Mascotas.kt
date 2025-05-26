package com.penia.appt2.data.model

import com.google.firebase.firestore.DocumentId

data class Mascota (
    @DocumentId // Anotación para mapear el ID del documento de Firestore
    var id: String = "",
    var name: String = "",
    var type: String = "", // Ej: "Perro", "Gato"
    var age: Int = 0,
    var ownerId: String = "", // Para asociar la mascota con un usuario específico
    var imageBase64: String = "" // Nueva propiedad para almacenar la imagen en base64
)