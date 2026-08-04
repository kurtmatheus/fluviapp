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

Pela esteira, criando uma tag:

```
git tag v0.0.3-rc.1 && git push origin v0.0.3-rc.1     # homologação
git tag v0.0.3      && git push origin v0.0.3          # produção, depois de aprovada
```

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

## O que continua manual, e não é pendência de build

**Cada tester precisa de dois passos no console**, porque o autocadastro saiu na P2.2c e o seed foi
removido:

1. **Authentication** → criar a conta (e-mail e senha);
2. **Firestore → `users`** → criar documento cujo **ID é o uid da conta**, com `papel` e `funcionarioId`.

O `uid` é **por projeto**: um documento trazido de outro projeto Firebase não casa, e o sintoma é o app
autenticar e dizer que a pessoa não está cadastrada. Foi exatamente o que travou o acesso em 2026-08-03.

**E continua manual por decisão** (ADR-0021 D0): a administração da plataforma vive **fora do aplicativo**.
Não é lacuna a tapar — é o mesmo princípio que tirou o autocadastro (P2.2c), impede qualquer cliente de
criar ou promover um `ADM` (anti-escalonamento) e removeu o seed. Não existe caminho, dentro do app, para
fabricar quem administra.

O que pode deixar de ser manual um dia é o **convite ao operador**, que é problema diferente: para ele o
primeiro acesso já deduz o vínculo pelo `Funcionario` de mesmo e-mail. O administrador continua nascendo
no console.