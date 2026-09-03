# Documentação do FluviApp — por onde entrar

Esta pasta guarda **como o app pensa**, não como ele se usa. São três coisas, e elas têm autoridades
diferentes:

| Onde | O que é | Autoridade |
|---|---|---|
| [`adr/`](adr/) | **decisões** tomadas, com o contexto que as justificou e o custo assumido | **máxima** — ADR não se apaga; quando muda de ideia, escreve-se outro |
| [`design/`](design/) | **estudos** que mediram o código e prepararam decisão | nenhuma — estudo propõe, ADR decide |
| [`historico.md`](historico.md) | a **linha do tempo**: em que ordem as decisões aconteceram e o que cada leva mudou | narrativa — não decide nada, explica por que a ordem foi essa |

> **Régua de precedência, em uma linha:** *ADR vence estudo; ADR mais novo vence ADR mais velho no ponto em
> que se cruzam — nunca no documento inteiro.*

---

## Os três índices

- **[`adr/README.md`](adr/README.md) — o índice de vigência.** Responde *o que ainda vale*, ADR por ADR, com
  a coluna que importa: **o que caiu, e por quem**. Tem também a tabela de **vocabulário vencido**, que é o
  que evita ler um ADR antigo com as palavras de hoje. **Consulte-o antes de citar qualquer ADR anterior ao
  0020** — quase todos foram emendados em algum ponto.
- **[`design/README.md`](design/README.md) — o índice de estudos**, dividido em cinco eixos (domínio, dados,
  apresentação, operação, produto). Diz de que cada documento trata e **quanto ele ainda vale**.
- **[`historico.md`](historico.md) — a linha do tempo.** Não substitui os dois acima; serve para a pergunta
  que eles não respondem: *por que essa decisão veio nessa hora?*

## Por onde começar, conforme a pergunta

| Se a pergunta é… | Comece por |
|---|---|
| *o que o negócio é* | [`design/dominio-da-plataforma.md`](design/dominio-da-plataforma.md) — o catálogo completo de entidades, tipos e regras puras |
| *como a passagem funciona* | [`design/dominio-passagem.md`](design/dominio-passagem.md), depois os ADRs **0023 a 0031** |
| *o que ainda falta construir* | [`design/mvp-roadmap.md`](design/mvp-roadmap.md) e a seção *O que está esperando decisão* do índice de ADRs |
| *por que isto está assim* | [`historico.md`](historico.md), e daí para o ADR da data |
| *posso mexer nisto?* | o índice de vigência — a coluna *o que caiu* costuma responder antes de o código responder |

## Como escrever o próximo

1. **O estudo vem antes** (`design/`), mapeando o código **como está** — com arquivo e linha.
2. **O ADR registra o que foi decidido e por quê**, não o que seria bonito. Nome:
   `NNNN-titulo-curto.md`, sequencial.
3. Superou algo? **Diga onde** — seção e decisão —, nunca *"supera o ADR-XX"* inteiro.
4. **Atualize os índices na mesma leva** — o de ADRs e, se o ADR fechou um estudo, o de estudos.

> A regra 4 tem cicatriz. A revisão de 2026-09-03 encontrou **sete estudos** marcados como *aberto* que
> ADRs já haviam fechado, e um que dizia bloquear uma fase concluída havia semanas. Nenhuma dessas linhas
> estava errada quando foi escrita — todas envelheceram sozinhas, e um índice que envelhece **mente com ar
> de autoridade**.

## Notas de leitura

- **A numeração começa no 0002.** O `0001` é o exemplo preenchido dentro de
  [`adr-template.md`](../adr-template.md), na raiz do repositório, e nunca virou arquivo próprio.
- **Vocabulário muda mais que decisão.** `Agente` virou `Funcionario`; `Navio` virou `Embarcacao`;
  `Trecho` foi dissolvido; `Constante`/`Catalogo` não existem. A tabela de vocabulário no índice de ADRs
  traduz — e é o primeiro lugar a olhar quando um documento antigo parece dizer algo estranho.
- **Estudo fechado não é estudo errado.** Ele fica como o caminho até a decisão, e às vezes é onde está a
  medição que o ADR só resume.
