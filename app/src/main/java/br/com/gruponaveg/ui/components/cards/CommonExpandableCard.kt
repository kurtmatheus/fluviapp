package br.com.gruponaveg.ui.components.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun CommonExpandableCard(
    modifier: Modifier,
    roundedCornerShape: Boolean,
    contentHeader: @Composable (ImageVector) -> Unit,
    contentExpand: @Composable () -> Unit
) {
    var expanded by remember {
        mutableStateOf(false)
    }

    var modifierCard = modifier
        .fillMaxWidth()
        .height(60.dp)
    var dropDownIcon = Icons.Filled.KeyboardArrowDown

    if (expanded) {
        modifierCard = modifier
            .fillMaxWidth()
            .heightIn(60.dp)
        dropDownIcon = Icons.Filled.KeyboardArrowUp

    }

    Card(
        modifier = modifierCard,
        onClick = {},
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondary,
            contentColor = MaterialTheme.colorScheme.onSecondary
        ),
        shape = if (!roundedCornerShape) RoundedCornerShape(0) else CardDefaults.shape,
        enabled = false
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp)
                    .background(MaterialTheme.colorScheme.secondary)
                    .clickable {
                        expanded = !expanded
                    },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                contentHeader(dropDownIcon)
            }
            contentExpand()
        }
    }
}