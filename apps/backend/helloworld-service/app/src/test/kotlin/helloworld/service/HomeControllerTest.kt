package helloworld.service;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import io.micronaut.function.aws.proxy.payload1.ApiGatewayProxyRequestEventFunction
import io.micronaut.function.aws.proxy.MockLambdaContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class HomeControllerTest {

    @Test
    fun testHandler() {
        val handler = ApiGatewayProxyRequestEventFunction()
        val request = APIGatewayProxyRequestEvent()
        request.httpMethod = "GET"
        request.path = "/"
        val response = handler.handleRequest(request, MockLambdaContext())

        assertEquals(200, response.statusCode)
        assertEquals("{\"message\":\"Hello World\"}", response.body)
        handler.applicationContext.close()
    }
}

