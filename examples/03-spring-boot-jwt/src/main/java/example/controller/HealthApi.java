package example.controller;

import com.posadskiy.restsecurity.annotation.Public;
import com.posadskiy.restsecurity.annotation.Security;

import java.util.Map;

@Security(roles = {"USER"})
public interface HealthApi {

    @Public
    Map<String, String> health();
}
