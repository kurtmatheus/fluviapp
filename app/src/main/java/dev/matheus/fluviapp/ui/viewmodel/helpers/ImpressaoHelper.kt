package dev.matheus.fluviapp.ui.viewmodel.helpers

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.Log
import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.business.ImpressaoPassagem
import dev.matheus.fluviapp.extensions.toastMessage
import dev.matheus.fluviapp.domain.passagem.StatusPassagem
import dev.matheus.fluviapp.domain.screendata.DadosImpressora
import dev.matheus.fluviapp.services.printerservice.PrinterService
import dev.matheus.fluviapp.services.printerservice.printer.ThermalPrinterConnection
import dev.matheus.fluviapp.services.repository.firebase.PassagemFirestoreRepository
import dev.matheus.fluviapp.ui.states.ImpressaoState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking

class ImpressaoHelper(
    private val uiState: MutableStateFlow<ImpressaoState>,
    private val passagemRepository: PassagemFirestoreRepository
) {

    private lateinit var context: Context

    fun atualizaSituacao(ehEmissaoDigital: Boolean = false) {
        if (!ImpressaoPassagem.IS_VIA_CLIENTE || ehEmissaoDigital) {
            runBlocking {
                try {
                    passagemRepository.transicionar(
                        idPassagem = ImpressaoPassagem.dadosPassagem.idPassagem,
                        novo = StatusPassagem.EMITIDA
                    )
                } catch (e: Exception) {
                    context.toastMessage(context.resources.getString(R.string.error_falha_passagem))
                }
            }
        }
    }

    private fun atualizarIsPrinting() {
        uiState.update {
            it.copy(
                isPrinting = !it.isPrinting
            )
        }
    }

    fun atualizarViaCliente(viaCliente: Boolean) {
        ImpressaoPassagem.IS_VIA_CLIENTE = viaCliente
    }

    fun atualizarExibirDialogViaEmbarcacao() {
        uiState.update {
            it.copy(
                isShowDialogImpressaoViaEmbarcacao = !it.isShowDialogImpressaoViaEmbarcacao
            )
        }
    }

    fun validarImprimir(
        context: Context,
    ) {
        this.context = context
        if (ImpressaoState.isPrinterSelected) {
            imprimir(context)
        } else {
            verificarImpressoras(context)
        }
    }

    private fun imprimir(
        context: Context,
    ) {
        atualizarIsPrinting()
        val thermalPrinterConnection = ThermalPrinterConnection(
            context = context,
            deviceAdress = ImpressaoState.impressoraSelecionada.endereco
        )
        try {
            runBlocking {
                val printerService = PrinterService(thermalPrinterConnection)

                if (thermalPrinterConnection.isOpen) {
                    printerService.printLn(ImpressaoPassagem.getComandoImpressao())
                    printerService.setTextAlignCenter()
                    printQrCode(printerService)
                    if (ImpressaoPassagem.IS_VIA_CLIENTE) {
                        printOperador(printerService = printerService, ImpressaoPassagem.VIA_CLIENTE)
                    } else {
                        printOperador(printerService = printerService, ImpressaoPassagem.VIA_EMPRESA)
                    }
                }
                context.toastMessage(context.resources.getString(R.string.msg_emissao_bem_sucedida))
                printerService.close()
                atualizarIsPrinting()
                atualizarExibirDialogViaEmbarcacao()
                atualizaSituacao()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception: ${e.message}", e)
            atualizarIsPrinting()
            context.toastMessage(context.resources.getString(R.string.error_emissao))
        }
    }

    private fun printOperador(printerService: PrinterService, via: String) {
        printerService.printLn(ImpressaoPassagem.getOperador())
        printerService.printLn(via)
        printerService.printLn("\n")
    }

    private fun printQrCode(printerService: PrinterService) {
        printerService.printLn(ImpressaoPassagem.LABEL_ID)
        printerService.printQRCode(ImpressaoPassagem.dadosPassagem.idPassagem, 200)

    }

    fun navigateBluetoothConfig(context: Context) {
        val activity = context as Activity
        val intentBluetooth = Intent().setAction(Settings.ACTION_BLUETOOTH_SETTINGS)
        activity.startActivityForResult(intentBluetooth, 0)
    }

    private fun atualizarListaImpressoras(listaImpressorasPareadas: MutableList<DadosImpressora>) {
        uiState.update {
            it.copy(
                listaImpressorasPareadas = listaImpressorasPareadas
            )
        }
    }

    internal fun atualizarExibirDialogSelecionarImpressora() {
        uiState.update {
            it.copy(
                exibirDialogImpressoras = !it.exibirDialogImpressoras
            )
        }
    }

    internal fun selecionarImpressora(dadosImpressora: DadosImpressora) {
        ImpressaoState.isPrinterSelected = true
        ImpressaoState.impressoraSelecionada = dadosImpressora
        atualizarExibirDialogSelecionarImpressora()
    }

    @SuppressLint("MissingPermission")
    private fun verificarImpressoras(context: Context) {
        val bluetoothManager =
            context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter

        val listaImpressorasPareadas = mutableListOf<DadosImpressora>()

        bluetoothAdapter.bondedDevices.toList().forEach {
            if (it.bluetoothClass.majorDeviceClass == 1536) {
                listaImpressorasPareadas.add(
                    DadosImpressora(
                        nome = it.name,
                        endereco = it.address
                    )
                )
            }
        }

        if (listaImpressorasPareadas.isEmpty()) {
            navigateBluetoothConfig(context = context)
        } else {
            atualizarListaImpressoras(listaImpressorasPareadas)
            atualizarExibirDialogSelecionarImpressora()
        }
    }

    companion object {
        private const val TAG = "impressaoHelper"
    }
}
