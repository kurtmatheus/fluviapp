package dev.matheus.fluviapp.di.module

import android.content.Context
import androidx.room.Room
import dev.matheus.fluviapp.database.FluviAppDatabase
import dev.matheus.fluviapp.database.dao.ContadorDao
import dev.matheus.fluviapp.database.dao.cadastro.ConstanteDao
import dev.matheus.fluviapp.database.dao.cadastro.passagem.AgenteDao
import dev.matheus.fluviapp.database.dao.cadastro.viagem.EmpresaDao
import dev.matheus.fluviapp.database.dao.cadastro.viagem.NavioDao
import dev.matheus.fluviapp.database.dao.cadastro.viagem.ViagemDao
import dev.matheus.fluviapp.database.dao.operacoes.UsuarioDao
import dev.matheus.fluviapp.database.dao.passagem.PassagemDao
import dev.matheus.fluviapp.database.dao.passagem.PassagemDigitalDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private const val DATABASE_NAME = "fluviApp.db"

@Module
@InstallIn(SingletonComponent::class)
class DatabaseModule {

    @Singleton
    @Provides
    fun provideDatabase(@ApplicationContext context: Context): FluviAppDatabase {
        return Room.databaseBuilder(
            context,
            FluviAppDatabase::class.java,
            DATABASE_NAME
        ).build()
    }

    @Provides
    fun provideUsuarioDao(db: FluviAppDatabase): UsuarioDao {
        return db.usuarioDao()
    }

    @Provides
    fun provideConstanteConteudoDao(db: FluviAppDatabase): ConstanteDao {
        return db.constanteDao()
    }

    @Provides
    fun provideEmpresaDao(db: FluviAppDatabase): EmpresaDao {
        return db.empresaDao()
    }

    @Provides
    fun provideNavioDao(db: FluviAppDatabase): NavioDao {
        return db.navioDao()
    }

    @Provides
    fun provideViagemDao(db: FluviAppDatabase): ViagemDao {
        return db.viagemDao()
    }

    @Provides
    fun provideAgenteDao(db: FluviAppDatabase): AgenteDao {
        return db.agenteDao()
    }

    @Provides
    fun providePassagemDao(db: FluviAppDatabase): PassagemDao {
        return db.passagemDao()
    }

    @Provides
    fun provideContadorDao(db: FluviAppDatabase): ContadorDao {
        return db.contadorDao()
    }

    @Provides
    fun providePassagemDigitalDao(db: FluviAppDatabase): PassagemDigitalDao {
        return db.passagemDigitalDao()
    }
}