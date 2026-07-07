# Briefing — Projeto Portfólio: App de Check-in de Passageiros e Veículos (Embarque Marítimo)

## Contexto

Este projeto é uma **reformulação de um aplicativo desenvolvido originalmente para uma empresa** (o projeto não avançou). Antes de qualquer publicação pública (GitHub, portfólio), **o código e a documentação precisam ser completamente descaracterizados** — sem nome da empresa, sem nomes reais de clientes/parceiros, sem qualquer dado ou regra de negócio específica que possa ser identificada como pertencente a terceiros. O objetivo é usar a ideia e o domínio do problema (check-in de embarque) como estudo de caso genérico e fictício.

## Objetivo do projeto

Demonstrar expertise em **Jetpack Compose + Firebase**, com um caso de uso realista, não-trivial, que mostre modelagem de dados relacional aplicada a Compose + Firestore, geração de bilhete, e suporte a cenário de conectividade instável (offline-first) — diferencial técnico relevante para o domínio (embarque marítimo).

## Passo 0 — Descaracterização (fazer ANTES de tudo)

- [ ] Renomear o projeto para algo genérico: ex. **"Embarka"**, "SeaCheck", "PortoGo" (sugestões — livre para escolher outro nome fictício).
- [ ] Revisar todo o código-fonte original em busca de:
  - Nome da empresa/cliente em strings, comentários, nomes de pacote (`com.empresa.xxx` → `com.seuusuario.embarka`), configs do Firebase, etc.
  - Logos, cores de marca, ou qualquer asset visual da empresa original.
  - Regras de negócio muito específicas de um cliente real (ex: rotas reais, nomes de embarcações reais, tarifas reais) — substituir por dados fictícios (ex: "Rota Ilha Fictícia ↔ Porto Modelo").
  - Qualquer chave de API, credencial ou variável de ambiente real — remover e recriar projeto Firebase novo, do zero, só para o portfólio.
- [ ] Reescrever o README do zero contando a história como "estudo de caso pessoal inspirado em um problema real do setor de transporte marítimo", sem mencionar a empresa.

## Escopo funcional sugerido

### Core (obrigatório para o MVP de portfólio)
1. **Cadastro de viagem** — embarcação (fictícia), rota, data/horário, capacidade de passageiros e veículos.
2. **Check-in de passageiro** — nome, documento, vínculo à viagem, geração automática de número de bilhete.
3. **Check-in de veículo** (opcional/complementar) — placa, tipo de veículo, vínculo à viagem.
4. **Geração de bilhete com QR Code** — para simular leitura rápida no embarque.
5. **Listagem de passageiros/veículos por viagem** — visão do operador de check-in.

### Diferenciais técnicos (para reforçar "expertise", não só "funciona")
6. **Modo offline-first** — check-in funciona sem internet e sincroniza com Firestore quando a conexão volta (usar `Firestore` com persistência local habilitada, ou uma camada própria de cache com `Room` + fila de sincronização).
7. **Autenticação simples** (Firebase Auth) — diferenciar "operador de check-in" de "administrador" (quem cadastra viagens).
8. **Validação de capacidade** — impedir check-in além da capacidade da embarcação (regra de negócio simples, mas mostra cuidado com integridade de dados).
9. **(Opcional/ambicioso)** Versão desktop/tablet da tela de check-in usando **Compose Multiplatform**, reaproveitando lógica de domínio — reforça a narrativa de "mobile moderno multiplataforma" do seu currículo.

## Stack sugerida

- **UI:** Jetpack Compose (Material 3)
- **Backend/dados:** Firebase (Firestore + Auth), com persistência offline habilitada
- **Arquitetura:** MVVM ou MVI simples, com camada de repositório isolando Firestore (facilita trocar/mockar em testes)
- **Geração de QR Code:** biblioteca ZXing ou similar
- **Testes:** ao menos alguns testes unitários na camada de ViewModel/Repository (mostra maturidade, não precisa cobertura total)

## Estrutura de dados sugerida (Firestore)

```
viagens/{viagemId}
  - embarcacao: string
  - rota: string
  - dataHora: timestamp
  - capacidadePassageiros: number
  - capacidadeVeiculos: number

viagens/{viagemId}/passageiros/{passageiroId}
  - nome: string
  - documento: string
  - numeroBilhete: string
  - checkinEm: timestamp
  - status: string (checkin_pendente | checkin_realizado)

viagens/{viagemId}/veiculos/{veiculoId}
  - placa: string
  - tipo: string
  - checkinEm: timestamp
```

## Entregáveis para o portfólio/README

- Descrição do problema de negócio (genérico): "sistema de check-in digital para substituir processo manual de embarque em transporte marítimo".
- GIFs/screenshots do fluxo de check-in e geração de bilhete.
- Explicação da decisão de arquitetura offline-first (isso é o tipo de detalhe que impressiona recrutador técnico).
- Link do repositório + instruções de setup (com Firebase de teste, nunca credenciais reais).

## Prompt sugerido para começar no Claude Code

> "Tenho um app Android em Kotlin (Jetpack Compose) para check-in de passageiros/veículos em embarque marítimo, originalmente feito para uma empresa. Preciso: (1) revisar todo o código e remover qualquer referência à empresa original — nomes, pacotes, assets, credenciais; (2) generalizar o domínio para um cenário fictício; (3) reestruturar como projeto de portfólio pessoal com boas práticas (MVVM, offline-first com Firestore). Vamos começar revisando a estrutura atual do projeto."
