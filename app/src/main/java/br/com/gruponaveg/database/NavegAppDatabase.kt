package br.com.gruponaveg.database

import androidx.room.Database
import androidx.room.RoomDatabase
import br.com.gruponaveg.database.dao.ContadorDao
import br.com.gruponaveg.database.dao.cadastro.ConstanteDao
import br.com.gruponaveg.database.dao.cadastro.passagem.AgenteDao
import br.com.gruponaveg.database.dao.cadastro.viagem.EmpresaDao
import br.com.gruponaveg.database.dao.cadastro.viagem.NavioDao
import br.com.gruponaveg.database.dao.cadastro.viagem.ViagemDao
import br.com.gruponaveg.database.dao.operacoes.UsuarioDao
import br.com.gruponaveg.database.dao.passagem.PassagemDao
import br.com.gruponaveg.database.dao.passagem.PassagemDigitalDao
import br.com.gruponaveg.model.ContadorBilhete
import br.com.gruponaveg.model.cadastro.constantes.Constante
import br.com.gruponaveg.model.cadastro.passagem.Agente
import br.com.gruponaveg.model.operacoes.Usuario
import br.com.gruponaveg.model.passagem.Passagem
import br.com.gruponaveg.model.passagem.PassagemDigital
import br.com.gruponaveg.model.viagem.Empresa
import br.com.gruponaveg.model.viagem.Navio
import br.com.gruponaveg.model.viagem.Viagem

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
        PassagemDigital::class
    ],
    version = 1,
    exportSchema = false
)
abstract class NavegAppDatabase : RoomDatabase() {
    abstract fun usuarioDao(): UsuarioDao
    abstract fun constanteDao(): ConstanteDao
    abstract fun empresaDao(): EmpresaDao
    abstract fun navioDao(): NavioDao
    abstract fun viagemDao(): ViagemDao
    abstract fun agenteDao(): AgenteDao
    abstract fun passagemDao(): PassagemDao
    abstract fun contadorDao(): ContadorDao
    abstract fun passagemDigitalDao(): PassagemDigitalDao
}