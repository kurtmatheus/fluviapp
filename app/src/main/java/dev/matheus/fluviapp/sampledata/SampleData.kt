package dev.matheus.fluviapp.sampledata

import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.extensions.formataParaMoedaBrasileira
import dev.matheus.fluviapp.domain.cadastro.constantes.Constante
import dev.matheus.fluviapp.domain.cadastro.constantes.Constante.Categoria.ACOMODACAO
import dev.matheus.fluviapp.domain.cadastro.constantes.Constante.Categoria.DOCUMENTO
import dev.matheus.fluviapp.domain.cadastro.constantes.Constante.Categoria.GRATUIDADE
import dev.matheus.fluviapp.domain.cadastro.constantes.Constante.Categoria.MUNICIPIO
import dev.matheus.fluviapp.domain.cadastro.constantes.Constante.Categoria.PAGAMENTO
import dev.matheus.fluviapp.domain.cadastro.constantes.Constante.Categoria.STATUS_PASSAGEM
import dev.matheus.fluviapp.domain.cadastro.constantes.Constante.Categoria.TIPO_PASSAGEM
import dev.matheus.fluviapp.domain.cadastro.constantes.Constante.Categoria.VEICULO
import dev.matheus.fluviapp.domain.cadastro.constantes.Constante.Descricao.CREDITO
import dev.matheus.fluviapp.domain.cadastro.constantes.Constante.Descricao.DEBITO
import dev.matheus.fluviapp.domain.cadastro.constantes.Constante.Descricao.DINHEIRO
import dev.matheus.fluviapp.domain.cadastro.constantes.Constante.Descricao.PIX
import dev.matheus.fluviapp.domain.operacoes.Funcionario
import dev.matheus.fluviapp.domain.operacoes.Usuario
import dev.matheus.fluviapp.domain.screendata.DadosImpressora
import dev.matheus.fluviapp.domain.viagem.Empresa
import dev.matheus.fluviapp.domain.viagem.Embarcacao
import dev.matheus.fluviapp.domain.viagem.TipoEmbarcacao
import java.math.BigDecimal


val listaTipoDocumentosSample =
    listOf(
        Constante("1", "RG", DOCUMENTO.name),
        Constante("2", "CPF", DOCUMENTO.name),
        Constante("3", "CNH", DOCUMENTO.name),
        Constante("4", "CNPJ", DOCUMENTO.name),
        Constante("5", "DNV", DOCUMENTO.name),
        Constante("6", "PASSAPORTE", DOCUMENTO.name),
        Constante("999", "SEM DOCUMENTO", DOCUMENTO.name)
    )
val listaEmbarcacaoSample = listOf(
    Embarcacao(
        "1",
        "F/B Modelo",
        TipoEmbarcacao.FERRY_BOAT,
        60,
        4,
        5,
        4,
        empresaId = "1", // Empresa "1" (NAVEGACAO MODELO) — vínculo por id (ADR-0008)
    )
)
val listaMunicipioSample = listOf(
    Constante("1", "Porto Norte", MUNICIPIO.name),
    Constante("2", "Ilha Central", MUNICIPIO.name),
    Constante("3", "Porto Sul", MUNICIPIO.name)
)
val listaAcomodacaoSample =
    listOf(
        Constante("1", "Rede", ACOMODACAO.name),
        Constante("2", "Suíte p/ 2 Pessoas", ACOMODACAO.name),
        Constante("3", "Suíte p/ 3 Pessoas", ACOMODACAO.name),
        Constante("4", "Camarote", ACOMODACAO.name)
    )
// `listaFuncionarioSample` saiu na F6.3: a Equipe passou a exibir **vínculos** (empresa + cargo), e uma
// amostra de 34 pessoas com agência e lotação como String era o retrato de um modelo que não existe mais.
// Descarte progressivo — as prévias das telas montam o pouco de que precisam, ali mesmo.

val listaFormaPagamentoSample = listOf(
    Constante("1", PIX.name, PAGAMENTO.name),
    Constante("2", DINHEIRO.name, PAGAMENTO.name),
    Constante("3", CREDITO.name, PAGAMENTO.name),
    Constante("4", DEBITO.name, PAGAMENTO.name)
)
val listaStatusPassagemSample = listOf(
    Constante("1", "A EMITIR", STATUS_PASSAGEM.name),
    Constante("2", "EMITIDA", STATUS_PASSAGEM.name),
    Constante("3", "EM TRANSITO", STATUS_PASSAGEM.name),
    Constante("4", "FINALIZADA", STATUS_PASSAGEM.name),
    Constante("5", "EM ANÁLISE", STATUS_PASSAGEM.name)
)
val listaTipoPassagemSample = listOf(
    Constante("1", "INTEIRA", TIPO_PASSAGEM.name),
    Constante("2", "MEIA", TIPO_PASSAGEM.name),
    Constante("3", "GRATUIDADE", TIPO_PASSAGEM.name),
)

val listaTipoGratuidadeSample = listOf(
    Constante("1", "CRIANCA MENOR QUE 6 ANOS", GRATUIDADE.name),
    Constante("2", "IDOSO", GRATUIDADE.name),
    Constante("3", "PROFISSIONAL EM MOVIMENTO", GRATUIDADE.name),
    Constante("4", "CORTESIA", GRATUIDADE.name),
    Constante("5", "PcD", GRATUIDADE.name),
    Constante("6", "ACOMPANHANTE - PeM", GRATUIDADE.name),
    Constante("7", "ACOMPANHANTE - PcD", GRATUIDADE.name),
    Constante("999", "SEM GRATUIDADE", GRATUIDADE.name)
)

val listaTipoVeiculoSample = listOf(
    Constante("1", "CARRO", VEICULO.name),
    Constante("2", "MOTO", VEICULO.name),
    Constante("3", "CAMINHAO", VEICULO.name),
    Constante("4", "CARRETA", VEICULO.name)
)

// Papel puro de plataforma: sem funcionarioId, porque ADM/GESTOR não têm registro na operação
// (ADR-0015 §8.1) — e, por consequência, não emitem passagem (§8.4).
val userAdminSample = Usuario(
    id = "1",
    email = "admin@fluviapp.com.br",
    username = "administrador",
    papel = "ADM"
)

val userGestorSample = Usuario(
    id = "2",
    email = "gestor@fluviapp.com.br",
    username = "gestor",
    papel = "GESTOR"
)

/**
 * O caso comum: OPERADOR no sistema, ligado 1-1 ao funcionário que carrega cargo/agência/lotação. O
 * e-mail é o MESMO do funcionário "1" (Ana Ribeiro) — é ele que casa as duas frentes no primeiro acesso
 * (ADR-0015 §2.1) antes de o `funcionarioId` assumir o vínculo.
 */
val userOperadorSample = Usuario(
    id = "3",
    email = "ana.ribeiro@fluviapp.com.br",
    username = "ana.ribeiro",
    papel = "OPERADOR",
    funcionarioId = "1",
)

/** O master da agência: papel de sistema é OPERADOR; o que manda é o cargo do funcionário "2". */
val userSupervisorSample = Usuario(
    id = "5",
    email = "bruno.costa@fluviapp.com.br",
    username = "bruno.costa",
    papel = "OPERADOR",
    funcionarioId = "2",
)

/** Provisionado e ainda **sem vínculo**: entra no app, mas não emite até a gestão ligar um funcionário. */
val userAutonomoSample = Usuario(
    id = "4",
    email = "autonomo@fluviapp.com.br",
    username = "sem.vinculo",
    papel = "OPERADOR",
)

val listaUserSample = listOf(
    userAdminSample,
    userGestorSample,
    userOperadorSample
)

val listaDadosImpressoraSample = listOf(
    DadosImpressora(
        nome = "IMPRESSORA MODELO",
        endereco = "00:11:22:33:44:55"
    ),
    DadosImpressora(
        nome = "IMPRESSORA MODELO",
        endereco = "00:11:22:33:44:55"
    ),
    DadosImpressora(
        nome = "IMPRESSORA MODELO",
        endereco = "00:11:22:33:44:55"
    )
)

val listaEmpresaSample = listOf(
    Empresa(
        id = "1",
        nome = "NAVEGACAO MODELO",
        razaoSocial = "MODELO NAVEGACAO FLUVIAL LTDA",
        cnpj = "11.222.333/0001-81",
        endereco = "Av. Central, nº 100 - Centro - Porto Sul",
        telefone1 = "(00) 90000-0001",
        telefone2 = "(00) 90000-0002"
    )
)