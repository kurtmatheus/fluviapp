package dev.matheus.fluviapp.telemetry

import dev.matheus.fluviapp.domain.operacoes.ContextoUsuario
import dev.matheus.fluviapp.services.repository.operacoes.SessaoUsuario
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * **De onde vem o evento** ([ADR-0032] D4) — observa quem está operando e empurra as coordenadas para a
 * [Telemetry].
 *
 * ### Por que empurra, em vez de a telemetria puxar
 *
 * Não é preferência de estilo, é o grafo: `Telemetry` é dependência de `RegistroCadastro`, que é de
 * `ColecaoFirestore`, que é do `FuncionarioRepository`, que é da `SessaoUsuario`. Uma telemetria que
 * injetasse a sessão fecharia o laço e o Dagger recusaria o grafo. Esta classe fica **fora** do ciclo
 * porque depende dos dois lados sem que nenhum deles dependa dela.
 *
 * ### Por que um escopo próprio
 *
 * O `@SyncScope` é **cancelado no logout** (é assim que os listeners param), e um observador cancelado ali
 * deixaria as coordenadas congeladas na sessão anterior — o pior desfecho possível para um dado cuja
 * função é dizer de onde o evento veio. Este escopo vive com o app.
 *
 * ### O que ela é instanciada para fazer
 *
 * Nada é injetado a partir dela; é o `FluviAppApplication` que a pede, e é essa injeção que a liga. Um
 * singleton que ninguém pede é um singleton que nunca nasce.
 */
@Singleton
class CoordenadasDaSessao @Inject constructor(
    sessaoUsuario: SessaoUsuario,
    telemetry: Telemetry,
) {
    private val escopo = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    init {
        escopo.launch {
            sessaoUsuario.observar().collect { contexto ->
                telemetry.definirCoordenadas(coordenadasDe(contexto))
            }
        }
    }
}

/**
 * As coordenadas de um contexto — **agregáveis, e nenhuma identidade**.
 *
 * O que entra responde perguntas de volume: *"quantas emissões por agência nesta semana"*, *"quantos
 * embarques por cargo"*. O que **não** entra é tudo que aponta para uma pessoa — `usuario.id`,
 * `funcionarioId`, e-mail, nome. Essa pergunta é do carimbo dentro do documento, onde a auditoria mora:
 * **o evento conta, o documento prova**.
 *
 * A agência entra por **id**, e não por nome: nome muda, repete e não relaciona (ADR-0008). Contexto nulo
 * devolve mapa vazio — sem sessão, um evento não tem de onde vir.
 */
fun coordenadasDe(contexto: ContextoUsuario?): Map<String, String> {
    if (contexto == null) return emptyMap()

    return buildMap {
        contexto.papel.takeIf { it.isNotBlank() }?.let { put(PARAM_PAPEL, it) }
        contexto.cargo?.takeIf { it.isNotBlank() }?.let { put(PARAM_CARGO, it) }
        contexto.vinculoAtivo?.empresaId?.takeIf { it.isNotBlank() }?.let { put(PARAM_AGENCIA, it) }
    }
}

const val PARAM_PAPEL = "papel"
const val PARAM_CARGO = "cargo"
const val PARAM_AGENCIA = "agencia_id"
