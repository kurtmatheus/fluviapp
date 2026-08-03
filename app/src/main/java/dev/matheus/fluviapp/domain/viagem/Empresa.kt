package dev.matheus.fluviapp.domain.viagem

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A **parte**: quem existe no mundo, com identidade e CNPJ (ADR-0016 §4). É superentidade — não é
 * "empresa de navegação" nem "agência": o que ela *faz* é a [AtuacaoDaEmpresa], e uma parte exerce
 * várias ao mesmo tempo.
 *
 * O `toDocumento()` saiu daqui (ADR-0019 D2): o domínio não conhece a forma do documento do Firestore.
 * A conversão mora do lado do DTO, e some de vez quando a fronteira virar `Map`.
 *
 * O `@Entity` continua por enquanto — o espelho Room sai na fatia de dados desta mesma frente (ADR-0017).
 */
@Entity(indices = [Index("id")])
data class Empresa(
    @PrimaryKey
    val id: String,
    val nome: String,
    val razaoSocial: String,
    val cnpj: String,
    val endereco: String,
    val telefone1: String,
    val telefone2: String,
)