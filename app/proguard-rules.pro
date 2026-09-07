# Regras do R8 — ligado em 2026-09-07, com `isMinifyEnabled` e `isShrinkResources`.
#
# O arquivo era o template do Android Studio, com tudo comentado, e ficou assim enquanto o R8 esteve
# desligado: `proguardFiles` estava declarado e nunca foi consultado.
#
# ## Por que ele é curto
#
# O que o R8 quebra é o que ele não consegue enxergar, e isso é quase sempre **reflexão**: a classe é
# alcançada por nome em tempo de execução, o encolhedor não vê a aresta, renomeia o campo e a leitura
# passa a devolver vazio — sem erro, sem log, sem pista.
#
# O app não tem mais nenhuma. As duas últimas — o `toObject` do perfil e o `.set(UsuarioDocumento(...))`
# do primeiro acesso — viraram `Map` na mesma fatia que ligou o R8, e o Gson saiu com o Room. A fronteira
# do Firestore inteira é `Map` desde o ADR-0025, então **não há data class cujo nome de campo precise
# sobreviver**. Preferiu-se não ter o que proteger a escrever `-keep` protegendo reflexão.
#
# O que as bibliotecas precisam vem delas: Firebase, ZXing, Hilt/Dagger e Compose publicam as próprias
# regras em `consumer-rules`, que o R8 aplica sem que este arquivo precise repeti-las. Copiar `-keep` de
# tutorial é o jeito mais silencioso de desligar o R8 sem desligá-lo.

# --- Rastro de pilha legível no Crashlytics ---
#
# Sem isto o relatório chega com nomes trocados e sem número de linha, e um crash de produção vira
# adivinhação. O `SourceFile` é renomeado para não expor a estrutura de pacotes; o mapping que o
# plugin do Crashlytics envia é o que devolve o nome original.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# --- Exceções do domínio ---
#
# `EmissaoException`, `QRCodeException` e `BarcodeSizeException` são lançadas e capturadas por tipo, então
# o R8 as segue sozinho. O que se perde sem esta regra é o **nome** delas no log: `Log.e(..., e)` imprime
# a classe, e um `a.b.c` no lugar de `QRCodeException` custa exatamente o tempo de descobrir qual erro é.
-keepnames class dev.matheus.fluviapp.exceptions.** { *; }
