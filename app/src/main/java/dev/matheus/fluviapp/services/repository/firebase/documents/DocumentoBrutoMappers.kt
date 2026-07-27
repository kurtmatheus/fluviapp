package dev.matheus.fluviapp.services.repository.firebase.documents

import dev.matheus.fluviapp.services.repository.firebase.DocumentoBruto

/**
 * Desserialização Map→Documento sem o `toObject` do Firestore (§10 Nível 2 — porta neutra). As chaves
 * são os nomes dos campos do `*Documento` (o Firestore mapeia por nome). Funções puras, testáveis; os
 * `to<Modelo>(id)` de cada Documento seguem fazendo a ponte Documento→modelo.
 */

fun DocumentoBruto.toViagemDocumento() = ViagemDocumento(
    codigo = texto("codigo"),
    origem = texto("origem"),
    destino = texto("destino"),
    empresa = texto("empresa"),
    navio = texto("navio"),
    empresaId = texto("empresaId"),
    navioId = texto("navioId"),
    tarifas = mapaDeDoubles("tarifas"),
)

fun DocumentoBruto.toNavioDocumento() = NavioDocumento(
    nome = texto("nome"),
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

fun DocumentoBruto.toFuncionarioDocumento() = FuncionarioDocumento(
    nome = texto("nome"),
    agencia = texto("agencia"),
    lotacao = texto("lotacao"),
)

fun DocumentoBruto.toConstanteDocumento() = ConstanteDocumento(
    descricao = texto("descricao"),
    categoria = texto("categoria"),
)

fun DocumentoBruto.toContadorDocumento() = ContadorDocumento(
    numeroBilhete = inteiro("numeroBilhete"),
)
