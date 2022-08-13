package my.noveldokusha

import my.noveldokusha.network.NetworkClient
import my.noveldokusha.scraper.Scraper
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class ScraperTest {

    // DATABASES TEST

    @Mock
    lateinit var networkClient: NetworkClient

    private lateinit var scraper: Scraper

    @Before
    fun setup() {
        scraper = Scraper(networkClient = networkClient)
    }

    @Test
    fun `databaseList items are compatible`() {
        for (database in scraper.databasesList)
            assertNotNull(scraper.getCompatibleDatabase(database.baseUrl))
    }

    @Test
    fun `databaseList items baseUrl ends with slash`() {
        for (database in scraper.databasesList)
            assertTrue(
                "${database::class.simpleName} baseUrl missing ending slash",
                database.baseUrl.endsWith("/")
            )
    }

    @Test
    fun `databaseList items have unique id`() {
        val groups = scraper.databasesList.groupBy { it.id }
        for (list in groups)
            assertEquals(
                "${list.value.joinToString { it::class.simpleName.toString() }}: id can't be the same value for multiple databases",
                1,
                list.value.size
            )
    }

    @Test
    fun `databaseList items have unique name`() {
        val groups = scraper.databasesList.groupBy { it.name }
        for (list in groups)
            assertEquals(
                "${list.value.joinToString { it::class.simpleName.toString() }}: name can't be the same value for multiple databases",
                1,
                list.value.size
            )
    }

    // SOURCES TEST

    @Test
    fun `sourceList items are compatible`() {
        for (source in scraper.sourcesList)
            assertNotNull(scraper.getCompatibleSource(source.baseUrl))
    }

    @Test
    fun `sourceList items baseUrl ends with slash`() {
        for (source in scraper.sourcesList)
            assertTrue(
                "${source::class.simpleName} baseUrl missing ending slash",
                source.baseUrl.endsWith("/")
            )
    }

    @Test
    fun `sourceList items have unique id`() {
        val groups = scraper.sourcesList.groupBy { it.id }
        for (list in groups)
            assertEquals(
                "${list.value.joinToString { it::class.simpleName.toString() }}: name can't be the same value for multiple sources",
                1,
                list.value.size
            )
    }

    @Test
    fun `sourceList items have unique name`() {
        val groups = scraper.sourcesList.groupBy { it.name }
        for (list in groups)
            assertEquals(
                "${list.value.joinToString { it::class.simpleName.toString() }}: name can't be the same value for multiple sources",
                1,
                list.value.size
            )
    }

}