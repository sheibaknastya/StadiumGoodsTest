import static org.junit.jupiter.api.Assertions.assertEquals;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.StringReader;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue;

class ApiTest {

    private static HttpClient httpClient;

    @BeforeAll
    static void setup() throws Exception {
        httpClient = new HttpClient();
        httpClient.start();
    }

    @ParameterizedTest
    @CsvSource({
        "pikachu,static,https://pokeapi.co/api/v2/ability/31/,true,3",
        "pikachu,lightning-rod,https://pokeapi.co/api/v2/ability/9/,false,1",
        "snorlax,gluttony,https://pokeapi.co/api/v2/ability/82/,true,1",
        "snorlax,thick-fat,https://pokeapi.co/api/v2/ability/47/,false,2",
        "snorlax,immunity,https://pokeapi.co/api/v2/ability/17/,false,3",
        "charizard,solar-power,https://pokeapi.co/api/v2/ability/94/,true,3",
        "charizard,blaze,https://pokeapi.co/api/v2/ability/66/,false,1"})
    void check(String name, String ability, String url, String isHidden,
               String slot) throws InterruptedException, ExecutionException, TimeoutException {

        ContentResponse response = httpClient.newRequest("https://pokeapi.co/api/v2/pokemon/".concat(name)).send();

        JsonReader jsonReader = Json.createReader(new StringReader(response.getContentAsString()));
        JsonObject json = jsonReader.readObject();
        jsonReader.close();

        for (JsonValue object : json.getJsonArray("abilities")) {
            if (object.asJsonObject().getJsonObject("ability").get("name").toString().equals('"' + ability + '"')) {
                assertEquals('"' + url + '"', object.asJsonObject().getJsonObject("ability").get("url").toString(),
                    "URL value mismatch.");
                assertEquals(isHidden, object.asJsonObject().get("is_hidden").toString(), "Hidden value mismatch.");
                assertEquals(slot, object.asJsonObject().get("slot").toString(), "Slot value mismatch.");
            }
        }
    }

    @AfterAll
    static void cleanup() throws Exception {
        if (httpClient != null) {
            httpClient.stop();
        }
    }
}
