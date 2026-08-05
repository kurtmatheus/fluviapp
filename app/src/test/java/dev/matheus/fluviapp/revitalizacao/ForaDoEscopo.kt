package dev.matheus.fluviapp.revitalizacao

/**
 * **Marcador de suíte**: esta classe de teste cobre código que ainda não foi revitalizado.
 *
 * A regra da revitalização (ADR-0020) é que o app vale uma entidade por vez — hoje a **Empresa** —, e que
 * só precisa passar o que é testável para a entidade em foco. Um teste vermelho de passagem, viagem ou
 * embarcacao não é regressão: é código que ainda não foi refeito, e consertá-lo de passagem seria trabalho
 * jogado fora quando a vez dele chegar.
 *
 * Uma classe está **dentro** do escopo quando o código que ela testa é executado em alguma parte da
 * jornada da entidade viva — do login ao painel, do painel ao formulário, do formulário ao Firestore.
 * Não é "tem Empresa no nome": `SincronizarColecaoTest` e `DocumentoBrutoMappersTest` estão dentro porque
 * o repositório de empresas passa por eles; `ExtrairDocumentoFormatadoTest` está fora porque as funções
 * que ele cobre só têm chamador em passagem.
 *
 * ### Como usar
 *
 * `@Category(ForaDoEscopo::class)` na classe. O Gradle a exclui de `testDebugUnitTest` (veja
 * `app/build.gradle.kts`). Para medir o que falta, rode a suíte inteira:
 *
 * ```
 * gradlew :app:testDebugUnitTest -PsuiteCompleta
 * ```
 *
 * **Revitalizar uma entidade é apagar as anotações das classes dela** — e o marcador some do projeto
 * quando a última sair, do mesmo modo que `SECOES_REVITALIZADAS` some quando igualar `SecaoMenu.entries`.
 */
interface ForaDoEscopo