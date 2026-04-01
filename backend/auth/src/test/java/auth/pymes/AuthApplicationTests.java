package auth.pymes;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class AuthApplicationTests {

    @MockBean
    private RedisConnectionFactory redisConnectionFactory;

	@Test
	void contextLoads() {
	}

}
