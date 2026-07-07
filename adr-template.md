# Template de ADR (Architecture Decision Record)

> Use um arquivo por decisão, dentro de `docs/adr/` em cada projeto.
> Nomeie como `0001-titulo-curto.md`, `0002-titulo-curto.md`, etc. (numeração sequencial).
> O objetivo NÃO é documentar tudo — só as decisões que tiveram trade-off real, onde outra escolha era plausível.

---

## ADR-000X: [Título curto da decisão]

**Status:** Aceita | Rejeitada | Substituída por ADR-000Y

**Contexto**
Qual problema ou situação levou a essa decisão precisar ser tomada? (2-4 frases)

**Opções consideradas**
1. Opção A — breve descrição
2. Opção B — breve descrição
3. Opção C (se houver)

**Decisão**
Qual opção foi escolhida e por quê, em termos objetivos (não "porque é melhor", mas "porque resolve X sem o custo Y que a opção Z teria").

**Consequências**
O que essa escolha custa ou limita no futuro? Toda decisão de arquitetura tem trade-off — nomear o custo é o que separa análise de arquiteto de opinião de desenvolvedor.

**Alternativas futuras**
Em que cenário essa decisão deveria ser revisitada? (ex: "se o volume de usuários passar de X" ou "se precisarmos suportar múltiplos idiomas")

---

### Exemplo preenchido (referência de tom/nível de detalhe)

## ADR-0001: Persistência offline-first com Firestore ao invés de backend próprio

**Status:** Aceita

**Contexto**
O app de check-in é usado em embarque marítimo, cenário com conectividade instável ou inexistente no momento do check-in. Os dados de passageiro/veículo não podem se perder nem exigir reenvio manual quando a conexão retorna.

**Opções consideradas**
1. Backend próprio (REST API + banco relacional) com sincronização manual implementada do zero.
2. Firestore com persistência offline nativa habilitada.
3. Room (SQLite local) como fonte única, sem sincronização em tempo real.

**Decisão**
Optado por Firestore com persistência offline habilitada. Resolve sincronização automática sem precisar implementar fila de retry manualmente, e mantém consistência eventual entre dispositivos quando múltiplos operadores fazem check-in na mesma viagem.

**Consequências**
Acopla o projeto ao ecossistema Firebase (custo de troca de provedor no futuro é alto). Modelagem de dados precisa respeitar limitações do Firestore (ex: queries compostas limitadas), o que exige desnormalização em alguns pontos.

**Alternativas futuras**
Se o projeto precisar de queries complexas de relatório (ex: cruzamento analítico entre múltiplas viagens/rotas), vale reavaliar um banco relacional complementar (ex: exportar para BigQuery/Postgres para análise), mantendo Firestore só na ponta operacional.
