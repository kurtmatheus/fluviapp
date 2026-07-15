package dev.matheus.fluviapp.database

import androidx.room.Database
import androidx.room.RoomDatabase
import dev.matheus.fluviapp.database.dao.ContadorDao
import dev.matheus.fluviapp.database.dao.cadastro.ConstanteDao
import dev.matheus.fluviapp.database.dao.cadastro.passagem.AgenteDao
import dev.matheus.fluviapp.database.dao.cadastro.viagem.EmpresaDao
import dev.matheus.fluviapp.database.dao.cadastro.viagem.NavioDao
import dev.matheus.fluviapp.database.dao.cadastro.viagem.ViagemDao
import dev.matheus.fluviapp.database.dao.operacoes.UsuarioDao
import dev.matheus.fluviapp.database.dao.passagem.PassagemDao
import dev.matheus.fluviapp.database.dao.passagem.PassagemDigitalDao
import dev.matheus.fluviapp.database.dao.passagem.RascunhoPassagemDao
import dev.matheus.fluviapp.model.ContadorBilhete
import dev.matheus.fluviapp.model.cadastro.constantes.Constante
import dev.matheus.fluviapp.model.cadastro.passagem.Agente
import dev.matheus.fluviapp.model.operacoes.Usuario
import dev.matheus.fluviapp.model.passagem.Passagem
import dev.matheus.fluviapp.model.passagem.PassagemDigital
import dev.matheus.fluviapp.model.viagem.Empresa
import dev.matheus.fluviapp.model.viagem.Navio
import dev.matheus.fluviapp.model.viagem.Viagem

@Database(
    entities = [
        Usuario::class,
        Constante::class,
        Empresa::class,
        Navio::class,
        Agente::class,
        Viagem::class,
        Passagem::class,
        ContadorBilhete::class,
        PassagemDigital::class,
        RascunhoPassagemEntity::class
    ],
    version = 7,
    exportSchema = false
)
abstract class FluviAppDatabase : RoomDatabase() {
    abstract fun usuarioDao(): UsuarioDao
    abstract fun constanteDao(): ConstanteDao
    abstract fun empresaDao(): EmpresaDao
    abstract fun navioDao(): NavioDao
    abstract fun viagemDao(): ViagemDao
    abstract fun agenteDao(): AgenteDao
    abstract fun passagemDao(): PassagemDao
    abstract fun contadorDao(): ContadorDao
    abstract fun passagemDigitalDao(): PassagemDigitalDao
    abstract fun rascunhoPassagemDao(): RascunhoPassagemDao
}