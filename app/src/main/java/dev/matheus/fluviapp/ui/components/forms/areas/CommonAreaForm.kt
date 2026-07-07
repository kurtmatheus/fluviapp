package dev.matheus.fluviapp.ui.components.forms.areas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.ui.components.forms.divider.FormDashedDivider
import dev.matheus.fluviapp.ui.components.texts.TextTitleBrownItalic

@Composable
fun CommonAreaForm(
    modifier: Modifier,
    titleArea: Int,
    contentArea: @Composable (Modifier) -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {

        TextTitleBrownItalic(
            text = stringResource(id = titleArea)
        )

        Spacer(modifier = modifier)
        contentArea(modifier)
        
        Spacer(modifier = modifier)
        FormDashedDivider(modifier = modifier.fillMaxWidth())
    }
}

@Preview(showBackground = true)
@Composable
fun CommonAreaFormPreview() {
        CommonAreaForm(
            modifier = Modifier,
            titleArea = R.string.form_area_title_passageiro
        ) {}
}