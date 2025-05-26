package com.penia.appt2.data.repository

import com.penia.appt2.data.model.Mascota
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class MascotaRepository(private val firestore: FirebaseFirestore) {

    // pets: coleccion en FireSTore
    private val petsCollection = firestore.collection("pets")

    /**
     * Añade una nueva mascota a Firestore.
     * @param pet El objeto Pet a añadir.
     * @return Un objeto Result<Boolean> que indica éxito o fracaso.
     */
    suspend fun addPet(pet: Mascota): Result<Boolean> {
        return try {
            // Firestore generará automáticamente un ID para el documento
            petsCollection.add(pet).await()
            Result.success(true)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Obtiene una lista de mascotas para un propietario específico en tiempo real.
     * Utiliza un Flow para emitir actualizaciones cada vez que los datos cambian en Firestore.
     *
     * @param ownerId El ID del propietario de las mascotas.
     * @return Un Flow de Result<List<Pet>>.
     */
    fun getPetsForOwner(ownerId: String): Flow<Result<List<Mascota>>> = callbackFlow {
        // Crea una consulta para obtener mascotas filtradas por ownerId
        // y ordenadas por nombre (opcional, puedes cambiar el criterio de ordenación)
        val subscription = petsCollection
            .whereEqualTo("ownerId", ownerId) // Filtra por el ID del propietario
            .orderBy("name", Query.Direction.ASCENDING) // Ordena por nombre ascendente
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    // Si hay un error, envía el fallo al Flow
                    trySend(Result.failure(e))
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    // Mapea los documentos a objetos Pet
                    val pets = snapshot.documents.mapNotNull { document ->
                        // Convierte el documento a un objeto Pet
                        // Asegúrate de que el ID del documento se mapee a la propiedad 'id' de Pet
                        document.toObject(Mascota::class.java)?.apply {
                            this.id = document.id // Asigna el ID del documento de Firestore
                        }
                    }
                    // Envía la lista de mascotas al Flow
                    trySend(Result.success(pets))
                } else {
                    // Si el snapshot es nulo (no debería ocurrir en un listener), envía una lista vacía
                    trySend(Result.success(emptyList()))
                }
            }

        // Cuando el Flow se cancela (ej. la Activity se destruye), se remueve el listener
        awaitClose { subscription.remove() }
    }

    /**
     * Actualiza una mascota existente en Firestore.
     * @param pet El objeto Pet con los datos actualizados (el ID debe coincidir con un documento existente).
     * @return Un objeto Result<Boolean> que indica éxito o fracaso.
     */
    suspend fun updatePet(pet: Mascota): Result<Boolean> {
        return try {
            // Actualiza el documento usando el ID de la mascota
            petsCollection.document(pet.id).set(pet).await()
            Result.success(true)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Elimina una mascota de Firestore.
     * @param petId El ID de la mascota a eliminar.
     * @return Un objeto Result<Boolean> que indica éxito o fracaso.
     */
    suspend fun deletePet(petId: String): Result<Boolean> {
        return try {
            petsCollection.document(petId).delete().await()
            Result.success(true)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

}