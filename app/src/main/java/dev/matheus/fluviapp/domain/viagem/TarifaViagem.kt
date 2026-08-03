    package dev.matheus.fluviapp.domain.viagem

import androidx.room.Entity

/**
 * Tarifa da **inteira** de UMA célula da tabela de tarifas da Viagem (ADR-0013) — uma linha por
 * `(viagemId, chave)`. É a **forma normalizada** no Room: o lado "SQL" do trade-off do ADR-0003,
 * escolhido para o balanço **agregar/filtrar por `viagemId` direto em SQL** (várias tarifas-base por
 * viagem). No Firestore a mesma tabela vive como **mapa aninhado** no `ViagemDocumento`; o mapper achata
 * mapa↔linhas.
 *
 * - `chave`: chave tarifária **canônica** — acomodação p/ passageiro (`REDE`/`SUITE`/`CAMAROTE`) ou classe
 *   p/ veículo (`CARRO`/`CARRETA`/`CAMINHAO`). Moto é por **regra** (cilindrada), não por célula.
 * - `valor`: tarifa da inteira, `Double` na fronteira (ADR-0013 §6), manuseada em `BigDecimal` scale 2 no
 *   cálculo.
 *
 * PK composta `(viagemId, chave)`: uma tarifa por célula; re-cadastrar a mesma célula sobrescreve.
 */
@Entity(primaryKeys = ["viagemId", "chave"])
data class TarifaViagem(
    val viagemId: String,
    val chave: String,
    val valor: Double,
)