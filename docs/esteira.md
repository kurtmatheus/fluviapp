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

## Distribuir

Pela esteira, criando uma tag:

```
git tag v0.0.2-alpha02 && git push origin v0.0.2-alpha02
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

O `versionCode` vem de `VERSION_CODE` quando a esteira o define (o número da execução), e cai num valor
fixo localmente. Dois artefatos com o mesmo código são indistinguíveis na lista do tester e no
Crashlytics.

## O que continua manual, e não é pendência de build

**Cada tester precisa de dois passos no console**, porque o autocadastro saiu na P2.2c e o seed foi
removido:

1. **Authentication** → criar a conta (e-mail e senha);
2. **Firestore → `users`** → criar documento cujo **ID é o uid da conta**, com `papel` e `funcionarioId`.

O `uid` é **por projeto**: um documento trazido de outro projeto Firebase não casa, e o sintoma é o app
autenticar e dizer que a pessoa não está cadastrada. Foi exatamente o que travou o acesso em 2026-08-03.

Isso deixa de ser manual quando o painel administrativo existir (ADR-0016) — é a razão de ele ser o
Pilar 3.A, e de a distribuição vir depois dele no roadmap.