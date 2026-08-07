package dev.matheus.fluviapp.di.module

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.google.gson.Gson
import dev.matheus.fluviapp.database.FluviAppDatabase
import dev.matheus.fluviapp.database.dao.ContadorDao
import dev.matheus.fluviapp.database.dao.cadastro.ConstanteDao
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
 * **Migração única — o schema nasce pronto (ADR-0015 §9).**
 *
 * O app não tem versão publicada, homologação nem dado real (o conteúdo é cadastrado pelo painel), então
 * manter 19 migrações históricas era guardar o caminho para um estado por onde ninguém passou. Elas foram
 * **colapsadas** neste DDL, que cria as tabelas já na forma final.
 *
 * O SQL não é transcrito à mão: é o `createSql` que o Room exporta em
 * `app/schemas/dev.matheus.fluviapp.database.FluviAppDatabase/2.json` (ligado por
 * `room.schemaLocation` no `build.gradle.kts`). Mudou uma entidade? Rode `:app:kspDebugKotlin` e
 * traga o `createSql` novo para cá — enquanto não houver homologação, o schema é **reescrito**, não
 * migrado. A partir da primeira versão publicada, migrações aditivas voltam a nascer daqui (3, 4, …).
 *
 * Instalação nova não executa nada disto (o Room cria as tabelas pelas entidades); a migração existe
 * para o caminho v1 → v2 e como registro executável do schema.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        DDL_V2.forEach(db::execSQL)
    }
}

/**
 * **v4 → v5: a tabela do Funcionário é derrubada** (F6.2) — e esta é a **primeira migração escrita
 * depois da primeira versão publicada**, que é exatamente o gatilho que o §9 previa.
 *
 * Até a v0.0.4 valia recriar: sem base instalada, não havia dado local a perder. Agora há aparelho com o
 * app em produção, e o que mora neste banco não é só cache do Firestore — são o **rascunho de passagem** e
 * o índice do **bilhete digital**, que existem só aqui. Cair no `fallbackToDestructiveMigration` levaria os
 * dois junto com uma tabela que ninguém mais lê.
 *
 * Um `DROP TABLE` de uma tabela que virou espelho de nada é a migração mais barata que existe, e ela paga
 * a diferença entre "o cadastro saiu do Room" e "o rascunho de alguém sumiu na atualização".
 */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DROP INDEX IF EXISTS `index_Funcionario_id`")
        db.execSQL("DROP TABLE IF EXISTS `Funcionario`")
    }
}

/** DDL da v2 — cópia fiel do `createSql` exportado pelo Room (uma linha por tabela/índice). */
private val DDL_V2 = listOf(
    "CREATE TABLE IF NOT EXISTS `Usuario` (`id` TEXT NOT NULL, `email` TEXT NOT NULL, `username` TEXT NOT NULL, `papel` TEXT NOT NULL, `funcionarioId` TEXT NOT NULL, `ultimoUsuarioLogado` INTEGER NOT NULL, PRIMARY KEY(`id`))",
    "CREATE INDEX IF NOT EXISTS `index_Usuario_id` ON `Usuario` (`id`)",
    "CREATE TABLE IF NOT EXISTS `Constante` (`id` TEXT NOT NULL, `descricaoNome` TEXT NOT NULL, `categoria` TEXT NOT NULL, PRIMARY KEY(`id`))",
    "CREATE INDEX IF NOT EXISTS `index_Constante_id` ON `Constante` (`id`)",
    "CREATE TABLE IF NOT EXISTS `Empresa` (`id` TEXT NOT NULL, `nome` TEXT NOT NULL, `razaoSocial` TEXT NOT NULL, `cnpj` TEXT NOT NULL, `endereco` TEXT NOT NULL, `telefone1` TEXT NOT NULL, `telefone2` TEXT NOT NULL, PRIMARY KEY(`id`))",
    "CREATE INDEX IF NOT EXISTS `index_Empresa_id` ON `Empresa` (`id`)",
    "CREATE TABLE IF NOT EXISTS `Navio` (`id` TEXT NOT NULL, `descricaoNome` TEXT NOT NULL, `capacidadeVeiculo` INTEGER NOT NULL, `capacidadeSuite2` INTEGER NOT NULL, `capacidadeSuite3` INTEGER NOT NULL, `capacidadeCamarote` INTEGER NOT NULL, `empresaId` TEXT NOT NULL, PRIMARY KEY(`id`))",
    "CREATE INDEX IF NOT EXISTS `index_Navio_id` ON `Navio` (`id`)",
    "CREATE TABLE IF NOT EXISTS `Funcionario` (`id` TEXT NOT NULL, `descricaoNome` TEXT NOT NULL, `agencia` TEXT NOT NULL, `lotacao` TEXT NOT NULL, `cargo` TEXT NOT NULL, `email` TEXT NOT NULL, PRIMARY KEY(`id`))",
    "CREATE INDEX IF NOT EXISTS `index_Funcionario_id` ON `Funcionario` (`id`)",
    "CREATE TABLE IF NOT EXISTS `Viagem` (`id` TEXT NOT NULL, `codigo` TEXT NOT NULL, `origem` TEXT NOT NULL, `destino` TEXT NOT NULL, `empresaId` TEXT NOT NULL, `navioId` TEXT NOT NULL, PRIMARY KEY(`id`))",
    "CREATE INDEX IF NOT EXISTS `index_Viagem_id` ON `Viagem` (`id`)",
    "CREATE TABLE IF NOT EXISTS `TarifaViagem` (`viagemId` TEXT NOT NULL, `chave` TEXT NOT NULL, `valor` REAL NOT NULL, PRIMARY KEY(`viagemId`, `chave`))",
    "CREATE TABLE IF NOT EXISTS `Passagem` (`id` TEXT NOT NULL, `numero` TEXT NOT NULL, `viagemId` TEXT NOT NULL, `navioId` TEXT NOT NULL, `empresaId` TEXT NOT NULL, `codigoViagem` TEXT NOT NULL, `empresa` TEXT NOT NULL, `navio` TEXT NOT NULL, `origem` TEXT NOT NULL, `destino` TEXT NOT NULL, `dataViagem` TEXT NOT NULL, `horaViagem` TEXT NOT NULL, `agencia` TEXT NOT NULL, `valorPix` REAL, `valorDinheiro` REAL, `valorDebito` REAL, `valorCredito` REAL, `tarifaBase` REAL, `observacao` TEXT, `tipoPassagem` TEXT, `gratuidade` TEXT, `acomodacao` TEXT, `nomePassageiro1` TEXT, `documentoPassageiro1` TEXT, `numeroDocumentoPassageiro1` TEXT, `dataNascimentoPassageiro1` TEXT, `nomePassageiro2` TEXT, `documentoPassageiro2` TEXT, `numeroDocumentoPassageiro2` TEXT, `dataNascimentoPassageiro2` TEXT, `nomePassageiro3` TEXT, `tipoDocumentoPassageiro3` TEXT, `numeroDocumentoPassageiro3` TEXT, `dataNascimentoPassageiro3` TEXT, `nomeResponsavelRetirada` TEXT, `documentoResponsavelRetirada` TEXT, `numeroDocumentoResponsavelRetirada` TEXT, `tipoVeiculo` TEXT, `modeloVeiculo` TEXT, `placaVeiculo` TEXT, `corVeiculo` TEXT, `cilindrada` TEXT, `funcionarioResponsavel` TEXT NOT NULL, `funcionarioId` TEXT NOT NULL, `status` TEXT NOT NULL, `embarcadaPorId` TEXT NOT NULL, `embarcadaPor` TEXT NOT NULL, `embarcadaEm` TEXT NOT NULL, PRIMARY KEY(`id`))",
    "CREATE INDEX IF NOT EXISTS `index_Passagem_id` ON `Passagem` (`id`)",
    "CREATE TABLE IF NOT EXISTS `ContadorBilhete` (`id` INTEGER NOT NULL, `contagem` INTEGER NOT NULL, PRIMARY KEY(`id`))",
    "CREATE INDEX IF NOT EXISTS `index_ContadorBilhete_id` ON `ContadorBilhete` (`id`)",
    "CREATE TABLE IF NOT EXISTS `PassagemDigital` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `idPassagem` TEXT NOT NULL, `caminho` TEXT NOT NULL)",
    "CREATE INDEX IF NOT EXISTS `index_PassagemDigital_id` ON `PassagemDigital` (`id`)",
    "CREATE TABLE IF NOT EXISTS `rascunho_passagem` (`id` INTEGER NOT NULL, `json` TEXT NOT NULL, PRIMARY KEY(`id`))",
)

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
            MIGRATION_1_2,
            MIGRATION_4_5,
        )
            // Sem distribuição, não há base instalada cujo dado se possa perder: a v3 recria em vez de
            // migrar (decisão do analista). Este argumento VENCE na P3.5 — a partir da primeira entrega
            // a testers, sair de uma versão publicada exige migração escrita.
            .fallbackToDestructiveMigration()
            // O banco ANDOU até a v20 nas máquinas de desenvolvimento e agora volta para a v2 — abrir um
            // arquivo mais novo que o schema é *downgrade*, e o Room lança por padrão. Sem dado real a
            // proteger, recriar é a resposta certa (e a única disponível: não existe caminho 20 → 2).
            .fallbackToDestructiveMigrationOnDowngrade()
            .build()
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
    fun provideViagemDao(db: FluviAppDatabase): ViagemDao {
        return db.viagemDao()
    }

    @Provides
    fun provideTarifaViagemDao(db: FluviAppDatabase): TarifaViagemDao {
        return db.tarifaViagemDao()
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