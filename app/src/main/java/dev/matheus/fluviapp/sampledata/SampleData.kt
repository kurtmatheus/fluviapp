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
import dev.matheus.fluviapp.model.cadastro.passagem.Agente.Agencia.NAVEG
import dev.matheus.fluviapp.model.cadastro.passagem.Agente.Lotacao.BELEM
import dev.matheus.fluviapp.model.cadastro.passagem.Agente.Lotacao.BREVES
import dev.matheus.fluviapp.model.cadastro.passagem.Agente.Lotacao.SANTANA
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
        navio = "F/B Regional",
        codigo = "BEL-SAN-001",
        origem = "Belém",
        destino = "Santana",
        capacidadeVeiculos = "50",
        capacidadeSuites = "12",
        capacidadeCamarotes = "6"
    ),
    DadosViagemCard(
        navio = "F/B Regional",
        codigo = "BEL-SAN-001",
        origem = "Belém",
        destino = "Santana",
        capacidadeVeiculos = "50",
        capacidadeSuites = "12",
        capacidadeCamarotes = "6"
    ),
    DadosViagemCard(
        navio = "F/B Regional",
        codigo = "BEL-SAN-001",
        origem = "Belém",
        destino = "Santana",
        capacidadeVeiculos = "50",
        capacidadeSuites = "12",
        capacidadeCamarotes = "6"
    ),
    DadosViagemCard(
        navio = "F/B Regional",
        codigo = "BEL-SAN-001",
        origem = "Belém",
        destino = "Santana",
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
        "F/B Regional",
        60,
        4,
        5,
        4,
        "NAVEGACAO MOCORONGO"
    )
)
val listaMunicipioSample = listOf(
    Constante("1", "Belém", MUNICIPIO.name),
    Constante("2", "Santana", MUNICIPIO.name),
    Constante("3", "Breves", MUNICIPIO.name)
)
val listaAcomodacaoSample =
    listOf(
        Constante("1", "Rede", ACOMODACAO.name),
        Constante("2", "Suíte p/ 2 Pessoas", ACOMODACAO.name),
        Constante("3", "Suíte p/ 3 Pessoas", ACOMODACAO.name),
        Constante("4", "Camarote", ACOMODACAO.name)
    )
val listaAgenteSample = listOf(
    Agente("1", "Odair", NAVEG.name, BELEM.name),
    Agente("2", "Rodoviária", NAVEG.name, BELEM.name),
    Agente("3", "Dell", "DELLTUR", SANTANA.name),
    Agente("4", "Jamaira", "MAYRATUR", SANTANA.name),
    Agente("5", "Nayara", "NAY TURISMO", SANTANA.name),
    Agente("6", "Eliene", "TRES IRMAOS", SANTANA.name),
    Agente("7", "Márcio", "PARA TURISMO", SANTANA.name),
    Agente("8", "Rose", "ROSE TURISMO", SANTANA.name),
    Agente("9", "Valdez", "CONEXAO", SANTANA.name),
    Agente("10", "Sônia", "SONIA TURISMO", SANTANA.name),
    Agente("11", "Ely Machado", "D'PAULA", SANTANA.name),
    Agente("12", "Fabio", "SANT'ANNA", SANTANA.name),
    Agente("13", "Naldo", "NATUR", SANTANA.name),
    Agente("14", "Márcio", "COAPABAM", BELEM.name),
    Agente("15", "Alex", "COAPABAM", BELEM.name),
    Agente("16", "Marcos", "COAPABAM", BELEM.name),
    Agente("17", "Joaquim", "COAPABAM", BELEM.name),
    Agente("18", "Paulo Eduardo", "COAPABAM", BELEM.name),
    Agente("19", "Edias", "COAPABAM", BELEM.name),
    Agente("20", "Bia", "BIATUR", BELEM.name),
    Agente("21", "Cleber", "BIATUR", BELEM.name),
    Agente("22", "Jeova Jire - Antonio", "SEM AGENCIA", BELEM.name),
    Agente("23", "Josias", "SEM AGENCIA", BELEM.name),
    Agente("24", "Suelen", "EVERTON", SANTANA.name),
    Agente("25", "Miguel", "EVERTON", SANTANA.name),
    Agente("26", "Baixinho Sempre com Deus", "EVERTON", SANTANA.name),
    Agente("27", "Jhones", "JF TURISMO", BREVES.name),
    Agente("28", "Marcos", "FORTE TURISMO", SANTANA.name),
    Agente("29", "Cley", "ESTRELA DO NORTE", SANTANA.name),
    Agente("30", "Maicon/Mateus", "NOSSA SRA DE NAZARE", SANTANA.name),
    Agente("31", "Everton", "EVERTON", SANTANA.name),
    Agente("32", "Kelly", "EVERTON", SANTANA.name),
    Agente("33", "Cigano", NAVEG.name, BELEM.name),
    Agente("999", "Sem Agente", "SEM AGENCIA", BELEM.name)
)

//val listaAgenciaSample =
//    listOf(
//        Agencia(1, "Naveg", 1),
//        Agencia(2, "Delltur", 2),
//        Agencia(3, "Mayratur", 2),
//        Agencia(4, "Três Irmãos", 2),
//        Agencia(5, "Pará Turismo", 2),
//        Agencia(6, "Rose Turismo", 2),
//        Agencia(7, "Conexão", 2),
//        Agencia(8, "Sonia Turismo", 2),
//        Agencia(9, "D'Paula", 2),
//        Agencia(10, "Sant'Anna", 2),
//        Agencia(11, "Natur", 2),
//        Agencia(12, "Coapabam", 1),
//        Agencia(13, "Biatur", 1),
//        Agencia(14, "Nay Turismo", 2),
//        Agencia(15, "Forte Viagens", 2),
//        Agencia(16, "Nossa Sra de Nazaré", 2),
//        Agencia(17, "Estrela do Norte", 2),
//        Agencia(18, "Everton", 2),
//        Agencia(19, "JF Turismo", 3),
//        Agencia(999, "Sem Agência", 1)
//    )

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
    empresaNome = "NAVEGAÇÃO MOCORONGO",
    navio = "F/B Regional",
    dataViagem = "31/12/2023",
    horaViagem = "12:00",
    origem = "BELEM - PA",
    destino = "SANTANA - AP",
    agencia = "Naveg",
    agente = "Odair",
    valorAPagar = BigDecimal("1000").formataParaMoedaBrasileira(),
    observacao = "TESTE DE OBSERVACAO",
    tipoPassagem = "INTEIRA",
    situacao = "PENDENTE",
    funcionario = "ADMINISTRADOR",
    nomePassageiro1 = "MATHEUS SAMPAIO",
    documentoPassageiro1 = "012.938.904-94",
    dataNascimento1 = "30/01/1996",
    nomePassageiro2 = "ODAIR MATOS",
    documentoPassageiro2 = "012.938.904-94",
    dataNascimento2 = "10/01/1975",
    acomodacao = "SUITE"
)

val dadosPassagemVeiculoSample = DadosPassagem(
    numero = "2444",
    empresaNome = "NAVEGAÇÃO MOCORONGO",
    navio = "F/B Regional",
    dataViagem = "31/12/2023",
    horaViagem = "12:00",
    origem = "Belém",
    destino = "Santana",
    agencia = "Naveg",
    agente = "Odair",
    valorTotal = BigDecimal("180").formataParaMoedaBrasileira(),
    valorPix = BigDecimal("100").formataParaMoedaBrasileira(),
    valorCredito = BigDecimal("50").formataParaMoedaBrasileira(),
    desconto = BigDecimal("30").formataParaMoedaBrasileira(),
    valorAPagar = BigDecimal("150").formataParaMoedaBrasileira(),
    observacao = "TESTE DE OBSERVACAO",
    tipoPassagem = "INTEIRA",
    situacao = "PENDENTE",
    funcionario = "ADMINISTRADOR",
    nomeResponsavelRetirada = "MATHEUS SAMPAIO",
    numeroDocumentoResponsavelRetirada = "012.938.904-94",
    idVeiculo = "2",
    tipoVeiculo = "MOTO",
    modeloVeiculo = "FAZER FZ 15 150CC",
    placaVeiculo = "QLP-4O90",
    corVeiculo = "VERMELHO"
)

val userAdminSample = Usuario(
    id = "1",
    email = "admin@naveg.com.br",
    senha = "admin",
    nome = "Kurt",
    cargo = "ADM"
)

val userGerenteSample = Usuario(
    id = "2",
    email = "odairmatos@naveg.com.br",
    senha = "odair",
    nome = "Odair",
    cargo = "DIRETOR"
)

val userColabSample = Usuario(
    id = "3",
    email = "adrianasampaio@naveg.com.br",
    senha = "adriana",
    nome = "Adriana",
    cargo = "Diretor"
)

val listaUserSample = listOf(
    userAdminSample,
    userGerenteSample,
    userColabSample
)

val listaDadosBalancoPassagems = listOf(
    DadosBalancoPassagem(
        navio = "F/B Regional"
    ),
    DadosBalancoPassagem(
        navio = "Ana Marques"
    )
)

val listaDadosImpressoraSample = listOf(
    DadosImpressora(
        nome = "MTP-II_5C86",
        endereco = "86:67:7A:01:5C:86"
    ),
    DadosImpressora(
        nome = "MTP-II_5C86",
        endereco = "86:67:7A:01:5C:86"
    ),
    DadosImpressora(
        nome = "MTP-II_5C86",
        endereco = "86:67:7A:01:5C:86"
    )
)

val listaEmpresaSample = listOf(
    Empresa(
        id = "1",
        nome = "NAVEGAÇÃO MOCORONGO",
        razaoSocial = "I. S. ROCHA LTDA",
        cnpj = "47.252.191/0001-42",
        endereco = "Av. São Paulo, nº 27 - Paraíso - Santana - AP",
        telefone1 = "(93) 99112-8702",
        telefone2 = "(93) 99180-6723"
    )
)
