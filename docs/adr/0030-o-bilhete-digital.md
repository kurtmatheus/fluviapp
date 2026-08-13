# ADR-0030: O bilhete digital — o documento do passageiro, e o arquivo que nasce ao ser visto

**Status:** Aceita (decisões do analista em 2026-08-13) · implementada na mesma leva

**Estudo que preparou:** [`docs/design/bilhete-digital.md`](../design/bilhete-digital.md)

---

## Contexto

A F9.2 apagou o bilhete digital com o resto do caminho antigo, e a F9.5 deixou um botão sem destino: o passo
6 da emissão oferecia **Bilhete digital** e a navegação recebia um callback vazio.

O estudo mediu as quatro peças demolidas e separou o que volta do que não volta. Este ADR registra as cinco
decisões que faltavam — todas do analista — e o que elas implicam.

Duas decisões anteriores continuam valendo sem discussão: o bilhete vai para a **galeria** com nome derivado
do `idPassagem` ([ADR-0017](0017-eixo-de-storage-firestore-only.md) D5) e o **QR carrega o id**, porque é
ponteiro ([ADR-0012](0012-ciclo-de-vida-passagem-e-embarque-qr.md)).

## Decisão

### D1 — O bilhete **não mascara** o documento, e a conferência mascara

*"Bilhete não é mascarado."*

A assimetria com o [ADR-0029] (onde o detalhamento passou a mascarar, por LGPD) é deliberada, e a régua é
**quem está por perto**:

- o **detalhamento** é lido numa tela de balcão voltada para a fila. Mascarar protege de terceiros — a pessoa
  atrás, a foto de tela alheia — e os dígitos finais bastam para conferir contra o cartão na mão;
- o **bilhete** é entregue a quem já sabe o próprio número, e é conferido contra a identidade na doca. Um
  comprovante com o documento cortado deixa de servir para o que comprovante serve.

Mascarar aqui seria proteger a pessoa dela mesma, e não protegeria de ninguém.

### D2 — O arquivo nasce **ao ser visto**

*"Salva ao ver."*

Não há botão de salvar. Pré-visualizar e ter o arquivo na galeria são o mesmo ato, e a razão é de balcão: um
segundo gesto é um gesto a ser esquecido com a fila esperando — e o bilhete que não foi salvo é o bilhete que
o passageiro não tem.

**Consequência técnica que essa decisão dá de graça:** como a tela **já desenha** o bilhete, a captura grava
**o que está na tela**. O caminho antigo renderizava duas vezes — uma num `Dialog` para o operador ver, outra
idêntica e escondida para capturar —, e duas renderizações do mesmo documento é como elas divergem.

### D3 — **Compartilhar continua**, e é o gesto que entrega

*"Compartilhar continua."*

Com o arquivo na galeria, o `ACTION_SEND` deixou de ser o **único** caminho para o passageiro receber — mas
continua sendo o caminho **direto**. Ele fica na navegação, e não no ViewModel: compartilhar é gesto de
plataforma, e o ViewModel desta casa não conhece `Context` ([ADR-0026](0026-orquestracao-e-apresentacao-da-passagem.md) D3).

### D4 — Reabrir **procura antes** de regenerar

*"Procurar antes."*

O caso comum do balcão é o passageiro voltar dizendo que perdeu o bilhete. Procurar o arquivo pelo nome
derivado do id é uma consulta ao `MediaStore`; regenerar é desenhar de novo o que já existe.

Regenerar continua **legítimo** e é exatamente o que acontece quando a busca não acha nada — porque o arquivo
é **cache de conveniência** e o dado de origem está no Firestore (ADR-0017 D5). *Arquivo ausente não é erro.*

### D5 — **É o mesmo bilhete** nos dois momentos, então ele tem destino próprio

*"Mesmo bilhete."*

Quem acabou de emitir e quem for buscar uma passagem antiga chegam ao **mesmo destino** (`bilhete/{id}`). A
alternativa — o bilhete como pedaço da tela de emissão — obrigaria a consulta futura a ganhar outra tela
desenhando o mesmo documento, e **dois lugares desenhando a mesma coisa é como eles passam a divergir**.

Custa uma rota agora; não custa nada quando a consulta voltar.

## Consequências

- **o `ColetorDeReferencias.completas` ganha seu consumidor.** Ele foi escrito na F9.4 com os dois regimes de
  junção e nunca havia sido chamado — o bilhete é ele, porque nada é congelado no agregado (ADR-0023 D8) e o
  nome de quem viaja só existe resolvendo o pool;
- **o índice local não volta**, e com ele some a última razão de a tabela `PassagemDigital` existir;
- **nenhuma permissão é pedida**: gravar na coleção de imagens do próprio app dispensa permissão desde a API
  29. O `WRITE_EXTERNAL_STORAGE` do manifesto é resíduo do caminho antigo — **fica marcado para sair**, e não
  sai aqui só porque o caminho de compatibilidade (API 28) ainda o usa;
- **a marca vetorizada importa mais aqui do que em qualquer tela**: o bilhete **vira imagem**, e um PNG de
  origem escalado chega serrilhado ao arquivo que o passageiro guarda;
- **o bilhete não tem estado próprio**: ele é sempre desenhado da `Passagem`. Cancelar, embarcar ou corrigir
  não "invalidam" um arquivo já salvo — quem valida lê o servidor pelo QR, e é lá que o estado mora.

## O que este ADR não decide

- **a impressão física e as vias** (navio, agência, cliente) — ADR próprio, por decisão do analista
  ([ADR-0029](0029-os-fluxos-da-emissao.md) D5), junto com o estudo da bobina térmica;
- **a tela de detalhes de uma passagem já emitida** e a **pesquisa**, que voltam depois — e que apontarão
  para este mesmo destino (D5);
- **retenção e custo de armazenamento** das imagens: sem consumidor planejado que pague essa conta, medir
  aqui seria a projeção que o [ADR-0025](0025-camada-de-dados-da-passagem.md) recusa por método.

## Referências

- [`docs/design/bilhete-digital.md`](../design/bilhete-digital.md) — o estudo, com as quatro peças medidas
- [ADR-0017](0017-eixo-de-storage-firestore-only.md) D5 — galeria e nome derivado; arquivo ausente é regenerar
- [ADR-0012](0012-ciclo-de-vida-passagem-e-embarque-qr.md) — o QR como ponteiro
- [ADR-0015](0015-rework-agente-equipe.md) §5 — a marca da agência assina o documento
- [ADR-0029](0029-os-fluxos-da-emissao.md) D5 — o desfecho da emissão, e o que fica para o ADR da impressão