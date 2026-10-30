package cn.wmhwiki.wangojbackend.exception;

import cn.wmhwiki.wangojbackend.dto.UserDTO;
import cn.wmhwiki.wangojbackend.common.BaseResponse;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 全局异常处理单测：重点覆盖 DTO 校验分支不再死代码。
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void validationReturnsFieldMessage() throws Exception {
        UserDTO dto = new UserDTO();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(dto, "userDTO");
        bindingResult.rejectValue("userAccount", "NotBlank", "用户名不能为空");
        bindingResult.rejectValue("userPassword", "NotBlank", "密码不能为空");
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(
                userDtoParameter(), bindingResult);

        ResponseEntity<BaseResponse<?>> response = handler.handleValidation(exception);

        assertEquals(400, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().getCode());
        assertTrue(response.getBody().getMsg().contains("用户名不能为空"));
        assertTrue(response.getBody().getMsg().contains("密码不能为空"));
    }

    @Test
    void badRequestStillUsesGenericMessage() {
        MissingServletRequestParameterException exception =
                new MissingServletRequestParameterException("current", "Integer");

        ResponseEntity<BaseResponse<?>> response = handler.handleBadRequest(exception);

        assertEquals(400, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertEquals("请求参数不合法", response.getBody().getMsg());
    }

    private static MethodParameter userDtoParameter() throws NoSuchMethodException {
        Method method = GlobalExceptionHandlerTest.class
                .getDeclaredMethod("dummyEndpoint", UserDTO.class);
        return new MethodParameter(method, 0);
    }

    @SuppressWarnings("unused")
    private void dummyEndpoint(UserDTO userDTO) {
        // 仅用于构造 MethodParameter
    }
}
