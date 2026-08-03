package dev.matheus.fluviapp.domain.cadastro.constantes

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import dev.matheus.fluviapp.domain.IObjetoSimplificado

@Entity(indices = [Index("id")])
data class Constante(
    @PrimaryKey
    override val id: String,
    override val descricaoNome: String,
    val categoria: String,
) : IObjetoSimplificado {
    enum class Descricao {
        //Passagem
        INTEIRA,
        MEIA,
        GRATUIDADE,

        //Gratuidade
        CORTESIA,
        
        //Acomodacao
        REDE,
        SUITE,
        CAMAROTE,

        //Documento
        CPF,
        CNPJ,
        CNH,
        PASSAPORTE,
        RG,

        //Veiculo
        CARRO,
        MOTO,
        CAMINHAO,
        CARRETA,

        //FORMA PAGAMENTO
        PIX,
        DINHEIRO,
        DEBITO,
        CREDITO,

        //Categoria Passagem
        VEICULO,
        PASSAGEIRO,

        //Situacao Passagem
        A_EMITIR,
        EMITIDA
    }

    enum class Categoria {
        MUNICIPIO,
        DOCUMENTO,
        VEICULO,
        CATEGORIA_PASSAGEM,
        STATUS_PASSAGEM,
        ACOMODACAO,
        TIPO_PASSAGEM,
        GRATUIDADE,
        PAGAMENTO
    }
}

fun Constante.Descricao.obterDescricaoFormatada(): String {
    return this.name.replace("_", " ")
}

fun Constante.Categoria.obterCategoriaFormatada(): String {
    return this.name.replace("_", " ")
}