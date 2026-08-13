package dev.matheus.fluviapp.services.repository.firebase.documents

import dev.matheus.fluviapp.domain.passagem.Acomodacao
import dev.matheus.fluviapp.domain.passagem.CarimboEmbarque
import dev.matheus.fluviapp.domain.passagem.CategoriaPassagem
import dev.matheus.fluviapp.domain.passagem.FormaPagamento
import dev.matheus.fluviapp.domain.passagem.Lancamento
import dev.matheus.fluviapp.domain.passagem.MetadadosPassagem
import dev.matheus.fluviapp.domain.passagem.Passagem
import dev.matheus.fluviapp.domain.passagem.PassagemDePassageiro
import dev.matheus.fluviapp.domain.passagem.PassagemDeVeiculo
import dev.matheus.fluviapp.domain.passagem.StatusPassagem
import dev.matheus.fluviapp.domain.passagem.TipoGratuidade
import dev.matheus.fluviapp.domain.passagem.TipoPassagem
import dev.matheus.fluviapp.domain.viagem.OcorrenciaViagem
import dev.matheus.fluviapp.services.repository.firebase.DocumentoBruto
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * A forma do documento em `passagens/{id}` — **documentação, não caminho** ([ADR-0019] D2, [ADR-0024] D1).
 *
 * Uma coleção só, com a [categoria] discriminando o sub-domínio. Não é preferência por documento homogêneo: é o
 * que a consulta pede, porque **ocupação e numeração são por ocorrência e atravessam categoria**. Com uma coleção
 * por categoria, contar quem embarca numa saída viraria N consultas somadas no cliente e a regra do servidor
 * existiria em três versões quase iguais — que é onde divergências nascem.
 *
 * ### O que mudou em relação ao documento que esta classe substitui
 *
 * | Antes | Agora | Por quê |
 * |---|---|---|
 * | `viagem` embutida (empresa, embarcação, origem, destino) | só [viagemId] + [data] | nada é congelado (ADR-0023 D8) |
 * | `passageiro1/2/3` embutidos | [clientes] — ids, titular na **posição 0** | participante é entidade de pool (D3) |
 * | `veiculo` embutido | [veiculoId] | idem |
 * | `valorPix`/`valorDinheiro`/`valorDebito`/`valorCredito` | [lancamentos] | somar por forma na escrita descarta informação (D4) |
 * | `dataViagem` `dd/MM/yyyy`, `embarcadaEm` `dd/MM/yyyy HH:mm` | **ISO-8601** | o formato antigo **não ordena** (D2) |
 * | `embarcadaPorId`/`embarcadaPor`/`embarcadaEm` planos | [embarque] ausente ou inteiro | o meio-preenchido deixa de ser escrevível (ADR-0018 D14) |
 * | `agencia`, `funcionarioResponsavel` (nomes) | só os ids | nome se resolve por referência |
 *
 * ### Por que os campos de consulta ficam no topo
 *
 * [categoria], [viagemId], [data], [status], [agenciaId] e [funcionarioId] são o que a consulta recorta e o que a
 * regra do servidor confere. Aninhá-los num mapa `metadados` custaria caminho em cada índice composto e em cada
 * linha de regra, sem comprar nada — o agrupamento que interessa é o do **domínio**, e lá eles já vivem em
 * [MetadadosPassagem]. O [embarque] é a exceção, e por razão oposta: ele existe para ser **ausente ou inteiro**.
 */
data class PassagemDocumento(
    /** O discriminador (D1): `PASSAGEIRO` ou `VEICULO`. Ausente ou ilegível → o documento não vira nada. */
    val categoria: String = "",
    /** A identidade **exibida**, por ocorrência. Distinta do id do documento, que é o que o QR carrega. */
    val numero: String = "",
    val viagemId: String = "",
    /** ISO-8601 `yyyy-MM-dd` — data de calendário, não instante (D2). */
    val data: String = "",
    val lancamentos: List<LancamentoDocumento> = emptyList(),
    val observacao: String? = null,
    val status: String = "",
    val funcionarioId: String = "",
    val agenciaId: String = "",
    /** ISO-8601 `yyyy-MM-ddTHH:mm:ss`. Como a emissão é pós-pagamento, é quando o dinheiro entrou. */
    val criadoEm: String = "",
    val alteradoEm: String = "",
    /** Ausente até embarcar; presente **inteiro** depois. */
    val embarque: CarimboEmbarqueDocumento? = null,
    // --- só quando `categoria == PASSAGEIRO` ---
    val acomodacao: String? = null,
    val tipo: String? = null,
    /**
     * Subtipo da gratuidade — presente **só** quando `tipo == GRATUIDADE` ([ADR-0028] D2).
     *
     * É campo de topo, e não um detalhe do tipo, porque é sobre ele que a **cota** conta: *"quantas
     * gratuidades de idoso já saíram nesta ocorrência?"* é uma consulta, e consulta não olha dentro de
     * estrutura aninhada sem custo de índice.
     */
    val gratuidade: String? = null,
    /** Ids do pool de clientes, **ordenados**: o primeiro é o titular (D3). */
    val clientes: List<String>? = null,
    // --- só quando `categoria == VEICULO` ---
    val veiculoId: String? = null,
    val responsavelRetirada: String? = null,
)

/** Um lançamento como a fronteira o guarda: `valor` em `Double`, porque `BigDecimal` é do domínio (D4). */
data class LancamentoDocumento(
    val id: String = "",
    val forma: String = "",
    val valor: Double = 0.0,
)

/** O carimbo do embarque: o **uid** de quem validou o QR — é ele que a regra confere contra `request.auth.uid`. */
data class CarimboEmbarqueDocumento(
    val porId: String = "",
    val em: String = "",
)

/**
 * `DocumentoBruto` → domínio, **despachando pela categoria** ([ADR-0024] D1).
 *
 * O codec lê o discriminador, escolhe o construtor e **recusa o que não reconhece** — o mesmo mecanismo da
 * Embarcação sem tipo e da Viagem sem dia. Documento com categoria ausente ou ilegível não vira passagem de
 * categoria padrão: **não vira nada**, sai da lista pelo `mapNotNull` e aparece na telemetria como recusa.
 *
 * ### As quatro recusas, e a régua que as une
 *
 * Recusa-se o documento que **não tem sujeito ou não tem lugar** — aquilo que nenhuma tela conserta:
 *
 * 1. **categoria** ilegível — não se sabe sequer que passagem é;
 * 2. **ocorrência** ilegível (sem `viagemId` ou com `data` que não é ISO) — bilhete sem travessia;
 * 3. **status** ilegível — a FSM é o que diz se o bilhete vale, e um valor fora dela não é "em branco";
 * 4. **sujeito ausente** — passagem de passageiro sem nenhum cliente, ou de veículo sem `veiculoId`. É o
 *    precedente do `Porto` sem localidade: referência quebrada que nenhuma tela repara.
 *
 * ### E a quinta, que inverte a assimetria do `FuncionarioDocumento`
 *
 * **Lançamento ilegível recusa a passagem inteira** (D5). Lá, item de lista ilegível some da lista sem levar o
 * dono junto — perder o nome de quem existe seria pior do que perder um vínculo ininterpretável. Aqui é
 * dinheiro: descartar um item faz o bilhete valer menos do que valeu — R$ 40 onde entraram R$ 50 —, em
 * silêncio, e esse número vai para o balanço. **Um bilhete que não aparece é um problema visível; um bilhete
 * com o valor errado é um problema invisível, e invisível é o que não se conserta.**
 *
 * Lista de lançamentos **vazia** não recusa: gratuidade não gera lançamento nenhum.
 */
fun DocumentoBruto.toPassagem(): Passagem? {
    val categoria = CategoriaPassagem.de(texto("categoria")) ?: return null
    val ocorrencia = OcorrenciaViagem.de(texto("viagemId"), texto("data")) ?: return null
    val metadados = metadadosDaPassagem() ?: return null
    val lancamentos = lancamentosDaPassagem() ?: return null

    val numero = texto("numero")
    val observacao = texto("observacao").takeIf { it.isNotBlank() }

    return when (categoria) {
        CategoriaPassagem.PASSAGEIRO -> {
            val acomodacao = Acomodacao.de(texto("acomodacao")) ?: return null
            val tipo = TipoPassagem.de(texto("tipo")) ?: return null
            val clientes = listaDeTextos("clientes")
            if (clientes.isEmpty()) return null

            PassagemDePassageiro(
                id = id,
                numero = numero,
                ocorrencia = ocorrencia,
                lancamentos = lancamentos,
                observacao = observacao,
                metadados = metadados,
                acomodacao = acomodacao,
                tipo = tipo,
                // Subtipo ilegível **não recusa o bilhete**, e a assimetria em relação ao `tipo` é de
                // consequência: sem tipo não se sabe o que se cobrou; sem subtipo perde-se a razão de uma
                // gratuidade que continua sendo gratuidade. A incoerência fica visível em `pendencias()`,
                // que é onde a tela a cobra — recusar aqui sumiria com o bilhete de quem já viajou.
                gratuidade = TipoGratuidade.de(texto("gratuidade")),
                clientes = clientes,
            )
        }

        CategoriaPassagem.VEICULO -> {
            val veiculoId = texto("veiculoId")
            if (veiculoId.isBlank()) return null

            PassagemDeVeiculo(
                id = id,
                numero = numero,
                ocorrencia = ocorrencia,
                lancamentos = lancamentos,
                observacao = observacao,
                metadados = metadados,
                veiculoId = veiculoId,
                responsavelRetirada = texto("responsavelRetirada").takeIf { it.isNotBlank() },
            )
        }
    }
}

/**
 * Os metadados; `null` quando o [StatusPassagem] é ilegível — a FSM é o que diz se o bilhete vale.
 *
 * O carimbo de embarque é **ausente ou inteiro**: um sub-objeto sem `porId` ou sem `em` é descartado por
 * completo, porque autoria sem instante (ou instante sem autoria) é exatamente o estado meio-preenchido que o
 * sub-objeto existe para impedir.
 */
private fun DocumentoBruto.metadadosDaPassagem(): MetadadosPassagem? {
    val status = StatusPassagem.de(texto("status")) ?: return null
    val embarque = (dados["embarque"] as? Map<*, *>)?.let { bruto ->
        val porId = bruto["porId"] as? String
        val em = bruto["em"] as? String
        if (porId.isNullOrBlank() || em.isNullOrBlank()) null else CarimboEmbarque(porId = porId, em = em)
    }

    return MetadadosPassagem(
        status = status,
        funcionarioId = texto("funcionarioId"),
        agenciaId = texto("agenciaId"),
        criadoEm = texto("criadoEm"),
        alteradoEm = texto("alteradoEm"),
        embarque = embarque,
    )
}

/**
 * Os lançamentos — `null` significa **recusar a passagem inteira** (D5), não "lista vazia".
 *
 * Um item é ilegível quando a forma não é conhecida ou o valor não é número. Nesses dois casos o dinheiro que
 * entrou deixa de ser reconstruível, e é isso que o `null` propaga.
 *
 * ### A escala, que a ida-e-volta cobrou
 *
 * O valor volta do Firestore como `Double` e é **normalizado em duas casas** na entrada do domínio. Sem isso,
 * R$ 100,00 gravado voltava como `BigDecimal` de escala 1 — numericamente igual e **diferente por `equals`**,
 * porque `BigDecimal` compara valor *e* escala. Um agregado que não sobrevive à própria travessia idêntico é
 * um agregado que nenhum teste de ida-e-volta protege, e é onde erro de centavo se esconde.
 *
 * `HALF_UP`, e não o `UP` das regras de tarifa: aqui não se **calcula** preço, se **lê** o que já entrou, e
 * arredondar sempre para cima inventaria centavo que ninguém pagou.
 */
private fun DocumentoBruto.lancamentosDaPassagem(): List<Lancamento>? {
    val brutos = dados["lancamentos"] ?: return emptyList()
    val lista = brutos as? List<*> ?: return null

    return lista.map { item ->
        val mapa = item as? Map<*, *> ?: return null
        val forma = FormaPagamento.de(mapa["forma"] as? String) ?: return null
        val valor = (mapa["valor"] as? Number)?.toDouble() ?: return null
        Lancamento(
            id = mapa["id"] as? String ?: return null,
            forma = forma,
            valor = BigDecimal.valueOf(valor).setScale(CASAS_DE_CENTAVO, RoundingMode.HALF_UP),
        )
    }
}

private const val CASAS_DE_CENTAVO = 2

/** Array de strings — a forma que o Firestore devolve os ids de [PassagemDocumento.clientes]. */
private fun DocumentoBruto.listaDeTextos(chave: String): List<String> =
    (dados[chave] as? List<*>)?.filterIsInstance<String>()?.filter { it.isNotBlank() }.orEmpty()

/**
 * Domínio → `Map`. O `id` não entra: ele é o nome do documento.
 *
 * O `when` é exaustivo de propósito — é ele que fará a **carga**, quando existir, ser um erro de compilação
 * aqui em vez de um campo que ninguém gravou (ADR-0023 D1).
 */
fun Passagem.paraMapa(): Map<String, Any?> = comunsParaMapa() + when (this) {
    is PassagemDePassageiro -> mapOf(
        "acomodacao" to acomodacao.name,
        "tipo" to tipo.name,
        "gratuidade" to gratuidade?.name,
        "clientes" to clientes,
    )

    is PassagemDeVeiculo -> mapOf(
        "veiculoId" to veiculoId,
        "responsavelRetirada" to responsavelRetirada,
    )
}

/** O que descreve **a travessia vendida**, e é igual em toda categoria (ADR-0023 D2). */
private fun Passagem.comunsParaMapa(): Map<String, Any?> = mapOf(
    "categoria" to categoria.name,
    "numero" to numero,
    "viagemId" to ocorrencia.viagemId,
    "data" to ocorrencia.dataIso,
    "lancamentos" to lancamentos.map {
        mapOf("id" to it.id, "forma" to it.forma.name, "valor" to it.valor.toDouble())
    },
    "observacao" to observacao,
    "status" to metadados.status.name,
    "funcionarioId" to metadados.funcionarioId,
    "agenciaId" to metadados.agenciaId,
    "criadoEm" to metadados.criadoEm,
    "alteradoEm" to metadados.alteradoEm,
    "embarque" to metadados.embarque?.let { mapOf("porId" to it.porId, "em" to it.em) },
)