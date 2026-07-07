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
import dev.matheus.fluviapp.model.cadastro.passagem.Agente
import dev.matheus.fluviapp.model.cadastro.passagem.Agente.Agencia.MATRIZ
import dev.matheus.fluviapp.model.cadastro.passagem.Agente.Lotacao.ILHA_CENTRAL
import dev.matheus.fluviapp.model.cadastro.passagem.Agente.Lotacao.PORTO_NORTE
import dev.matheus.fluviapp.model.cadastro.passagem.Agente.Lotacao.PORTO_SUL
import dev.matheus.fluviapp.model.operacoes.Usuario
import dev.matheus.fluviapp.model.screendata.DadosBalancoPassagem
import dev.matheus.fluviapp.model.screendata.DadosBotoesMenus
import dev.matheus.fluviapp.model.screendata.DadosImpressora
import dev.matheus.fluviapp.model.screendata.DadosPassagem
import dev.matheus.fluviapp.model.screendata.DadosViagemCard
import dev.matheus.fluviapp.model.screendata.MenuBotoesCategoria
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

val listaMenuBotoesCategoriaSample = listOf(
    MenuBotoesCategoria(
        tituloCategoria = R.string.label_menu_viagens,
        iconCategoria = R.drawable.ic_navio_75,
        dadosBotoesMenus = listaBotoesMenuViagensSample
    ),
    MenuBotoesCategoria(
        tituloCategoria = R.string.label_menu_agentes,
        iconCategoria = R.drawable.ic_user_75,
        dadosBotoesMenus = listaBotoesMenuAgenteSample
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
        "NAVEGACAO MODELO"
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
val listaAgenteSample = listOf(
    Agente("1", "Ana Ribeiro", MATRIZ.name, PORTO_NORTE.name),
    Agente("2", "Bruno Costa", MATRIZ.name, PORTO_NORTE.name),
    Agente("3", "Carla Dias", "AGENCIA HORIZONTE", ILHA_CENTRAL.name),
    Agente("4", "Daniel Alves", "AGENCIA MARE", ILHA_CENTRAL.name),
    Agente("5", "Elena Faria", "AGENCIA AURORA", ILHA_CENTRAL.name),
    Agente("6", "Fabio Gomes", "AGENCIA HORIZONTE", ILHA_CENTRAL.name),
    Agente("7", "Gabriela Lima", "AGENCIA MARE", ILHA_CENTRAL.name),
    Agente("8", "Hugo Melo", "AGENCIA AURORA", ILHA_CENTRAL.name),
    Agente("9", "Igor Nunes", "AGENCIA HORIZONTE", ILHA_CENTRAL.name),
    Agente("10", "Julia Pires", "AGENCIA MARE", ILHA_CENTRAL.name),
    Agente("11", "Karla Rocha", "AGENCIA AURORA", ILHA_CENTRAL.name),
    Agente("12", "Lucas Souza", "AGENCIA HORIZONTE", ILHA_CENTRAL.name),
    Agente("13", "Marina Teles", "AGENCIA MARE", ILHA_CENTRAL.name),
    Agente("14", "Nadia Vaz", "AGENCIA LITORAL", PORTO_NORTE.name),
    Agente("15", "Otavio Reis", "AGENCIA LITORAL", PORTO_NORTE.name),
    Agente("16", "Paula Matos", "AGENCIA LITORAL", PORTO_NORTE.name),
    Agente("17", "Rafael Braga", "AGENCIA LITORAL", PORTO_NORTE.name),
    Agente("18", "Sofia Cunha", "AGENCIA LITORAL", PORTO_NORTE.name),
    Agente("19", "Tiago Moraes", "AGENCIA LITORAL", PORTO_NORTE.name),
    Agente("20", "Ursula Pinto", "AGENCIA NORTE", PORTO_NORTE.name),
    Agente("21", "Vitor Campos", "AGENCIA NORTE", PORTO_NORTE.name),
    Agente("22", "Wesley Aragao", "SEM AGENCIA", PORTO_NORTE.name),
    Agente("23", "Xavier Luz", "SEM AGENCIA", PORTO_NORTE.name),
    Agente("24", "Yasmin Freitas", "AGENCIA AURORA", ILHA_CENTRAL.name),
    Agente("25", "Ze Carlos", "AGENCIA AURORA", ILHA_CENTRAL.name),
    Agente("26", "Alice Barros", "AGENCIA AURORA", ILHA_CENTRAL.name),
    Agente("27", "Bernardo Sa", "AGENCIA SUL", PORTO_SUL.name),
    Agente("28", "Cecilia Mota", "AGENCIA MARE", ILHA_CENTRAL.name),
    Agente("29", "Diego Farias", "AGENCIA HORIZONTE", ILHA_CENTRAL.name),
    Agente("30", "Elisa Prado", "AGENCIA AURORA", ILHA_CENTRAL.name),
    Agente("31", "Felipe Aragao", "AGENCIA AURORA", ILHA_CENTRAL.name),
    Agente("32", "Gisele Nery", "AGENCIA AURORA", ILHA_CENTRAL.name),
    Agente("33", "Heitor Vasques", MATRIZ.name, PORTO_NORTE.name),
    Agente("999", "Sem Agente", "SEM AGENCIA", PORTO_NORTE.name)
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
    Constante("3", "CAMINHAO", VEICULO.name)
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
    podeSelecionarFormaPagamento = true,
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
    podeSelecionarFormaPagamento = true,
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

val userAdminSample = Usuario(
    id = "1",
    email = "admin@fluviapp.com.br",
    senha = "admin",
    nome = "Administrador",
    cargo = "ADM"
)

val userGerenteSample = Usuario(
    id = "2",
    email = "gerente@fluviapp.com.br",
    senha = "gerente",
    nome = "Gerente",
    cargo = "DIRETOR"
)

val userColabSample = Usuario(
    id = "3",
    email = "operador@fluviapp.com.br",
    senha = "operador",
    nome = "Operador",
    cargo = "Diretor"
)

val listaUserSample = listOf(
    userAdminSample,
    userGerenteSample,
    userColabSample
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