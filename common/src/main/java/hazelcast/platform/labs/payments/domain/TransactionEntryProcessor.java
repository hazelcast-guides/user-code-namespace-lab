package hazelcast.platform.labs.payments.domain;

import com.hazelcast.map.EntryProcessor;

import java.util.Map;

/*
 * This EntryProcessor is used to process an authorization request.  The code will
 * be executed on the node where the relevant Card entry lives.  Note that processing
 * the authorization can result in updates to the Card entry.
 *
 * EntryProcessors are a Hazelcast mechanism for performing read/update operations safely,
 * without the need for holding a lock or other concurrency management techniques.
 * See https://docs.hazelcast.org/docs/5.3.5/javadoc/index.html?com/hazelcast/map/EntryProcessor.html
 * for more.
 */
public class TransactionEntryProcessor implements EntryProcessor<String, Card, String> {
    public TransactionEntryProcessor(Transaction t) {
        this.transaction = t;
    }
    private final Transaction transaction;
    @Override
    public String process(Map.Entry<String, Card> entry) {
        Card card = entry.getValue();
        if (card == null)
            return Transaction.Status.INVALID_CARD.name();

        if (transaction.getAmount() > 5000)
            return Transaction.Status.DECLINED_BIG_TXN.name();

        if (card.getLocked())
                return Transaction.Status.DECLINED_LOCKED.name();

        if (card.getAuthorizedDollars() + transaction.getAmount() <= card.getCreditLimitDollars()){
            card.addAuthorizedDollars(transaction.getAmount());
            entry.setValue(card);
            return Transaction.Status.APPROVED.name();
        } else {
            return Transaction.Status.DECLINED_OVER_AUTH_LIMIT.name();
        }
    }
}
