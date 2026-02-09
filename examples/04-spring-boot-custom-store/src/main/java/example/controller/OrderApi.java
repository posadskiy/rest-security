package example.controller;

import com.posadskiy.restsecurity.annotation.Security;
import com.posadskiy.restsecurity.rest.SecuredRequestContext;
import example.dto.OrderDto;

import java.util.List;

public interface OrderApi {

    @Security(roles = {"USER"})
    List<OrderDto> getMyOrders(SecuredRequestContext request);

    /** Same-user enforcement: non-admin can only see orders for themselves. */
    @Security(roles = {"USER"})
    List<OrderDto> getOrdersForUser(SecuredRequestContext request, String userId);
}
