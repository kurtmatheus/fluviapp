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
import dev.matheus.fluviapp.database.dao.cadastro.viagem.TarifaViagemDao
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

/**
 * v7 → v8: `Viagem.empresaId`/`navioId` — vínculo vivo por id (ADR-0008), replicando o piloto do
 * Navio nas relações da Viagem. Aditiva; os nomes (empresa/navio) seguem, pois são substrato do
 * snapshot da Passagem e da derivação do código (por isso não é Fase 3/drop aqui).
 */
val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE Viagem ADD COLUMN empresaId TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE Viagem ADD COLUMN navioId TEXT NOT NULL DEFAULT ''")
    }
}

/**
 * v8 → v9: remove `Viagem.empresa`/`navio` (nomes) — ADR-0008 Fase 3: o vínculo com Empresa/Navio é
 * só por id. Os nomes são resolvidos na fronteira (código + snapshot da Passagem). Recria a tabela
 * (SQLite não garante DROP COLUMN no minSdk 26; padrão da 4→5/6→7). Espelho do Firestore → seguro.
 */
val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `Viagem_novo` (`id` TEXT NOT NULL, `codigo` TEXT NOT NULL, " +
                "`origem` TEXT NOT NULL, `destino` TEXT NOT NULL, `empresaId` TEXT NOT NULL, " +
                "`navioId` TEXT NOT NULL, PRIMARY KEY(`id`))"
        )
        db.execSQL(
            "INSERT INTO `Viagem_novo` (`id`, `codigo`, `origem`, `destino`, `empresaId`, `navioId`) " +
                "SELECT `id`, `codigo`, `origem`, `destino`, `empresaId`, `navioId` FROM `Viagem`"
        )
        db.execSQL("DROP TABLE `Viagem`")
        db.execSQL("ALTER TABLE `Viagem_novo` RENAME TO `Viagem`")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_Viagem_id` ON `Viagem` (`id`)")
    }
}

/**
 * v9 → v10: `Passagem.viagemId` — ponteiro estável p/ a Viagem (ADR-0008, Fase 2 da Passagem). A
 * Passagem passa a relacionar por id, mantendo o snapshot por valor (codigoViagem/empresa/navio/…).
 * Aditiva e não-destrutiva (padrão da 3→4/5→6/7→8) — preserva PassagemDigital (única local-only).
 */
val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE Passagem ADD COLUMN viagemId TEXT NOT NULL DEFAULT ''")
    }
}

/**
 * v10 → v11: `Passagem.navioId`/`empresaId` — ids congelados no snapshot (ADR-0008, Fase 2 da
 * Passagem). O balanço passa a agregar por navioId (frozen), não pela Viagem viva. Aditiva/não-
 * destrutiva. empresaId entra dormente (relação Passagem→Empresa por id é alvo futuro).
 */
val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE Passagem ADD COLUMN navioId TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE Passagem ADD COLUMN empresaId TEXT NOT NULL DEFAULT ''")
    }
}

/**
 * v11 → v12: `Passagem.funcionarioId` — dono estável (uid do criador) para autorização por
 * identidade (ADR-0010 Fase 2), fechando o débito de posse-por-nome do ADR-0008. Aditiva e não-
 * destrutiva (padrão da 9→10/10→11). Sem backfill: app de portfólio, passagens nascem em runtime já
 * carimbadas; bilhetes anteriores ficam com id vazio (tratados como "sem dono" no gate de edição).
 */
val MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE Passagem ADD COLUMN funcionarioId TEXT NOT NULL DEFAULT ''")
    }
}

/**
 * v12 → v13: registro do embarque na `Passagem` (ADR-0012): `embarcadaPorId` (uid do operador que
 * validou o QR), `embarcadaPor` (nome, snapshot de exibição) e `embarcadaEm` (quando). Aditiva e não-
 * destrutiva (padrão da 9→10/10→11/11→12). Sem backfill: bilhetes anteriores ficam com "" (nunca
 * embarcados via QR).
 */
val MIGRATION_12_13 = object : Migration(12, 13) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE Passagem ADD COLUMN embarcadaPorId TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE Passagem ADD COLUMN embarcadaPor TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE Passagem ADD COLUMN embarcadaEm TEXT NOT NULL DEFAULT ''")
    }
}

/**
 * v13 → v14: tabela-filha `TarifaViagem` (ADR-0013) — tabela de tarifas da Viagem na forma normalizada
 * (uma linha por `viagemId`+`chave`), o lado "SQL" do trade-off do ADR-0003, para o balanço agregar por
 * viagem. Aditiva e não-destrutiva (padrão da 2→3: `CREATE TABLE IF NOT EXISTS`). PK composta
 * `(viagemId, chave)`; `valor` REAL (Double na fronteira, ADR-0013 §6). Espelho do mapa no Firestore.
 */
val MIGRATION_13_14 = object : Migration(13, 14) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `TarifaViagem` " +
                "(`viagemId` TEXT NOT NULL, `chave` TEXT NOT NULL, `valor` REAL NOT NULL, " +
                "PRIMARY KEY(`viagemId`, `chave`))"
        )
    }
}

/**
 * v14 → v15: `Passagem.tarifaBase` — a tarifa da inteira congelada na emissão (ADR-0013), fonte da tarifa
 * devida e do desconto derivado. Aditiva e não-destrutiva; coluna anulável (null cobre bilhetes anteriores
 * e o veículo, cuja tarifa por classe é Fase 3). Sem backfill (portfólio; regenera via seed).
 */
val MIGRATION_14_15 = object : Migration(14, 15) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE Passagem ADD COLUMN tarifaBase REAL")
    }
}

/**
 * v15 → v16: `Passagem.cilindrada` — o cc da moto que justificou a tarifaBase (ADR-0013), registro do
 * bilhete e prefill na edição. Aditiva e não-destrutiva; coluna anulável (null cobre não-moto/anteriores).
 */
val MIGRATION_15_16 = object : Migration(15, 16) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE Passagem ADD COLUMN cilindrada TEXT")
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
            MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12,
            MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16,
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
    fun provideTarifaViagemDao(db: FluviAppDatabase): TarifaViagemDao {
        return db.tarifaViagemDao()
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