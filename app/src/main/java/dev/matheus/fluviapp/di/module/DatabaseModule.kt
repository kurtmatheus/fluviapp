package dev.matheus.fluviapp.di.module

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.google.gson.Gson
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
import dev.matheus.fluviapp.database.dao.passagem.RascunhoPassagemDao
import dev.matheus.fluviapp.services.repository.rascunho.RascunhoPassagemStoreRoom
import dev.matheus.fluviapp.services.repository.rascunho.RascunhoStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private const val DATABASE_NAME = "fluviApp.db"

/**
 * v1 → v2: capability [Agente.podeSelecionarFormaPagamento] (ver ADR-0002/0003).
 * Demonstra o trade-off SQL×NoSQL: no Firestore (AgenteDocumento) o campo entrou
 * de graça (schemaless); no Room tipado ele custa este ALTER TABLE. Migração
 * não-destrutiva de propósito — preserva PassagemDigital (única entidade local-only).
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE Agente ADD COLUMN podeSelecionarFormaPagamento INTEGER NOT NULL DEFAULT 0"
        )
    }
}

/**
 * v2 → v3: tabela do rascunho de passagem (memória cacheada em JSON — ADR-0004). Slot único
 * (id, json). Aditiva e não-destrutiva.
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `rascunho_passagem` " +
                "(`id` INTEGER NOT NULL, `json` TEXT NOT NULL, PRIMARY KEY(`id`))"
        )
    }
}

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
        ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build()
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

    @Provides
    fun provideRascunhoPassagemDao(db: FluviAppDatabase): RascunhoPassagemDao {
        return db.rascunhoPassagemDao()
    }

    @Provides
    @Singleton
    fun provideGson(): Gson {
        return Gson()
    }

    @Provides
    @Singleton
    fun provideRascunhoStore(impl: RascunhoPassagemStoreRoom): RascunhoStore {
        return impl
    }
}