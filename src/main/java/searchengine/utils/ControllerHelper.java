package searchengine.utils;

import org.json.simple.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import java.util.Map;
import static java.util.Collections.singletonMap;

public class ControllerHelper {
    public ResponseEntity<String> resultError(String message) {
        return new ResponseEntity<>(generateResponse(singletonMap("result", false),
                singletonMap("error", message)), HttpStatus.OK);
    }

    public ResponseEntity<String> resultOK() {
        return new ResponseEntity<>(generateResponse(singletonMap("result", true)), HttpStatus.OK);
    }

    @SafeVarargs
    public final String generateResponse(Map<Object, Object>... params) {
        JSONObject JSONObject = new JSONObject();
        for (Map<Object, Object> param : params) {
            JSONObject.putAll(param);
        }
        return JSONObject.toString();
    }
}
