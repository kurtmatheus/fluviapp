package dev.matheus.fluviapp

import dev.matheus.fluviapp.revitalizacao.ForaDoEscopo
import org.junit.experimental.categories.Category
import org.junit.Test

import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@Category(ForaDoEscopo::class)
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }
}
