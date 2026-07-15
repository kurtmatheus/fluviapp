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

/**
 * v3 → v4: `Passagem` passa a registrar quem vendeu (agência/agente), antes descartados no
 * save (ADR-0002/0003). Habilita a derivação da capability no PassagemDadosPassagemMapper.
 */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE Passagem ADD COLUMN agencia TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE Passagem ADD COLUMN agente TEXT NOT NULL DEFAULT ''")
    }
}

/**
 * v4 → v5: remove a coluna `senha` da `Usuario` (ADR-0005: sem senha no device). SQLite não
 * garante DROP COLUMN no minSdk 26, então recria a tabela (padrão Room). `Usuario` é cache do
 * Firestore `users`, então recriar é seguro (re-sincroniza no login).
 */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `Usuario_novo` (`id` TEXT NOT NULL, `email` TEXT NOT NULL, " +
                "`nome` TEXT NOT NULL, `cargo` TEXT NOT NULL, `ultimoUsuarioLogado` INTEGER NOT NULL, " +
                "PRIMARY KEY(`id`))"
        )
        db.execSQL(
            "INSERT INTO `Usuario_novo` (`id`, `email`, `nome`, `cargo`, `ultimoUsuarioLogado`) " +
                "SELECT `id`, `email`, `nome`, `cargo`, `ultimoUsuarioLogado` FROM `Usuario`"
        )
        db.execSQL("DROP TABLE `Usuario`")
        db.execSQL("ALTER TABLE `Usuario_novo` RENAME TO `Usuario`")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_Usuario_id` ON `Usuario` (`id`)")
    }
}

/**
 * v5 → v6: `Navio.empresaId` — link estável para Empresa (ADR-0008, Fase 0/1). Mesmo trade-off da
 * 1→2: schemaless de graça no Firestore, ALTER TABLE tipado no Room. Aditiva e não-destrutiva;
 * `empresa` (nome) segue dormente até a Fase 3.
 */
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE Navio ADD COLUMN empresaId TEXT NOT NULL DEFAULT ''")
    }
}

/**
 * v6 → v7: remove `Navio.empresa` (nome) — ADR-0008 Fase 3: o vínculo com Empresa é só por
 * `empresaId`. SQLite não garante DROP COLUMN no minSdk 26, então recria a tabela (padrão da 4→5).
 * Navio é espelho do Firestore, então recriar é seguro (re-sincroniza).
 */
val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `Navio_novo` (`id` TEXT NOT NULL, `descricaoNome` TEXT NOT NULL, " +
                "`capacidadeVeiculo` INTEGER NOT NULL, `capacidadeSuite2` INTEGER NOT NULL, " +
                "`capacidadeSuite3` INTEGER NOT NULL, `capacidadeCamarote` INTEGER NOT NULL, " +
                "`empresaId` TEXT NOT NULL, PRIMARY KEY(`id`))"
        )
        db.execSQL(
            "INSERT INTO `Navio_novo` (`id`, `descricaoNome`, `capacidadeVeiculo`, `capacidadeSuite2`, " +
                "`capacidadeSuite3`, `capacidadeCamarote`, `empresaId`) " +
                "SELECT `id`, `descricaoNome`, `capacidadeVeiculo`, `capacidadeSuite2`, " +
                "`capacidadeSuite3`, `capacidadeCamarote`, `empresaId` FROM `Navio`"
        )
        db.execSQL("DROP TABLE `Navio`")
        db.execSQL("ALTER TABLE `Navio_novo` RENAME TO `Navio`")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_Navio_id` ON `Navio` (`id`)")
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
        ).addMigrations(
            MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7,
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