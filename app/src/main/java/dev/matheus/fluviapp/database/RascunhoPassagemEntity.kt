package dev.matheus.fluviapp.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Rascunho de passagem persistido como JSON (memória cacheada, ADR-0003). Slot único (id fixo):
 * o SQL guarda só o blob — o dinamismo da forma vive no JSON, não em colunas tipadas.
 */
@Entity(tableName = "rascunho_passagem")
data class RascunhoPassagemEntity(
    @PrimaryKey val id: Int = ID_UNICO,
    val json: String,
) {
    companion object {
        const val ID_UNICO = 1
    }
}