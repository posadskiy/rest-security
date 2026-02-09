package example.controller;

import com.posadskiy.restsecurity.context.SecurityContextHolder;
import com.posadskiy.restsecurity.rest.SecuredRequestContext;
import example.dto.OrderDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api/orders")
public class OrderController implements OrderApi {

    @Override
    @GetMapping
    public List<OrderDto> getMyOrders(SecuredRequestContext request) {
        String userId = SecurityContextHolder.getContext().userId();
        return listOrdersForUser(userId);
    }

    @Override
    @GetMapping("/{userId}")
    public List<OrderDto> getOrdersForUser(SecuredRequestContext request, @PathVariable String userId) {
        // Same-user enforced by BPP: SecuredRequest(sessionId, userId) so non-admin can only access own userId
        return listOrdersForUser(userId);
    }

    private static List<OrderDto> listOrdersForUser(String userId) {
        return Stream.of(
                new OrderDto("ord-1", userId, "Order one"),
                new OrderDto("ord-2", userId, "Order two")
        ).toList();
    }
}
