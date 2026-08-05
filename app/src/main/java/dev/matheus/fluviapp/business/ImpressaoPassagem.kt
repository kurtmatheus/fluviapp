package dev.matheus.fluviapp.business

import dev.matheus.fluviapp.extensions.formatarDataHoraBarrasBr
import dev.matheus.fluviapp.domain.screendata.DadosPassagem
import dev.matheus.fluviapp.services.printerservice.TXT_2HEIGHT
import dev.matheus.fluviapp.services.printerservice.TXT_ALIGN_CT
import dev.matheus.fluviapp.services.printerservice.TXT_ALIGN_LT
import dev.matheus.fluviapp.services.printerservice.TXT_BOLD_OFF
import dev.matheus.fluviapp.services.printerservice.TXT_BOLD_ON
import dev.matheus.fluviapp.services.printerservice.TXT_NORMAL
import java.time.LocalDateTime

class ImpressaoPassagem {
    companion object {
        lateinit var dadosPassagem: DadosPassagem

        private const val LINE_TOTAL_LENGTH = 31

        var IS_VIA_CLIENTE = false

        private const val TITLE_DADOS_PASSAGEM = "DADOS DA PASSAGEM"

        // Labels empresa
        private const val LABEL_CNPJ = "CNPJ"
        private const val LABEL_RAZAO_SOCIAL = "Razão Social"

        // Labels Viagem
        private const val LABEL_TRECHO = "Trecho"
        private const val LABEL_DATA_VIAGEM = "Data"
        private const val LABEL_HORA = "Horário"

        // Labels Passagem
        private const val LABEL_NOME = "Nome"
        private const val LABEL_TIPO_DOCUMENTO = "Documento"
        private const val LABEL_N_DOCUMENTO = "Nº do Documento"
        private const val LABEL_DATA_NASCIMENTO = "Data de Nascimento"
        private const val LABEL_TIPO_PASSAGEM = "Tipo de Passagem"
        private const val LABEL_TIPO_GRATUIDADE = "Tipo de Gratuidade"
        private const val LABEL_VEICULO_MODELO = "Modelo"
        private const val LABEL_VEICULO_PLACA = "Placa"
        private const val LABEL_VEICULO_COR = "Cor"

        // Labels Valores
        private const val LABEL_TARIFA = "Tarifa"
        private const val LABEL_VALOR_TOTAL = "Valor Total"
        private const val LABEL_DESCONTO = "Desconto"
        private const val LABEL_VALOR_A_PAGAR = "Valor a Pagar"
        private const val LABEL_VALOR_PAGO = "Valor Pago"

        // Labels Dados Finais
        private const val EMITIDO_EM = "Emitido em"
        private const val LABEL_AGENCIA = "Agência"

        internal const val LABEL_ID = "Identificação do Bilhete"
        private const val LABEL_OPERADOR = "Operador"
        private const val LABEL_OBS = "Observação"

        const val VIA_CLIENTE = "VIA DO CLIENTE"
        const val VIA_EMPRESA = "VIA DO EMBARCACAO"

        private fun getCabecalho(): String {
            return buildString {
                append(String(TXT_ALIGN_CT))
                append(getNomeEmpresa())
                append(String(TXT_BOLD_ON))
                append(setLineLabelCampo(LABEL_CNPJ, dadosPassagem.empresaCnpj))
                append(setLineLabelCampo(LABEL_RAZAO_SOCIAL, dadosPassagem.empresaRazaoSocial))
                append(String(TXT_BOLD_OFF))
                append("${dadosPassagem.empresaEndereco}\n")
                append("${dadosPassagem.empresaTelefone1}/${dadosPassagem.empresaTelefone2}\n")
                append("\n")
            }
        }

        private fun getNomeEmpresa(): String {
            return buildString {
                append(String(TXT_BOLD_ON))
                append(String(TXT_2HEIGHT))
                append("${dadosPassagem.empresaNome}\n")
                append(String(TXT_NORMAL))
                append(String(TXT_BOLD_OFF))
            }
        }

        private fun getNumeroBilhete(): String {
            return buildString {
                append(String(TXT_BOLD_ON))
                append("Bilhete de Passagem\n")
                append("\n")
                append(String(TXT_2HEIGHT))
                append("Nº: ${dadosPassagem.numero}\n")
                append(String(TXT_NORMAL))
                append(String(TXT_BOLD_OFF))
                append(String(TXT_ALIGN_LT))
                append("\n")
            }
        }

        private fun getDadosViagem(): String {
            return buildString {
                append(getDadoDestaqueCentralizado(dadosPassagem.embarcacao.uppercase()))
                append(String(TXT_BOLD_ON))
                append(setLineLabelCampo(LABEL_TRECHO.uppercase(), "${dadosPassagem.origem.uppercase()}/${dadosPassagem.destino.uppercase()}"))
                append("$LABEL_DATA_VIAGEM: ${dadosPassagem.dataViagem} $LABEL_HORA: ${dadosPassagem.horaViagem}\n")
                append(String(TXT_BOLD_OFF))
                append("\n")
            }
        }

        private fun getDadoDestaqueCentralizado(dadoDestaque: String): String {
            return buildString {
                append(String(TXT_ALIGN_CT))
                append(String(TXT_2HEIGHT))
                append(dadoDestaque)
                append("\n")
                append(String(TXT_NORMAL))
                append(String(TXT_ALIGN_LT))
                append("\n")
            }
        }

        private fun getAcomodacaoTipoVeiculo(): String {
            return buildString {
                if (!dadosPassagem.ehVeiculo) {
                    append(getDadoDestaqueCentralizado("[${dadosPassagem.acomodacao}]"))
                } else {
                    append(getDadoDestaqueCentralizado("[${dadosPassagem.tipoVeiculo}]"))
                }
            }
        }

        private fun getDadosValores(): String {
            return buildString {
                if (dadosPassagem.ehRede) {
                    append(setLabelLineCampoAlignRight(LABEL_TARIFA.uppercase(), dadosPassagem.tarifa))
                }
                append(setLabelLineCampoAlignRight(LABEL_VALOR_TOTAL, dadosPassagem.valorTotal))
                append(setLabelLineCampoAlignRight(LABEL_DESCONTO, dadosPassagem.desconto))
                append(setLabelLineCampoAlignRight(LABEL_VALOR_A_PAGAR, dadosPassagem.valorAPagar))
                append("\n")
                append(setLabelLineCampoAlignRight(LABEL_VALOR_PAGO, dadosPassagem.valorAPagar))
                append("\n")
            }
        }

        private fun getDadosPassagem(): String {
            return buildString {
                append(setTituloSecao())
                if (!dadosPassagem.ehVeiculo) {
                    append(
                        getDadosPassageiros(
                            nome = dadosPassagem.nomePassageiro1,
                            tipoDocumeto = dadosPassagem.tipoDocumentoPassageiro1,
                            nDocumento = dadosPassagem.documentoPassageiro1
                        )
                    )
//                append(setLabel(LABEL_TIPO_PASSAGEM))
//                append(setCampo(dadosPassagem.tipoPassagem))
//                if (dadosPassagem.temGratuidade) {
//                    append(setLabel(LABEL_TIPO_GRATUIDADE))
//                    append(setCampo(dadosPassagem.tipoGratuidade))
//                }
                    if (dadosPassagem.tem2Pessoas) {
                        append(
                            getDadosPassageiros(
                                nome = dadosPassagem.nomePassageiro2,
                                tipoDocumeto = dadosPassagem.tipoDocumentoPassageiro2,
                                nDocumento = dadosPassagem.documentoPassageiro2
                            )
                        )

                        if (dadosPassagem.tem3Pessoas) {
                            append(
                                getDadosPassageiros(
                                    nome = dadosPassagem.nomePassageiro3,
                                    tipoDocumeto = dadosPassagem.tipoDocumentoPassageiro2,
                                    nDocumento = dadosPassagem.documentoPassageiro3
                                )
                            )
                        }
                    }
                } else {
                    append(getDadosVeiculo())
                }
            }
        }

        private fun getDadosPassageiros(
            nome: String,
            tipoDocumeto: String,
            nDocumento: String,
        ): String {
            return buildString {
                append(setLabel(LABEL_NOME))
                append(setCampo(nome))
                append(setLabelLineCampoAlignRight(LABEL_TIPO_DOCUMENTO, tipoDocumeto))
                append(setLabelLineCampoAlignRight(LABEL_N_DOCUMENTO, nDocumento))
//                append(setLabel(LABEL_DATA_NASCIMENTO))
//                append(setCampoAlignRight(dadosPassagem.dataNascimento1))
                append("\n")
            }
        }

        private fun getDadosVeiculo(): String {
            return buildString {
                if (dadosPassagem.nomeResponsavelRetirada.isNotBlank()) {
                    append(setLabel(LABEL_NOME))
                    append(setCampo(dadosPassagem.nomeResponsavelRetirada))
                }
                append(setLabelLineCampoAlignRight(LABEL_VEICULO_MODELO, dadosPassagem.modeloVeiculo))
                append(setLabelLineCampoAlignRight(LABEL_VEICULO_PLACA, dadosPassagem.placaVeiculo))
                append(setLabelLineCampoAlignRight(LABEL_VEICULO_COR, dadosPassagem.corVeiculo))
                append("\n")
            }
        }

        private fun getEmitidoEm(): String {
            return buildString {
                append(String(TXT_ALIGN_CT))
                append(String(TXT_BOLD_ON))
                append("$EMITIDO_EM: ")
                append(String(TXT_BOLD_OFF))
                append(setCampo(LocalDateTime.now().formatarDataHoraBarrasBr()))
                append("\n")
                append(String(TXT_ALIGN_LT))
                if (!IS_VIA_CLIENTE) {
                    append(setLabelLineCampoAlignRight(LABEL_AGENCIA, dadosPassagem.agencia))
                    append(setLabel(LABEL_OBS))
                    append(setCampo(dadosPassagem.observacao))
                }
                append("\n")
            }
        }

        fun getOperador(): String {
            return buildString {
                append(setLineLabelCampo(LABEL_OPERADOR, dadosPassagem.funcionario))
            }
        }

        private fun setTituloSecao(): String {
            return buildString {
                append(String(TXT_ALIGN_CT))
                append(String(TXT_BOLD_ON))
                append("$TITLE_DADOS_PASSAGEM\n")
                append("\n")
                append(String(TXT_BOLD_OFF))
                append(String(TXT_ALIGN_LT))
            }
        }

        private fun setLabel(label: String): String {
            return buildString {
                append(String(TXT_BOLD_ON))
                append("$label:\n")
                append(String(TXT_BOLD_OFF))
            }
        }

        private fun setCampo(campo: String): String {
            return buildString {
                append("$campo\n")
            }
        }

        private fun setLabelLineCampoAlignRight(label: String, campo: String): String {
            return buildString {
                append(String(TXT_ALIGN_CT))
                append(String(TXT_BOLD_ON))
                append("$label: ")
                append(String(TXT_BOLD_OFF))
                append(alignRightCampo(campo, label.length + 2, campo.length))
                append(String(TXT_ALIGN_LT))
            }
        }

        private fun alignRightCampo(campo: String, lengthLabel: Int, lengthCampo: Int): String {
            val offLabel = LINE_TOTAL_LENGTH - lengthLabel - lengthCampo
            return buildString {
                for (i in 0..offLabel step (1)) {
                    append(" ")
                }
                append(campo)
                append("\n")
            }
        }

        private fun setLineLabelCampo(label: String, campo: String): String {
            return buildString {
                append("$label: $campo\n")
            }
        }

        fun getComandoImpressao(): String {
            val comandoImpressao = buildString {
                append(getCabecalho())
                append(getNumeroBilhete())
                append(getDadosViagem())
                append(getAcomodacaoTipoVeiculo())
                append(getDadosValores())
                append(getDadosPassagem())
                append(getEmitidoEm())
            }

            return comandoImpressao
        }
    }
}