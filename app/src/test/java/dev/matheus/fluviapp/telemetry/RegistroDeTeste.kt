package dev.matheus.fluviapp.telemetry

/**
 * Um [RegistroCadastro] sobre um [FakeTelemetry] descartável — para os testes que **constroem** um
 * ViewModel de formulário sem se interessar pela telemetria dele.
 *
 * Existe porque o registrador virou dependência dos oito formulários em 2026-09-07 ([ADR-0032] D4), e
 * dezessete construções em teste passaram a precisar de um. Escrever `RegistroCadastro(FakeTelemetry())`
 * em cada uma diria, dezessete vezes, uma coisa que não é o assunto de nenhuma delas.
 *
 * Quem **testa** o registro não usa isto: monta o próprio fake e o inspeciona — é o que fazem
 * `RegistroCadastroTest` e os testes de falha dos formulários.
 */
fun registroCadastroDeTeste() = RegistroCadastro(FakeTelemetry())
