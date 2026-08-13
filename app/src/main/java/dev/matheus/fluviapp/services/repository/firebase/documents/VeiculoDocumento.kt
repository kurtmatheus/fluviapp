package dev.matheus.fluviapp.services.repository.firebase.documents

import dev.matheus.fluviapp.domain.passagem.ClasseVeiculo
import dev.matheus.fluviapp.domain.veiculo.Veiculo
import dev.matheus.fluviapp.services.repository.firebase.DocumentoBruto

/**
 * A forma do documento em `veiculos/{placa}` — **documentação, não caminho** ([ADR-0019] D2).
 *
 * Mesmo regime do pool de clientes ([ADR-0018] D5): assinatura por agência, curadoria da plataforma, id
 * derivado da chave natural. A diferença é a **qualidade da chave**: a placa é única por construção, sem o
 * par CPF × RG que faz o outro pool acumular duplicata legítima. Então **este pool não polui** — duplicata
 * aqui só nasce de digitação errada, e é contra isso que existe a máscara na entrada (D15, F9.5).
 *
 * ### O que ele guarda, e o que deliberadamente não guarda
 *
 * Guarda o que **identifica o veículo**: classe, modelo, cor e — só na moto — a cilindrada. A cilindrada é
 * atributo do veículo ainda que justifique a tarifa: é o cc que distingue uma moto de outra na travessia.
 *
 * **O responsável pela retirada não mora aqui.** É pessoa, então é `Cliente`, e o vínculo é da **passagem**:
 * quem retira muda a cada travessia, enquanto o veículo é o mesmo. Guardá-lo aqui faria o último responsável
 * parecer o dono.
 */
data class VeiculoDocumento(
    /** Chave natural, na grafia canônica (sem hífen, caixa alta). É também o id do documento. */
    val placa: String = "",
    val tipo: String = "",
    /** Ausente quando o tipo **já é** o modelo (carreta, caminhão) — [ClasseVeiculo.exigeModelo]. */
    val modelo: String? = null,
    val cor: String = "",
    /** Só moto. Número porque sobre ele se faz conta (a faixa de 100 cm³ da tarifa). */
    val cilindrada: Int? = null,
    val agenciaIds: List<String> = emptyList(),
)

/**
 * `DocumentoBruto` → domínio. **`null` quando não é um veículo**: sem **placa** não há chave natural, e sem
 * **classe** não se sabe o que ele exige nem o que ele custa — um veículo de classe desconhecida não vira
 * carro por omissão, como a Embarcação sem tipo não vira ferry.
 *
 * O que **não** recusa é a pendência: modelo faltando numa van, ou cilindrada faltando numa moto, é entrada
 * incompleta — e quem responde por isso é [Veiculo.pendencias], que diz **qual** campo falta para a tela
 * cobrar. Recusar aqui esconderia do operador um veículo que existe e que ele consegue completar.
 */
fun DocumentoBruto.toVeiculo(): Veiculo? {
    val placa = placaCanonica(texto("placa"))
    if (placa.isBlank()) return null
    val tipo = ClasseVeiculo.de(texto("tipo")) ?: return null

    return Veiculo(
        id = id,
        placa = placa,
        tipo = tipo,
        modelo = texto("modelo").takeIf { it.isNotBlank() },
        cor = texto("cor"),
        cilindrada = (dados["cilindrada"] as? Number)?.toInt()?.takeIf { it > 0 },
        agenciaIds = (dados["agenciaIds"] as? List<*>)?.filterIsInstance<String>()?.toSet().orEmpty(),
    )
}

/** Domínio → `Map`. O `id` não entra: ele é o nome do documento, que aqui é a própria placa. */
fun Veiculo.paraMapa(): Map<String, Any?> = mapOf(
    "placa" to placaCanonica(placa),
    "tipo" to tipo.name,
    "modelo" to modelo,
    "cor" to cor,
    "cilindrada" to cilindrada,
    "agenciaIds" to agenciaIds.toList(),
)

/**
 * A grafia canônica da placa: sem separador e em caixa alta.
 *
 * Existe porque a placa é **chave**, e chave que admite duas grafias não é chave: `abc-1d23` e `ABC1D23` são
 * o mesmo veículo, e sem esta normalização virariam dois documentos — reintroduzindo, por digitação, a
 * duplicata que este pool não deveria ter.
 */
fun placaCanonica(bruta: String?): String =
    bruta.orEmpty().filter { it.isLetterOrDigit() }.uppercase()