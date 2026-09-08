# Esteira: de implementar a distribuir

Como um commit vira um APK na mão de um tester, e o que precisa existir para isso não depender de
ninguém lembrar de rodar nada.

## Os três workflows

| Arquivo | Dispara em | O que faz |
|---|---|---|
| `.github/workflows/ci.yml` | push, PR | suíte de regras (escopo) + testes JVM (escopo) + build debug |
| `.github/workflows/regras.yml` | mudou `firestore.rules` ou a suíte | **suíte completa**; publica as regras só por acionamento manual |
| `.github/workflows/distribuicao.yml` | tag `v*` | gate + APK assinado + App Distribution |

Duas escolhas que valem explicação:

**O CI roda o escopo; o deploy de regras roda tudo.** O recorte da revitalização (ADR-0020) diz o que o
*aplicativo* exercita hoje — e é por isso que "vermelho" volta a significar "quebrei algo de pé". Mas a
regra publicada vale para o **banco inteiro**, inclusive coleções cujo app ainda não foi refeito. O
servidor não tem o luxo do recorte, então lá o gate é `SUITE_COMPLETA=1`.

**A publicação das regras não é automática.** Regra de segurança que sobe sozinha a cada push é deploy de
produção sem ninguém olhando. O workflow testa em todo push e só publica quando alguém pede
(`workflow_dispatch` com `publicar: true`).

## Pré-requisitos

### 1. Um remoto git

Não existe ainda. Sem ele os workflows são documentação. O repositório é portfólio, portanto público — e é
por isso que `google-services.json`, `*.jks` e `keystore.properties` são gitignored, e chegam à esteira
como secrets.

### 2. Secrets do repositório

Em *Settings → Secrets and variables → Actions*:

| Secret | Como obter |
|---|---|
| `GOOGLE_SERVICES_JSON` | `base64 -w0 app/google-services.json` |
| `KEYSTORE_BASE64` | `base64 -w0 ~/keys/fluviapp-release.jks` |
| `KEYSTORE_PASSWORD` | a senha do keystore |
| `KEY_ALIAS` | `fluviapp` |
| `KEY_PASSWORD` | a senha da chave |
| `FIREBASE_SERVICE_ACCOUNT` | conteúdo do JSON da service account (ver abaixo) |
| `FIREBASE_APP_ID` | `1:401291798654:android:dd00a1600619f6832c102f` |
| `FIREBASE_TESTERS` | e-mails separados por vírgula |

No PowerShell, o base64 sai assim:

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("app\google-services.json"))
```

> **Cuidado com o app id.** O `google-services.json` tem **dois**: o de `br.com.fluviapp` (o certo, o
> `applicationId` atual) e o de `dev.matheus.fluviapp`, herdado do pacote antigo. Distribuir pelo segundo
> entrega o APK a um app que ninguém tem instalado, sem erro nenhum.

### 3. Service account

Console do Google Cloud do projeto `fluvi-app-dev` → *IAM → Contas de serviço → Criar*. Papéis:

- **Firebase App Distribution Admin** — para distribuir;
- **Firebase Rules Admin** — para publicar as regras.

Gere uma chave JSON e cole o conteúdo inteiro em `FIREBASE_SERVICE_ACCOUNT`. Ela substitui o
`firebase login` interativo, que não existe num runner.

## Homologação e produção

Não há dois ambientes: há **um projeto Firebase e dois grupos de testers**. O que separa os canais é a
**forma da tag**, na convenção do SemVer — pré-lançamento tem hífen:

| Tag | Canal | Grupo |
|---|---|---|
| `v0.0.3-rc.1` | homologação | `homologacao` |
| `v0.0.3` | produção | `producao` |
| (workflow_dispatch) | ensaio | `homologacao` |

**Aprovar é criar a tag sem sufixo apontando para o commit já homologado.** Não existe promoção do
binário: o `google-services.json` fica embutido no APK, então o que se promove é o commit, e a esteira
recompila. Como projeto e configuração são os mesmos, o artefato de produção difere apenas em versão — o
`versionCode` (número do run) e o `versionName` (a tag) sobem sozinhos, que é o "bump" da aprovação.

A audiência é administrada por **grupos no console**, não por lista de e-mails em secret: quem entra ou
sai da homologação não exige mexer no repositório.

> **Quando isto deixar de bastar.** No dia em que o dado passar a importar, homologação e produção
> compartilhando o mesmo Firestore vira problema — um tester apagando uma empresa apaga a de todos. A
> saída é um segundo projeto (`fluvi-app-prod`) com *product flavors* e `applicationIdSuffix`, ao custo
> de outro bootstrap de admin, outro deploy de regras e secrets duplicados. Enquanto não há usuário nem
> dado que doa perder, isso é complexidade paga por um risco que ainda não existe.

## Distribuir

Pela esteira, criando uma tag — **anotada** (`-a`), porque a mensagem dela é o que o tester vai ler:

```
git tag -a v0.0.3-rc.1 -m "o que mudou" && git push origin v0.0.3-rc.1   # homologação
git tag -a v0.0.3      -m "o que mudou" && git push origin v0.0.3        # produção, depois de aprovada
```

> **A nota da versão sai da mensagem da tag, e não do nome dela** (desde 2026-09-05). Até a `rc.14` o
> tester recebia `v0.0.5-rc.14` como nota — o nome do ref, que não diz o que testar —, enquanto a
> descrição da entrega ficava no repositório, que é justamente onde o tester não está. A esteira passa a
> ler o corpo da tag e a mandá-lo por `--release-notes-file`; **arquivo e não variável**, porque a
> mensagem é multilinha e multilinha atravessando `$GITHUB_OUTPUT` quebra pela crase de um trecho de
> código. Se a tag for **leve** (sem `-a`), não há corpo para ler e sobra o nome, que é o comportamento
> antigo — nada falha, só se perde a nota. No `workflow_dispatch`, vale o campo *notas* do formulário.

À mão, da máquina (foi assim que a primeira entrega saiu):

```
gradlew :app:assembleRelease
firebase appdistribution:distribute app/build/outputs/apk/release/app-release.apk ^
  --app 1:401291798654:android:dd00a1600619f6832c102f ^
  --testers seu@email.com --release-notes "..."
```

Localmente exige `keystore.properties` preenchido (veja `keystore.properties.example`) e `firebase login`
feito. Sem o `keystore.properties`, o release ainda compila — só sai sem assinatura, e APK sem assinatura
não instala em lugar nenhum.

**A identidade do artefato sai da esteira, não do arquivo:**

| Campo | Na esteira | Local |
|---|---|---|
| `versionCode` | `git rev-list --count HEAD` (contagem de commits) | valor fixo do `build.gradle.kts` |
| `versionName` | `VERSION_NAME` = a tag, sem o `v` | valor fixo do `build.gradle.kts` |

> **Por que não `github.run_number`.** Ele é um contador *por workflow* e começa em 1 — a primeira
> distribuição saiu com `versionCode=1` e o Android recusou a instalação por downgrade, porque o aparelho
> já tinha o 10 de um build local. Também zera se o workflow for renomeado. A contagem de commits é
> monotônica pela natureza do histórico e igual em qualquer máquina (exige `fetch-depth: 0` no checkout).

Dois artefatos com o mesmo `versionCode` são indistinguíveis na lista do tester e no Crashlytics. E o
nome vir da tag evita a divergência silenciosa que existia antes: bastava esquecer de subir a linha do
`versionName` para o APK dizer `alpha01` numa entrega marcada como `alpha02`, e o tester relatar um bug
numa versão que não existe. Num `workflow_dispatch` (sem tag) o nome cai no fallback — o `ref_name` ali é
o nome da branch, e um APK chamado "master" não diz nada a ninguém.

## O que o release carrega — R8 e recursos

**Desde 2026-09-07 o release é minificado.** `isMinifyEnabled` e `isShrinkResources` estão ligados no
`buildTypes.release`, e o efeito é grande porque nada disso estava ligado antes:

| | sem R8 | com R8 |
|---|---|---|
| APK assinado | 19.343.421 B (18,45 MB) | **4.182.807 B (3,99 MB)** |
| `res/` dentro do APK | 2,20 MB | **0,13 MB** |

O corte de recurso tem um dono nomeável: os **46 PNGs da marca antiga** (`naveg_logo1_*`/`naveg_logo2_*`)
eram 2,04 MB e nenhum código os referencia. O `shrinkResources` os tira do release **sem apagá-los do
disco** — eles seguem no diretório de trabalho, fora do artefato.

### A ordem que fez isso ser seguro

O que o R8 quebra é o que ele **não enxerga**, e isso é quase sempre reflexão: a classe é alcançada por
nome em tempo de execução, o encolhedor não vê a aresta, renomeia o campo, e a leitura passa a devolver
vazio — sem erro, sem log, sem pista. Um bug assim atravessa o build verde e aparece no aparelho do tester.

Por isso a reflexão saiu **antes** de o R8 entrar, e eram três pontos: o `toObject` do perfil, o
`toObject` do funcionário e o `.set(UsuarioDocumento(...))` do primeiro acesso — este último achado pelo
compilador durante a troca, não pela medição. Os três viraram `Map`, como o resto da fronteira desde o
ADR-0025; o Gson já tinha saído junto com o Room. **O app não tem mais serialização por reflexão**, e é por
isso que o `proguard-rules.pro` tem duas regras e não vinte.

O que existe nele:

- `-keepattributes SourceFile,LineNumberTable` + `-renamesourcefileattribute` — sem elas o Crashlytics
  chega com nomes trocados e sem número de linha, e um crash de produção vira adivinhação. O `mapping.txt`
  que o plugin do Crashlytics envia é o que devolve o nome original;
- `-keepnames` nas exceções do domínio, para que `QRCodeException` apareça no log com esse nome.

O que **não** existe, de propósito: `-keep` copiado de tutorial. Firebase, ZXing, Hilt e Compose publicam
as próprias regras em `consumer-rules`, que o R8 aplica sozinho. Acumular `-keep` preventivo é o jeito
mais silencioso de desligar o R8 sem desligá-lo.

### Verificação, e por que ela é manual

**Nenhum teste automatizado deste projeto roda sobre release.** `connectedDebugAndroidTest` roda em debug,
onde o R8 não passou — então uma suíte verde não diz nada sobre o artefato que o tester recebe. A
verificação de um release minificado é instalar e percorrer: abrir, entrar, emitir, ler o QR, ver o
bilhete.

Ao instalar localmente, dois tropeços conhecidos:

```
INSTALL_FAILED_UPDATE_INCOMPATIBLE   # debug e release têm assinaturas diferentes: desinstale antes
INSTALL_FAILED_VERSION_DOWNGRADE     # o release local usa a contagem de commits; o debug usa 10
```

Ou seja: instalar um release local **impede** o `connectedDebugAndroidTest` seguinte até desinstalar. Se o
`adb uninstall` falhar, `adb shell pm uninstall --user 0 br.com.fluviapp`.

### O que ficou medido de quebra

Os únicos `.so` que o release empacota são `libandroidx.graphics.path` e `libdatastore_shared_counter`, e
**ambos estão alinhados a 16 KB** (`LOAD align = 0x4000`). A dívida que bloqueava o `targetSdk 35` era o
`.so` do CameraX a 4 KB, e o CameraX saiu quando o leitor de QR passou a ser o ZXing. O `targetSdk`
continua em **34** — subi-lo muda comportamento no Android 15, e isso é decisão, não consequência.

## O que continua manual, e não é pendência de build

**Só o `ADM` precisa dos dois passos no console.** Para `GESTOR` e `OPERADOR`, quem convida é o app —
seção **Usuários → Novo usuário** (F6.6): o `ADM` grava um convite em `convites/{email}`, a pessoa entra
com o próprio primeiro acesso e cria a senha dela. O `users/{uid}` nasce com o papel **do convite**.

Os dois passos, então, valem para **o administrador**:

1. **Authentication** → criar a conta (e-mail e senha);
2. **Firestore → `users`** → criar documento cujo **ID é o uid da conta**, com `papel` e `funcionarioId`.

O `uid` é **por projeto**: um documento trazido de outro projeto Firebase não casa, e o sintoma é o app
autenticar e dizer que a pessoa não está cadastrada. Foi exatamente o que travou o acesso em 2026-08-03.

**E o `ADM` continua manual por decisão** (ADR-0021 D0, preservado pela
[ADR-0032](adr/0032-o-acesso-politica-sessao-e-ciclo-de-vida.md) D6): quem administra a plataforma nasce
**fora do aplicativo**. Não é lacuna a tapar — é o mesmo princípio que tirou o autocadastro (P2.2c) e
removeu o seed, e ele passou a estar escrito na regra: o convite **recusa** `papel: "ADM"`, no formulário
e no servidor. Não há convite de `ADM`, logo também não há promoção a `ADM`.

*Este trecho mandava fazer os dois passos para todo tester, e ficou vencido de 2026-08-08 (a F6.6, que fez
o papel vir do convite) até 2026-09-08.*

**O que o app passou a fazer, e economiza console:** desativar e reativar o acesso de alguém, e definir
data de expiração (ADR-0032 D6) — na mesma seção Usuários. Um tester que sai de um ciclo de testes se
desativa pelo app; o acesso dele para de valer **no servidor**, e não só na tela.