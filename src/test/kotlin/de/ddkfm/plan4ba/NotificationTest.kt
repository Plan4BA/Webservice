import com.mashape.unirest.http.Unirest
import de.ddkfm.plan4ba.models.Lecture
import de.ddkfm.plan4ba.models.Notification
import de.ddkfm.plan4ba.models.OK
import de.ddkfm.plan4ba.models.SimpleNotification
import de.ddkfm.plan4ba.utils.DBService
import de.ddkfm.plan4ba.utils.toListModel
import de.ddkfm.plan4ba.utils.toModel
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.lang.IllegalArgumentException
import kotlin.test.assertNotNull

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NotificationTest {
    val endpoint = Webservice.endpoint
    init {
        Webservice.start()
    }
    @Test
    fun getAllNotifications() {
        val token = Webservice.getShortToken()
        val notifications = Unirest.get("$endpoint/notifications")
            .bearer(token.token)
            .asString()
            .getOrThrow()
            .toListModel<SimpleNotification>()
        println(notifications)
        assertNotNull(notifications)
        if(notifications.isNullOrEmpty()) {
            val n = Notification(0, randomString(10), randomString(20), randomString(5), token.userId)
            DBService.create(n)
            val notifications1 = Unirest.get("$endpoint/notifications")
                .bearer(token.token)
                .asString()
                .getOrThrow()
                .toListModel<SimpleNotification>()
            println(notifications1)
            assertNotNull(notifications1)
        }
    }

    @Test
    fun deleteNotification() {
        val token = Webservice.getShortToken()
        var notifications = Unirest.get("$endpoint/notifications")
            .bearer(token.token)
            .asString()
            .getOrThrow()
            .toListModel<SimpleNotification>()
        println(notifications)
        if(notifications.isNullOrEmpty()) {
            val n = Notification(0, randomString(10), randomString(20), randomString(5), token.userId)
            DBService.create(n)
            notifications = Unirest.get("$endpoint/notifications")
                .bearer(token.token)
                .asString()
                .getOrThrow()
                .toListModel()
        }
        if(notifications == null || notifications.isEmpty())
            throw IllegalArgumentException()
        println(notifications)
        val randomNotification = notifications.random()
        val deleteResponse = Unirest.delete("$endpoint/notifications/${randomNotification.id}")
            .bearer(token.token)
            .asString()
            .getOrThrow()
            .toModel<OK>()
        assertNotNull(deleteResponse)
        val newNotifications = Unirest.get("$endpoint/notifications")
            .bearer(token.token)
            .asString()
            .getOrThrow()
            .toListModel<SimpleNotification>() ?: throw IllegalArgumentException()
        assert(newNotifications.size < notifications.size)
    }

}