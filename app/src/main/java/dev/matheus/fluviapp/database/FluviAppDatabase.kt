package dev.matheus.fluviapp.database

import androidx.room.Database
import androidx.room.RoomDatabase
import dev.matheus.fluviapp.database.dao.cadastro.ConstanteDao
import dev.matheus.fluviapp.database.dao.operacoes.UsuarioDao
import dev.matheus.fluviapp.database.dao.passagem.RascunhoPassagemDao
import dev.matheus.fluviapp.domain.cadastro.constantes.Constante
import dev.matheus.fluviapp.domain.operacoes.Usuario

@Database(
    entities = [
        Usuario::class,
        Constante::class,
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
    // v8: a **Passagem** sai do Room (F9.2, ADR-0017 F5) e leva junto o contador de bilhete e o índice do
    // bilhete digital. As três somem pela mesma decisão em três formas: a passagem porque o fato
    // compartilhado vive no Firestore; o contador porque passou a ser **por ocorrência**, em subcoleção da
    // viagem com incremento atômico (ADR-0024 D6); e o bilhete digital porque o arquivo vai para a galeria,
    // com nome derivado do id — um índice local para achar o que o sistema de arquivos já indexa não tem
    // substituto porque não tinha função.
    version = 8,
    exportSchema = true
)
abstract class FluviAppDatabase : RoomDatabase() {
    abstract fun usuarioDao(): UsuarioDao
    abstract fun constanteDao(): ConstanteDao

    /**
     * O rascunho — **o habitante que fica** ([ADR-0027] D5, revisão do ADR-0025 D7).
     *
     * Ele deixou de ser resíduo: um snapshot passa a ser uma **passagem incompleta**, com vários por agente e
     * uma tela de recuperação, *"com garantia do Room"*. Por isso o Firestore-only não o alcança — ele vale
     * para o **fato compartilhado**, e o atendimento em curso é local por natureza; é por ser local que ele
     * sobrevive a app fechado e rede ausente.
     */
    abstract fun rascunhoPassagemDao(): RascunhoPassagemDao
}