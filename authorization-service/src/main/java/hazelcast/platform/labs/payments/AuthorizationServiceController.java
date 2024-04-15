package hazelcast.platform.labs.payments;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import hazelcast.platform.labs.payments.domain.Card;
import hazelcast.platform.labs.payments.domain.Names;
import hazelcast.platform.labs.payments.domain.Transaction;
import hazelcast.platform.labs.payments.domain.TransactionEntryProcessor;
import jakarta.annotation.PostConstruct;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthorizationServiceController {

    @PostConstruct
    public void init(){
        // use the default instantiation process
        hz = HazelcastClient.newHazelcastClient();
        cardMap = hz.getMap(Names.CARD_MAP_NAME);
    }
    private HazelcastInstance hz;
    private IMap<String, Card> cardMap;

    @PostMapping("/authorize")
    public String authorize(@RequestBody Transaction transaction){
        TransactionEntryProcessor tep = new TransactionEntryProcessor(transaction);
        return cardMap.executeOnKey(transaction.getCardNumber(), tep);
    }
}
