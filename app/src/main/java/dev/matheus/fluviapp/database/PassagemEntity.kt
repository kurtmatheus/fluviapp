package dev.matheus.fluviapp.database

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * **A linha achatada do bilhete — a Passagem como o Room a guarda, e nada mais.**
 *
 * Ela morava em `domain/passagem/Passagem.kt` e acumulava três papéis num arquivo: entidade do Room, modelo de
 * domínio e fonte dos snapshots. A F9.1 tirou dois: o modelo agora é a [dev.matheus.fluviapp.domain.passagem.Passagem]
 * **selada por categoria** (ADR-0023 D1), e os snapshots morreram com o D8 do mesmo ADR.
 *
 * **Por que mover em vez de renomear.** O nome precisava se libertar para a raiz nova — o mesmo movimento que a
 * F8.1 fez com o `ViagemDocumento` —, e a pergunta era como. Chamá-la de "legado" seria inventar um rótulo; **isto
 * é uma entidade do Room**, e entidade do Room vive em `database/`, ao lado da `RascunhoPassagemEntity`. O nome
 * novo diz o que ela é, e o pacote diz por quanto tempo: até a **F9.2**, quando a Passagem sai do Room
 * (ADR-0017 F5) e este arquivo é apagado com a tabela e o DAO.
 *
 * **Não construa nada sobre ela.** Ela existe para que o app compile e as telas escuras continuem de pé enquanto
 * as fatias andam; os 49 campos planos, os três passageiros repetidos e as quatro colunas de valor são exatamente
 * o que os ADR-0023/0024 desfazem.
 */
// `tableName` fixado: sem ele, renomear a classe renomearia a **tabela** (o Room usa o nome da classe), e uma
// tabela que muda de nome pede migração escrita — desde a v0.0.4 elas são escritas, não recriadas, porque
// `fallbackToDestructiveMigration` levaria o rascunho de quem tem o app instalado. Pedir migração para uma tabela
// que a F9.2 apaga seria trabalho com prazo de validade de uma fatia.
@Entity(tableName = "Passagem", indices = [Index("id")])
data class PassagemEntity(
    @PrimaryKey
    val id: String,
    val numero: String,
    // Ponteiro estável para a Viagem (ADR-0008): id p/ relacionar/agregar. codigoViagem e
    // empresa/embarcacao/origem/destino seguem como snapshot por valor (histórico imutável do bilhete).
    val viagemId: String = "",
    // Ids da embarcação/empresa congelados no momento da emissão (snapshot). O balanço agrega por embarcacaoId
    // (frozen) — rename/reatribuição posterior na Viagem não altera bilhetes históricos. empresaId
    // fica dormente até a relação Passagem→Empresa por id.
    val embarcacaoId: String = "",
    val empresaId: String = "",
    val codigoViagem: String,
    val empresa: String,
    val embarcacao: String,
    val origem: String,
    val destino: String,
    val dataViagem: String,
    val horaViagem: String,
    /**
     * Agência emissora, congelada na emissão (ADR-0015 §3). **Derivada**, não digitada: é a agência do
     * funcionário que emitiu. O campo `agente` que ficava aqui morreu em P2.3 — ele guardava o nome do
     * emissor, que é exatamente o que [funcionarioResponsavel] já guarda; duas colunas para o mesmo
     * fato só criam a chance de discordarem.
     */
    val agencia: String = "",
    /**
     * **A agência emissora por id** — o `empresaId` do vínculo ativo de quem emitiu, congelado como o
     * resto do snapshot (F7).
     *
     * Ele nasce antes de ter leitor porque o campo ao lado não serve para recortar: [agencia] é um
     * **nome**, e nome muda, repete e não relaciona. Enquanto ele for a única coordenada, "as passagens
     * da minha empresa" é uma comparação de texto — e o recorte por empresa da F9 precisa de um id que
     * já esteja gravado nos bilhetes de antes dela.
     *
     * Não confundir com [empresaId], que é a empresa **da viagem** (quem transporta). Aqui é quem
     * **vendeu**: numa mesma saída, as duas costumam ser diferentes, e é justamente por isso que a
     * ocupação atravessa empresas e o faturamento não.
     */
    val agenciaId: String = "",
    val valorPix: Double? = null,
    val valorDinheiro: Double? = null,
    val valorDebito: Double? = null,
    val valorCredito: Double? = null,
    // Tarifa da inteira congelada na emissão (ADR-0013): a célula da tabela da Viagem para a acomodação
    // escolhida. É a base de que a tarifa devida (meia = metade, gratuidade = 0) e o desconto derivam.
    // Aditivo; null cobre bilhetes anteriores e o veículo (tarifa por classe é Fase 3).
    val tarifaBase: Double? = null,
    val observacao: String? = null,
    val tipoPassagem: String? = null,
    val gratuidade: String? = null,
    val acomodacao: String? = null,
    val nomePassageiro1: String? = null,
    val documentoPassageiro1: String? = null,
    val numeroDocumentoPassageiro1: String? = null,
    val dataNascimentoPassageiro1: String? = null,
    val nomePassageiro2: String? = null,
    val documentoPassageiro2: String? = null,
    val numeroDocumentoPassageiro2: String? = null,
    val dataNascimentoPassageiro2: String? = null,
    val nomePassageiro3: String? = null,
    val tipoDocumentoPassageiro3: String? = null,
    val numeroDocumentoPassageiro3: String? = null,
    val dataNascimentoPassageiro3: String? = null,
    val nomeResponsavelRetirada: String? = null,
    val documentoResponsavelRetirada: String? = null,
    val numeroDocumentoResponsavelRetirada: String? = null,
    val tipoVeiculo: String? = null,
    val modeloVeiculo: String? = null,
    val placaVeiculo: String? = null,
    val corVeiculo: String? = null,
    // Cilindrada da moto (ADR-0013): o cc que justificou a tarifaBase; registro do bilhete. Aditivo.
    val cilindrada: String? = null,
    val funcionarioResponsavel: String,
    // Dono estável da passagem = uid do criador (ADR-0010 Fase 2). Congelado na emissão; o nome
    // (funcionarioResponsavel) segue como snapshot de exibição. Default "" cobre bilhetes anteriores.
    val funcionarioId: String = "",
    val status: String,
    // Registro do embarque (ADR-0012): quem validou o QR (uid, chave estável ADR-0008), o nome como
    // snapshot de exibição, e quando. Aditivos; default "" cobre bilhetes não embarcados/anteriores.
    val embarcadaPorId: String = "",
    val embarcadaPor: String = "",
    val embarcadaEm: String = "",
) {
    @Ignore
    val temPassageiro2 = !nomePassageiro2.isNullOrEmpty()

    @Ignore
    val temPassageiro3 = !nomePassageiro3.isNullOrEmpty()

    @Ignore
    val ehVeiculo = !placaVeiculo.isNullOrEmpty()
}



