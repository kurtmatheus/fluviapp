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
import dev.matheus.fluviapp.domain.operacoes.Agencia.MATRIZ
import dev.matheus.fluviapp.domain.operacoes.Funcionario.Lotacao.ILHA_CENTRAL
import dev.matheus.fluviapp.domain.operacoes.Funcionario.Lotacao.PORTO_NORTE
import dev.matheus.fluviapp.domain.operacoes.Funcionario.Lotacao.PORTO_SUL
import dev.matheus.fluviapp.domain.operacoes.Usuario
import dev.matheus.fluviapp.domain.screendata.DadosContagemPassagem
import dev.matheus.fluviapp.domain.screendata.DadosImpressora
import dev.matheus.fluviapp.domain.screendata.DadosPassagem
import dev.matheus.fluviapp.domain.screendata.DadosViagemCard
import dev.matheus.fluviapp.domain.viagem.Empresa
import dev.matheus.fluviapp.domain.viagem.Navio
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
    Funcionario("1", "Ana Ribeiro", MATRIZ.name, PORTO_NORTE.name, email = "ana.ribeiro@fluviapp.com.br"),
    // O seed também precisa de um master de agência: sem SUPERVISOR nenhum, o eixo de negócio da
    // política (ADR-0015 §8.2) nunca aparece em execução — só nos testes.
    Funcionario("2", "Bruno Costa", MATRIZ.name, PORTO_NORTE.name, Funcionario.Cargo.SUPERVISOR.name, email = "bruno.costa@fluviapp.com.br"),
    Funcionario("3", "Carla Dias", "AGENCIA HORIZONTE", ILHA_CENTRAL.name, email = "carla.dias@fluviapp.com.br"),
    Funcionario("4", "Daniel Alves", "AGENCIA MARE", ILHA_CENTRAL.name, email = "daniel.alves@fluviapp.com.br"),
    Funcionario("5", "Elena Faria", "AGENCIA AURORA", ILHA_CENTRAL.name, email = "elena.faria@fluviapp.com.br"),
    Funcionario("6", "Fabio Gomes", "AGENCIA HORIZONTE", ILHA_CENTRAL.name, email = "fabio.gomes@fluviapp.com.br"),
    Funcionario("7", "Gabriela Lima", "AGENCIA MARE", ILHA_CENTRAL.name, email = "gabriela.lima@fluviapp.com.br"),
    Funcionario("8", "Hugo Melo", "AGENCIA AURORA", ILHA_CENTRAL.name, email = "hugo.melo@fluviapp.com.br"),
    Funcionario("9", "Igor Nunes", "AGENCIA HORIZONTE", ILHA_CENTRAL.name, email = "igor.nunes@fluviapp.com.br"),
    Funcionario("10", "Julia Pires", "AGENCIA MARE", ILHA_CENTRAL.name, email = "julia.pires@fluviapp.com.br"),
    Funcionario("11", "Karla Rocha", "AGENCIA AURORA", ILHA_CENTRAL.name, email = "karla.rocha@fluviapp.com.br"),
    Funcionario("12", "Lucas Souza", "AGENCIA HORIZONTE", ILHA_CENTRAL.name, email = "lucas.souza@fluviapp.com.br"),
    Funcionario("13", "Marina Teles", "AGENCIA MARE", ILHA_CENTRAL.name, email = "marina.teles@fluviapp.com.br"),
    Funcionario("14", "Nadia Vaz", "AGENCIA LITORAL", PORTO_NORTE.name, email = "nadia.vaz@fluviapp.com.br"),
    Funcionario("15", "Otavio Reis", "AGENCIA LITORAL", PORTO_NORTE.name, email = "otavio.reis@fluviapp.com.br"),
    Funcionario("16", "Paula Matos", "AGENCIA LITORAL", PORTO_NORTE.name, email = "paula.matos@fluviapp.com.br"),
    Funcionario("17", "Rafael Braga", "AGENCIA LITORAL", PORTO_NORTE.name, email = "rafael.braga@fluviapp.com.br"),
    Funcionario("18", "Sofia Cunha", "AGENCIA LITORAL", PORTO_NORTE.name, email = "sofia.cunha@fluviapp.com.br"),
    Funcionario("19", "Tiago Moraes", "AGENCIA LITORAL", PORTO_NORTE.name, email = "tiago.moraes@fluviapp.com.br"),
    Funcionario("20", "Ursula Pinto", "AGENCIA NORTE", PORTO_NORTE.name, email = "ursula.pinto@fluviapp.com.br"),
    Funcionario("21", "Vitor Campos", "AGENCIA NORTE", PORTO_NORTE.name, email = "vitor.campos@fluviapp.com.br"),
    Funcionario("22", "Wesley Aragao", "SEM AGENCIA", PORTO_NORTE.name, email = "wesley.aragao@fluviapp.com.br"),
    Funcionario("23", "Xavier Luz", "SEM AGENCIA", PORTO_NORTE.name, email = "xavier.luz@fluviapp.com.br"),
    Funcionario("24", "Yasmin Freitas", "AGENCIA AURORA", ILHA_CENTRAL.name, email = "yasmin.freitas@fluviapp.com.br"),
    Funcionario("25", "Ze Carlos", "AGENCIA AURORA", ILHA_CENTRAL.name, email = "ze.carlos@fluviapp.com.br"),
    Funcionario("26", "Alice Barros", "AGENCIA AURORA", ILHA_CENTRAL.name, email = "alice.barros@fluviapp.com.br"),
    Funcionario("27", "Bernardo Sa", "AGENCIA SUL", PORTO_SUL.name, email = "bernardo.sa@fluviapp.com.br"),
    Funcionario("28", "Cecilia Mota", "AGENCIA MARE", ILHA_CENTRAL.name, email = "cecilia.mota@fluviapp.com.br"),
    Funcionario("29", "Diego Farias", "AGENCIA HORIZONTE", ILHA_CENTRAL.name, email = "diego.farias@fluviapp.com.br"),
    Funcionario("30", "Elisa Prado", "AGENCIA AURORA", ILHA_CENTRAL.name, email = "elisa.prado@fluviapp.com.br"),
    Funcionario("31", "Felipe Aragao", "AGENCIA AURORA", ILHA_CENTRAL.name, email = "felipe.aragao@fluviapp.com.br"),
    Funcionario("32", "Gisele Nery", "AGENCIA AURORA", ILHA_CENTRAL.name, email = "gisele.nery@fluviapp.com.br"),
    Funcionario("33", "Heitor Vasques", MATRIZ.name, PORTO_NORTE.name, email = "heitor.vasques@fluviapp.com.br"),
    Funcionario("999", "Sem Agente", "SEM AGENCIA", PORTO_NORTE.name, email = "sem.agente@fluviapp.com.br"),
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
    // Sem idPassagem o bilhete não desenha o QR de embarque (é o ponteiro do ADR-0012) — e o preview
    // ficaria mostrando um bilhete que não embarca.
    idPassagem = "passagem-modelo-1",
    numero = "2444",
    empresaNome = "NAVEGACAO MODELO",
    navio = "F/B Modelo",
    dataViagem = "31/12/2023",
    horaViagem = "12:00",
    origem = "PORTO NORTE",
    destino = "ILHA CENTRAL",
    agencia = MATRIZ.name,
    valorAPagar = BigDecimal("1000").formataParaMoedaBrasileira(),
    observacao = "TESTE DE OBSERVACAO",
    tipoPassagem = "INTEIRA",
    situacao = "PENDENTE",
    funcionario = "ADMINISTRADOR",
    nomePassageiro1 = "JOAO DA SILVA",
    // CPFs de mock com dígito verificador válido (ADR-0020 D2): desde que a validação entrou, o
    // "000.000.000-00" de antes seria recusado no form — mock inválido vira demo quebrada.
    documentoPassageiro1 = "529.982.247-25",
    dataNascimento1 = "30/01/1996",
    nomePassageiro2 = "MARIA OLIVEIRA",
    documentoPassageiro2 = "123.456.789-09",
    dataNascimento2 = "10/01/1975",
    acomodacao = "SUITE"
)

val dadosPassagemVeiculoSample = DadosPassagem(
    idPassagem = "passagem-modelo-2",
    numero = "2444",
    empresaNome = "NAVEGACAO MODELO",
    navio = "F/B Modelo",
    dataViagem = "31/12/2023",
    horaViagem = "12:00",
    origem = "Porto Norte",
    destino = "Ilha Central",
    agencia = "Matriz",
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
    numeroDocumentoResponsavelRetirada = "111.444.777-35",
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

val listaDadosContagemPassagems = listOf(
    DadosContagemPassagem(
        navio = "F/B Modelo"
    ),
    DadosContagemPassagem(
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
        cnpj = "11.222.333/0001-81",
        endereco = "Av. Central, nº 100 - Centro - Porto Sul",
        telefone1 = "(00) 90000-0001",
        telefone2 = "(00) 90000-0002"
    )
)