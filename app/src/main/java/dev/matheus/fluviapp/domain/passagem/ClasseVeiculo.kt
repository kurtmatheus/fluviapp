package dev.matheus.fluviapp.domain.passagem

/**
 * Classe do veículo embarcado como tipo de domínio ([ADR-0018] D7), no lugar da linha de catálogo
 * `Constante.Categoria.VEICULO`. Entra junto da F1 do [ADR-0020] porque sem ela o
 * [dev.matheus.fluviapp.domain.viagem.TipoEmbarcacao] não tem o que admitir — a regra dele é justamente
 * um conjunto destas.
 *
 * A classe governa **exigências e tarifa**: a moto é a única cuja tarifa depende de cilindrada (ADR-0013,
 * por faixa de 100 cm³), e o par caminhão/carreta é o que só a balsa carrega (ADR-0016 §8).
 *
 * ### O que a F9.1 acrescentou, e por quê ([ADR-0023] D4)
 *
 * **`VAN` e `SUV`** entraram porque o analista os nomeou como tipos com **modelo nomeado**, ao lado do carro.
 *
 * **`exigeModelo`** nasceu para dizer o que estava implícito: *"carreta ou caminhão já equivalentes ao modelo"*.
 * Nesses dois, perguntar o modelo é perguntar duas vezes a mesma coisa. Com a regra no tipo, a primeira
 * divergência do [ADR-0018] D19 se corrige **na origem** — o validador exigia modelo **sempre**, de modo que
 * carreta e caminhão não passavam.
 */
enum class ClasseVeiculo(
    val rotulo: String,
    /** A tarifa desta classe depende da cilindrada informada. Só a moto. */
    val exigeCilindrada: Boolean,
    /** Há um modelo a informar, ou o tipo **já é** o modelo? (ADR-0023 D4) */
    val exigeModelo: Boolean,
) {
    CARRO("Carro", exigeCilindrada = false, exigeModelo = true),
    MOTO("Moto", exigeCilindrada = true, exigeModelo = true),
    VAN("Van", exigeCilindrada = false, exigeModelo = true),
    SUV("SUV", exigeCilindrada = false, exigeModelo = true),
    CAMINHAO("Caminhão", exigeCilindrada = false, exigeModelo = false),
    CARRETA("Carreta", exigeCilindrada = false, exigeModelo = false);

    /** Carga pesada — o recorte que separa a balsa das demais embarcações (ADR-0016 §8). */
    val ehPesado: Boolean get() = this == CAMINHAO || this == CARRETA

    companion object {
        /** Fronteira String→enum; `null` se desconhecido (fail-closed). Tolerante à grafia legada. */
        fun de(valor: String?): ClasseVeiculo? {
            val normalizado = valor?.trim()?.uppercase()?.replace(" ", "_") ?: return null
            return entries.firstOrNull { it.name == normalizado }
        }
    }
}