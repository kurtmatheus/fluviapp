package dev.matheus.fluviapp.ui.components.forms.fields

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color.Companion.Red
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.ui.theme.FluviAppTheme

@Composable
fun FormTextFieldBrownTrailingIcon(
    modifier: Modifier,
    value: String,
    label: Int,
    onValueChange: (String) -> Unit,
    keyboardOptions: KeyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
    visualTransformation: VisualTransformation = VisualTransformation.None,
    focusManager: FocusManager = LocalFocusManager.current,
    readOnly: Boolean = false,
    isError: Boolean = false,
    textoErro: Int = R.string.error_camp_obrig,
    onFocusChange: () -> Unit = {},
    trailingIcon: @Composable () -> Unit = {}
) {
    OutlinedTextField(
        modifier = modifier.onFocusChanged {
            if (it.isFocused) onFocusChange()
        },
        trailingIcon = trailingIcon,
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(id = label)) },
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = (KeyboardActions(
            onNext = {
                focusManager.moveFocus(FocusDirection.Next)
            }
        )),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = MaterialTheme.colorScheme.onBackground,
            unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
            focusedBorderColor = MaterialTheme.colorScheme.onBackground,
            unfocusedBorderColor = MaterialTheme.colorScheme.onBackground,
            focusedTrailingIconColor = MaterialTheme.colorScheme.onBackground,
            unfocusedTrailingIconColor = MaterialTheme.colorScheme.onBackground,
            focusedLabelColor = MaterialTheme.colorScheme.onBackground,
            unfocusedLabelColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
            errorTrailingIconColor = Red,
            errorBorderColor = Red,
            errorLabelColor = Red,
            errorSupportingTextColor = Red
        ),
        shape = RoundedCornerShape(16.dp),
        readOnly = readOnly,
        isError = isError,
        supportingText = { if (isError) Text(text = stringResource(id = textoErro)) },
    )
}


@Composable
fun FormTextFieldBrownLeadingIcon(
    modifier: Modifier,
    value: String,
    label: Int,
    onValueChange: (String) -> Unit,
    keyboardOptions: KeyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
    visualTransformation: VisualTransformation = VisualTransformation.None,
    focusManager: FocusManager = LocalFocusManager.current,
    readOnly: Boolean = false,
    enabled: Boolean = true,
    isError: Boolean = false,
    textoErro: Int = R.string.error_camp_obrig,
    onFocusChange: () -> Unit = {},
    leadingIcon: @Composable () -> Unit = {},
) {
    OutlinedTextField(
        modifier = modifier.onFocusChanged {
            if (it.isFocused) onFocusChange()
        },
        leadingIcon = leadingIcon,
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(id = label)) },
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = (KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) })),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = MaterialTheme.colorScheme.onBackground,
            unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
            focusedBorderColor = MaterialTheme.colorScheme.onBackground,
            unfocusedBorderColor = MaterialTheme.colorScheme.onBackground,
            focusedLabelColor = MaterialTheme.colorScheme.onBackground,
            unfocusedLabelColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
            focusedLeadingIconColor = MaterialTheme.colorScheme.onBackground,
            unfocusedLeadingIconColor = MaterialTheme.colorScheme.onBackground,
            disabledContainerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.7f),
            disabledLeadingIconColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
            errorLeadingIconColor = Red,
            errorBorderColor = Red,
            errorLabelColor = Red,
            errorSupportingTextColor = Red
        ),
        shape = RoundedCornerShape(16.dp),
        readOnly = readOnly,
        enabled = enabled,
        isError = isError,
        supportingText = { if (isError) Text(text = stringResource(id = textoErro)) }
    )
}

@Composable
fun FormTextFieldBrownLeadingTrailingIcon(
    modifier: Modifier,
    value: String,
    label: Int,
    onValueChange: (String) -> Unit,
    keyboardOptions: KeyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
    visualTransformation: VisualTransformation = VisualTransformation.None,
    focusManager: FocusManager = LocalFocusManager.current,
    readOnly: Boolean = false,
    isError: Boolean = false,
    textoErro: Int = R.string.error_camp_obrig,
    onFocusChange: () -> Unit = {},
    leadingIcon: @Composable () -> Unit = {},
    trailingIcon: @Composable () -> Unit = {}
) {
    OutlinedTextField(
        modifier = modifier.onFocusChanged {
            if (it.isFocused) onFocusChange()
        },
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(id = label)) },
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = (KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) })),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = MaterialTheme.colorScheme.onBackground,
            unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
            focusedBorderColor = MaterialTheme.colorScheme.onBackground,
            unfocusedBorderColor = MaterialTheme.colorScheme.onBackground,
            focusedTrailingIconColor = MaterialTheme.colorScheme.onBackground,
            unfocusedTrailingIconColor = MaterialTheme.colorScheme.onBackground,
            focusedLabelColor = MaterialTheme.colorScheme.onBackground,
            unfocusedLabelColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
            focusedLeadingIconColor = MaterialTheme.colorScheme.onBackground,
            unfocusedLeadingIconColor = MaterialTheme.colorScheme.onBackground,
            errorLeadingIconColor = Red,
            errorTrailingIconColor = Red,
            errorBorderColor = Red,
            errorLabelColor = Red,
            errorSupportingTextColor = Red
        ),
        shape = RoundedCornerShape(16.dp),
        readOnly = readOnly,
        isError = isError,
        supportingText = { if (isError) Text(text = stringResource(id = textoErro)) }
    )
}

@Composable
fun FormTextFieldBrownNoIcon(
    modifier: Modifier,
    value: String,
    label: Int,
    onValueChange: (String) -> Unit,
    keyboardOptions: KeyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
    visualTransformation: VisualTransformation = VisualTransformation.None,
    focusManager: FocusManager = LocalFocusManager.current,
    shape: Shape = RoundedCornerShape(16.dp),
    readOnly: Boolean = false,
    enabled: Boolean = true,
    isError: Boolean = false,
    textoErro: Int = R.string.error_camp_obrig,
    /**
     * Texto de apoio **abaixo do campo**, no lugar que o Material reserva para ele — usado para o que
     * comenta o valor sem ser erro (a confirmação do IBGE, por exemplo). O erro tem precedência: quando
     * há erro, é ele que ocupa a linha, porque duas mensagens ali competem e a que importa é a que
     * impede de salvar.
     */
    textoApoio: String? = null,
    onFocusChange: () -> Unit = {}

) {
    OutlinedTextField(
        modifier = modifier.onFocusChanged {
            if (it.isFocused) onFocusChange()
        },
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(id = label)) },
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = (KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) })),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = MaterialTheme.colorScheme.onBackground,
            unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
            focusedBorderColor = MaterialTheme.colorScheme.onBackground,
            unfocusedBorderColor = MaterialTheme.colorScheme.onBackground,
            focusedLabelColor = MaterialTheme.colorScheme.onBackground,
            unfocusedLabelColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
            disabledContainerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.7f),
            disabledLeadingIconColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
            errorBorderColor = Red,
            errorLabelColor = Red,
            errorSupportingTextColor = Red
        ),
        shape = shape,
        readOnly = readOnly,
        enabled = enabled,
        isError = isError,
        supportingText = {
            when {
                isError -> Text(text = stringResource(id = textoErro))
                textoApoio != null -> Text(text = textoApoio)
            }
        }
    )
}

/**
 * Igual ao [FormTextFieldBrownLeadingIcon], mas com **rótulo dinâmico** (`String`, não `@StringRes`) — p/
 * campos guiados por dado, como a tarifa por acomodação (ADR-0013), cujo rótulo vem do catálogo `Constante`
 * e não de um recurso fixo.
 */
@Composable
fun FormTextFieldBrownLeadingIconLabelText(
    modifier: Modifier,
    value: String,
    label: String,
    onValueChange: (String) -> Unit,
    keyboardOptions: KeyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
    focusManager: FocusManager = LocalFocusManager.current,
    isError: Boolean = false,
    textoErro: Int = R.string.error_camp_obrig,
    leadingIcon: @Composable () -> Unit = {},
) {
    OutlinedTextField(
        modifier = modifier,
        leadingIcon = leadingIcon,
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        keyboardOptions = keyboardOptions,
        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) }),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = MaterialTheme.colorScheme.onBackground,
            unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
            focusedBorderColor = MaterialTheme.colorScheme.onBackground,
            unfocusedBorderColor = MaterialTheme.colorScheme.onBackground,
            focusedLabelColor = MaterialTheme.colorScheme.onBackground,
            unfocusedLabelColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
            focusedLeadingIconColor = MaterialTheme.colorScheme.onBackground,
            unfocusedLeadingIconColor = MaterialTheme.colorScheme.onBackground,
            errorLeadingIconColor = Red,
            errorBorderColor = Red,
            errorLabelColor = Red,
            errorSupportingTextColor = Red,
        ),
        shape = RoundedCornerShape(16.dp),
        isError = isError,
        supportingText = { if (isError) Text(text = stringResource(id = textoErro)) },
    )
}

@Preview(showBackground = true)
@Composable
private fun FormTextFieldBrownTrailingIconPreview() {
    FluviAppTheme {
        Column {
            FormTextFieldBrownNoIcon(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                value = "",
                label = R.string.label_email,
                onValueChange = {},
                isError = true
            )

            FormTextFieldBrownNoIcon(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                value = "",
                label = R.string.label_email,
                onValueChange = {},
            )

            FormTextFieldBrownLeadingIcon(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                value = "",
                label = R.string.label_email,
                onValueChange = {},
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Person, contentDescription = null)
                }
            )

            FormTextFieldBrownTrailingIcon(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                value = "",
                label = R.string.label_email,
                onValueChange = {},
                trailingIcon = {
                    Icon(imageVector = Icons.Default.Person, contentDescription = null)
                },
                textoErro = R.string.error_camp_obrig
            )

            FormTextFieldBrownLeadingTrailingIcon(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                value = "",
                label = R.string.label_senha,
                onValueChange = {},
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Lock, contentDescription = null)
                },
                trailingIcon = {
                    Icon(imageVector = Icons.Default.Visibility, contentDescription = null)
                },
                textoErro = R.string.error_camp_obrig
            )

            FormTextFieldBrownLeadingIcon(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                value = "0",
                label = R.string.label_valor_pago,
                onValueChange = {},
                leadingIcon = {
                    Icon(imageVector = Icons.Default.AttachMoney, contentDescription = null)
                },
                enabled = false
            )
        }
    }
}