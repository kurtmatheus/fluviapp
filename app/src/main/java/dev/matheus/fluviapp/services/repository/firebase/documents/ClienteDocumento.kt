package dev.matheus.fluviapp.services.repository.firebase.documents

import dev.matheus.fluviapp.domain.cliente.Cliente
import dev.matheus.fluviapp.domain.documento.TipoDocumento
import dev.matheus.fluviapp.services.repository.firebase.DocumentoBruto
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * A forma do documento em `clientes/{chaveNatural}` — **documentação, não caminho** ([ADR-0019] D2).
 *
 * ### O id do documento **é** a chave natural, e isso não é conveniência
 *
 * O [ADR-0018] D3 descreve a escrita do pool como *"tentar um e cair no outro"* — criar a entrada que não
 * existe, ou assinar a que existe —, e explica por que ela não pode começar com uma busca: **quem emite não
 * lê o que ainda não assinou**. A leitura é recortada por `agenciaIds`, então procurar antes de escrever
 * devolveria *"não existe"* para uma pessoa que existe, e o pool ganharia uma entrada duplicada por agência —
 * exatamente o que o D3 recusa ao dizer que uma pessoa é **um** documento, não um por tenant.
 *
 * Com o id derivado da chave (`CPF:12345678901`), *tentar criar* aponta para o documento certo sem lê-lo, e
 * quem decide se é criação ou assinatura é **o servidor** — que enxerga o que o cliente não pode enxergar.
 *
 * Isso **não** faz do id um identificador de pessoa: o [ADR-0018] D2 já diz que **uma entrada identifica a
 * credencial**, não a pessoa. A mesma pessoa com CPF numa agência e RG noutra continua sendo duas entradas, e
 * isso é aceito — *"não é redundância, é questão de análise de dados"*.
 *
 * ### PII, e o que isso muda aqui
 *
 * Nome, documento, nascimento e telefone. A máscara na exibição é **política de dado pessoal** (a razão de
 * fundo do [TipoDocumento]), e por isso mora no tipo, não numa linha de catálogo. O que **este** arquivo faz é
 * guardar o valor canônico — normalizado pelo próprio tipo, sem separadores —, porque é sobre ele que a chave
 * natural se calcula: `529.982.247-25` e `52998224725` não podem virar duas pessoas.
 */
data class ClienteDocumento(
    val nome: String = "",
    val tipoDocumento: String = "",
    /** Canônico: só o que é significativo, sem pontuação (ver [TipoDocumento.normalizar]). */
    val numeroDocumento: String = "",
    /** ISO-8601 `yyyy-MM-dd` — a régua da F8.1: tipo dentro do app, texto na borda. */
    val dataNascimento: String = "",
    /** Opcional: é o único campo que **não** identifica ninguém — existe para alcançar a pessoa. */
    val telefone: String? = null,
    /** As agências que já a atenderam: existência é global, visibilidade é local ([ADR-0018] D3). */
    val agenciaIds: List<String> = emptyList(),
)

/**
 * `DocumentoBruto` → domínio. **`null` quando não é um cliente**, e as três recusas seguem a régua do `Porto`:
 * recusa-se o que nenhuma tela conserta.
 *
 * 1. **tipo de documento** ilegível — sem ele o número não se interpreta nem se normaliza, e a chave natural
 *    (que é a identidade do pool) não se calcula;
 * 2. **número em branco** — *"não existe criança sem documento nesse negócio"* ([ADR-0018] D4): uma entrada
 *    sem credencial não identifica nada e não se reaproveita, que é a única razão de o pool existir;
 * 3. **nascimento ilegível** — é ele que **decide a gratuidade de criança**. Um cliente com data em branco
 *    entraria no fluxo de emissão valendo meia idade nenhuma, e a regra erraria em silêncio.
 *
 * O **nome em branco não recusa**, e a assimetria é a mesma do `Porto`: nome ruim é cadastro malfeito, que
 * alguém corrige; credencial ausente é uma entrada que não serve para o que o pool existe.
 */
fun DocumentoBruto.toCliente(): Cliente? {
    val tipo = TipoDocumento.de(texto("tipoDocumento")) ?: return null
    val numero = tipo.normalizar(texto("numeroDocumento"))
    if (numero.isBlank()) return null
    val nascimento = runCatching { LocalDate.parse(texto("dataNascimento"), FORMATO_ISO) }.getOrNull()
        ?: return null

    return Cliente(
        id = id,
        nome = texto("nome"),
        tipoDocumento = tipo,
        numeroDocumento = numero,
        dataNascimento = nascimento,
        telefone = texto("telefone").takeIf { it.isNotBlank() },
        agenciaIds = (dados["agenciaIds"] as? List<*>)?.filterIsInstance<String>()?.toSet().orEmpty(),
    )
}

/**
 * Domínio → `Map`. O `id` não entra: ele é o nome do documento — e aqui isso é literal, porque o nome do
 * documento **é** a [Cliente.chaveNatural].
 *
 * O `agenciaIds` sai como **lista**, e não como o `Set` do domínio: o Firestore não tem conjunto. Quem
 * garante que assinar duas vezes é o mesmo que assinar uma é o `arrayUnion` da escrita, não este mapa.
 */
fun Cliente.paraMapa(): Map<String, Any?> = mapOf(
    "nome" to nome,
    "tipoDocumento" to tipoDocumento.name,
    "numeroDocumento" to tipoDocumento.normalizar(numeroDocumento),
    "dataNascimento" to dataNascimento.format(FORMATO_ISO),
    "telefone" to telefone,
    "agenciaIds" to agenciaIds.toList(),
)

private val FORMATO_ISO: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE