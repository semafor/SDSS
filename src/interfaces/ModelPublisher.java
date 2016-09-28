package interfaces;

/**
 * Interface that normalizes event flow between a model publisher and a model
 * subscriber.
 * 
 *
 */
public interface ModelPublisher
{
    public void addSubscriber(ModelSubscriber modelSubscriber);
}
