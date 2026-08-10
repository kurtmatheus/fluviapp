package dev.matheus.fluviapp.services.repository.firebase.documents

import dev.matheus.fluviapp.services.repository.firebase.DocumentoBruto

/**
 * Desserialização Map→Documento sem o `toObject` do Firestore (§10 Nível 2 — porta neutra). As chaves
 * são os nomes dos campos do `*Documento` (o Firestore mapeia por nome). Funções puras, testáveis; os
 * `to<Modelo>(id)` de cada Documento seguem fazendo a ponte Documento→modelo.
 */

// `toViagemDocumento` saiu na F8.0, com o último chamador: o repositório da Viagem-trecho. O
// snapshot dentro da Passagem sobreviveu como `ViagemCongeladaDocumento` — e lá ele é lido pelo
// `toObject` do Firestore, não por aqui. Mesmo descarte progressivo do `toFuncionarioDocumento`.

fun DocumentoBruto.toEmbarcacaoDocumento() = EmbarcacaoDocumento(
    nome = texto("nome"),
    tipo = texto("tipo"),
    capacidadeVeiculo = inteiro("capacidadeVeiculo"),
    capacidadeSuite2 = inteiro("capacidadeSuite2"),
    capacidadeSuite3 = inteiro("capacidadeSuite3"),
    capacidadeCamarote = inteiro("capacidadeCamarote"),
    empresaId = texto("empresaId"),
)

fun DocumentoBruto.toEmpresaDocumento() = EmpresaDocumento(
    nome = texto("nome"),
    razaoSocial = texto("razaoSocial"),
    cnpj = texto("cnpj"),
    endereco = texto("endereco"),
    telefone1 = texto("telefone1"),
    telefone2 = texto("telefone2"),
)

// `toFuncionarioDocumento` saiu na F6.2, com o último chamador: a Equipe lê por `DocumentoBruto.toFuncionario()`,
// como as quatro entidades já revitalizadas. Descarte progressivo — o que fica sem uso sai na fatia em que
// deixou de ser necessário, e não numa limpeza final.

fun DocumentoBruto.toConstanteDocumento() = ConstanteDocumento(
    descricao = texto("descricao"),
    categoria = texto("categoria"),
)

fun DocumentoBruto.toContadorDocumento() = ContadorDocumento(
    numeroBilhete = inteiro("numeroBilhete"),
)
