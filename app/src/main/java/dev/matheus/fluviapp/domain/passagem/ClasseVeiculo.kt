package dev.matheus.fluviapp.domain.passagem

/**
 * Classe do veículo embarcado como tipo de domínio ([ADR-0018] D7), no lugar da linha de catálogo
 * `Constante.Categoria.VEICULO`. Entra junto da F1 do [ADR-0020] porque sem ela o
 * [dev.matheus.fluviapp.domain.viagem.TipoEmbarcacao] não tem o que admitir — a regra dele é justamente
 * um conjunto destas.
 *
 * A classe governa **exigências e tarifa**: a moto é a única cuja tarifa depende de cilindrada (ADR-0013,
 * por faixa de 100 cm³), e o par caminhão/carreta é o que só a balsa carrega (ADR-0016 §8).
 */
enum class ClasseVeiculo(
    val rotulo: String,
    /** A tarifa desta classe depende da cilindrada informada. Só a moto. */
    val exigeCilindrada: Boolean,
) {
    CARRO("Carro", exigeCilindrada = false),
    MOTO("Moto", exigeCilindrada = true),
    CAMINHAO("Caminhão", exigeCilindrada = false),
    CARRETA("Carreta", exigeCilindrada = false);

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