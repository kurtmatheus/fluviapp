package dev.matheus.fluviapp.database

import androidx.room.Database
import androidx.room.RoomDatabase
import dev.matheus.fluviapp.database.dao.ContadorDao
import dev.matheus.fluviapp.database.dao.cadastro.ConstanteDao
import dev.matheus.fluviapp.database.dao.operacoes.FuncionarioDao
import dev.matheus.fluviapp.database.dao.cadastro.viagem.NavioDao
import dev.matheus.fluviapp.database.dao.cadastro.viagem.TarifaViagemDao
import dev.matheus.fluviapp.database.dao.cadastro.viagem.ViagemDao
import dev.matheus.fluviapp.database.dao.operacoes.UsuarioDao
import dev.matheus.fluviapp.database.dao.passagem.PassagemDao
import dev.matheus.fluviapp.database.dao.passagem.PassagemDigitalDao
import dev.matheus.fluviapp.database.dao.passagem.RascunhoPassagemDao
import dev.matheus.fluviapp.domain.ContadorBilhete
import dev.matheus.fluviapp.domain.cadastro.constantes.Constante
import dev.matheus.fluviapp.domain.operacoes.Funcionario
import dev.matheus.fluviapp.domain.operacoes.Usuario
import dev.matheus.fluviapp.domain.passagem.Passagem
import dev.matheus.fluviapp.domain.passagem.PassagemDigital
import dev.matheus.fluviapp.domain.viagem.Navio
import dev.matheus.fluviapp.domain.viagem.TarifaViagem
import dev.matheus.fluviapp.domain.viagem.Viagem

@Database(
    entities = [
        Usuario::class,
        Constante::class,
        Navio::class,
        Funcionario::class,
        Viagem::class,
        TarifaViagem::class,
        Passagem::class,
        ContadorBilhete::class,
        PassagemDigital::class,
        RascunhoPassagemEntity::class
    ],
    // v3: a tabela Empresa saiu — a coleção passou a viver só no Firestore (ADR-0017 D1, ADR-0020 F5).
    version = 3,
    exportSchema = true
)
abstract class FluviAppDatabase : RoomDatabase() {
    abstract fun usuarioDao(): UsuarioDao
    abstract fun constanteDao(): ConstanteDao
    abstract fun navioDao(): NavioDao
    abstract fun viagemDao(): ViagemDao
    abstract fun tarifaViagemDao(): TarifaViagemDao
    abstract fun funcionarioDao(): FuncionarioDao
    abstract fun passagemDao(): PassagemDao
    abstract fun contadorDao(): ContadorDao
    abstract fun passagemDigitalDao(): PassagemDigitalDao
    abstract fun rascunhoPassagemDao(): RascunhoPassagemDao
}