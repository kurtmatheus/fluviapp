package br.com.gruponaveg.di.module

import android.content.Context
import androidx.room.Room
import br.com.gruponaveg.database.NavegAppDatabase
import br.com.gruponaveg.database.dao.ContadorDao
import br.com.gruponaveg.database.dao.cadastro.ConstanteDao
import br.com.gruponaveg.database.dao.cadastro.passagem.AgenteDao
import br.com.gruponaveg.database.dao.cadastro.viagem.EmpresaDao
import br.com.gruponaveg.database.dao.cadastro.viagem.NavioDao
import br.com.gruponaveg.database.dao.cadastro.viagem.ViagemDao
import br.com.gruponaveg.database.dao.operacoes.UsuarioDao
import br.com.gruponaveg.database.dao.passagem.PassagemDao
import br.com.gruponaveg.database.dao.passagem.PassagemDigitalDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private const val DATABASE_NAME = "navegApp.db"

@Module
@InstallIn(SingletonComponent::class)
class DatabaseModule {

    @Singleton
    @Provides
    fun provideDatabase(@ApplicationContext context: Context): NavegAppDatabase {
        return Room.databaseBuilder(
            context,
            NavegAppDatabase::class.java,
            DATABASE_NAME
        ).build()
    }

    @Provides
    fun provideUsuarioDao(db: NavegAppDatabase): UsuarioDao {
        return db.usuarioDao()
    }

    @Provides
    fun provideConstanteConteudoDao(db: NavegAppDatabase): ConstanteDao {
        return db.constanteDao()
    }

    @Provides
    fun provideEmpresaDao(db: NavegAppDatabase): EmpresaDao {
        return db.empresaDao()
    }

    @Provides
    fun provideNavioDao(db: NavegAppDatabase): NavioDao {
        return db.navioDao()
    }

    @Provides
    fun provideViagemDao(db: NavegAppDatabase): ViagemDao {
        return db.viagemDao()
    }

    @Provides
    fun provideAgenteDao(db: NavegAppDatabase): AgenteDao {
        return db.agenteDao()
    }

    @Provides
    fun providePassagemDao(db: NavegAppDatabase): PassagemDao {
        return db.passagemDao()
    }

    @Provides
    fun provideContadorDao(db: NavegAppDatabase): ContadorDao {
        return db.contadorDao()
    }

    @Provides
    fun providePassagemDigitalDao(db: NavegAppDatabase): PassagemDigitalDao {
        return db.passagemDigitalDao()
    }
}