package my.noveldokusha

import my.noveldokusha.scraper.scrubber
import org.junit.Test

import org.junit.Assert.*

/**
 * Local unit test, which will execute on the development machine (host).
 * See [testing documentation](http://d.android.com/tools/testing).
 *
 */
class Scraper
{

    // DATABASES TEST

    @Test
    fun databaseList_IsCompatible()
    {
        for(database in scrubber.databasesList)
            assertNotNull(scrubber.getCompatibleDatabase(database.baseUrl))
    }

    @Test
    fun databaseList_BaseUrlEndsWithSlash()
    {
        for(database in scrubber.databasesList)
            assertTrue(
                "${database::class.simpleName} baseUrl missing ending slash",
                database.baseUrl.endsWith("/")
            )
    }

    @Test
    fun databaseList_CheckUniqueId()
    {
        val groups = scrubber.databasesList.groupBy { it.id }
        for(list in groups)
            assertEquals(
                "${ list.value.joinToString { it::class.simpleName.toString() }}: id can't be the same value for multiple databases",
                1,
                list.value.size
            )
    }

    @Test
    fun databaseList_CheckUniqueName()
    {
        val groups = scrubber.databasesList.groupBy { it.name }
        for(list in groups)
            assertEquals(
                "${ list.value.joinToString { it::class.simpleName.toString() }}: name can't be the same value for multiple databases",
                1,
                list.value.size
            )
    }

    // SOURCES TEST

    @Test
    fun sourceList_IsCompatible()
    {
        for(source in scrubber.sourcesList)
            assertNotNull(scrubber.getCompatibleSource(source.baseUrl))
    }

    @Test
    fun sourceList_BaseUrlEndsWithSlash()
    {
        for(source in scrubber.sourcesList)
            assertTrue(
                "${source::class.simpleName} baseUrl missing ending slash",
                source.baseUrl.endsWith("/")
            )
    }

    @Test
    fun sourceList_CheckUniqueName()
    {
        val groups = scrubber.sourcesList.groupBy { it.name }
        for(list in groups)
            assertEquals(
                "${ list.value.joinToString { it::class.simpleName.toString() }}: name can't be the same value for multiple sources",
                1,
                list.value.size
            )
    }

}