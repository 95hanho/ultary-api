package me._hanho.ultary.domain.auth;

import static org.assertj.core.api.Assertions.*;
import java.util.Map;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import me._hanho.ultary.domain.auth.dto.request.*;
import me._hanho.ultary.domain.auth.support.PhoneAuthCodeStore;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT, properties="ai.moderation.worker-enabled=false")
class PhoneDemoIntegrationTest {
    @Autowired AuthService auth;
    @Autowired ObjectMapper json;
    @Autowired PhoneAuthCodeStore codes;
    Object previous;
    String phone;
    @BeforeEach void setup() {
        previous=ReflectionTestUtils.getField(auth,"phoneDemoMode");
        phone="010"+String.format("%08d",new java.security.SecureRandom().nextInt(100000000));
    }
    @AfterEach void cleanup(){ReflectionTestUtils.setField(auth,"phoneDemoMode",previous);codes.remove(phone);}
    @Test void demoShowsARealCodeAndStillRejectsWrongCodes() throws Exception {
        ReflectionTestUtils.setField(auth,"phoneDemoMode",true);
        var request=new PhoneAuthRequest();request.setPhone(phone);request.setPurpose("PASSWORD");
        var response=auth.requestPhoneAuth(null,request);
        assertThat(response.getDemoCode()).matches("[0-9]{6}");
        String wrong=response.getDemoCode().equals("000000")?"999999":"000000";
        var invalid=json.convertValue(Map.of("phoneAuthToken",response.getPhoneAuthToken(),"code",wrong),PhoneVerifyRequest.class);
        assertThatThrownBy(()->auth.verifyPhoneAuth(invalid)).isInstanceOf(me._hanho.ultary.common.exception.BusinessException.class);
        var valid=json.convertValue(Map.of("phoneAuthToken",response.getPhoneAuthToken(),"code",response.getDemoCode()),PhoneVerifyRequest.class);
        assertThat(auth.verifyPhoneAuth(valid).getPhoneAuthCompleteToken()).isNotBlank();
    }
    @Test void normalModeNeverReturnsTheCode() throws Exception {
        ReflectionTestUtils.setField(auth,"phoneDemoMode",false);
        var request=new PhoneAuthRequest();request.setPhone(phone);request.setPurpose("PASSWORD");
        var response=auth.requestPhoneAuth(null,request);
        assertThat(response.getDemoCode()).isNull();
        assertThat(json.writeValueAsString(response)).doesNotContain("demoCode");
    }
}
