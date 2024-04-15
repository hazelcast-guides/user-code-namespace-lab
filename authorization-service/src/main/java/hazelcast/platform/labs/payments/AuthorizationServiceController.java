package hazelcast.platform.labs.payments;

import hazelcast.platform.labs.payments.domain.Transaction;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthorizationServiceController {

    @PostMapping("/authorize")
    public String authorize(@RequestBody Transaction transaction){
        return Transaction.Status.APPROVED.name();
    }
}
