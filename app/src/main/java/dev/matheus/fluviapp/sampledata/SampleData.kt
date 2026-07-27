package dev.matheus.fluviapp.sampledata

import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.extensions.formataParaMoedaBrasileira
import dev.matheus.fluviapp.model.cadastro.constantes.Constante
import dev.matheus.fluviapp.model.cadastro.constantes.Constante.Categoria.ACOMODACAO
import dev.matheus.fluviapp.model.cadastro.constantes.Constante.Categoria.DOCUMENTO
import dev.matheus.fluviapp.model.cadastro.constantes.Constante.Categoria.GRATUIDADE
import dev.matheus.fluviapp.model.cadastro.constantes.Constante.Categoria.MUNICIPIO
import dev.matheus.fluviapp.model.cadastro.constantes.Constante.Categoria.PAGAMENTO
import dev.matheus.fluviapp.model.cadastro.constantes.Constante.Categoria.STATUS_PASSAGEM
import dev.matheus.fluviapp.model.cadastro.constantes.Constante.Categoria.TIPO_PASSAGEM
import dev.matheus.fluviapp.model.cadastro.constantes.Constante.Categoria.VEICULO
import dev.matheus.fluviapp.model.cadastro.constantes.Constante.Descricao.CREDITO
import dev.matheus.fluviapp.model.cadastro.constantes.Constante.Descricao.DEBITO
import dev.matheus.fluviapp.model.cadastro.constantes.Constante.Descricao.DINHEIRO
import dev.matheus.fluviapp.model.cadastro.constantes.Constante.Descricao.PIX
import dev.matheus.fluviapp.model.operacoes.Funcionario
import dev.matheus.fluviapp.model.operacoes.Agencia.MATRIZ
import dev.matheus.fluviapp.model.operacoes.Funcionario.Lotacao.ILHA_CENTRAL
import dev.matheus.fluviapp.model.operacoes.Funcionario.Lotacao.PORTO_NORTE
import dev.matheus.fluviapp.model.operacoes.Funcionario.Lotacao.PORTO_SUL
import dev.matheus.fluviapp.model.operacoes.Usuario
import dev.matheus.fluviapp.model.screendata.DadosBalancoPassagem
import dev.matheus.fluviapp.model.screendata.DadosBotoesMenus
import dev.matheus.fluviapp.model.screendata.DadosImpressora
import dev.matheus.fluviapp.model.screendata.DadosPassagem
import dev.matheus.fluviapp.model.screendata.DadosViagemCard
import dev.matheus.fluviapp.model.viagem.Empresa
import dev.matheus.fluviapp.model.viagem.Navio
import java.math.BigDecimal

val listaDadosDadosViagemHomeSampleCards = listOf(
    DadosViagemCard(
        navio = "F/B Modelo",
        codigo = "PN-IC-001",
        origem = "Porto Norte",
        destino = "Ilha Central",
        capacidadeVeiculos = "50",
        capacidadeSuites = "12",
        capacidadeCamarotes = "6"
    ),
    DadosViagemCard(
        navio = "F/B Modelo",
        codigo = "PN-IC-001",
        origem = "Porto Norte",
        destino = "Ilha Central",
        capacidadeVeiculos = "50",
        capacidadeSuites = "12",
        capacidadeCamarotes = "6"
    ),
    DadosViagemCard(
        navio = "F/B Modelo",
        codigo = "PN-IC-001",
        origem = "Porto Norte",
        destino = "Ilha Central",
        capacidadeVeiculos = "50",
        capacidadeSuites = "12",
        capacidadeCamarotes = "6"
    ),
    DadosViagemCard(
        navio = "F/B Modelo",
        codigo = "PN-IC-001",
        origem = "Porto Norte",
        destino = "Ilha Central",
        capacidadeVeiculos = "60",
        capacidadeSuites = "9",
        capacidadeSuites2Pessoas = "8",
        capacidadeSuites3Pessoas = "1",
        capacidadeCamarotes = "4"
    )
)

val listaBotoesMenuPassagensSample = listOf(
    DadosBotoesMenus(
        title = R.string.btn_pesquisar_passagens,
        icon = R.drawable.ic_lupa_75
    ),
    DadosBotoesMenus(
        title = R.string.btn_balanco_vendas,
        icon = R.drawable.ic_relatorio_75
    )
)

val listaBotoesMenuViagensSample = listOf(
    DadosBotoesMenus(
        title = R.string.btn_nova_viagem,
        icon = R.drawable.ic_add_75
    ),
    DadosBotoesMenus(
        title = R.string.btn_pesquisar_viagens,
        icon = R.drawable.ic_lupa_75
    )
)

val listaBotoesMenuAgenteSample = listOf(
    DadosBotoesMenus(
        title = R.string.btn_novo_agente,
        icon = R.drawable.ic_add_75
    ),
    DadosBotoesMenus(
        title = R.string.btn_pesquisar_agente,
        icon = R.drawable.ic_lupa_75
    )
)

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
val listaNavioSample = listOf(
    Navio(
        "1",
        "F/B Modelo",
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
val listaFuncionarioSample = listOf(
    Funcionario("1", "Ana Ribeiro", MATRIZ.name, PORTO_NORTE.name),
    // O seed também precisa de um master de agência: sem SUPERVISOR nenhum, o eixo de negócio da
    // política (ADR-0015 §8.2) nunca aparece em execução — só nos testes.
    Funcionario("2", "Bruno Costa", MATRIZ.name, PORTO_NORTE.name, Funcionario.Cargo.SUPERVISOR.name),
    Funcionario("3", "Carla Dias", "AGENCIA HORIZONTE", ILHA_CENTRAL.name),
    Funcionario("4", "Daniel Alves", "AGENCIA MARE", ILHA_CENTRAL.name),
    Funcionario("5", "Elena Faria", "AGENCIA AURORA", ILHA_CENTRAL.name),
    Funcionario("6", "Fabio Gomes", "AGENCIA HORIZONTE", ILHA_CENTRAL.name),
    Funcionario("7", "Gabriela Lima", "AGENCIA MARE", ILHA_CENTRAL.name),
    Funcionario("8", "Hugo Melo", "AGENCIA AURORA", ILHA_CENTRAL.name),
    Funcionario("9", "Igor Nunes", "AGENCIA HORIZONTE", ILHA_CENTRAL.name),
    Funcionario("10", "Julia Pires", "AGENCIA MARE", ILHA_CENTRAL.name),
    Funcionario("11", "Karla Rocha", "AGENCIA AURORA", ILHA_CENTRAL.name),
    Funcionario("12", "Lucas Souza", "AGENCIA HORIZONTE", ILHA_CENTRAL.name),
    Funcionario("13", "Marina Teles", "AGENCIA MARE", ILHA_CENTRAL.name),
    Funcionario("14", "Nadia Vaz", "AGENCIA LITORAL", PORTO_NORTE.name),
    Funcionario("15", "Otavio Reis", "AGENCIA LITORAL", PORTO_NORTE.name),
    Funcionario("16", "Paula Matos", "AGENCIA LITORAL", PORTO_NORTE.name),
    Funcionario("17", "Rafael Braga", "AGENCIA LITORAL", PORTO_NORTE.name),
    Funcionario("18", "Sofia Cunha", "AGENCIA LITORAL", PORTO_NORTE.name),
    Funcionario("19", "Tiago Moraes", "AGENCIA LITORAL", PORTO_NORTE.name),
    Funcionario("20", "Ursula Pinto", "AGENCIA NORTE", PORTO_NORTE.name),
    Funcionario("21", "Vitor Campos", "AGENCIA NORTE", PORTO_NORTE.name),
    Funcionario("22", "Wesley Aragao", "SEM AGENCIA", PORTO_NORTE.name),
    Funcionario("23", "Xavier Luz", "SEM AGENCIA", PORTO_NORTE.name),
    Funcionario("24", "Yasmin Freitas", "AGENCIA AURORA", ILHA_CENTRAL.name),
    Funcionario("25", "Ze Carlos", "AGENCIA AURORA", ILHA_CENTRAL.name),
    Funcionario("26", "Alice Barros", "AGENCIA AURORA", ILHA_CENTRAL.name),
    Funcionario("27", "Bernardo Sa", "AGENCIA SUL", PORTO_SUL.name),
    Funcionario("28", "Cecilia Mota", "AGENCIA MARE", ILHA_CENTRAL.name),
    Funcionario("29", "Diego Farias", "AGENCIA HORIZONTE", ILHA_CENTRAL.name),
    Funcionario("30", "Elisa Prado", "AGENCIA AURORA", ILHA_CENTRAL.name),
    Funcionario("31", "Felipe Aragao", "AGENCIA AURORA", ILHA_CENTRAL.name),
    Funcionario("32", "Gisele Nery", "AGENCIA AURORA", ILHA_CENTRAL.name),
    Funcionario("33", "Heitor Vasques", MATRIZ.name, PORTO_NORTE.name),
    Funcionario("999", "Sem Agente", "SEM AGENCIA", PORTO_NORTE.name)
)

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

val dadosPassagemSample = DadosPassagem(
    numero = "2444",
    empresaNome = "NAVEGACAO MODELO",
    navio = "F/B Modelo",
    dataViagem = "31/12/2023",
    horaViagem = "12:00",
    origem = "PORTO NORTE",
    destino = "ILHA CENTRAL",
    agencia = "Matriz",
    agente = "Agente Modelo",
    valorAPagar = BigDecimal("1000").formataParaMoedaBrasileira(),
    observacao = "TESTE DE OBSERVACAO",
    tipoPassagem = "INTEIRA",
    situacao = "PENDENTE",
    funcionario = "ADMINISTRADOR",
    nomePassageiro1 = "JOAO DA SILVA",
    documentoPassageiro1 = "000.000.000-00",
    dataNascimento1 = "30/01/1996",
    nomePassageiro2 = "MARIA OLIVEIRA",
    documentoPassageiro2 = "000.000.000-00",
    dataNascimento2 = "10/01/1975",
    acomodacao = "SUITE"
)

val dadosPassagemVeiculoSample = DadosPassagem(
    numero = "2444",
    empresaNome = "NAVEGACAO MODELO",
    navio = "F/B Modelo",
    dataViagem = "31/12/2023",
    horaViagem = "12:00",
    origem = "Porto Norte",
    destino = "Ilha Central",
    agencia = "Matriz",
    agente = "Agente Modelo",
    valorTotal = BigDecimal("180").formataParaMoedaBrasileira(),
    valorPix = BigDecimal("100").formataParaMoedaBrasileira(),
    valorCredito = BigDecimal("50").formataParaMoedaBrasileira(),
    desconto = BigDecimal("30").formataParaMoedaBrasileira(),
    valorAPagar = BigDecimal("150").formataParaMoedaBrasileira(),
    observacao = "TESTE DE OBSERVACAO",
    tipoPassagem = "INTEIRA",
    situacao = "PENDENTE",
    funcionario = "ADMINISTRADOR",
    nomeResponsavelRetirada = "JOAO DA SILVA",
    numeroDocumentoResponsavelRetirada = "000.000.000-00",
    idVeiculo = "2",
    tipoVeiculo = "MOTO",
    modeloVeiculo = "MOTO 150CC MODELO",
    placaVeiculo = "ABC1D23",
    corVeiculo = "VERMELHO"
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

/** O caso comum: OPERADOR no sistema, ligado 1-1 ao funcionário que carrega cargo/agência/lotação. */
val userAgenteSample = Usuario(
    id = "3",
    email = "agente@fluviapp.com.br",
    username = "agente",
    papel = "OPERADOR",
    funcionarioId = "1",
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
    userAgenteSample
)

val listaDadosBalancoPassagems = listOf(
    DadosBalancoPassagem(
        navio = "F/B Modelo"
    ),
    DadosBalancoPassagem(
        navio = "F/B Modelo II"
    )
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
        cnpj = "00.000.000/0001-00",
        endereco = "Av. Central, nº 100 - Centro - Porto Sul",
        telefone1 = "(00) 90000-0001",
        telefone2 = "(00) 90000-0002"
    )
)