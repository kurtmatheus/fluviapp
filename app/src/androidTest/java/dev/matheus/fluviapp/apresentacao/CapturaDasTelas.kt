package dev.matheus.fluviapp.apresentacao

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.test.platform.app.InstrumentationRegistry
import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.domain.documento.TipoDocumento
import dev.matheus.fluviapp.domain.localidade.Uf
import dev.matheus.fluviapp.domain.operacoes.Atuacao
import dev.matheus.fluviapp.domain.passagem.Acomodacao
import dev.matheus.fluviapp.domain.passagem.CategoriaPassagem
import dev.matheus.fluviapp.domain.passagem.ClasseVeiculo
import dev.matheus.fluviapp.domain.passagem.FormaPagamento
import dev.matheus.fluviapp.domain.passagem.TipoPassagem
import dev.matheus.fluviapp.domain.screendata.SECOES_REVITALIZADAS
import dev.matheus.fluviapp.domain.viagem.DIAS_DA_SEMANA
import dev.matheus.fluviapp.domain.viagem.Embarcacao
import dev.matheus.fluviapp.domain.viagem.Empresa
import dev.matheus.fluviapp.domain.viagem.TipoEmbarcacao
import dev.matheus.fluviapp.domain.viagem.rotulo
import dev.matheus.fluviapp.ui.components.passagem.bilheteDeCamaroteTriplo
import dev.matheus.fluviapp.ui.components.passagem.bilheteDeRedeInteira
import dev.matheus.fluviapp.ui.components.passagem.bilheteDeVeiculoPesado
import dev.matheus.fluviapp.ui.screens.LoginScreen
import dev.matheus.fluviapp.ui.screens.MainScreen
import dev.matheus.fluviapp.ui.screens.PrimeiroAcessoScreen
import dev.matheus.fluviapp.ui.screens.RecuperarSenhaScreen
import dev.matheus.fluviapp.ui.screens.SplashScreen
import dev.matheus.fluviapp.ui.screens.forms.embarcacao.FormEmbarcacaoScreen
import dev.matheus.fluviapp.ui.screens.forms.embarcacao.ResultSearchEmbarcacaoScreen
import dev.matheus.fluviapp.ui.screens.forms.empresa.FormEmpresaScreen
import dev.matheus.fluviapp.ui.screens.forms.empresa.ResultSearchEmpresaScreen
import dev.matheus.fluviapp.ui.screens.forms.funcionarios.ResultSearchFuncionarioScreen
import dev.matheus.fluviapp.ui.screens.forms.localidade.FormLocalidadeScreen
import dev.matheus.fluviapp.ui.screens.forms.localidade.ResultSearchLocalidadeScreen
import dev.matheus.fluviapp.ui.screens.forms.porto.FormPortoScreen
import dev.matheus.fluviapp.ui.screens.forms.porto.ResultSearchPortoScreen
import dev.matheus.fluviapp.ui.screens.forms.rota.FormRotaScreen
import dev.matheus.fluviapp.ui.screens.forms.usuario.ResultSearchUsuarioScreen
import dev.matheus.fluviapp.ui.screens.forms.viagem.FormViagemScreen
import dev.matheus.fluviapp.ui.screens.passagem.BilheteScreen
import dev.matheus.fluviapp.ui.screens.passagem.EmbarqueScreen
import dev.matheus.fluviapp.ui.screens.passagem.PesquisaPassagemScreen
import dev.matheus.fluviapp.ui.screens.passagem.emissao.EmissaoScreen
import dev.matheus.fluviapp.ui.states.EmbarcacaoOpcao
import dev.matheus.fluviapp.ui.states.EmbarcacaoResultado
import dev.matheus.fluviapp.ui.states.EmpresaOpcao
import dev.matheus.fluviapp.ui.states.FormEmbarcacaoUiState
import dev.matheus.fluviapp.ui.states.FormEmpresaUiState
import dev.matheus.fluviapp.ui.states.FormLocalidadeUiState
import dev.matheus.fluviapp.ui.states.FormPortoUiState
import dev.matheus.fluviapp.ui.states.FormRotaUiState
import dev.matheus.fluviapp.ui.states.FormViagemUiState
import dev.matheus.fluviapp.ui.states.FuncionarioResultado
import dev.matheus.fluviapp.ui.states.InicioDaTela
import dev.matheus.fluviapp.ui.states.LocalidadeOpcao
import dev.matheus.fluviapp.ui.states.LocalidadeResultado
import dev.matheus.fluviapp.ui.states.LoginUiState
import dev.matheus.fluviapp.ui.states.MainScreenUiState
import dev.matheus.fluviapp.ui.states.PesquisaEmbarcacaoUiState
import dev.matheus.fluviapp.ui.states.PesquisaEmpresaUiState
import dev.matheus.fluviapp.ui.states.PesquisaFuncionarioUiState
import dev.matheus.fluviapp.ui.states.PesquisaLocalidadeUiState
import dev.matheus.fluviapp.ui.states.PesquisaPortoUiState
import dev.matheus.fluviapp.ui.states.PesquisaUsuarioUiState
import dev.matheus.fluviapp.ui.states.PortoOpcao
import dev.matheus.fluviapp.ui.states.PortoResultado
import dev.matheus.fluviapp.ui.states.PrimeiroAcessoUiState
import dev.matheus.fluviapp.ui.states.RecuperarSenhaUiState
import dev.matheus.fluviapp.ui.states.RotaOpcao
import dev.matheus.fluviapp.ui.states.UsuarioResultado
import dev.matheus.fluviapp.ui.states.ViagemDisponivelCard
import dev.matheus.fluviapp.ui.states.passagem.BilheteEmEdicao
import dev.matheus.fluviapp.ui.states.passagem.CabecalhoDaViagem
import dev.matheus.fluviapp.ui.states.passagem.ClienteEmEdicao
import dev.matheus.fluviapp.ui.states.passagem.ConferenciaDeEmbarque
import dev.matheus.fluviapp.ui.states.passagem.ConfirmacaoDaEmissao
import dev.matheus.fluviapp.ui.states.passagem.EmbarqueUiState
import dev.matheus.fluviapp.ui.states.passagem.EmissaoUiState
import dev.matheus.fluviapp.ui.states.passagem.LancamentoConferido
import dev.matheus.fluviapp.ui.states.passagem.LancamentoEmEdicao
import dev.matheus.fluviapp.ui.states.passagem.PagamentoEmEdicao
import dev.matheus.fluviapp.ui.states.passagem.ParticipanteEmEdicao
import dev.matheus.fluviapp.ui.states.passagem.PassagemNaLista
import dev.matheus.fluviapp.ui.states.passagem.PesquisaPassagemUiState
import dev.matheus.fluviapp.ui.states.passagem.PessoaConferida
import dev.matheus.fluviapp.ui.states.passagem.VeiculoEmEdicao
import dev.matheus.fluviapp.ui.theme.FluviAppTheme
import dev.matheus.fluviapp.ui.viewmodel.passagem.BilheteUiState
import java.io.File
import java.time.LocalDate
import org.junit.Rule
import org.junit.Test

/**
 * **As telas da apresentação institucional.** Cada método produz UM arquivo PNG nomeado pelo `data-slot`
 * do documento (`fluviapp-apresentacao.html`), que hoje reserva cada espaço com um `<img src="">`.
 *
 * ### Por que renderizar, e não fotografar o app rodando
 *
 * A "Lista de capturas" do documento pede estados, não telas: *empresa ainda sem concessão*, *conta
 * vinculada a mais de uma empresa*, *gratuidade*, *camarote com três pessoas*, *bilhetes em mais de um
 * status*. Fotografar isso exigiria encenar cada um no Firestore e desfazê-lo depois — trabalho de
 * encenação que dura o tempo de um print.
 *
 * As telas do FluviApp não têm ViewModel dentro: recebem o estado por parâmetro. Então o estado da
 * captura é **escrito aqui**, e a tela desenhada é exatamente a que o app monta — mesma tipografia,
 * mesmo tema, mesmo Material3.
 *
 * ### O recorte
 *
 * Aparelho inteiro, sem barras do sistema, normalizado em **1080 x 2340** — a proporção 9/19,5 que a
 * moldura do documento desenha, para a imagem entrar sem corte (`object-fit: cover`).
 *
 * Isto **não é teste de regressão visual**: não há imagem de referência, nada falha por diferença de
 * pixel. É produção de mídia, e o critério de sucesso é o arquivo existir.
 */
class CapturaDasTelas {

    @get:Rule
    val regra = createAndroidComposeRule<ComponentActivity>()

    // ============================================================ 01 · Entrada

    @Test
    fun entrada_splash() = captura("entrada-splash") {
        SplashScreen()
    }

    @Test
    fun entrada_login() = captura("entrada-login") {
        LoginScreen(state = LoginUiState())
    }

    @Test
    fun entrada_primeiro_acesso() = captura("entrada-primeiro-acesso") {
        PrimeiroAcessoScreen(state = PrimeiroAcessoUiState(nome = "Ana Ribeiro"))
    }

    @Test
    fun entrada_recuperar_senha() = captura("entrada-recuperar-senha") {
        RecuperarSenhaScreen(state = RecuperarSenhaUiState(email = "ana.ribeiro@fluviapp.com.br"))
    }

    // ============================================================ 02 · Painel

    @Test
    fun painel_inicio_empresa() = captura("painel-inicio-empresa") {
        MainScreen(painel(InicioDaTela.DaEmpresa(saidasDaSemana)))
    }

    @Test
    fun painel_inicio_plataforma() = captura("painel-inicio-plataforma") {
        MainScreen(painel(InicioDaTela.DaPlataforma, nome = "Kurt"))
    }

    @Test
    fun painel_sem_concessao() = captura("painel-sem-concessao") {
        MainScreen(painel(InicioDaTela.SemConcessao))
    }

    /**
     * O menu **aberto** — o gesto é o mesmo do app: tocar no nome na barra superior.
     *
     * O alvo é o nome, e não o ícone: a descrição "Ícone Usuário" existe **duas vezes** na árvore — na
     * barra e no cabeçalho do próprio menu, que já está composto fora da tela.
     */
    @Test
    fun painel_menu() = captura(
        slot = "painel-menu",
        antesDeGravar = {
            regra.onNode(hasText("Odair") and hasClickAction()).performClick()
            regra.waitForIdle()
        },
    ) {
        MainScreen(painel(InicioDaTela.DaEmpresa(saidasDaSemana)))
    }

    // ============================================================ 03 · Cadastros da plataforma

    @Test
    fun cadastros_empresas_lista() = captura("cadastros-empresas-lista") {
        ResultSearchEmpresaScreen(uiState = PesquisaEmpresaUiState(resultados = empresas))
    }

    /** Formulário de **edição**, com as concessões já marcadas — é o que o slot pede. */
    @Test
    fun cadastros_empresas_formulario() = captura("cadastros-empresas-formulario") {
        FormEmpresaScreen(uiState = empresaEmEdicao)
    }

    /**
     * A metade que o slot destaca — as concessões — mora abaixo da dobra, então esta captura rola até
     * ela. Fica de reserva, para quem preferir mostrar a concessão em vez da identificação.
     */
    @Test
    fun cadastros_empresas_formulario_concessoes() = captura(
        slot = "cadastros-empresas-formulario-concessoes",
        antesDeGravar = {
            repeat(2) {
                regra.onRoot().performTouchInput { swipeUp() }
                regra.waitForIdle()
            }
        },
    ) {
        FormEmpresaScreen(uiState = empresaEmEdicao)
    }

    private val empresaEmEdicao
        get() = FormEmpresaUiState(
            titulo = R.string.subtitle_editar_empresa,
            nome = "NAVEGA MODELO",
            razaoSocial = "Navega Modelo Transportes LTDA",
            // Só dígitos: a máscara é do CAMPO (`CnpjVisualTransformation`), e o estado guarda o
            // número cru. Dar-lhe um valor já formatado estoura o `OffsetMapping`.
            cnpj = "00000000000100",
            endereco = "Rodovia Arthur Bernardes, 1000 — Belém/PA",
            telefone1 = "(91) 99999-0001",
            atuacoes = setOf(Atuacao.AGENCIAMENTO, Atuacao.TRANSPORTE),
            embarcacoes = frota,
            embarcacoesConcedidas = setOf("emb1"),
            portos = portos,
            portosConcedidos = setOf("p1", "p2"),
        )

    @Test
    fun cadastros_flotilha_lista() = captura("cadastros-flotilha-lista") {
        ResultSearchEmbarcacaoScreen(
            uiState = PesquisaEmbarcacaoUiState(
                listaEmpresas = listOf("NAVEGA MODELO", "TRANSPORTE ILHA"),
                resultados = listOf(
                    EmbarcacaoResultado("1", "F/B Modelo", "Ferry Boat", "NAVEGA MODELO"),
                    EmbarcacaoResultado("2", "N/M Comandante", "Navio", "NAVEGA MODELO"),
                    EmbarcacaoResultado("3", "Lancha Litoral", "Lancha", "TRANSPORTE ILHA"),
                ),
            ),
        )
    }

    /** O tipo que **leva veículo**: a capacidade de veículo existe. O contraste está em `-lancha`. */
    @Test
    fun cadastros_flotilha_formulario() = captura("cadastros-flotilha-formulario") {
        FormEmbarcacaoScreen(uiState = embarcacaoDoTipo(TipoEmbarcacao.FERRY_BOAT, "F/B Modelo"))
    }

    /** O contraste: escolhida a lancha, a pergunta de veículo **não existe** — não fica cinza. */
    @Test
    fun cadastros_flotilha_formulario_lancha() = captura("cadastros-flotilha-formulario-lancha") {
        FormEmbarcacaoScreen(uiState = embarcacaoDoTipo(TipoEmbarcacao.LANCHA, "Lancha Litoral"))
    }

    @Test
    fun cadastros_localidades_lista() = captura("cadastros-localidades-lista") {
        ResultSearchLocalidadeScreen(
            uiState = PesquisaLocalidadeUiState(
                resultados = listOf(
                    LocalidadeResultado("1", "Belém/PA", "1501402"),
                    LocalidadeResultado("2", "Parintins/AM", "1303205"),
                    LocalidadeResultado("3", "Santarém/PA", "1506807"),
                ),
            ),
        )
    }

    @Test
    fun cadastros_localidades_formulario() = captura("cadastros-localidades-formulario") {
        FormLocalidadeScreen(
            uiState = FormLocalidadeUiState(
                titulo = R.string.subtitle_editar_localidade,
                codigoIbge = "1501402",
                municipio = "Belém",
                uf = Uf.PA,
            ),
        )
    }

    @Test
    fun cadastros_portos_lista() = captura("cadastros-portos-lista") {
        ResultSearchPortoScreen(
            uiState = PesquisaPortoUiState(
                resultados = listOf(
                    PortoResultado("1", "Porto de Val-de-Cães", "Belém/PA"),
                    PortoResultado("2", "Porto de Parintins", "Parintins/AM"),
                    PortoResultado("3", "Porto de Santarém", "Santarém/PA"),
                ),
            ),
        )
    }

    @Test
    fun cadastros_portos_formulario() = captura("cadastros-portos-formulario") {
        FormPortoScreen(
            uiState = FormPortoUiState(
                titulo = R.string.subtitle_editar_porto,
                nome = "Porto de Val-de-Cães",
                localidadeId = "1",
                localidades = listOf(
                    LocalidadeOpcao("1", "Belém/PA"),
                    LocalidadeOpcao("2", "Parintins/AM"),
                ),
            ),
        )
    }

    // ============================================================ 04 · Operação compartilhada

    @Test
    fun operacao_rotas() = captura("operacao-rotas") {
        FormRotaScreen(
            uiState = FormRotaUiState(
                portos = portos,
                portoOrigem = portos[0].rotulo,
                portoDestino = portos[1].rotulo,
                distanciaMn = "420.5",
                tempoMedioH = "30",
            ),
        )
    }

    @Test
    fun operacao_viagens() = captura("operacao-viagens") {
        FormViagemScreen(
            uiState = FormViagemUiState(
                rotas = rotas,
                rota = rotas.first().rotulo,
                embarcacoes = listOf(EmbarcacaoOpcao("emb1", "F/B Modelo")),
                embarcacao = "F/B Modelo",
                diasDaSemana = DIAS_DA_SEMANA.map { it.rotulo },
                diaSemana = DIAS_DA_SEMANA[1].rotulo,
                horaDigitada = "1800",
            ),
        )
    }

    // ============================================================ 05 · Pessoas

    @Test
    fun pessoas_equipe() = captura("pessoas-equipe") {
        ResultSearchFuncionarioScreen(
            uiState = PesquisaFuncionarioUiState(
                empresas = listOf(EmpresaOpcao("e1", "Navegação Norte")),
                resultados = listOf(
                    FuncionarioResultado(
                        id = "1",
                        nome = "Ana Ribeiro",
                        email = "ana.ribeiro@fluviapp.com.br",
                        vinculo = "Navegação Norte · SUPERVISOR",
                    ),
                    FuncionarioResultado(
                        id = "2",
                        nome = "Bruno Costa",
                        email = "bruno.costa@fluviapp.com.br",
                        vinculo = "Navegação Norte · AGENTE",
                    ),
                ),
            ),
        )
    }

    @Test
    fun pessoas_usuarios() = captura("pessoas-usuarios") {
        ResultSearchUsuarioScreen(
            uiState = PesquisaUsuarioUiState(
                resultados = listOf(
                    UsuarioResultado("adm@fluviapp.com.br", "Kurt", "ADM", "", "Ativo"),
                    UsuarioResultado(
                        email = "ana.ribeiro@fluviapp.com.br",
                        nome = "Ana Ribeiro",
                        papel = "OPERADOR",
                        vinculo = "Navegação Norte · SUPERVISOR",
                        situacao = "Ativo",
                    ),
                    UsuarioResultado(
                        email = "bruno.costa@fluviapp.com.br",
                        nome = "Bruno Costa",
                        papel = "OPERADOR",
                        vinculo = "Navegação Norte · AGENTE",
                        situacao = "Convidado",
                    ),
                ),
            ),
        )
    }

    // ============================================================ 06 · Emissão

    @Test
    fun emissao_categoria() = captura("emissao-categoria") {
        EmissaoScreen(state = EmissaoUiState(cabecalho = cabecalho))
    }

    @Test
    fun emissao_acomodacao() = captura("emissao-acomodacao") {
        EmissaoScreen(state = EmissaoUiState(cabecalho = cabecalho, indiceDoPasso = 1))
    }

    @Test
    fun emissao_tipo_tarifario() = captura("emissao-tipo-tarifario") {
        EmissaoScreen(
            state = EmissaoUiState(
                cabecalho = cabecalho,
                indiceDoPasso = 2,
                bilhete = BilheteEmEdicao(acomodacao = Acomodacao.REDE),
            ),
        )
    }

    /** O passo só existe porque o tipo escolhido foi **gratuidade** — é o roteiro crescendo. */
    @Test
    fun emissao_tipo_gratuidade() = captura("emissao-tipo-gratuidade") {
        EmissaoScreen(
            state = EmissaoUiState(
                cabecalho = cabecalho,
                indiceDoPasso = 3,
                bilhete = BilheteEmEdicao(
                    acomodacao = Acomodacao.REDE,
                    tipo = TipoPassagem.GRATUIDADE,
                ),
            ),
        )
    }

    /** Suíte comporta mais de um: aí a pergunta é *quantos*, e o tipo tarifário nem se pergunta. */
    @Test
    fun emissao_quantidade_pessoas() = captura("emissao-quantidade-pessoas") {
        EmissaoScreen(
            state = EmissaoUiState(
                cabecalho = cabecalho,
                indiceDoPasso = 2,
                bilhete = BilheteEmEdicao(acomodacao = Acomodacao.SUITE),
            ),
        )
    }

    @Test
    fun emissao_natureza_veiculo() = captura("emissao-natureza-veiculo") {
        EmissaoScreen(
            state = EmissaoUiState(
                cabecalho = cabecalho,
                indiceDoPasso = 1,
                bilhete = BilheteEmEdicao(categoria = CategoriaPassagem.VEICULO),
            ),
        )
    }

    /** Moto: o formulário pede modelo **e** cilindrada. O contraste está em `-carreta`. */
    @Test
    fun emissao_dados_veiculo() = captura("emissao-dados-veiculo") {
        EmissaoScreen(state = veiculoDaClasse(ClasseVeiculo.MOTO, "ABC1D23", "Honda CG 160"))
    }

    /** Carreta: o modelo e oferecido e nao cobrado, e a cilindrada nao existe (ADR-0031). */
    @Test
    fun emissao_dados_veiculo_carreta() = captura("emissao-dados-veiculo-carreta") {
        EmissaoScreen(state = veiculoDaClasse(ClasseVeiculo.CARRETA, "RST4E56", ""))
    }

    /** Camarote com três pessoas: o titular é o passo 4, e cada acompanhante acrescenta um. */
    @Test
    fun emissao_quem_viaja() = captura("emissao-quem-viaja") {
        EmissaoScreen(
            state = EmissaoUiState(
                cabecalho = cabecalho,
                indiceDoPasso = 3,
                bilhete = BilheteEmEdicao(acomodacao = Acomodacao.CAMAROTE),
                participante = ParticipanteEmEdicao.DePassageiro(
                    pessoas = listOf(
                        ClienteEmEdicao(
                            nome = "Ana Ribeiro",
                            tipoDocumento = TipoDocumento.CPF,
                            numeroDocumento = "52998224725",
                            dataNascimento = "30/01/1996",
                        ),
                        ClienteEmEdicao(),
                        ClienteEmEdicao(),
                    ),
                ),
            ),
        )
    }

    @Test
    fun emissao_pagamento() = captura("emissao-pagamento") {
        EmissaoScreen(
            state = EmissaoUiState(
                cabecalho = cabecalho,
                indiceDoPasso = 4,
                bilhete = BilheteEmEdicao(acomodacao = Acomodacao.REDE),
                pagamento = PagamentoEmEdicao(
                    lancamentos = listOf(LancamentoEmEdicao(FormaPagamento.PIX, "150,00")),
                ),
            ),
        )
    }

    /** O documento chega **mascarado** — o detalhamento fica aberto de frente para a fila (LGPD). */
    @Test
    fun emissao_conferencia() = captura("emissao-conferencia") {
        EmissaoScreen(
            state = EmissaoUiState(
                cabecalho = cabecalho,
                indiceDoPasso = 4,
                confirmacao = ConfirmacaoDaEmissao(
                    cabecalho = cabecalho,
                    bilhete = "Rede · Inteira",
                    pessoas = listOf(
                        PessoaConferida(
                            papel = "Passageiro",
                            nome = "Ana Ribeiro",
                            documento = TipoDocumento.CPF.rotulo + " " +
                                TipoDocumento.CPF.mascarar("52998224725"),
                            nascimento = "30/01/1996",
                        ),
                    ),
                    lancamentos = listOf(LancamentoConferido("PIX", "R$ 150,00")),
                    total = "R$ 150,00",
                    observacao = null,
                    agencia = "NAVEGA MODELO",
                ),
            ),
        )
    }

    // ============================================================ 07 · O bilhete digital

    /** O topo do documento — a marca da agência e a travessia. Não é o que o slot pede; fica de reserva. */
    @Test
    fun bilhete_digital_topo() = captura("bilhete-digital-topo") {
        BilheteScreen(state = bilhete)
    }

    /** O slot pede o **rodapé**, onde mora o QR — então a captura rola até lá antes de gravar. */
    @Test
    fun bilhete_digital() = captura(
        slot = "bilhete-digital",
        antesDeGravar = {
            repeat(3) {
                regra.onRoot().performTouchInput { swipeUp() }
                regra.waitForIdle()
            }
        },
    ) {
        BilheteScreen(state = bilhete)
    }

    /**
     * **O bilhete de veículo, com o rótulo mais longo que existe** — `Carreta Cavalinho`, dezessete
     * caracteres, contra os nove de *Caminhão* que eram o máximo antes do ADR-0031.
     *
     * A captura existe para medir uma coisa que nenhum teste de asserção mede: se a caixa **Classe**, que
     * divide a linha por `weight(1f)` com Modelo, Cor e Cilindrada, ainda cabe. Truncar aqui seria pior do
     * que feio — o bilhete é o documento que vai para a mão de quem viaja.
     */
    @Test
    fun bilhete_digital_veiculo() = captura("bilhete-digital-veiculo") {
        BilheteScreen(
            state = bilhete.copy(
                bilhete = bilheteDeVeiculoPesado.copy(
                    veiculo = bilheteDeVeiculoPesado.veiculo?.copy(
                        classe = "Carreta Cavalinho",
                        modelo = "Scania R450",
                    ),
                ),
            ),
        )
    }

    // ============================================================ 08 · Embarque

    /** Bilhete válido e **ainda não embarcado**: é o estado em que o botão de confirmar faz sentido. */
    @Test
    fun embarque_conferencia() = captura("embarque-conferencia") {
        EmbarqueScreen(
            state = EmbarqueUiState(
                conferencia = ConferenciaDeEmbarque(
                    numero = "#12",
                    bilhete = "Rede · Inteira",
                    travessia = TRAVESSIA,
                    partida = PARTIDA,
                    status = "EMITIDA",
                ),
            ),
        )
    }

    // ============================================================ 09 · Consulta

    /** Bilhetes em **mais de um status** — é o que o slot pede, e é o que a lista apagada mostra. */
    @Test
    fun consulta_passagens() = captura("consulta-passagens") {
        PesquisaPassagemScreen(
            state = PesquisaPassagemUiState(
                data = LocalDate.of(2026, 8, 18),
                buscou = true,
                resultados = listOf(
                    PassagemNaLista("p1", "#12", "Rede · Inteira", PARTIDA, "EMITIDA", encerrada = false),
                    PassagemNaLista("p2", "#13", "Suíte", PARTIDA, "EMBARCADA", encerrada = true),
                    PassagemNaLista("p3", "#14", "Rede · Meia", PARTIDA, "EMITIDA", encerrada = false),
                    PassagemNaLista("p4", "#15", "Veículo · Moto", PARTIDA, "CANCELADA", encerrada = true),
                ),
            ),
        )
    }

    // ============================================================ a sequência do GIF

    /**
     * **Uma emissão só, do começo ao fim** — os quadros do `emissao-fluxo-completo`, que é o único slot
     * de movimento que a renderização consegue produzir.
     *
     * As capturas avulsas acima são um **catálogo de passos**, não um caminho: cada uma escolhe o estado
     * que mostra melhor aquele passo, e por isso o contador delas não encadeia (3/5 seguido de 4/7). Aqui
     * o estado é o mesmo objeto avançando, então o contador conta a mesma história que a tela.
     *
     * O caminho é **camarote com dois acompanhantes**, e a escolha não é estética: é onde o roteiro
     * derivado *cresce* — 5 passos prometidos no começo viram 7 quando se pede três lugares, e cada
     * acompanhante acrescenta a pergunta de quem é. É o que o bloco do documento diz em texto.
     *
     * A "Lista de capturas" pede gratuidade **e** mais de um acompanhante no mesmo caminho; o domínio
     * não admite os dois juntos (gratuidade só existe na rede, que vende um lugar — `roteiroDe`), então
     * a sequência mostra o segundo.
     *
     * Os quatro primeiros quadros reaproveitam as capturas avulsas, que já valem para este caminho:
     * `painel-inicio-empresa`, `emissao-categoria`, `emissao-acomodacao` e `emissao-quem-viaja`.
     */
    /**
     * O passo em que se escolhe **quantos** — e aqui a trilha ainda promete 5.
     *
     * O participante tem UMA pessoa neste quadro, e não três: o roteiro é derivado do estado, então uma
     * lista de três já montada faria a trilha anunciar 7 passos **antes** de alguém pedir três lugares.
     * O crescimento tem de acontecer no quadro seguinte, que é onde a resposta foi dada.
     */
    @Test
    fun sequencia_03_quantidade() = captura("sequencia-03-quantidade") {
        EmissaoScreen(state = camarote(indice = 2, pessoasPreenchidas = 0, lugares = 1))
    }

    @Test
    fun sequencia_05_acompanhante_1() = captura("sequencia-05-acompanhante-1") {
        EmissaoScreen(state = camarote(indice = 4, pessoasPreenchidas = 2))
    }

    @Test
    fun sequencia_06_acompanhante_2() = captura("sequencia-06-acompanhante-2") {
        EmissaoScreen(state = camarote(indice = 5, pessoasPreenchidas = 3))
    }

    @Test
    fun sequencia_07_pagamento() = captura("sequencia-07-pagamento") {
        EmissaoScreen(
            state = camarote(indice = 6, pessoasPreenchidas = 3).copy(
                pagamento = PagamentoEmEdicao(
                    lancamentos = listOf(LancamentoEmEdicao(FormaPagamento.DINHEIRO, "450,00")),
                ),
            ),
        )
    }

    @Test
    fun sequencia_08_conferencia() = captura("sequencia-08-conferencia") {
        EmissaoScreen(
            state = camarote(indice = 6, pessoasPreenchidas = 3).copy(
                confirmacao = ConfirmacaoDaEmissao(
                    cabecalho = cabecalho,
                    bilhete = "Camarote",
                    pessoas = viajantes.map {
                        PessoaConferida(
                            papel = if (it === viajantes.first()) "Titular" else "Acompanhante",
                            nome = it.nome,
                            documento = TipoDocumento.CPF.rotulo + " " +
                                TipoDocumento.CPF.mascarar(it.numeroDocumento),
                            nascimento = it.dataNascimento,
                        )
                    },
                    lancamentos = listOf(LancamentoConferido("Dinheiro", "R$ 450,00")),
                    total = "R$ 450,00",
                    observacao = null,
                    agencia = "NAVEGA MODELO",
                ),
            ),
        )
    }

    /** O desfecho da mesma emissão: o bilhete de camarote triplo, rolado até o QR. */
    @Test
    fun sequencia_09_bilhete() = captura(
        slot = "sequencia-09-bilhete",
        antesDeGravar = {
            repeat(3) {
                regra.onRoot().performTouchInput { swipeUp() }
                regra.waitForIdle()
            }
        },
    ) {
        BilheteScreen(state = bilhete.copy(bilhete = bilheteDeCamaroteTriplo))
    }

    /** Os três que viajam juntos — o titular e dois acompanhantes. */
    private val viajantes = listOf(
        ClienteEmEdicao(
            nome = "Ana Ribeiro",
            tipoDocumento = TipoDocumento.CPF,
            numeroDocumento = "52998224725",
            dataNascimento = "30/01/1996",
        ),
        ClienteEmEdicao(
            nome = "Bruno Costa",
            tipoDocumento = TipoDocumento.CPF,
            numeroDocumento = "39053344705",
            dataNascimento = "12/07/1988",
        ),
        ClienteEmEdicao(
            nome = "Célia Nunes",
            tipoDocumento = TipoDocumento.CPF,
            numeroDocumento = "16899535009",
            dataNascimento = "03/11/2001",
        ),
    )

    /**
     * O estado da sequência num dado passo. [pessoasPreenchidas] é quantas das três já têm dados — é o
     * que faz o formulário do acompanhante aparecer preenchido no quadro em que ele acabou de ser
     * respondido, em vez de vazio no caminho inteiro.
     */
    private fun camarote(indice: Int, pessoasPreenchidas: Int, lugares: Int = 3) = EmissaoUiState(
        cabecalho = cabecalho,
        indiceDoPasso = indice,
        bilhete = BilheteEmEdicao(acomodacao = Acomodacao.CAMAROTE),
        participante = ParticipanteEmEdicao.DePassageiro(
            pessoas = viajantes.take(lugares).mapIndexed { posicao, pessoa ->
                if (posicao < pessoasPreenchidas) pessoa else ClienteEmEdicao()
            },
        ),
    )

    // ============================================================ os dados de cena

    private val cabecalho = CabecalhoDaViagem(
        travessia = TRAVESSIA,
        partida = PARTIDA,
        embarcacao = "F/B Modelo",
    )

    private val bilhete = BilheteUiState(
        carregando = false,
        naoEncontrado = false,
        bilhete = bilheteDeRedeInteira,
        arquivo = Uri.EMPTY,
        chaveDaOcorrencia = "viagem-modelo-1@2026-08-18",
    )

    private val saidasDaSemana = listOf(
        ViagemDisponivelCard(
            id = "viagem-modelo-1@2026-08-18",
            viagemId = "viagem-modelo-1",
            partida = PARTIDA,
            rota = TRAVESSIA,
            embarcacao = "F/B Modelo",
            chegada = "Qui 00:00",
        ),
        ViagemDisponivelCard(
            id = "viagem-modelo-2@2026-08-21",
            viagemId = "viagem-modelo-2",
            partida = "Sexta-feira, 21/08 · 06:00",
            rota = "Porto de Parintins · Parintins/AM → Porto de Val-de-Cães · Belém/PA",
            embarcacao = "F/B Modelo",
            chegada = "Sáb 12:00",
        ),
    )

    private val empresas = listOf(
        Empresa(
            "1",
            "NAVEGA MODELO",
            "Navega Modelo Transportes LTDA",
            "00.000.000/0001-00",
            "Rodovia Arthur Bernardes, 1000 — Belém/PA",
            "(91) 99999-0001",
            "",
        ),
        Empresa(
            "2",
            "TRANSPORTE ILHA",
            "Transporte Ilha SA",
            "11.111.111/0001-11",
            "Av. Beira-Rio, 200 — Parintins/AM",
            "(92) 99999-0002",
            "",
        ),
        Empresa(
            "3",
            "RIO SUL",
            "Rio Sul Navegação ME",
            "22.222.222/0001-22",
            "Cais do Porto — Santarém/PA",
            "(93) 99999-0003",
            "",
        ),
    )

    private val frota = listOf(
        Embarcacao("emb1", "F/B Modelo", TipoEmbarcacao.FERRY_BOAT, 40, 12, 8, 6, "1"),
        Embarcacao("emb2", "Lancha Litoral", TipoEmbarcacao.LANCHA, 0, 0, 0, 0, "2"),
    )

    private val portos = listOf(
        PortoOpcao("p1", "Porto de Val-de-Cães · Belém/PA"),
        PortoOpcao("p2", "Porto de Parintins · Parintins/AM"),
        PortoOpcao("p3", "Porto de Santarém · Santarém/PA"),
    )

    private val rotas = listOf(
        RotaOpcao("r1", "Porto de Val-de-Cães · Belém/PA → Porto de Parintins · Parintins/AM"),
    )

    private fun painel(inicio: InicioDaTela, nome: String = "Odair") = MainScreenUiState(
        userName = nome,
        secoesVisiveis = SECOES_REVITALIZADAS.toList(),
        inicio = inicio,
    )

    private fun embarcacaoDoTipo(tipo: TipoEmbarcacao, nome: String) = FormEmbarcacaoUiState(
        nome = nome,
        empresa = "NAVEGA MODELO",
        tipo = tipo,
        capacidadeVeiculo = if (tipo.levaVeiculo) "40" else "",
        capacidadeSuite2 = "12",
        capacidadeSuite3 = "8",
        capacidadeCamarote = "6",
        listaEmpresas = empresas,
    )

    private fun veiculoDaClasse(classe: ClasseVeiculo, placa: String, modelo: String) = EmissaoUiState(
        cabecalho = cabecalho,
        // Terceiro passo desde o ADR-0031 D8: categoria, natureza, classe, e entao os dados.
        indiceDoPasso = 3,
        bilhete = BilheteEmEdicao(categoria = CategoriaPassagem.VEICULO),
        participante = ParticipanteEmEdicao.DeVeiculo(
            VeiculoEmEdicao(placa = placa, modelo = modelo, natureza = classe.natureza, classe = classe),
        ),
    )

    // ============================================================ o mecanismo

    /**
     * Monta a tela em tela cheia, deixa o Compose assentar, executa o gesto de cena (se houver) e grava
     * o PNG com o nome do slot.
     */
    private fun captura(
        slot: String,
        escuro: Boolean = false,
        antesDeGravar: () -> Unit = {},
        conteudo: @Composable () -> Unit,
    ) {
        telaCheia()

        regra.setContent {
            FluviAppTheme(darkTheme = escuro) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                ) { conteudo() }
            }
        }
        regra.waitForIdle()
        antesDeGravar()

        grava(padroniza(regra.onRoot().captureToImage().asAndroidBitmap()), slot)
    }

    private fun telaCheia() {
        val atividade = regra.activity
        regra.runOnUiThread {
            WindowCompat.setDecorFitsSystemWindows(atividade.window, false)
            WindowInsetsControllerCompat(atividade.window, atividade.window.decorView)
                .hide(WindowInsetsCompat.Type.systemBars())
        }
        regra.waitForIdle()
    }

    /**
     * Normaliza para **1080 x 2340**, que é a proporção da moldura do documento.
     *
     * A janela do aparelho é um pouco mais baixa que a tela física — sobra o que as barras do sistema
     * ocupariam. As duas faixas são preenchidas com a cor **da própria borda** da captura: é o que se
     * veria num aparelho de verdade em modo borda a borda, onde a barra de aplicativo se estende por
     * baixo da barra de status e o fundo por baixo da de navegação.
     */
    private fun padroniza(original: Bitmap): Bitmap {
        if (original.width == LARGURA && original.height == ALTURA) return original

        val escala = LARGURA.toFloat() / original.width
        val ajustada = if (escala == 1f) {
            original
        } else {
            Bitmap.createScaledBitmap(original, LARGURA, (original.height * escala).toInt(), true)
        }
        if (ajustada.height >= ALTURA) {
            return Bitmap.createBitmap(ajustada, 0, 0, LARGURA, ALTURA)
        }

        val alvo = Bitmap.createBitmap(LARGURA, ALTURA, Bitmap.Config.ARGB_8888)
        val tela = Canvas(alvo)
        val topo = (ALTURA - ajustada.height) / 2

        tela.drawColor(ajustada.getPixel(LARGURA / 2, 0))
        tela.drawRect(
            0f,
            (topo + ajustada.height).toFloat(),
            LARGURA.toFloat(),
            ALTURA.toFloat(),
            Paint().apply { color = ajustada.getPixel(LARGURA / 2, ajustada.height - 1) },
        )
        tela.drawBitmap(ajustada, 0f, topo.toFloat(), null)
        return alvo
    }

    private fun grava(imagem: Bitmap, slot: String) {
        val contexto = InstrumentationRegistry.getInstrumentation().targetContext
        val pasta = File(contexto.getExternalFilesDir(null), PASTA).apply { mkdirs() }
        val arquivo = File(pasta, slot + ".png")
        arquivo.outputStream().use { imagem.compress(Bitmap.CompressFormat.PNG, 100, it) }
        println("[captura] " + slot + " -> " + imagem.width + "x" + imagem.height)
    }

    private fun texto(id: Int) = regra.activity.getString(id)

    private companion object {
        const val PASTA = "apresentacao"
        const val LARGURA = 1080
        const val ALTURA = 2340
        const val TRAVESSIA = "Porto de Val-de-Cães · Belém/PA → Porto de Parintins · Parintins/AM"
        const val PARTIDA = "Terça-feira, 18/08 · 18:00"
    }
}
