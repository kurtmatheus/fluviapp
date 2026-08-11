package dev.matheus.fluviapp.database

import androidx.room.Database
import androidx.room.RoomDatabase
import dev.matheus.fluviapp.database.dao.ContadorDao
import dev.matheus.fluviapp.database.dao.cadastro.ConstanteDao
import dev.matheus.fluviapp.database.dao.operacoes.UsuarioDao
import dev.matheus.fluviapp.database.dao.passagem.PassagemDao
import dev.matheus.fluviapp.database.dao.passagem.PassagemDigitalDao
import dev.matheus.fluviapp.database.dao.passagem.RascunhoPassagemDao
import dev.matheus.fluviapp.domain.ContadorBilhete
import dev.matheus.fluviapp.domain.cadastro.constantes.Constante
import dev.matheus.fluviapp.domain.operacoes.Usuario
import dev.matheus.fluviapp.database.PassagemEntity
import dev.matheus.fluviapp.domain.passagem.PassagemDigital

@Database(
    entities = [
        Usuario::class,
        Constante::class,
        PassagemEntity::class,
        ContadorBilhete::class,
        PassagemDigital::class,
        RascunhoPassagemEntity::class
    ],
    // v3: a tabela Empresa saiu — a coleção passou a viver só no Firestore (ADR-0017 D1, ADR-0020 F5).
    // v4: a Embarcação (ex-`Navio`) segue o mesmo caminho, pela mesma razão. O que sobra aqui é cache do
    // Firestore e o resíduo local (rascunho, bilhete digital) — nenhum deles é fonte da verdade.
    // v5: o Funcionário sai (F6.2), e a saída dele é a que destrava a forma nova — `vinculos` é lista, e
    // lista em tabela pediria TypeConverter e migração para um formato que muda na fatia seguinte.
    // v6: `PassagemEntity.agenciaId` (F7).
    // v7: a `Viagem` do trecho disfarçado e a `TarifaViagem` caem (F8.0) — as ÚLTIMAS tabelas que eram
    // espelho de coleção. O que resta aqui ou é resíduo local (rascunho, bilhete digital) ou espera a
    // vez dele (Usuario, Constante, PassagemEntity, contador).
    version = 7,
    exportSchema = true
)
abstract class FluviAppDatabase : RoomDatabase() {
    abstract fun usuarioDao(): UsuarioDao
    abstract fun constanteDao(): ConstanteDao
    abstract fun passagemDao(): PassagemDao
    abstract fun contadorDao(): ContadorDao
    abstract fun passagemDigitalDao(): PassagemDigitalDao
    abstract fun rascunhoPassagemDao(): RascunhoPassagemDao
}