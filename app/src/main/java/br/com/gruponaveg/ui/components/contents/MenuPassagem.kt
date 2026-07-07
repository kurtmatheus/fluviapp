package br.com.gruponaveg.ui.components.contents

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import br.com.gruponaveg.R
import br.com.gruponaveg.model.screendata.DadosBotoesMenus
import br.com.gruponaveg.sampledata.listaBotoesMenuPassagensSample
import br.com.gruponaveg.sampledata.listaBotoesMenuViagensSample
import br.com.gruponaveg.ui.components.cards.CardBotaoMenu

@Composable
fun MenuPassagem(
    modifier: Modifier,
    titulo: Int,
    listaBotoes: List<DadosBotoesMenus> = emptyList()
) {
    Column {
        CommonTopRow(modifier = modifier, titulo = titulo)

        LazyColumn {
            items(listaBotoes) {
                CardBotaoMenu(modifier = modifier, dados = it)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ConteudoMenuPassagensPreview() {
        MenuPassagem(
            modifier = Modifier,
            titulo = R.string.subtitle_menu_passagens,
            listaBotoes = listaBotoesMenuPassagensSample
        )
}
