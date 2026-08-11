package dev.matheus.fluviapp.domain.passagem

/**
 * O que o **sistema anota** sobre a passagem ([ADR-0023] D7) — e só o [status] aparece em tela.
 *
 * A distinção é de projeto, não de arrumação: o `status` tem semântica de negócio (é a FSM do ADR-0012, e é por
 * isso que ele é tipo), enquanto o resto existe para **auditar**. Um metadado que fosse à tela viraria campo, e
 * campo pede edição.
 *
 * ### Os dois ids são inferidos, nunca digitados
 *
 * [funcionarioId] e [agenciaId] saem do **vínculo ativo** de quem emite (ADR-0015 P2.3, ADR-0016 §6). Um campo de
 * agência no formulário seria a chance permanente de ele discordar de quem está logado — foi o que aconteceu
 * enquanto a agência era texto.
 *
 * ### Nenhum nome aqui
 *
 * Onde havia `funcionarioResponsavel`, `agencia` e `embarcadaPor` (nomes congelados ao lado dos ids), ficou só o
 * id: **no domínio nada é congelado** (ADR-0023 D8). O nome se resolve por referência na leitura.
 *
 * Instantes em **ISO-8601** ([ADR-0024] D2) — é o que ordena e o que permite faixa por período. É também a
 * correção de um defeito real: o carimbo de embarque gravava `dd/MM/yyyy HH:mm`, formato que **não ordena**, e
 * por isso *"quem embarcou entre tal e tal hora"* era pergunta sem resposta possível.
 */
data class MetadadosPassagem(
    val status: StatusPassagem,
    val funcionarioId: String,
    val agenciaId: String,
    /** ISO-8601 (`2026-08-18T14:32:00`). Como a emissão é pós-pagamento, é **quando o dinheiro entrou**. */
    val criadoEm: String,
    val alteradoEm: String,
    /** Ausente até embarcar — ver [CarimboEmbarque]. */
    val embarque: CarimboEmbarque? = null,
)

/**
 * O carimbo do embarque: **ou existe inteiro, ou não existe** ([ADR-0018] D14).
 *
 * Antes eram três campos planos com default `""`, e isso deixava representável o estado meio-preenchido — status
 * `EMBARCADA` sem autoria, ou autoria sem instante. Como sub-objeto ausente/presente, esse estado **não se
 * escreve**.
 *
 * O que carrega é o **uid** de quem validou o QR, e não um nome: é o uid que a regra do servidor confere contra
 * `request.auth.uid` (ADR-0012), e é o que torna forjar autoria impossível. O nome exibido se resolve por
 * referência (ADR-0023 D8).
 */
data class CarimboEmbarque(
    val porId: String,
    /** ISO-8601. */
    val em: String,
)